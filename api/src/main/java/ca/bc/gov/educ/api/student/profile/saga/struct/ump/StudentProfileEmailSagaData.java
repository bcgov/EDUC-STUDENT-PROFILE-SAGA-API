package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileEmailSagaData {
  private String firstName;
  private String emailAddress;
  private String identityType;
  private String rejectionReason;
}
