package io.quarkus.security.deployment;

import static io.quarkus.security.deployment.SecurityTransformerUtils.DENY_ALL;

import java.lang.reflect.Method;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.SecurityBuildTimeConfig;
import io.quarkus.security.runtime.SecurityCheckStorage;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.security.runtime.interceptor.AuthenticatedInterceptor;
import io.quarkus.security.runtime.interceptor.DenyAllInterceptor;
import io.quarkus.security.runtime.interceptor.PermitAllInterceptor;
import io.quarkus.security.runtime.interceptor.RolesAllowedInterceptor;
import io.quarkus.security.runtime.interceptor.SecurityConstrainer;
import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.DeniedClassBuildItem;

public class SecurityProcessor {

    private static final Logger log = Logger.getLogger(SecurityProcessor.class);

    SecurityConfig security;

    /**
     * Register the Elytron-provided password factory SPI implementation
     *
     */
    @BuildStep
    void services(BuildProducer<JCAProviderBuildItem> jcaProviders) {
        // Create JCAProviderBuildItems for any configured provider names
        if (security.securityProviders != null) {
            for (String providerName : security.securityProviders) {
                jcaProviders.produce(new JCAProviderBuildItem(providerName));
                log.debugf("Added providerName: %s", providerName);
            }
        }
    }

    /**
     * Register the classes for reflection in the requested named providers
     *
     * @param classes - ReflectiveClassBuildItem producer
     * @param jcaProviders - JCAProviderBuildItem for requested providers
     */
    @BuildStep
    void registerJCAProviders(BuildProducer<ReflectiveClassBuildItem> classes, List<JCAProviderBuildItem> jcaProviders) {
        for (JCAProviderBuildItem provider : jcaProviders) {
            List<String> providerClasses = registerProvider(provider.getProviderName());
            for (String className : providerClasses) {
                classes.produce(new ReflectiveClassBuildItem(true, true, className));
                log.debugf("Register JCA class: %s", className);
            }
        }
    }

