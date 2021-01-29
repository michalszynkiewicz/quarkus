package io.quarkus.rest.rest.client.microprofile;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;

public class AsyncInterceptorHandler {
    private final List<AsyncInvocationInterceptor> interceptors = new ArrayList<>();

    public void add(AsyncInvocationInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    public void prepare() {
        for (AsyncInvocationInterceptor interceptor : interceptors) {
            interceptor.prepareContext();
        }
    }

    public void startInvocation() {
        for (AsyncInvocationInterceptor interceptor : interceptors) {
            interceptor.applyContext();
        }
    }

    public void cleanUp() {
        for (AsyncInvocationInterceptor interceptor : interceptors) {
            interceptor.removeContext();
        }
    }
}
