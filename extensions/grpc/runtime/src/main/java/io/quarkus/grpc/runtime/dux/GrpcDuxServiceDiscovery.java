package io.quarkus.grpc.runtime.dux;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import com.google.common.base.Preconditions;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;
import io.smallrye.dux.Dux;
import io.smallrye.dux.ServiceDiscovery;
import io.smallrye.dux.ServiceInstance;
import io.smallrye.dux.utils.DuxAddressUtils;
import io.smallrye.dux.utils.HostAndPort;
import io.smallrye.mutiny.Multi;

/**
 * for gRPC, the service instance id must be immutable.
 * Even a change of attributes of a service instance must result in changing the service instance id.
 */
public class GrpcDuxServiceDiscovery extends NameResolverProvider {
    private static final Logger log = Logger.getLogger(GrpcDuxServiceDiscovery.class);
    public static final String DUX = "dux";
    public static final Attributes.Key<ServiceInstance> SERVICE_INSTANCE = Attributes.Key.create("service-instance");

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 6; // slightly more important than the default 5
    }

    @Override
    public String getDefaultScheme() {
        return "dux";
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
        if (!DUX.equals(targetUri.getScheme())) {
            return null;
        }
        NameResolver.ServiceConfigParser configParser = args.getServiceConfigParser();
        // mstodo try with dux and ssl
        return new NameResolver() {
            Listener2 listener;
            boolean resolving, shutdown; // mstodo should this be volatile?
            ServiceDiscovery serviceDiscovery;
            String serviceName;

            @Override
            public String getServiceAuthority() {
                return targetUri.getAuthority(); // mstodo what to do with this???
            }

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public void start(Listener2 listener) {
                Preconditions.checkState(this.listener == null, "already started");
                this.listener = listener;
                serviceName = targetUri.getHost();
                serviceDiscovery = Dux.getInstance().getServiceDiscovery(serviceName);
                resolve();
            }

            private void resolve() {
                // mstodo should dux handle refresh itself?
                // mstodo probably some service discovery providers can push updates
                if (resolving || shutdown) {
                    // mstodo: dns resolver has a check for cacheRefreshRequired here
                    return;
                }
                resolving = true;
                refresh();
            }

            @Override
            public void refresh() { // mstodo this may be called really often
                Multi<ServiceInstance> serviceInstances = serviceDiscovery.getServiceInstances();
                serviceInstances.collect().asList()
                        .subscribe()
                        .with(this::informListener);
            }

            private void informListener(List<ServiceInstance> instances) {
                ArrayList<EquivalentAddressGroup> addresses = new ArrayList<>();

                for (ServiceInstance instance : instances) {
                    HostAndPort hostAndPort = DuxAddressUtils.parseToHostAndPort(instance.getValue(), 9000, serviceName);

                    List<SocketAddress> socketAddresses = new ArrayList<>();
                    try {
                        for (InetAddress inetAddress : InetAddress.getAllByName(hostAndPort.host)) {
                            socketAddresses.add(new InetSocketAddress(inetAddress, hostAndPort.port));
                        }
                    } catch (UnknownHostException e) {
                        log.errorf(e, "Ignoring wrong host: '%s' for service name '%s'", hostAndPort.host, serviceName);
                    }

                    if (!socketAddresses.isEmpty()) {
                        Attributes attributes = Attributes.newBuilder()
                                .set(SERVICE_INSTANCE, instance)
                                .build();
                        EquivalentAddressGroup addressGroup = new EquivalentAddressGroup(socketAddresses, attributes);
                        addresses.add(addressGroup);
                    }
                }

                if (addresses.isEmpty()) {
                    log.error("Failed to determine working socket addresses for service-name: " + serviceName);
                    listener.onError(Status.FAILED_PRECONDITION);
                } else {
                    ConfigOrError serviceConfig = configParser.parseServiceConfig(mapConfigForServiceName());
                    listener.onResult(ResolutionResult.newBuilder()
                            .setAddresses(addresses)
                            .setServiceConfig(serviceConfig)
                            .build());
                }
            }

            private Map<String, List<Map<String, Map<String, String>>>> mapConfigForServiceName() {
                return Map.of("loadBalancingConfig", List.of(
                        Map.of("dux", Map.of("service-name", serviceName))));
            }
        };
    }
}
