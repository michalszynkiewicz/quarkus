package io.quarkus.grpc.runtime.supports.context;

import javax.enterprise.context.RequestScoped;

@RequestScoped
class GrpcRequestContextMarkerBean {
    private volatile boolean createdWithGrpc;

    boolean isCreatedWithGrpc() {
        return createdWithGrpc;
    }

    void setCreatedWithGrpc(boolean createdWithGrpc) {
        this.createdWithGrpc = createdWithGrpc;
    }
}
