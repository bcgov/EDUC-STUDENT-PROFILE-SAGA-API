package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.SagaEventHandler;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;


@Component
@Slf4j
public class MessageSubscriber extends MessagePubSub {

  @Getter(PRIVATE)
  private final Map<String, SagaEventHandler> handlers = new HashMap<>();

  @Autowired
  public MessageSubscriber(final Connection con) {
    super.connection = con;
  }

  public void subscribe(String topic, SagaEventHandler eventHandler) {
    if(!handlers.containsKey(topic)){
      handlers.put(topic, eventHandler);
    }
    String queue = topic.replace("_", "-");
    var dispatcher = connection.createDispatcher(onMessage(eventHandler));
    dispatcher.subscribe(topic, queue);
  }

  /**
   * On message message handler.
   *
   * @return the message handler
   */
  private MessageHandler onMessage(SagaEventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        log.info("Message received is :: {} ", message);
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


}
