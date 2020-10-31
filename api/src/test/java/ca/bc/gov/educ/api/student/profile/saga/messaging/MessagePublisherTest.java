package ca.bc.gov.educ.api.student.profile.saga.messaging;

import ca.bc.gov.educ.api.student.profile.saga.props.ApplicationProperties;
import io.nats.streaming.AckHandler;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static ca.bc.gov.educ.api.student.profile.saga.constants.SagaTopicsEnum.STUDENT_PROFILE_API_TOPIC;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The type Message publisher test.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class MessagePublisherTest {

  /**
   * The Executor service.
   */
  @Mock
  private ExecutorService executorService;
  /**
   * The Connection.
   */
  @Mock
  private StreamingConnection connection;
  /**
   * The Connection factory.
   */
  @Mock
  private StreamingConnectionFactory connectionFactory;
  /**
   * The Application properties.
   */
  @Autowired
  private ApplicationProperties applicationProperties;
  /**
   * The Message publisher.
   */
  private MessagePublisher messagePublisher;

  /**
   * Sets up.
   *
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   */
  @Before
  public void setUp() throws IOException, InterruptedException {
    initMocks(this);
    messagePublisher = new MessagePublisher(applicationProperties, false);
    messagePublisher.setExecutorService(executorService);
    messagePublisher.setConnectionFactory(connectionFactory);
    when(connectionFactory.createConnection()).thenReturn(connection);
    messagePublisher.connect();
  }


  /**
   * Test dispatch message given message should publish.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDispatchMessage_givenMessage_shouldPublish() throws Exception {
    messagePublisher.dispatchMessage(STUDENT_PROFILE_API_TOPIC.toString(), "Test".getBytes());
    verify(connection, atMostOnce()).publish(eq(STUDENT_PROFILE_API_TOPIC.toString()), aryEq("Test".getBytes()), any(AckHandler.class));
  }

  /**
   * Test retry publish given message should publish.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRetryPublish_givenMessage_shouldPublish() throws Exception {
    messagePublisher.retryPublish(STUDENT_PROFILE_API_TOPIC.toString(), "Test".getBytes());
    verify(connection, atMostOnce()).publish(eq(STUDENT_PROFILE_API_TOPIC.toString()), aryEq("Test".getBytes()), any(AckHandler.class));
  }


  /**
   * Test close should close.
   *
   * @throws Exception the exception
   */
  @Test
  public void testClose_shouldClose() throws Exception {
    messagePublisher.close();
    verify(connection, atMostOnce()).close();
  }

  /**
   * Test close given exception should close.
   *
   * @throws Exception the exception
   */
  @Test
  public void testClose_givenException_shouldClose() throws Exception {
    doThrow(new IOException("Test")).when(connection).close();
    messagePublisher.close();
    verify(connection, atMostOnce()).close();
  }

  /**
   * Test on ack given exception should retry publish.
   *
   */
  @Test
  public void testOnAck_givenException_shouldRetryPublish() {
    var ackHandler = messagePublisher.getAckHandler();
    ackHandler.onAck(UUID.randomUUID().toString(), new Exception());
    ackHandler.onAck(UUID.randomUUID().toString(), STUDENT_PROFILE_API_TOPIC.toString(), "Test".getBytes(), null);
    ackHandler.onAck(UUID.randomUUID().toString(), STUDENT_PROFILE_API_TOPIC.toString(), "Test".getBytes(), new Exception());
    verify(executorService, atMostOnce()).execute(any(Runnable.class));
  }

  /**
   * Test connection lost handler given exception should create connection.
   *
   * @throws Exception the exception
   */
  @Test
  public void testConnectionLostHandler_givenException_shouldCreateConnection() throws Exception {
    when(connectionFactory.createConnection()).thenThrow(new IOException("Test")).thenReturn(connection);
    messagePublisher.connectionLostHandler(connection, new Exception());
    verify(connectionFactory, atLeast(3)).createConnection();
  }

}
