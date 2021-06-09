package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.helpers.LogHelper;
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
@SuppressWarnings("java:S2142")
public class MessageSubscriber {


  private final Connection connection;

  @Autowired
  public MessageSubscriber(final Connection con, final List<SagaEventHandler> sagaEventHandlers) {
    this.connection = con;
    sagaEventHandlers.forEach(handler -> this.subscribe(handler.getTopicToSubscribe(), handler));
  }

  public void subscribe(final String topic, final SagaEventHandler eventHandler) {
    final String queue = topic.replace("_", "-");
    final var dispatcher = this.connection.createDispatcher(this.onMessage(eventHandler));
    dispatcher.subscribe(topic, queue);
  }

  /**
   * On message message handler.
   *
   * @return the message handler
   */
  private MessageHandler onMessage(final SagaEventHandler eventHandler) {
    return (Message message) -> {
      if (message != null) {
        try {
          final var eventString = new String(message.getData());
          LogHelper.logMessagingEventDetails(eventString);
          final var event = JsonUtil.getJsonObjectFromString(Event.class, eventString);
          eventHandler.executeSagaEvent(event);
        } catch (final Exception e) {
          log.error("Exception ", e);
        }
      }
    };
  }


}
