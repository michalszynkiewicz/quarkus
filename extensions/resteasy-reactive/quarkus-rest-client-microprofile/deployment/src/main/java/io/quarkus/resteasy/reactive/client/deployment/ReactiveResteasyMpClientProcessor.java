package io.quarkus.resteasy.reactive.client.deployment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.SessionScoped;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanRegistrarBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.ScopeInfo;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.rest.rest.client.microprofile.RestClientCDIDelegateBuilder;
import io.quarkus.rest.rest.client.microprofile.recorder.RestClientRecorder;

class ReactiveResteasyMpClientProcessor {

    private static final Logger log = Logger.getLogger(ReactiveResteasyMpClientProcessor.class);

    private static final DotName REST_CLIENT = DotName.createSimple(RestClient.class.getName());
    private static final DotName REGISTER_REST_CLIENT = DotName.createSimple(RegisterRestClient.class.getName());
    private static final DotName SESSION_SCOPED = DotName.createSimple(SessionScoped.class.getName());

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setup(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            RestClientRecorder restClientRecorder) {
        restClientRecorder.setRestClientBuilderResolver();
        additionalBeans.produce(new AdditionalBeanBuildItem(RestClient.class));
    }

    @BuildStep
    void addMpClientEnricher(BuildProducer<JaxrsClientEnricherBuildItem> enrichers) {
        enrichers.produce(new JaxrsClientEnricherBuildItem(new MicroProfileRestClientEnricher()));
    }

    // mstodo inject rest client class names from
    @BuildStep
    void addRestClientBeans(Capabilities capabilities,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<BeanRegistrarBuildItem> beanRegistrars) {

        CompositeIndex index = CompositeIndex.create(beanArchiveIndexBuildItem.getIndex(), combinedIndexBuildItem.getIndex());
        Set<AnnotationInstance> registerRestClientAnnos = new HashSet<>(index.getAnnotations(REGISTER_REST_CLIENT));

        // mstodo try to replace with synthetic bean
        beanRegistrars.produce(new BeanRegistrarBuildItem(new BeanRegistrar() {

            @Override
            public void register(RegistrationContext registrationContext) {
                final Config config = ConfigProvider.getConfig();

                for (AnnotationInstance registerRCAnnotation : registerRestClientAnnos) {
                    ClassInfo restClientInterface = registerRCAnnotation.target().asClass();
                    DotName restClientName = restClientInterface.name();
                    BeanConfigurator<Object> configurator = registrationContext.configure(restClientName);
                    // The spec is not clear whether we should add superinterfaces too - let's keep aligned with SmallRye for now
                    configurator.addType(restClientName);
                    configurator.addQualifier(REST_CLIENT);

                    final String configPrefix = computeConfigPrefix(restClientName, registerRCAnnotation);
                    final ScopeInfo scope = computeDefaultScope(capabilities, config, restClientInterface, configPrefix);
                    configurator.scope(scope);
                    configurator.creator(m -> {
                        // return new RestClientBase(proxyType, baseUri).create();
                        ResultHandle interfaceHandle = m.loadClass(restClientName.toString());

                        AnnotationValue baseUri = registerRCAnnotation.value("baseUri");

                        ResultHandle baseUriHandle = m.load(baseUri != null ? baseUri.asString() : "");
                        ResultHandle configPrefixHandle = m.load(configPrefix);
                        ResultHandle baseHandle = m.newInstance(
                                MethodDescriptor.ofConstructor(RestClientCDIDelegateBuilder.class, Class.class, String.class, String.class),
                                interfaceHandle, baseUriHandle, configPrefixHandle);
                        ResultHandle ret = m.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(RestClientCDIDelegateBuilder.class, "create", Object.class), baseHandle);
                        m.returnValue(ret);
                    });
                    // mstodo sometimes we're getting duplicates here
                    configurator.destroyer(BeanDestroyer.CloseableDestroyer.class);
                    configurator.done();
                }
            }
        }));
    }

    private String computeConfigPrefix(DotName interfaceName, AnnotationInstance registerRestClientAnnotation) {
        AnnotationValue configKeyValue = registerRestClientAnnotation.value("configKey");
        return configKeyValue != null
                ? configKeyValue.asString()
                : interfaceName.toString();
    }

    private ScopeInfo computeDefaultScope(Capabilities capabilities, Config config,
            ClassInfo restClientInterface,
            String configPrefix) {
        ScopeInfo scopeToUse = null;
        final Optional<String> scopeConfig = config
                .getOptionalValue(String.format(RestClientCDIDelegateBuilder.REST_SCOPE_FORMAT, configPrefix), String.class);

        if (scopeConfig.isPresent()) {
            final DotName scope = DotName.createSimple(scopeConfig.get());
            final BuiltinScope builtinScope = BuiltinScope.from(scope);
            if (builtinScope != null) { // override default @Dependent scope with user defined one.
                scopeToUse = builtinScope.getInfo();
            } else if (capabilities.isPresent(Capability.SERVLET)) {
                if (scope.equals(SESSION_SCOPED)) {
                    scopeToUse = new ScopeInfo(SESSION_SCOPED, true);
                }
            }

            if (scopeToUse == null) {
                log.warn(String.format(
                        "Unsupported default scope %s provided for rest client %s. Defaulting to @Dependent.",
                        scope, restClientInterface.name()));
                scopeToUse = BuiltinScope.DEPENDENT.getInfo();
            }
        } else {
            final Set<DotName> annotations = restClientInterface.annotations().keySet();
            for (final DotName annotationName : annotations) {
                final BuiltinScope builtinScope = BuiltinScope.from(annotationName);
                if (builtinScope != null) {
                    scopeToUse = builtinScope.getInfo();
                    break;
                }
                if (annotationName.equals(SESSION_SCOPED)) {
                    scopeToUse = new ScopeInfo(SESSION_SCOPED, true);
                    break;
                }
            }
        }

        // Initialize a default @Dependent scope as per the spec
        return scopeToUse != null ? scopeToUse : BuiltinScope.DEPENDENT.getInfo();
    }
}
