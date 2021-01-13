package io.quarkus.resteasy.reactive.client.deployment;

import static io.quarkus.deployment.Feature.RESTEASY_REACTIVE_JAXRS_CLIENT;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.resteasy.reactive.client.impl.ClientImpl;
import org.jboss.resteasy.reactive.client.impl.WebTargetImpl;
import org.jboss.resteasy.reactive.common.core.GenericTypeMapping;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.common.model.MethodParameter;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.model.ResourceMethod;
import org.jboss.resteasy.reactive.common.model.ResourceReader;
import org.jboss.resteasy.reactive.common.model.ResourceWriter;
import org.jboss.resteasy.reactive.common.model.RestClientInterface;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaderWriter;
import org.jboss.resteasy.reactive.common.processor.AdditionalReaders;
import org.jboss.resteasy.reactive.common.processor.AdditionalWriters;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.BeanParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.ClientBeanParamInfo;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.CookieParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.HeaderParamItem;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.Item;
import io.quarkus.resteasy.reactive.client.deployment.beanparam.QueryParamItem;
import io.quarkus.resteasy.reactive.client.runtime.ResteasyReactiveClientRecorder;
import io.quarkus.resteasy.reactive.common.deployment.ApplicationResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.QuarkusFactoryCreator;
import io.quarkus.resteasy.reactive.common.deployment.ResourceScanningResultBuildItem;
import io.quarkus.resteasy.reactive.common.deployment.SerializersUtil;
import io.quarkus.resteasy.reactive.common.runtime.ResteasyReactiveConfig;
import io.quarkus.resteasy.reactive.spi.MessageBodyReaderBuildItem;
import io.quarkus.resteasy.reactive.spi.MessageBodyWriterBuildItem;
import io.quarkus.runtime.RuntimeValue;

public class JaxrsClientProcessor {

    @BuildStep
    void addFeature(BuildProducer<FeatureBuildItem> features) { // mstodo polish this!
        try {
            File.createTempFile("-1featureregistration", "tmp");
        } catch (IOException e) {
            e.printStackTrace();
        }
        features.produce(new FeatureBuildItem(RESTEASY_REACTIVE_JAXRS_CLIENT));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setupClientProxies(ResteasyReactiveClientRecorder recorder,
            BeanContainerBuildItem beanContainerBuildItem,
            ApplicationResultBuildItem applicationResultBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<MessageBodyReaderBuildItem> messageBodyReaderBuildItems,
            List<MessageBodyWriterBuildItem> messageBodyWriterBuildItems,
            List<JaxrsClientEnricherBuildItem> enricherBuildItems,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            ResourceScanningResultBuildItem resourceScanningResultBuildItem,
            ResteasyReactiveConfig config,
            RecorderContext recorderContext,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformerBuildItemBuildProducer) {

        Serialisers serialisers = recorder.createSerializers();

        SerializersUtil.setupSerializers(recorder, reflectiveClassBuildItemBuildProducer, messageBodyReaderBuildItems,
                messageBodyWriterBuildItems, beanContainerBuildItem, applicationResultBuildItem, serialisers,
                RuntimeType.CLIENT);

        if (resourceScanningResultBuildItem == null
                || resourceScanningResultBuildItem.getResult().getClientInterfaces().isEmpty()) {
            recorder.setupClientProxies(new HashMap<>());
            return;
        }
        ResourceScanningResult result = resourceScanningResultBuildItem.getResult();

        AdditionalReaders additionalReaders = new AdditionalReaders();
        AdditionalWriters additionalWriters = new AdditionalWriters();

        IndexView index = beanArchiveIndexBuildItem.getIndex();
        ClientEndpointIndexer clientEndpointIndexer = new ClientEndpointIndexer.Builder()
                .setIndex(index)
                .setExistingConverters(new HashMap<>())
                .setScannedResourcePaths(result.getScannedResourcePaths())
                .setConfig(new org.jboss.resteasy.reactive.common.ResteasyReactiveConfig(config.inputBufferSize.asLongValue(),
                        config.singleDefaultProduces, config.defaultProduces))
                .setAdditionalReaders(additionalReaders)
                .setHttpAnnotationToMethod(result.getHttpAnnotationToMethod())
                .setInjectableBeans(new HashMap<>())
                .setFactoryCreator(new QuarkusFactoryCreator(recorder, beanContainerBuildItem.getValue()))
                .setAdditionalWriters(additionalWriters)
                .setDefaultBlocking(applicationResultBuildItem.getResult().isBlocking())
                .setHasRuntimeConverters(false).build();

        Map<String, RuntimeValue<Function<WebTarget, ?>>> clientImplementations = new HashMap<>();
        for (Map.Entry<DotName, String> i : result.getClientInterfaces().entrySet()) {
            ClassInfo clazz = index.getClassByName(i.getKey());
            //these interfaces can also be clients
            //so we generate client proxies for them
            RestClientInterface clientProxy = clientEndpointIndexer.createClientProxy(clazz,
                    i.getValue());
            if (clientProxy != null) {
                RuntimeValue<Function<WebTarget, ?>> proxyProvider = generateClientInvoker(recorderContext, clientProxy,
                        enricherBuildItems, generatedClassBuildItemBuildProducer, clazz, index);
                if (proxyProvider != null) {
                    clientImplementations.put(clientProxy.getClassName(), proxyProvider);
                }
            }

        }

        recorder.setupClientProxies(clientImplementations);

        for (AdditionalReaderWriter.Entry additionalReader : additionalReaders.get()) {
            Class readerClass = additionalReader.getHandlerClass();
            ResourceReader reader = new ResourceReader();
            reader.setBuiltin(true);
            reader.setFactory(recorder.factory(readerClass.getName(), beanContainerBuildItem.getValue()));
            reader.setMediaTypeStrings(Collections.singletonList(additionalReader.getMediaType()));
            recorder.registerReader(serialisers, additionalReader.getEntityClass().getName(), reader);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, readerClass.getName()));
        }

