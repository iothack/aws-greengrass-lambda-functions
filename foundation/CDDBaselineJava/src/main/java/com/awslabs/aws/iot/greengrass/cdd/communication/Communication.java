package com.awslabs.aws.iot.greengrass.cdd.communication;

import com.amazonaws.greengrass.javasdk.model.GGIotDataException;
import com.amazonaws.greengrass.javasdk.model.GGLambdaException;
import com.awslabs.aws.iot.greengrass.cdd.events.*;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public interface Communication {
    Pattern builderPattern = Pattern.compile("([^.$]+)\\$\\1Builder$");

    default void register(Object object) {
        getEventBus().register(object);
    }

    @Subscribe
    default void catchall(Object object) {
        failOnBuilder(object);
    }

    @Subscribe
    default void publishObjectEvent(PublishObjectEvent publishObjectEvent) {
        try {
            publish(publishObjectEvent.getTopic(), publishObjectEvent.getObject());
        } catch (Exception e) {
            // Do not throw exceptions in event bus subscriber methods, but publish the exception so debugging is easier
            publishMessageEvent(ImmutablePublishMessageEvent.builder().topic(publishObjectEvent.getTopic()).message("Publish or serialization of object failed [" + e.getMessage() + "]").build());
        }
    }

    @Subscribe
    default void publishMessageEvent(PublishMessageEvent publishMessageEvent) {
        LoggerFactory.getLogger(Communication.class).info("Publish message event #3");
        try {
            Map map = new HashMap<>();
            LoggerFactory.getLogger(Communication.class).info("Publish message event #4");
            map.put("message", publishMessageEvent.getMessage());
            LoggerFactory.getLogger(Communication.class).info("Publish message event #5");
            publish(publishMessageEvent.getTopic(), map);
            LoggerFactory.getLogger(Communication.class).info("Publish message event #6");
        } catch (Exception e) {
            // Do not throw exceptions in event bus subscriber methods
            LoggerFactory.getLogger(Communication.class).info("Publish message event #7 [" + e.getMessage() + "]");
        }
    }

    @Subscribe
    default void publishBinaryEvent(PublishBinaryEvent publishBinaryEvent) {
        try {
            publish(publishBinaryEvent.getTopic(), publishBinaryEvent.getBytes());
        } catch (Exception e) {
            // Do not throw exceptions in event bus subscriber methods
        }
    }

    @Subscribe
    default void deadEvent2(DeadEvent deadEvent) {
        LoggerFactory.getLogger(Communication.class).info("Dead event #2");
        LoggerFactory.getLogger(Communication.class).info("Dead event #2 data [" + deadEvent.getEvent() + ", " + deadEvent.getSource() + "]");
    }

    EventBus getEventBus();

    default void publishObjectEvent(String topic, Object object) {
        getEventBus().post(ImmutablePublishObjectEvent.builder().topic(topic).object(object).build());
    }

    default void publishMessageEvent(String topic, String message) {
        LoggerFactory.getLogger(Communication.class).info("Publish message event #1");
        getEventBus().post(ImmutablePublishMessageEvent.builder().topic(topic).message(message).build());
        LoggerFactory.getLogger(Communication.class).info("Publish message event #2");
    }

    default void publishBinaryEvent(String topic, byte[] bytes) {
        getEventBus().post(ImmutablePublishBinaryEvent.builder().topic(topic).bytes(bytes).build());
    }

    void publish(String topic, Object message) throws GGIotDataException, GGLambdaException;

    void publish(String topic, byte[] bytes) throws GGIotDataException, GGLambdaException;

    default void failOnBuilder(Object object) {
        if (isBuilder(object)) {
            publishMessageEvent(ImmutablePublishMessageEvent.builder().topic("debug").message("Attempted to publish a builder [" + object.getClass().getTypeName() + "]").build());
        }
    }

    default boolean isBuilder(Object object) {
        return builderPattern.matcher(object.getClass().getTypeName()).find();
    }
}
