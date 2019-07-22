package io.quarkus.reactivemessaging.http;

import org.reactivestreams.Processor;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 17/07/2019
 */
public class WebsocketProcessorCreated {
    private final String path;
    private final Processor<WebsocketMessage<?>, WebsocketMessage<?>> processor;

    public WebsocketProcessorCreated(String path,
            Processor<WebsocketMessage<?>, WebsocketMessage<?>> processor) {
        this.path = path;
        this.processor = processor;
    }

    public boolean matchesPath(String path) {
        return this.path.equals(path);
    }

    public Processor<WebsocketMessage<?>, WebsocketMessage<?>> getProcessor() {
        return processor;
    }

    @Override
    public String toString() {
        return "WebsocketProcessorCreated{" +
                "path='" + path + '\'' +
                ", processor=" + processor +
                '}';
    }
}
