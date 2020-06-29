package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenReqEmailSagaData {
  private String firstName;
  private String emailAddress;
  private String identityType;
  private String rejectionReason;
}
