package ca.bc.gov.educ.api.student.profile.saga.props;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {

  @Value("${nats.streaming.server.url}")
  @Getter
  private String natsUrl;

  @Value("${nats.streaming.server.clusterId}")
  @Getter
  private String natsClusterId;

  @Value("${nats.streaming.server.clientId}")
  @Getter
  private String natsClientId;
}