        for (AdditionalReaderWriter.Entry entry : additionalWriters.get()) {
            Class writerClass = entry.getHandlerClass();
            ResourceWriter writer = new ResourceWriter();
            writer.setBuiltin(true);
            writer.setFactory(recorder.factory(writerClass.getName(), beanContainerBuildItem.getValue()));
            writer.setMediaTypeStrings(Collections.singletonList(entry.getMediaType()));
            recorder.registerWriter(serialisers, entry.getEntityClass().getName(), writer);
            reflectiveClassBuildItemBuildProducer
                    .produce(new ReflectiveClassBuildItem(true, false, false, writerClass.getName()));
        }

    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void registerInvocationCallbacks(CombinedIndexBuildItem index, ResteasyReactiveClientRecorder recorder) {

        Collection<ClassInfo> invocationCallbacks = index.getComputingIndex()
                .getAllKnownImplementors(ResteasyReactiveDotNames.INVOCATION_CALLBACK);

        GenericTypeMapping genericTypeMapping = new GenericTypeMapping();
        for (ClassInfo invocationCallback : invocationCallbacks) {
            try {
                List<Type> typeParameters = JandexUtil.resolveTypeParameters(invocationCallback.name(),
                        ResteasyReactiveDotNames.INVOCATION_CALLBACK, index.getComputingIndex());
                recorder.registerInvocationHandlerGenericType(genericTypeMapping, invocationCallback.name().toString(),
                        typeParameters.get(0).name().toString());
            } catch (Exception ignored) {

            }
        }
        recorder.setGenericTypeMapping(genericTypeMapping);
    }

