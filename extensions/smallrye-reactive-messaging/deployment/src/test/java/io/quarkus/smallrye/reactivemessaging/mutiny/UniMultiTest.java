package io.quarkus.smallrye.reactivemessaging.mutiny;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class UniMultiTest {
    // mstodo check without requesting

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultiBean.class));

    @Inject
    MultiBean multiBean;

    @Test
    public void shouldGatherEvenFromMultiBean() {
        System.out.println("Test started");
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> multiBean.results.size() > 5);

        Assertions.assertThat(multiBean.results).contains(2, 4, 6, 8, 10, 12);
    }
}
