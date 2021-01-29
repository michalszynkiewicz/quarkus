package io.quarkus.rest.rest.client.microprofile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;

// mstodo pull stuff out?
public interface AsyncHandlerProvider {

    Handler get();

    AsyncHandlerProvider NO_OP = new AsyncHandlerProvider() {
        @Override
        public Handler get() {
            return Handler.NO_OP;
        }
    };

    class Impl implements AsyncHandlerProvider {

        final List<AsyncInvocationInterceptorFactory> factories;

        public Impl(List<AsyncInvocationInterceptorFactory> asyncInvocationFactories) {
            factories = asyncInvocationFactories;
        }

        public Handler get() {
            List<AsyncInvocationInterceptor> interceptors = new ArrayList<>();
            for (AsyncInvocationInterceptorFactory factory : factories) {
                AsyncInvocationInterceptor interceptor = factory.newInterceptor();
                interceptor.prepareContext();
                interceptors.add(interceptor);
            }

            return new Handler() {
                @Override
                public void start() {
                    for (AsyncInvocationInterceptor interceptor : interceptors) {
                        interceptor.applyContext();
                    }
                }

                @Override
                public void stop() {
                    for (AsyncInvocationInterceptor interceptor : interceptors) {
                        interceptor.applyContext();
                    }
                }

            };
        }

    }

    interface Handler {
        Handler NO_OP = new Handler() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }
        };

        void start();

        void stop();

    }
}
