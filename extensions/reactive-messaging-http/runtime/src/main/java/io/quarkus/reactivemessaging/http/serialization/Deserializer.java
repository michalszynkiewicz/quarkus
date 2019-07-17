package io.quarkus.reactivemessaging.http.serialization;

import io.netty.handler.codec.http.HttpRequest;

/**
 * mstodo: Header
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 11/07/2019
 */
public interface Deserializer<T> {
    T deserialize(HttpRequest request);
}
