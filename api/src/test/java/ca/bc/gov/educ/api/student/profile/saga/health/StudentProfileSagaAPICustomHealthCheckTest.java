package ca.bc.gov.educ.api.student.profile.saga.health;

import ca.bc.gov.educ.api.student.profile.saga.BaseSagaApiTest;
import io.nats.client.Connection;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

public class StudentProfileSagaAPICustomHealthCheckTest extends BaseSagaApiTest {

  @Autowired
  Connection natsConnection;

  @Autowired
  private StudentProfileSagaAPICustomHealthCheck healthCheck;

  @Test
  public void testGetHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    assertThat(this.healthCheck.getHealth(true)).isNotNull();
    assertThat(this.healthCheck.getHealth(true).getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testGetHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    assertThat(this.healthCheck.getHealth(true)).isNotNull();
    assertThat(this.healthCheck.getHealth(true).getStatus()).isEqualTo(Status.UP);
  }


  @Test
  public void testHealth_givenClosedNatsConnection_shouldReturnStatusDown() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CLOSED);
    assertThat(this.healthCheck.health()).isNotNull();
    assertThat(this.healthCheck.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  public void testHealth_givenOpenNatsConnection_shouldReturnStatusUp() {
    when(this.natsConnection.getStatus()).thenReturn(Connection.Status.CONNECTED);
    assertThat(this.healthCheck.health()).isNotNull();
    assertThat(this.healthCheck.health().getStatus()).isEqualTo(Status.UP);
  }
}
