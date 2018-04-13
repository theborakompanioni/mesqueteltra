package org.tbk.mesqueteltra.moquette.ipfs;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.tbk.mesqueteltra.moquette.config.IpfsableMqttServer;
import org.tbk.mesqueteltra.mqtt.Mqttable;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PublishInternalMqttIpfsSubscriber implements InitializingBean, DisposableBean {

    private final Mqttable ipfsService;
    private final IpfsableMqttServer server;

    private volatile Disposable subscribe;

    public PublishInternalMqttIpfsSubscriber(Mqttable ipfsService, IpfsableMqttServer server) {
        this.ipfsService = requireNonNull(ipfsService);
        this.server = requireNonNull(server);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("starting IPFS->MQTT bridge ...");

        this.subscribe = ipfsService.subscribeToAll()
                .retryWhen(new RetryWithLinearDelay(1_000, 1_000 * 30))
                .doOnError(e -> log.error("IPFS->MQTT bridge error", e))
                .subscribe(msg -> server.internalPublishFromIpfs(msg.getMessage(), msg.getClientId()));
    }

    @Override
    public void destroy() throws Exception {
        log.info("disposing IPFS->MQTT bridge ...");
        this.subscribe.dispose();
    }

    public class RetryWithLinearDelay implements Function<Flux<Throwable>, Publisher<?>> {
        private final int maxRetries;
        private final int retryDelayMillis;
        private final int maxDelayMillis;
        private volatile int retryCount;

        public RetryWithLinearDelay(final int retryDelayMillis, final int maxDelayMillis) {
            this(retryDelayMillis, maxDelayMillis, Integer.MAX_VALUE);
        }

        public RetryWithLinearDelay(final int retryDelayMillis, final int maxDelayMillis, final int maxRetries) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.maxDelayMillis = maxDelayMillis;
            this.retryCount = 0;
        }

        @Override
        public Publisher<?> apply(Flux<Throwable> flux) {
            return flux
                    .flatMap((Function<Throwable, Publisher<?>>) throwable -> {
                        if (++retryCount < maxRetries) {
                            int delayMillis = Math.min(retryCount * retryDelayMillis, maxDelayMillis);
                            // When this publisher calls onNext, the original
                            // Observable will be retried (i.e. re-subscribed).
                            return Flux.just(1)
                                    .delayElements(Duration.of(delayMillis, ChronoUnit.MILLIS));
                        }

                        // Max retries hit. Just pass the error along.
                        return Flux.error(throwable);
                    });
        }
    }
}