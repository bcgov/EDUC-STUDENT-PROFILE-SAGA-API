package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.props.ApplicationProperties;
import io.nats.streaming.AckHandler;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class MessagePublisher {
  private final ExecutorService executorService = Executors.newFixedThreadPool(2);
  private StreamingConnection connection;
  private final StreamingConnectionFactory connectionFactory;

  @Autowired
  public MessagePublisher(final ApplicationProperties applicationProperties) throws IOException, InterruptedException {
    Options.Builder builder = new Options.Builder();
    builder.natsUrl(applicationProperties.getNatsUrl());
    builder.clusterId(applicationProperties.getNatsClusterId());
    builder.clientId("student-profile-saga-publisher" + UUID.randomUUID().toString());
    builder.connectionLostHandler(this::connectionLostHandler);
    Options options = builder.build();
    connectionFactory = new StreamingConnectionFactory(options);
    connection = connectionFactory.createConnection();
  }


  public void dispatchMessage(String subject, byte[] message) throws InterruptedException, TimeoutException, IOException {
    connection.publish(subject, message, getAckHandler());
  }


  @SuppressWarnings("java:S2142")
  private AckHandler getAckHandler() {
    return new AckHandler() {
      @Override
      public void onAck(String guid, Exception err) {
        log.trace("already handled.");
      }

      @Override
      public void onAck(String guid, String subject, byte[] data, Exception ex) {

        if (ex != null) {
          executorService.execute(() -> {
            try {
              retryPublish(subject, data);
            } catch (InterruptedException | TimeoutException | IOException e) {
              log.error("Exception", e);
            }
          });

        } else {
          log.trace("acknowledgement received {}", guid);
        }
      }
    };
  }

  public void retryPublish(String subject, byte[] message) throws InterruptedException, TimeoutException, IOException {
    log.trace("retrying...");
    connection.publish(subject, message, getAckHandler());
  }

  /**
   * This method will keep retrying for a connection.
   */
  @SuppressWarnings("java:S2142")
  private void connectionLostHandler(StreamingConnection streamingConnection, Exception e) {
    if (e != null) {
      int numOfRetries = 1;
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
    }
  }


}
