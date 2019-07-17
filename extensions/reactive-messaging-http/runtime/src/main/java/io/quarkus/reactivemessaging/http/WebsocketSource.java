package io.quarkus.reactivemessaging.http;

import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import io.reactivex.processors.BehaviorProcessor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 09/07/2019
 */
public class WebsocketSource {

    public PublisherBuilder<WebsocketMessage<?>> getSource(BehaviorProcessor<WebsocketMessage<?>> processor) {
        return ReactiveStreams.fromPublisher(processor);
    }

}
