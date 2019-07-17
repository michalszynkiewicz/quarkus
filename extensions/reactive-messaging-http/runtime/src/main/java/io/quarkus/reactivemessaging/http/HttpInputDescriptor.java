package io.quarkus.reactivemessaging.http;

import java.util.Objects;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/07/2019
 */
class HttpInputDescriptor {
    final String path;
    final String method;

    HttpInputDescriptor(String path, String method) {
        this.path = path;
        this.method = method;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof HttpInputDescriptor && methodAndPathMatch((HttpInputDescriptor) o);
    }

    private boolean methodAndPathMatch(HttpInputDescriptor that) {
        return path.equals(that.path) && method.equals(that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, method);
    }

    @Override
    public String toString() {
        return "path='" + path + '\'' + ", method='" + method + '\'';
    }
}