package io.quarkus.restclient.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderFillingException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.restclient.runtime.QuarkusHeaderFillerFactoryBase;

// mstodo simplify, pull out a class
public class ClientHeaderParamsProcessor {
    private static final String INIT = "<init>";

    private static final Type STRING_TYPE = Type.create(DotName.createSimple(String.class.getName()), Type.Kind.CLASS);
    private static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class,
            Object.class);
    private static final String FACTORY_CLASS_NAME = "io.quarkus.restclient.runtime.generated.QuarkusHeaderFillerFactory";

    @BuildStep
    void createHeaderFillers(ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        Map<ClassInfo, String> interfaceMocks = new HashMap<>();

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, false);

        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .superClass(QuarkusHeaderFillerFactoryBase.class)
                .className(FACTORY_CLASS_NAME)
                .build()) {

            MethodCreator constructor = classCreator.getMethodCreator(INIT, void.class);
            constructor.invokeSpecialMethod(
                    MethodDescriptor.ofMethod(QuarkusHeaderFillerFactoryBase.class, "<init>", void.class),
                    constructor.getThis());

            ResultHandle fillerMapField = constructor.readInstanceField(
                    FieldDescriptor.of(FACTORY_CLASS_NAME, "map", Map.class), constructor.getThis());

            Index index = applicationIndex.getIndex();
            List<AnnotationData> clientHeaderParams = new ArrayList<>();

            for (AnnotationInstance annotation : index.getAnnotations(DotNames.CLIENT_HEADER_PARAM)) {
                clientHeaderParams.add(new AnnotationData(annotation, annotation.target()));
            }

            for (AnnotationInstance groupingAnnotation : index.getAnnotations(DotNames.CLIENT_HEADER_PARAMS)) {
                for (AnnotationInstance annotationInstance : groupingAnnotation.value().asNestedArray()) {
                    clientHeaderParams.add(new AnnotationData(annotationInstance, groupingAnnotation.target()));
                }
            }

            List<AnnotationData> methodHeaderParams = new ArrayList<>();
            List<AnnotationData> classHeaderParams = new ArrayList<>();
            for (AnnotationData data : clientHeaderParams) {
                AnnotationTarget target = data.target;
                switch (target.kind()) {
                    case CLASS:
                        classHeaderParams.add(data);
                        break;
                    case METHOD:
                        methodHeaderParams.add(data);
                        break;
                    default:
                        throw new RestClientDefinitionException(
                                "ClientHeaderParam annotation found on an unsupported element: " + target);
                }
            }

            // only for validation
            Multimap<MethodInfo, String> definedParams = HashMultimap.create();

            for (AnnotationData methodParameter : methodHeaderParams) {
                MethodInfo method = (MethodInfo) methodParameter.target;
                AnnotationInstance methodParamInstance = methodParameter.annotation;
                String headerName = methodParamInstance.value("name").asString();
                if (!definedParams.put(method, headerName)) {
                    boolean required = methodParamInstance.value("required") == null
                            || methodParamInstance.value("required").asBoolean();
                    constructor.invokeInterfaceMethod(MAP_PUT, fillerMapField,
                            createIdentifier(constructor, method, methodParameter.annotation,
                                    required),
                            createFailingFiller("Duplicate " + ClientHeaderParam.class.getSimpleName() +
                                    " annotation definitions found on " + method.toString(),
                                    method,
                                    headerName,
                                    constructor,
                                    generatedClass));
                }
                String[] methodSpecifierStrings = methodParamInstance.value().asStringArray();
                if (methodSpecifierStrings.length != 1 || !isMethodCall(methodSpecifierStrings[0])) {
                    continue;
                }
                AnnotationValue requiredAnn = methodParamInstance.value("required");
                boolean required = requiredAnn == null || requiredAnn.asBoolean();
                constructor.invokeInterfaceMethod(MAP_PUT, fillerMapField,
                        createIdentifier(constructor, method, methodParamInstance, required),
                        createHeaderFiller(methodParamInstance, method, constructor, headerName, generatedClass, index,
                                required, interfaceMocks));
            }

            for (AnnotationData classParameter : classHeaderParams) {
                String[] methodSpecifierStrings = classParameter.annotation.value().asStringArray();
                if (methodSpecifierStrings.length != 1 || !isMethodCall(methodSpecifierStrings[0])) {
                    continue;
                }
                ClassInfo aClass = (ClassInfo) classParameter.target;
                String headerName = classParameter.annotation.value("name").asString();
                for (MethodInfo method : aClass.methods()) {
                    if (definedParams.put(method, headerName)
                            && method.annotations().stream().map(AnnotationInstance::name)
                                    .anyMatch(DotNames.JAXRS_ANNOTATIONS::contains)) {
                        AnnotationValue requiredAnn = classParameter.annotation.value("required");
                        boolean required = requiredAnn == null || requiredAnn.asBoolean();
                        constructor.invokeInterfaceMethod(MAP_PUT, fillerMapField,
                                createIdentifier(constructor, method, classParameter.annotation, required),
                                createHeaderFiller(classParameter.annotation, method, constructor, headerName, generatedClass,
                                        index,
                                        required, interfaceMocks));
                    }
                }
            }
            constructor.returnValue(null);
        }
    }

    private boolean isMethodCall(String headerValue) {
        return headerValue != null
                && headerValue.startsWith("{")
                && headerValue.endsWith("}");
    }

    private ResultHandle createHeaderFiller(AnnotationInstance headerParam,
            MethodInfo methodInfo,
            MethodCreator methodCreator,
            String headerName,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            Index index,
            boolean required,
            Map<ClassInfo, String> interfaceMocks) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        String className = methodInfo.declaringClass().toString();
        String fillerClassName = className + "HeaderFiller"
                + HashUtil.sha1(className + "#" + methodInfo.toString() + "#" + headerName);
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .superClass(QuarkusHeaderFillerFactoryBase.QuarkusHeaderFiller.class)
                .className(fillerClassName)
                .build()) {
            MethodCreator failIfInvalid = classCreator.getMethodCreator("failIfInvalid", void.class);

            MethodCreator generateValues = classCreator.getMethodCreator("generateValues", List.class);
            String methodSpecifierString = headerParam.value().asStringArray()[0];
            String methodSpecifier = methodSpecifierString.substring(1, methodSpecifierString.length() - 1);

            createGenerateValuesMethod(index, generateValues, failIfInvalid, methodSpecifier, methodInfo, headerName, required,
                    generatedClass, interfaceMocks);
        }
        return methodCreator.newInstance(MethodDescriptor.ofConstructor(fillerClassName));
    }

    private ResultHandle createFailingFiller(String message,
            MethodInfo methodInfo,
            String headerName,
            MethodCreator methodCreator,
            BuildProducer<GeneratedClassBuildItem> generatedClass) {
        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
        String className = methodInfo.declaringClass().toString();
        String fillerClassName = className + "HeaderFiller"
                + HashUtil.sha1(className + "#" + methodInfo.toString() + "#" + headerName);
        try (ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .superClass(QuarkusHeaderFillerFactoryBase.QuarkusHeaderFiller.class)
                .className(fillerClassName)
                .build()) {
            MethodCreator failIfInvalid = classCreator.getMethodCreator("failIfInvalid", void.class);

            MethodCreator generateValues = classCreator.getMethodCreator("generateValues", List.class);

            failIfInvalid.throwException(RestClientDefinitionException.class, message);
            generateValues.returnValue(generateValues.loadNull());
        }
        return methodCreator.newInstance(MethodDescriptor.ofConstructor(fillerClassName));
    }

    private void createGenerateValuesMethod(Index index,
            MethodCreator generateValues,
            MethodCreator failIfInvalid,
            String methodSpecifier,
            MethodInfo methodInfo,
            String headerName,
            boolean required,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            Map<ClassInfo, String> interfaceMocks) {
        int lastDot = methodSpecifier.lastIndexOf('.');
        if (lastDot == methodSpecifier.length()) {
            failIfInvalid.throwException(RestClientDefinitionException.class,
                    "Invalid string to specify method: " + methodSpecifier +
                            " for header: '" + headerName + "' on class " + methodInfo.declaringClass().name());
            generateValues.returnValue(generateValues.loadNull());
        }
        ClassInfo classToCall;
        String methodToCall;
        ResultHandle objectToCall = null;
        if (lastDot > -1) {
            // class.method specified, we should treat it as a static method and just call it:
            String className = methodSpecifier.substring(0, lastDot);
            methodToCall = methodSpecifier.substring(lastDot + 1);

            classToCall = index.getClassByName(DotName.createSimple(className));
            if (classToCall == null) {
                failIfInvalid.throwException(RestClientDefinitionException.class,
                        "Class " + className + " that should fill ClientHeaderParam for " + methodInfo + " on "
                                + methodInfo.declaringClass() + " not found.");
                generateValues.returnValue(generateValues.loadNull());
                return;
            }
        } else {
            // default method
            String mockClassName = mockInterface(methodInfo.declaringClass(), generatedClass, index, interfaceMocks);
            objectToCall = generateValues.newInstance(MethodDescriptor.ofConstructor(mockClassName));

            classToCall = methodInfo.declaringClass();
            methodToCall = methodSpecifier;
        }

        MethodInfo specifiedMethod = getMethodCalled(failIfInvalid, classToCall, methodToCall);
        if (specifiedMethod == null) {
            // getMethodCalled handled error reporting, it's enough to make generateValues return null
            generateValues.returnValue(generateValues.loadNull());
            return;
        }
        boolean hasParameter = specifiedMethod.parameters().size() > 0;

        TryBlock tryBlock = generateValues.tryBlock();
        CatchBlockCreator catchBlock = tryBlock.addCatch(Throwable.class);
        String headerGenMethodDesc = classToCall + "#" + methodToCall;
        if (required) {
            catchBlock.throwException(ClientHeaderFillingException.class,
                    "Failed to invoke header generation method: " + headerGenMethodDesc,
                    catchBlock.getCaughtException());
        } else {
            ResultHandle logField = catchBlock
                    .readStaticField(
                            FieldDescriptor.of(QuarkusHeaderFillerFactoryBase.QuarkusHeaderFiller.class, "log", Logger.class));
            catchBlock.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(Logger.class, "warnv", void.class, Throwable.class, String.class, Object.class),
                    logField,
                    catchBlock.getCaughtException(), catchBlock.load("Invoking header generation method {0} failed"),
                    catchBlock.load(headerGenMethodDesc));
            catchBlock.returnValue(
                    catchBlock
                            .invokeStaticMethod(MethodDescriptor.ofMethod(Collections.class, "emptyList", List.class)));
        }

        ResultHandle result;
        if (objectToCall == null) {
            result = hasParameter
                    ? tryBlock.invokeStaticMethod(specifiedMethod, tryBlock.load(headerName))
                    : tryBlock.invokeStaticMethod(specifiedMethod);
        } else {
            result = hasParameter
                    ? tryBlock.invokeInterfaceMethod(specifiedMethod, objectToCall, tryBlock.load(headerName))
                    : tryBlock.invokeInterfaceMethod(specifiedMethod, objectToCall);
        }
        result = mapToList(tryBlock, failIfInvalid,
                classToCall, specifiedMethod,
                methodInfo.declaringClass().toString(), result);
        tryBlock.returnValue(result);
        if (result != null) {
            failIfInvalid.returnValue(null);
        }
    }

    // mstodo check with a default method inherited from an interface
    // mstodo check with a JAX-RS interface that inherits some @Paths from another interface
    private String mockInterface(ClassInfo declaringClass, BuildProducer<GeneratedClassBuildItem> generatedClass,
            Index index, Map<ClassInfo, String> interfaceMocks) {
        // we have an interface, we have to call a default method on it, we generate a (very simplistic) implementation:

        return interfaceMocks.computeIfAbsent(declaringClass, classInfo -> {
            String mockName = declaringClass.toString() + HashUtil.sha1(declaringClass.toString());
            ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClass, true);
            List<DotName> interfaceNames = declaringClass.interfaceNames();
            Set<MethodInfo> methods = new HashSet<>();
            for (DotName interfaceName : interfaceNames) {
                ClassInfo interfaceClass = index.getClassByName(interfaceName);
                methods.addAll(interfaceClass.methods());
            }
            methods.addAll(declaringClass.methods());

            try (ClassCreator classCreator = ClassCreator.builder().className(mockName).interfaces(declaringClass.toString())
                    .classOutput(classOutput)
                    .build()) {

                for (MethodInfo method : methods) {
                    if (Modifier.isAbstract(method.flags())) {
                        MethodCreator methodCreator = classCreator.getMethodCreator(MethodDescriptor.of(method));
                        methodCreator.returnValue(methodCreator.loadNull());
                    }
                }
            }
            return mockName;
        });
    }

    private ResultHandle mapToList(BytecodeCreator methodCreator, MethodCreator failIfInvalid,
            ClassInfo classInfo, MethodInfo method,
            String clientInterface,
            ResultHandle originalResult) {
        Type returnType = method.returnType();
        switch (returnType.kind()) {
            case CLASS:
                if (!returnType.name().equals(DotNames.STRING)) {
                    return throwWrongFillerMethodReturn(failIfInvalid, classInfo, method.name(), clientInterface, returnType);
                }
                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Collections.class, "singletonList", List.class, Object.class),
                        originalResult);
            case ARRAY:
                if (!returnType.asArrayType().component().name().equals(DotNames.STRING)) {
                    return throwWrongFillerMethodReturn(failIfInvalid, classInfo, method.name(), clientInterface, returnType);
                }

                return methodCreator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Arrays.class, "asList", List.class, Object[].class),
                        originalResult);
            case PARAMETERIZED_TYPE:
                if (!returnType.name().equals(DotNames.LIST)) {
                    return throwWrongFillerMethodReturn(failIfInvalid, classInfo, method.name(), clientInterface, returnType);
                }
                return originalResult;
            default:
                return throwWrongFillerMethodReturn(failIfInvalid, classInfo, method.name(), clientInterface, returnType);
        }
    }

    private <T> T throwWrongFillerMethodReturn(MethodCreator failIfInvalid, ClassInfo classInfo, String methodName,
            String clientInterface,
            Type returnType) {
        failIfInvalid.throwException(RestClientDefinitionException.class,
                "ClientHeaderParam method has to return one of: String, String[], List<String>. "
                        + classInfo + "#" + methodName + " used on " + clientInterface + " returns " + returnType.name());
        return null;
    }

    private MethodInfo getMethodCalled(MethodCreator failIfInvalid, ClassInfo classInfo, String methodName) {
        MethodInfo noParamMethod = classInfo.method(methodName);
        MethodInfo stringParamMethod = classInfo.method(methodName, STRING_TYPE);

        if (noParamMethod != null && stringParamMethod != null) {
            failIfInvalid.throwException(RestClientDefinitionException.class, "ClientHeaderParam defines an ambiguous method: "
                    + methodName + ". Two methods of the given name defined.");
            return null;
        }
        if (noParamMethod == null && stringParamMethod == null) {
            failIfInvalid.throwException(RestClientDefinitionException.class, "ClientHeaderParam defines invalid method: "
                    + methodName + ". No method of the given name exists.");
            return null;
        }

        return noParamMethod == null ? stringParamMethod : noParamMethod;
    }

    private ResultHandle createIdentifier(MethodCreator methodCreator,
            MethodInfo method,
            AnnotationInstance annotationInstance,
            boolean required) {
        return methodCreator.newInstance(
                MethodDescriptor.ofConstructor(
                        QuarkusHeaderFillerFactoryBase.Identifier.class,
                        String.class,
                        String.class,
                        String.class,
                        Boolean.class),
                methodCreator.load(method.declaringClass().name().toString()),
                methodCreator.load(annotationInstance.value().asStringArray()[0]),
                methodCreator.load(annotationInstance.value("name").asString()),
                methodCreator.load(required));
    }

    private class AnnotationData {
        private final AnnotationInstance annotation;
        private final AnnotationTarget target;

        public AnnotationData(AnnotationInstance annotation, AnnotationTarget target) {
            this.annotation = annotation;
            this.target = target;
        }
    }
}
