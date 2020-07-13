package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.exception.SagaRuntimeException;
import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.SagaEventHandler;
import ca.bc.gov.educ.api.student.profile.saga.props.ApplicationProperties;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import io.nats.streaming.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

/**
 * This listener uses durable queue groups of nats streaming client. A durable
 * queue group allows you to have all members leave but still maintain state.
 * When a member re-joins, it starts at the last position in that group. <b>DO
 * NOT call unsubscribe on the subscription.</b> please see the below for
 * details. Closing the Group The last member calling Unsubscribe will close
 * (that is destroy) the group. So if you want to maintain durability of the
 * group, <b>you should not be calling Unsubscribe.</b>
 * <p>
 * So unlike for non-durable queue subscribers, it is possible to maintain a
 * queue group with no member in the server. When a new member re-joins the
 * durable queue group, it will resume from where the group left of, actually
 * first receiving all unacknowledged messages that may have been left when the
 * last member previously left.
 */
@Component
@Slf4j
@SuppressWarnings("java:S2142")
public class MessageSubscriber {

  private StreamingConnection connection;
  private final StreamingConnectionFactory connectionFactory;
  @Getter(PRIVATE)
  private final Map<String, SagaEventHandler> handlers = new HashMap<>();

  @Autowired
  public MessageSubscriber(final ApplicationProperties applicationProperties) throws IOException, InterruptedException {
    Options options = new Options.Builder().natsUrl(applicationProperties.getNatsUrl())
        .clusterId(applicationProperties.getNatsClusterId())
        .clientId("student-profile-saga-subscriber-" + UUID.randomUUID().toString())
        .connectionLostHandler(this::connectionLostHandler).build();
    connectionFactory = new StreamingConnectionFactory(options);
    connection = connectionFactory.createConnection();
  }

  public void subscribe(String topic, SagaEventHandler eventHandler) {
    if(!handlers.containsKey(topic)){
      handlers.put(topic, eventHandler);
    }

    String queue = topic.replace("_", "-");
    SubscriptionOptions options = new SubscriptionOptions.Builder().durableName(queue + "-consumer").build();// ":" is not allowed in durable name by NATS.
    try {
      connection.subscribe(topic, queue, onMessage(eventHandler), options);
    } catch (IOException | InterruptedException | TimeoutException e) {
      throw new SagaRuntimeException(e);
    }
  }

  private MessageHandler onMessage(SagaEventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.trace("Message received is :: {} ", message);
        try {
          var eventString = new String(message.getData());
          var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.onSagaEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }

  /**
   * This method will keep retrying for a connection.
   */

  private void connectionLostHandler(StreamingConnection streamingConnection, Exception e) {
    if (e != null) {
      int numOfRetries = 1;
      numOfRetries = retryConnection(numOfRetries);
      retrySubscription(numOfRetries);
    }
  }

  private void retrySubscription(int numOfRetries) {
    while (true) {
      try {
        log.trace("retrying subscription as connection was lost :: retrying ::" + numOfRetries++);
        for (Map.Entry<String, SagaEventHandler> entry : handlers.entrySet()) {
          this.subscribe(entry.getKey(), entry.getValue());
        }
        log.info("successfully resubscribed after {} attempts", numOfRetries);
        break;
      } catch (SagaRuntimeException exception) {
        log.error("exception occurred while retrying subscription", exception);
        try {
          double sleepTime = (2 * numOfRetries);
          TimeUnit.SECONDS.sleep((long) sleepTime);
        } catch (InterruptedException exc) {
          log.error("InterruptedException occurred while retrying subscription", exc);
        }
      }
    }
  }

  private int retryConnection(int numOfRetries) {
    while (true) {
      try {
        log.trace("retrying connection as connection was lost :: retrying ::" + numOfRetries++);
        connection = connectionFactory.createConnection();
        log.info("successfully reconnected after {} attempts", numOfRetries);
        break;
      } catch (IOException | InterruptedException ex) {
        log.error("exception occurred", ex);
        try {
          double sleepTime = (2 * numOfRetries);
          TimeUnit.SECONDS.sleep((long) sleepTime);
        } catch (InterruptedException exc) {
          log.error("exception occurred", exc);
        }
      }
    }
    return numOfRetries;
  }
}
