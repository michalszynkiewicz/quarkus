package org.jboss.resteasy.reactive.common.processor.scanning;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.DELETE;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.GET;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.HEAD;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.OPTIONS;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PATCH;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.POST;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.PUT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.common.processor.NameBindingUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;

public class ResteasyReactiveScanner {

    public static Map<DotName, String> BUILTIN_HTTP_ANNOTATIONS_TO_METHOD = new HashMap<>();

    static {
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(GET, "GET");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(POST, "POST");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(HEAD, "HEAD");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(PUT, "PUT");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(DELETE, "DELETE");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(PATCH, "PATCH");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD.put(OPTIONS, "OPTIONS");
        BUILTIN_HTTP_ANNOTATIONS_TO_METHOD = Collections.unmodifiableMap(BUILTIN_HTTP_ANNOTATIONS_TO_METHOD);
    }

    public static ApplicationScanningResult scanForApplicationClass(IndexView index) {
        Collection<ClassInfo> applications = index
                .getAllKnownSubclasses(ResteasyReactiveDotNames.APPLICATION);
        Set<String> allowedClasses = new HashSet<>();
        Set<String> singletonClasses = new HashSet<>();
        Set<String> globalNameBindings = new HashSet<>();
        boolean filterClasses = false;
        Application application = null;
        ClassInfo selectedAppClass = null;
        boolean blocking = false;
        for (ClassInfo applicationClassInfo : applications) {
            if (selectedAppClass != null) {
                throw new RuntimeException("More than one Application class: " + applications);
            }
            selectedAppClass = applicationClassInfo;
            // FIXME: yell if there's more than one
            String applicationClass = applicationClassInfo.name().toString();
            try {
                Class<?> appClass = Thread.currentThread().getContextClassLoader().loadClass(applicationClass);
                application = (Application) appClass.getConstructor().newInstance();
                Set<Class<?>> classes = application.getClasses();
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                    }
                    filterClasses = true;
                }
                classes = application.getSingletons().stream().map(Object::getClass).collect(Collectors.toSet());
                if (!classes.isEmpty()) {
                    for (Class<?> klass : classes) {
                        allowedClasses.add(klass.getName());
                        singletonClasses.add(klass.getName());
                    }
                    filterClasses = true;
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new RuntimeException("Unable to handle class: " + applicationClass, e);
            }
            if (applicationClassInfo.classAnnotation(ResteasyReactiveDotNames.BLOCKING) != null) {
                blocking = true;
            } else if (applicationClassInfo.classAnnotation(ResteasyReactiveDotNames.NON_BLOCKING) != null) {
                blocking = false;
            }
        }
        if (selectedAppClass != null) {
            globalNameBindings = NameBindingUtil.nameBindingNames(index, selectedAppClass);
        }
        return new ApplicationScanningResult(allowedClasses, singletonClasses, globalNameBindings, filterClasses, application,
                selectedAppClass, blocking);
    }

    public static ResourceScanningResult scanResources(
            IndexView index) {
        Collection<AnnotationInstance> paths = index.getAnnotations(ResteasyReactiveDotNames.PATH);

        Collection<AnnotationInstance> allPaths = new ArrayList<>(paths);

        if (allPaths.isEmpty()) {
            // no detected @Path, bail out
            return null;
        }

        Map<DotName, ClassInfo> scannedResources = new HashMap<>();
        Map<DotName, String> scannedResourcePaths = new HashMap<>();
        Map<DotName, ClassInfo> possibleSubResources = new HashMap<>();
        Map<DotName, String> pathInterfaces = new HashMap<>();
        Map<DotName, MethodInfo> resourcesThatNeedCustomProducer = new HashMap<>();
        List<MethodInfo> methodExceptionMappers = new ArrayList<>();

        Set<DotName> interfacesWithPathOnMethods = new HashSet<>();

        for (AnnotationInstance annotation : allPaths) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                ClassInfo clazz = annotation.target().asClass();
                if (!Modifier.isInterface(clazz.flags())) {
                    scannedResources.put(clazz.name(), clazz);
                    scannedResourcePaths.put(clazz.name(), annotation.value().asString());
                } else {
                    pathInterfaces.put(clazz.name(), annotation.value().asString());
                }
                MethodInfo ctor = hasJaxRsCtorParams(clazz);
                if (ctor != null) {
                    resourcesThatNeedCustomProducer.put(clazz.name(), ctor);
                }
                List<AnnotationInstance> exceptionMapperAnnotationInstances = clazz.annotations()
                        .get(ResteasyReactiveDotNames.SERVER_EXCEPTION_MAPPER);
                if (exceptionMapperAnnotationInstances != null) {
                    for (AnnotationInstance instance : exceptionMapperAnnotationInstances) {
                        if (instance.target().kind() != AnnotationTarget.Kind.METHOD) {
                            continue;
                        }
                        methodExceptionMappers.add(instance.target().asMethod());
                    }
                }
            } else if (annotation.target().kind() == AnnotationTarget.Kind.METHOD) {
                System.out.println("found path annotation on ");
                ClassInfo clazz = annotation.target().asMethod().declaringClass();
                if (Modifier.isInterface(clazz.flags())) {
                    interfacesWithPathOnMethods.add(clazz.name());
                }
            }
        }

        // for clients it is enought to have @PATH annotations on methods only
        for (DotName interfaceName : interfacesWithPathOnMethods) {
            if (!pathInterfaces.containsKey(interfaceName)) {
                pathInterfaces.put(interfaceName, "");
            }
        }

        Map<DotName, String> clientInterfaces = new HashMap<>(pathInterfaces);

        Map<DotName, String> clientInterfaceSubtypes = new HashMap<>();
        for (DotName interfaceName : clientInterfaces.keySet()) {
            addClientSubInterfaces(interfaceName, index, clientInterfaceSubtypes, clientInterfaces);
        }
        clientInterfaces.putAll(clientInterfaceSubtypes);

        for (Map.Entry<DotName, String> i : pathInterfaces.entrySet()) {
            for (ClassInfo clazz : index.getAllKnownImplementors(i.getKey())) {
                if (!Modifier.isAbstract(clazz.flags())) {
                    if ((clazz.enclosingClass() == null || Modifier.isStatic(clazz.flags())) &&
                            clazz.enclosingMethod() == null) {
                        if (!scannedResources.containsKey(clazz.name())) {
                            scannedResources.put(clazz.name(), clazz);
                            scannedResourcePaths.put(clazz.name(), i.getValue());
                        }
                    }
                }
            }
        }

        Map<DotName, String> httpAnnotationToMethod = new HashMap<>(BUILTIN_HTTP_ANNOTATIONS_TO_METHOD);
        Collection<AnnotationInstance> httpMethodInstances = index.getAnnotations(ResteasyReactiveDotNames.HTTP_METHOD);
        for (AnnotationInstance httpMethodInstance : httpMethodInstances) {
            if (httpMethodInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            httpAnnotationToMethod.put(httpMethodInstance.target().asClass().name(), httpMethodInstance.value().asString());
        }

        // for clients it is also enough to only have @GET, @POST, etc on methods and no PATH whatsoever
        Set<DotName> methodAnnotations = httpAnnotationToMethod.keySet();
        for (DotName methodAnnotation : methodAnnotations) {
            for (AnnotationInstance methodAnnotationInstance : index.getAnnotations(methodAnnotation)) {
                if (methodAnnotationInstance.target().kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo annotatedMethod = methodAnnotationInstance.target().asMethod();
                    ClassInfo classWithJaxrsMethod = annotatedMethod.declaringClass();
                    if (Modifier.isAbstract(annotatedMethod.flags())
                            && Modifier.isAbstract(classWithJaxrsMethod.flags())
                            && !clientInterfaces.containsKey(classWithJaxrsMethod.name())) {
                        clientInterfaces.put(classWithJaxrsMethod.name(), "");
                    }
                }
            }
        }

        Set<String> beanParams = new HashSet<>();

        Set<ClassInfo> beanParamAsBeanUsers = new HashSet<>(scannedResources.values());
        beanParamAsBeanUsers.addAll(possibleSubResources.values());

        for (AnnotationInstance beanParamAnnotation : index.getAnnotations(ResteasyReactiveDotNames.BEAN_PARAM)) {
            AnnotationTarget target = beanParamAnnotation.target();
            // FIXME: this isn't right wrt generics
            // mstodo exclude stuff not used by any server endpoints
            switch (target.kind()) {
                case FIELD:
                    FieldInfo field = target.asField();
                    if (beanParamAsBeanUsers.contains(field.declaringClass())) {
                        beanParams.add(field.type().name().toString());
                    }
                    break;
                case METHOD:
                    MethodInfo setterMethod = target.asMethod();
                    if (beanParamAsBeanUsers.contains(setterMethod.declaringClass())) {
                        Type setterParamType = setterMethod.parameters().get(0);
                        beanParams.add(setterParamType.name().toString());
                    }
                    break;
                case METHOD_PARAMETER:
                    MethodInfo method = target.asMethodParameter().method();
                    if (beanParamAsBeanUsers.contains(method.declaringClass())) {
                        int paramIndex = target.asMethodParameter().position();
                        Type paramType = method.parameters().get(paramIndex);
                        beanParams.add(paramType.name().toString());
                    }
                    break;
                default:
                    break;
            }
        }

        return new ResourceScanningResult(scannedResources,
                scannedResourcePaths, possibleSubResources, pathInterfaces, clientInterfaces, resourcesThatNeedCustomProducer,
                beanParams,
                httpAnnotationToMethod, methodExceptionMappers);
    }

    private static void addClientSubInterfaces(DotName interfaceName, IndexView index,
            Map<DotName, String> clientInterfaceSubtypes, Map<DotName, String> clientInterfaces) {
        Collection<ClassInfo> subclasses = index.getKnownDirectImplementors(interfaceName);
        for (ClassInfo subclass : subclasses) {
            if (!clientInterfaces.containsKey(subclass.name()) && Modifier.isInterface(subclass.flags())
                    && !clientInterfaceSubtypes.containsKey(subclass.name())) {
                clientInterfaceSubtypes.put(subclass.name(), clientInterfaces.get(interfaceName));
                addClientSubInterfaces(subclass.name(), index, clientInterfaceSubtypes, clientInterfaces);
            }
        }

    }

    private static MethodInfo hasJaxRsCtorParams(ClassInfo classInfo) {
        List<MethodInfo> methods = classInfo.methods();
        List<MethodInfo> ctors = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.name().equals("<init>")) {
                ctors.add(method);
            }
        }
        if (ctors.size() != 1) { // we only need to deal with a single ctor here
            return null;
        }
        MethodInfo ctor = ctors.get(0);
        if (ctor.parameters().size() == 0) { // default ctor - we don't need to do anything
            return null;
        }

        boolean needsHandling = false;
        for (DotName dotName : ResteasyReactiveDotNames.RESOURCE_CTOR_PARAMS_THAT_NEED_HANDLING) {
            if (ctor.hasAnnotation(dotName)) {
                needsHandling = true;
                break;
            }
        }
        return needsHandling ? ctor : null;
    }

}
