package io.quarkus.grpc.runtime.devmode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.grpc.HandlerRegistry;
import io.grpc.ServerInterceptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.internal.ServerImpl;
import io.quarkus.grpc.runtime.ServerCalls;
import io.quarkus.grpc.runtime.StreamCollector;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.grpc.VertxServer;

public class GrpcServerReloader {
    private static final List<VertxServer> servers = Collections.synchronizedList(new ArrayList<>());

    private static final List<ServerServiceDefinition> serviceDefinitions = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, ServerMethodDefinition<?, ?>> methods = new ConcurrentHashMap<>();

    public static Collection<VertxServer> getServers() {
        return Collections.unmodifiableCollection(servers);
    }

    public static void clearServers() {
        servers.clear();
    }

    public static void addServer(VertxServer grpcServer) {
        servers.add(grpcServer);
    }

    public static StreamCollector devModeCollector() {
        if (ProfileManager.getLaunchMode() != LaunchMode.DEVELOPMENT) {
            throw new IllegalStateException("Attempted to initialize development mode StreamCollector in non-development mode");
        }
        return new DevModeStreamsCollector();
    }

    public static void init() {
        try {
            ServerCalls.setStreamCollector(GrpcServerReloader.devModeCollector());
            Field registryField = ServerImpl.class.getDeclaredField("registry");
            registryField.setAccessible(true);
            // all servers should have the same service definitions and methods, choose one and init everything based on it:
            if (servers.isEmpty()) {
                return;
            }
            initServicesAndMethods(registryField);

            for (VertxServer server : getServers()) {
                Object registryObject = registryField.get(server.getRawServer());
                forceSet(registryObject, "services", GrpcServerReloader.serviceDefinitions);
                forceSet(registryObject, "methods", GrpcServerReloader.methods);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reinitialize gRPC server", e);
        }
    }

    private static void initServicesAndMethods(Field registryField) throws IllegalAccessException, NoSuchFieldException {
        VertxServer representative = servers.get(0);
        HandlerRegistry registryObject = (HandlerRegistry) registryField.get(representative.getRawServer());
        serviceDefinitions.addAll(registryObject.getServices());
        Field methodsField = registryObject.getClass().getDeclaredField("methods");
        methodsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ServerMethodDefinition<?, ?>> methods = (Map<String, ServerMethodDefinition<?, ?>>) methodsField
                .get(registryObject);
        GrpcServerReloader.methods.putAll(methods);
    }

    public static void reset() {
        try {
            methods.clear();
            serviceDefinitions.clear();

            for (VertxServer server : getServers()) {
                forceSet(server.getRawServer(), "interceptors", null);
            }

            StreamCollector streamCollector = ServerCalls.getStreamCollector();
            if (!(streamCollector instanceof DevModeStreamsCollector)) {
                throw new IllegalStateException("Non-dev mode streams collector used in development mode");
            }
            ((DevModeStreamsCollector) streamCollector).shutdown();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reinitialize gRPC server", e);
        }
    }

    public static void reinitialize(List<ServerServiceDefinition> serviceDefinitions,
            Map<String, ServerMethodDefinition<?, ?>> methods,
            List<ServerInterceptor> sortedInterceptors) {
        GrpcServerReloader.methods.putAll(methods);
        GrpcServerReloader.serviceDefinitions.addAll(serviceDefinitions);
        try {
            ServerInterceptor[] interceptorsArray = sortedInterceptors.toArray(new ServerInterceptor[0]);
            for (VertxServer server : getServers()) {
                forceSet(server.getRawServer(), "interceptors", interceptorsArray);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to reinitialize gRPC server data", e);
        }
    }

    private static void forceSet(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);

        field.set(object, value);
    }

    public static void removeServer(VertxServer grpcServer) {
        servers.remove(grpcServer);
    }
}
