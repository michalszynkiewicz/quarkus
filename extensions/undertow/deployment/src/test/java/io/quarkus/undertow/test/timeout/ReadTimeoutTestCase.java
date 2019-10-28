package io.quarkus.undertow.test.timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.RestAssuredURLManager;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class ReadTimeoutTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withConfigurationResource("application-timeout.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TimeoutTestServlet.class));

    private int port = RestAssuredURLManager.getHttpPort();
    private String host = RestAssuredURLManager.getHost();
    private InetSocketAddress hostAddress;
    private SocketChannel client;

    @BeforeEach
    public void init() throws IOException {
        port = RestAssuredURLManager.getHttpPort();
        host = RestAssuredURLManager.getHost();
        hostAddress = new InetSocketAddress(host, port);
        client = SocketChannel.open(hostAddress);
        TimeoutTestServlet.invoked = false;
    }

    @AfterEach
    public void cleanUp() throws IOException {
        client.close();
    }

    @Test
    public void shouldNotProcessRequestWrittenTooSlowly() throws IOException, InterruptedException {
        requestWithDelay(1000L);

        ByteBuffer buffer = ByteBuffer.allocate(100000);
        client.read(buffer);

        assertFalse(TimeoutTestServlet.invoked);
    }

    @Test
    public void shouldProcessSlowlyProcessedRequest() throws IOException, InterruptedException {
        requestWithDelay(100L, "Processing-Time: 1500");

        ByteBuffer buffer = ByteBuffer.allocate(100000);
        client.read(buffer);
        MatcherAssert.assertThat(new String(buffer.array(), StandardCharsets.UTF_8),
                Matchers.containsString(TimeoutTestServlet.TIMEOUT_SERVLET));
        assertTrue(TimeoutTestServlet.invoked);
    }

    private void requestWithDelay(long sleepTime, String... headers)
            throws IOException, InterruptedException {
        String content = "message content";
        writeToChannel("GET /timeout HTTP/1.1\r\n");
        writeToChannel("Content-Length: " + ("The \r\n" + content).getBytes("UTF-8").length);
        for (String header : headers) {
            writeToChannel("\r\n" + header);
        }
        writeToChannel("\r\nHost: " + host);
        writeToChannel("\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n");
        writeToChannel("The \r\n");
        Thread.sleep(sleepTime);
        writeToChannel(content);
    }

    private void writeToChannel(String s) {
        try {
            byte[] message = s.getBytes("UTF-8");
            ByteBuffer buffer = ByteBuffer.wrap(message);
            client.write(buffer);
            buffer.clear();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to channel", e);
        }
    }
}
