package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.orchestrator.base.SagaEventHandler;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.Event;
import ca.bc.gov.educ.api.student.profile.saga.utils.JsonUtil;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class MessageSubscriber {


  private final Connection connection;

  @Autowired
  public MessageSubscriber(final Connection con, List<SagaEventHandler> sagaEventHandlers) {
    this.connection = con;
    sagaEventHandlers.forEach(handler -> subscribe(handler.getTopicToSubscribe(), handler));
  }

  public void subscribe(String topic, SagaEventHandler eventHandler) {
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
        try {
          var eventString = new String(message.getData());
          var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.executeSagaEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }


}
