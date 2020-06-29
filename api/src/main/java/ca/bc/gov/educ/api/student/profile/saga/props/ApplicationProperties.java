package ca.bc.gov.educ.api.student.profile.saga.props;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class ApplicationProperties {

  @Value("${nats.streaming.server.url}")
  private String natsUrl;

  @Value("${nats.streaming.server.clusterId}")
  private String natsClusterId;
}
