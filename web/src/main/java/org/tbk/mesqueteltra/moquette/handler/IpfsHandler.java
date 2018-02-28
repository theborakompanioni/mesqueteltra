package org.tbk.mesqueteltra.moquette.handler;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.RateLimiter;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish;
import reactor.core.scheduler.Schedulers;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsHandler implements ServerWithInternalPublish.InterceptHandlerWithInternalMessageSupport {

    private final IpfsService ipfsService;

    // we do not want to spam the network - its a demo!
    private final RateLimiter rateLimiter = RateLimiter.create(0.2f);

    public IpfsHandler(IpfsService ipfsService) {
        this.ipfsService = requireNonNull(ipfsService);
    }

    @Override
    public String getID() {
        return "IpfsHandler";
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {

    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        final String content = msg.getPayload().toString(Charsets.UTF_8);
        doOnPublish(msg.getTopicName(), content);


    }

    @Override
    public void onInternalPublish(ServerWithInternalPublish.InterceptInternalPublishedMessage msg) {
        final String content = msg.getMsg().content().toString(Charsets.UTF_8);
        doOnPublish(msg.getTopicName(), content);
    }


    private void doOnPublish(String topic, String content) {
        if (rateLimiter.tryAcquire()) {
            ipfsService.publish(topic, content)
                    .subscribeOn(Schedulers.elastic())
                    .subscribe();
        }
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
    }
}