package ca.bc.gov.educ.api.student.profile.saga.props;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationProperties {
  public static final String CORRELATION_ID = "correlationID";
  @Value("${nats.server}")
  private String server;

  @Value("${nats.maxReconnect}")
  private int maxReconnect;

  @Value("${nats.connectionName}")
  private String connectionName;
}
