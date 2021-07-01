package io.quarkus.grpc.runtime.dux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.logging.Logger;

import io.grpc.ConnectivityState;
import io.grpc.ConnectivityStateInfo;
import io.grpc.EquivalentAddressGroup;
import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.internal.JsonUtil;
import io.smallrye.dux.Dux;
import io.smallrye.dux.ServiceInstance;

public class GrpcLoadBalancerProvider extends LoadBalancerProvider {

    private static final Logger log = Logger.getLogger(GrpcLoadBalancerProvider.class);

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getPriority() {
        return 9;
    }

    @Override
    public String getPolicyName() {
        return "dux";
    }

    @Override
    public NameResolver.ConfigOrError parseLoadBalancingPolicyConfig(Map<String, ?> rawConfig) {
        try {
            return NameResolver.ConfigOrError
                    .fromConfig(new DuxLoadBalancerConfig(JsonUtil.getString(rawConfig, "service-name")));
        } catch (RuntimeException e) {
            return NameResolver.ConfigOrError.fromError(
                    Status.fromThrowable(e).withDescription(

                            // mstodo replace Dux everywhere
                            "Failed to parse Dux config: " + rawConfig));
        }
    }

    @Override
    public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper) {
        return new LoadBalancer() {

            String serviceName;

            @Override
            public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses) {
                List<EquivalentAddressGroup> addresses = resolvedAddresses.getAddresses();
                DuxLoadBalancerConfig config = (DuxLoadBalancerConfig) resolvedAddresses.getLoadBalancingPolicyConfig();

                Map<ServiceInstance, Subchannel> subChannels = new TreeMap<>(Comparator.comparingLong(ServiceInstance::getId));
                Set<ServiceInstance> activeSubchannels = Collections.newSetFromMap(new ConcurrentHashMap<>());
                AtomicReference<ConnectivityState> state = new AtomicReference<>(ConnectivityState.CONNECTING);

                serviceName = config.serviceName;

                final DuxSubchannelPicker picker = new DuxSubchannelPicker(subChannels, serviceName, activeSubchannels);

                for (EquivalentAddressGroup addressGroup : addresses) {
                    ServiceInstance serviceInstance = addressGroup.getAttributes()
                            .get(GrpcDuxServiceDiscovery.SERVICE_INSTANCE);
                    CreateSubchannelArgs subChannelArgs = CreateSubchannelArgs.newBuilder()
                            .setAddresses(addressGroup)
                            .setAttributes(addressGroup.getAttributes())
                            .build();

                    Subchannel subchannel = helper.createSubchannel(subChannelArgs);
                    subchannel.start(new SubchannelStateListener() {
                        @Override
                        public void onSubchannelState(ConnectivityStateInfo stateInfo) {
                            switch (stateInfo.getState()) {
                                case READY:
                                    activeSubchannels.add(serviceInstance);
                                    if (state.getAndSet(ConnectivityState.READY) != ConnectivityState.READY) {
                                        helper.updateBalancingState(state.get(), picker);
                                    }
                                    break;
                                case CONNECTING:
                                case TRANSIENT_FAILURE:
                                case IDLE:
                                case SHUTDOWN:
                                    activeSubchannels.remove(serviceInstance);
                                    log.debugf("subchannel changed state to %s", stateInfo.getState());
                                    if (activeSubchannels.isEmpty()
                                            && state.compareAndSet(ConnectivityState.READY, stateInfo.getState())) {
                                        helper.updateBalancingState(state.get(), picker);
                                    }
                                    break;
                            }
                        }
                    });
                    subChannels.put(serviceInstance, subchannel);
                }

                helper.updateBalancingState(state.get(), picker);
            }

            @Override
            public void handleNameResolutionError(Status error) {
                log.errorf("Name resolution failed for service '%s'", serviceName);
            }

            @Override
            public void shutdown() {
                log.debugf("Shutting down load balancer for service '%s'", serviceName);
            }
        };
    }

    static class DuxLoadBalancerConfig {
        final String serviceName;

        DuxLoadBalancerConfig(String serviceName) {
            this.serviceName = serviceName;
        }
    }

    static class DuxSubchannelPicker extends LoadBalancer.SubchannelPicker {
        private final Map<ServiceInstance, LoadBalancer.Subchannel> subChannels;
        private final String serviceName;
        private final Set<ServiceInstance> activeServerInstances;

        DuxSubchannelPicker(Map<ServiceInstance, LoadBalancer.Subchannel> subChannels,
                String serviceName, Set<ServiceInstance> activeServerInstances) {
            this.subChannels = subChannels;
            this.serviceName = serviceName;
            this.activeServerInstances = activeServerInstances;
        }

        @Override
        public LoadBalancer.PickResult pickSubchannel(LoadBalancer.PickSubchannelArgs args) {
            ServiceInstance serviceInstance = pickServerInstance();

            LoadBalancer.Subchannel subchannel = subChannels.get(serviceInstance);
            return LoadBalancer.PickResult.withSubchannel(subchannel);
            // mstodo add stream channel tracer to trace failures
        }

        private ServiceInstance pickServerInstance() {
            io.smallrye.dux.LoadBalancer lb = Dux.getInstance().getLoadBalancer(serviceName);

            Set<ServiceInstance> toChooseFrom = this.activeServerInstances;
            if (activeServerInstances.isEmpty()) {
                toChooseFrom = subChannels.keySet();
            }
            return lb.selectServiceInstance(new ArrayList<>(toChooseFrom)); // mstodo remove the need of wrapping
        }
    }
}
