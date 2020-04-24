package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentProfileEmailSagaData {
  private String firstName;
  private String emailAddress;
}