    @BuildStep
    void transformSecurityAnnotations(
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            BuildProducer<DeniedClassBuildItem> deniedClasses,
            ApplicationIndexBuildItem applicationIndex,
            SecurityBuildTimeConfig config) {
        if (config.denyUnannotated) {
            transformers.produce(new AnnotationsTransformerBuildItem(new DenyingUnannotatedTransformer()));

            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                if (DenyingUnannotatedTransformer.shouldDenyMethodsByDefault(classInfo)) {
                    deniedClasses.produce(new DeniedClassBuildItem(classInfo.name()));
                }
            }
        }
    }

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SecurityAnnotationsRegistrar()));
        Class[] interceptors = { AuthenticatedInterceptor.class, DenyAllInterceptor.class, PermitAllInterceptor.class,
                RolesAllowedInterceptor.class };
        beans.produce(new AdditionalBeanBuildItem(interceptors));
        beans.produce(new AdditionalBeanBuildItem(SecurityHandler.class, SecurityConstrainer.class));
    }

    @BuildStep
    void gatherSecuredMethods(ApplicationIndexBuildItem applicationIndex,
            List<DeniedClassBuildItem> deniedClassBuildItem,
            SecurityBuildTimeConfig config,
            BuildProducer<SecuredMethodBuildItem> securedMethods) {
        Set<DotName> securityAnnotations = SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet();
        Index index = applicationIndex.getIndex();
        Set<DotName> deniedClasses = deniedClassBuildItem.stream()
                .map(DeniedClassBuildItem::getName)
                .collect(Collectors.toSet());

        for (ClassInfo classInfo : index.getKnownClasses()) {
            AnnotationInstance classLevelAnnotation = getSingle(classInfo.classAnnotations(), securityAnnotations);
            boolean denyByDefault = false;
            if (classLevelAnnotation == null && deniedClasses.contains(classInfo.name())) {
                denyByDefault = true;
            }

            for (MethodInfo methodInfo : classInfo.methods()) {
                AnnotationInstance methodLevelAnnotation = getSingle(methodInfo.annotations(), securityAnnotations);

                methodLevelAnnotation = methodLevelAnnotation != null ? methodLevelAnnotation : classLevelAnnotation;

                if (methodLevelAnnotation != null) {
                    securedMethods.produce(new SecuredMethodBuildItem(methodInfo, methodLevelAnnotation));
                } else if (denyByDefault) {
                    securedMethods.produce(new SecuredMethodBuildItem(methodInfo, DENY_ALL, null));
                }
            }
        }
    }

    @BuildStep
    void createSecurityStorageBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            List<SecuredMethodBuildItem> securedMethods) {
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className("io.quarkus.security.runtime.SecurityCheckStorageImpl")
                .superClass(SecurityCheckStorage.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        try (MethodCreator ctor = classCreator.getMethodCreator("<init>", "V")) {
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(SecurityCheckStorage.class), ctor.getThis());
            for (SecuredMethodBuildItem securedMethodBuildItem : securedMethods) {
                registerSecuredMethod(ctor, securedMethodBuildItem);
            }

            ctor.returnValue(null);
        }

        classCreator.close();
    }

    private void registerSecuredMethod(MethodCreator ctor, SecuredMethodBuildItem securedMethod) {
        try {
            MethodInfo method = securedMethod.getMethodInfo();
            ResultHandle aClass = ctor.loadClass(method.declaringClass().name().toString());
            ResultHandle methodName = ctor.load(method.name());
            ResultHandle params = paramTypes(ctor, method.parameters());

            ResultHandle securityAnnotation = ctor.loadClass(securedMethod.annotationClass().toString());

            ResultHandle annotationParameters = annotationValues(ctor, securedMethod.value());

            Method registerAnnotation = SecurityCheckStorage.class.getDeclaredMethod("registerAnnotation", Class.class,
                    String.class, Class[].class, Class.class, String[].class);
            ctor.invokeVirtualMethod(MethodDescriptor.ofMethod(registerAnnotation), ctor.getThis(),
                    aClass, methodName, params, securityAnnotation, annotationParameters);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("registerAnnotation method not found on on SecurityCheckStorage", e);
        }
    }

    private ResultHandle annotationValues(MethodCreator methodCreator, String[] values) {
        if (values != null) {
            ResultHandle result = methodCreator.newArray(String.class, methodCreator.load(values.length));
            int i = 0;
            for (String val : values) {
                methodCreator.writeArrayValue(result, i, methodCreator.load(val));
            }
            return result;
        }
        return methodCreator.loadNull();
    }

    private ResultHandle paramTypes(MethodCreator ctor, List<Type> parameters) {
        ResultHandle result = ctor.newArray(Class.class, ctor.load(parameters.size()));

        for (int i = 0; i < parameters.size(); i++) {
            ctor.writeArrayValue(result, i, ctor.loadClass(parameters.get(i).toString()));
        }

        return result;
    }

    private Map<MethodInfo, AnnotationInstance> gatherSecurityAnnotations(Set<DotName> securityAnnotations,
            Set<ClassInfo> classesWithSecurity,
            AnnotationStore annotationStore) {
        Map<MethodInfo, AnnotationInstance> methodAnnotations = new HashMap<>();
        for (ClassInfo classInfo : classesWithSecurity) {
            Collection<AnnotationInstance> classAnnotations = annotationStore.getAnnotations(classInfo);
            AnnotationInstance classLevelAnnotation = getSingle(classAnnotations, securityAnnotations);

            for (MethodInfo method : classInfo.methods()) {
                AnnotationInstance methodAnnotation = getSingle(annotationStore.getAnnotations(method), securityAnnotations);
                methodAnnotation = methodAnnotation == null ? classLevelAnnotation : methodAnnotation;
                if (methodAnnotation != null) {
                    methodAnnotations.put(method, methodAnnotation);
                }
            }
        }
        return methodAnnotations;
    }

    private AnnotationInstance getSingle(Collection<AnnotationInstance> classAnnotations, Set<DotName> securityAnnotations) {
        AnnotationInstance result = null;
        for (AnnotationInstance annotation : classAnnotations) {
            if (securityAnnotations.contains(annotation.name())) {
                if (result != null) {
                    throw new IllegalStateException("Duplicate security annotations on class " + annotation.target());
                }
                result = annotation;
            }
        }

        return result;
    }

    /**
     * Determine the classes that make up the provider and its services
     *
     * @param providerName - JCA provider name
     * @return class names that make up the provider and its services
     */
    private List<String> registerProvider(String providerName) {
        ArrayList<String> providerClasses = new ArrayList<>();
        Provider provider = Security.getProvider(providerName);
        providerClasses.add(provider.getClass().getName());
        Set<Provider.Service> services = provider.getServices();
        for (Provider.Service service : services) {
            String serviceClass = service.getClassName();
            providerClasses.add(serviceClass);
            // Need to pull in the key classes
            String supportedKeyClasses = service.getAttribute("SupportedKeyClasses");
            if (supportedKeyClasses != null) {
                String[] keyClasses = supportedKeyClasses.split("\\|");
                providerClasses.addAll(Arrays.asList(keyClasses));
            }
        }
        return providerClasses;
    }

    @BuildStep(providesCapabilities = Capabilities.SECURITY)
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityAssociation.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IdentityProviderManagerCreator.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityProxy.class));
    }
}