    private RuntimeValue<Function<WebTarget, ?>> generateClientInvoker(RecorderContext recorderContext,
            RestClientInterface restClientInterface, List<JaxrsClientEnricherBuildItem> enrichers,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer, ClassInfo interfaceClass,
            IndexView index) {
        boolean subResource = false;
        //if the interface contains sub resource locator methods we ignore it
        for (ResourceMethod i : restClientInterface.getMethods()) {
            if (i.getHttpMethod() == null) {
                subResource = true;
            }
            break;
        }
        if (subResource) {
            return null;
        }
        String name = restClientInterface.getClassName() + "$$QuarkusRestClientInterface";
        MethodDescriptor ctorDesc = MethodDescriptor.ofConstructor(name, WebTarget.class.getName());
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                name, null, Object.class.getName(),
                Closeable.class.getName(), restClientInterface.getClassName())) {

            FieldDescriptor target = FieldDescriptor.of(name, "target", WebTarget.class);
            c.getFieldCreator(target).setModifiers(Modifier.FINAL);

            MethodCreator ctor = c.getMethodCreator(ctorDesc);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());

            ResultHandle res = ctor.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class, String.class),
                    ctor.getMethodParam(0), ctor.load(restClientInterface.getPath()));
            ctor.writeInstanceField(target, ctor.getThis(), res);

            for (JaxrsClientEnricherBuildItem enricher : enrichers) {
                enricher.getEnricher().enrichWebTarget(ctor, res, interfaceClass, index);
            }
            ctor.returnValue(null);

            // create `void close()` method:
            MethodCreator closeCreator = c.getMethodCreator(MethodDescriptor.ofMethod(Closeable.class, "close", void.class));
            ResultHandle webTarget = closeCreator.readInstanceField(target, closeCreator.getThis());
            ResultHandle webTargetImpl = closeCreator.checkCast(webTarget, WebTargetImpl.class);
            ResultHandle restClient = closeCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(WebTargetImpl.class, "getRestClient", ClientImpl.class), webTargetImpl);
            closeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(ClientImpl.class, "close", void.class), restClient);
            closeCreator.returnValue(null);

            // create methods from the jaxrs interface
            for (ResourceMethod method : restClientInterface.getMethods()) {
                MethodCreator methodCreator = c.getMethodCreator(method.getName(), method.getReturnType(),
                        Arrays.stream(method.getParameters()).map(s -> s.type).toArray());

                ResultHandle targetField = methodCreator.readInstanceField(target, methodCreator.getThis());
                if (method.getPath() != null) {
                    methodCreator.writeInstanceField(target, methodCreator.getThis(), methodCreator
                            .invokeInterfaceMethod(MethodDescriptor.ofMethod(WebTarget.class, "path", WebTarget.class,
                                    String.class), targetField, methodCreator.load(method.getPath())));
                }

                Integer bodyParameterIdx = null;

                Map<MethodDescriptor, ResultHandle> invocationBuilderEnrichers = new HashMap<>();

                for (int paramIdx = 0; paramIdx < method.getParameters().length; ++paramIdx) {
                    MethodParameter param = method.getParameters()[paramIdx];
                    // mstodo we need a wrapper on it so that it can be used together with field, etc?
                    if (param.parameterType == ParameterType.QUERY) {
                        //TODO: converters
                        addQueryParam(methodCreator, target, param.name,
                                methodCreator.getMethodParam(paramIdx));
                    } else if (param.parameterType == ParameterType.BEAN) {
                        ClientBeanParamInfo beanParam = (ClientBeanParamInfo) param;
                        MethodDescriptor enricherMethod = MethodDescriptor.ofMethod(name,
                                method.getName() + "$$enrichInvocationBuilder$$" + paramIdx, Invocation.Builder.class,
                                Invocation.Builder.class, param.type);
                        MethodCreator enricherMethodCreator = c.getMethodCreator(enricherMethod);

                        AssignableResultHandle invocationBuilderRef = enricherMethodCreator
                                .createVariable(Invocation.Builder.class);
                        enricherMethodCreator.assign(invocationBuilderRef, enricherMethodCreator.getMethodParam(0));
                        addBeanParamData(methodCreator, enricherMethodCreator,
                                invocationBuilderRef,
                                target, beanParam.getItems(),
                                methodCreator.getMethodParam(paramIdx));
                        enricherMethodCreator.returnValue(invocationBuilderRef);
                        invocationBuilderEnrichers.put(enricherMethod, methodCreator.getMethodParam(paramIdx));
                    } else if (param.parameterType == ParameterType.BODY) {
                        bodyParameterIdx = paramIdx;
                    }
                }

                ResultHandle builder;
                targetField = methodCreator.readInstanceField(target, methodCreator.getThis());
                if (method.getProduces() == null || method.getProduces().length == 0) {
                    builder = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class), targetField);
                } else {

                    ResultHandle array = methodCreator.newArray(String.class, method.getProduces().length);
                    for (int i = 0; i < method.getProduces().length; ++i) {
                        methodCreator.writeArrayValue(array, i, methodCreator.load(method.getProduces()[i]));
                    }
                    builder = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(WebTarget.class, "request", Invocation.Builder.class, String[].class),
                            targetField, array);
                }

                for (Map.Entry<MethodDescriptor, ResultHandle> invocationBuilderEnricher : invocationBuilderEnrichers
                        .entrySet()) {
                    builder = methodCreator.invokeVirtualMethod(invocationBuilderEnricher.getKey(), methodCreator.getThis(),
                            builder, invocationBuilderEnricher.getValue());
                }

                //TODO: async return types

                ResultHandle result;
                String mediaTypeValue = MediaType.APPLICATION_JSON;
                if (bodyParameterIdx != null) {
                    String[] consumes = method.getConsumes();
                    if (consumes != null && consumes.length > 0) {
                        if (consumes.length > 1) {
                            throw new IllegalArgumentException(
                                    "Multiple `@Consumes` values used in a MicroProfile Rest Client: " +
                                            restClientInterface.getClassName()
                                            + " Unable to determine a single `Content-Type`.");
                        }
                        mediaTypeValue = consumes[0];
                    }
                    ResultHandle mediaType = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                            methodCreator.load(mediaTypeValue));

                    ResultHandle entity = methodCreator.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Entity.class, "entity", Entity.class, Object.class, MediaType.class),
                            methodCreator.getMethodParam(bodyParameterIdx),
                            mediaType);
                    result = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    Entity.class, Class.class),
                            builder, methodCreator.load(method.getHttpMethod()), entity,
                            methodCreator.loadClass(method.getSimpleReturnType()));
                } else {
                    result = methodCreator.invokeInterfaceMethod(
                            MethodDescriptor.ofMethod(Invocation.Builder.class, "method", Object.class, String.class,
                                    Class.class),
                            builder, methodCreator.load(method.getHttpMethod()),
                            methodCreator.loadClass(method.getSimpleReturnType()));
                }
                methodCreator.returnValue(result);
            }

        }
        String creatorName = restClientInterface.getClassName() + "$$QuarkusRestClientInterfaceCreator";
        try (ClassCreator c = new ClassCreator(new GeneratedClassGizmoAdaptor(generatedClassBuildItemBuildProducer, true),
                creatorName, null, Object.class.getName(), Function.class.getName())) {

            MethodCreator apply = c
                    .getMethodCreator(MethodDescriptor.ofMethod(creatorName, "apply", Object.class, Object.class));
            apply.returnValue(apply.newInstance(ctorDesc, apply.getMethodParam(0)));
        }
        return recorderContext.newInstance(creatorName);

    }

    private void addBeanParamData(BytecodeCreator methodCreator,
            BytecodeCreator invocationBuidlerEnricher, // Invocation.Builder executePut$$enrichInvocationBuilder${noOfBeanParam}(Invocation.Builder)
            AssignableResultHandle invocationBuilder,
            FieldDescriptor target,
            List<Item> beanParamItems,
            ResultHandle param) {

        BytecodeCreator creator = methodCreator.ifNotNull(param).trueBranch();
        BytecodeCreator invoEnricher = invocationBuidlerEnricher.ifNotNull(invocationBuidlerEnricher.getMethodParam(1))
                .trueBranch();
        for (Item item : beanParamItems) {
            switch (item.type()) {
                case BEAN_PARAM:
                    BeanParamItem beanParamItem = (BeanParamItem) item;
                    ResultHandle beanParamElementHandle = beanParamItem.extract(creator, param);
                    addBeanParamData(creator, invoEnricher, invocationBuilder, target, beanParamItem.items(),
                            beanParamElementHandle);
                    break;
                case QUERY_PARAM:
                    QueryParamItem queryParam = (QueryParamItem) item;
                    addQueryParam(creator, target, queryParam.name(), queryParam.extract(creator, param));
                    break;
                case COOKIE:
                    CookieParamItem cookieParam = (CookieParamItem) item;
                    addCookieParam(invoEnricher, invocationBuilder,
                            cookieParam.getCookieName(),
                            cookieParam.extract(invoEnricher, invoEnricher.getMethodParam(1)));
                    break;
                case HEADER_PARAM:
                    HeaderParamItem headerParam = (HeaderParamItem) item;
                    addHeaderParam(invoEnricher, invocationBuilder,
                            headerParam.getHeaderName(),
                            headerParam.extract(invoEnricher, invoEnricher.getMethodParam(1)));
                    break;
                default:
                    throw new IllegalStateException("Unimplemented"); // mstodo form params, etc
            }
        }
    }

    private void addQueryParam(BytecodeCreator methodCreator,
            FieldDescriptor target,
            String paramName, ResultHandle queryParamHandle) {
        ResultHandle targetField = methodCreator.readInstanceField(target, methodCreator.getThis());
        ResultHandle array = methodCreator.newArray(Object.class, 1);
        methodCreator.writeArrayValue(array, 0, queryParamHandle);
        ResultHandle alteredTarget = methodCreator.invokeInterfaceMethod(
                MethodDescriptor.ofMethod(WebTarget.class, "queryParam", WebTarget.class,
                        String.class, Object[].class),
                targetField, methodCreator.load(paramName), array);
        methodCreator.writeInstanceField(target, methodCreator.getThis(), alteredTarget);
    }

    private void addHeaderParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle headerParamHandle) {
        invoBuilderEnricher.assign(invocationBuilder,
                invoBuilderEnricher.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "header", Invocation.Builder.class, String.class,
                                Object.class),
                        invocationBuilder, invoBuilderEnricher.load(paramName), headerParamHandle));
    }

    private void addCookieParam(BytecodeCreator invoBuilderEnricher, AssignableResultHandle invocationBuilder,
            String paramName, ResultHandle cookieParamHandle) {
        invoBuilderEnricher.assign(invocationBuilder,
                invoBuilderEnricher.invokeInterfaceMethod(
                        MethodDescriptor.ofMethod(Invocation.Builder.class, "cookie", Invocation.Builder.class, String.class,
                                String.class),
                        invocationBuilder, invoBuilderEnricher.load(paramName), cookieParamHandle));
    }

}
