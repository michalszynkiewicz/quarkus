package io.quarkus.reactivemessaging.http.runtime;

public class ReactiveHttpException extends Exception {
    private final int statusCode;

    public ReactiveHttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
