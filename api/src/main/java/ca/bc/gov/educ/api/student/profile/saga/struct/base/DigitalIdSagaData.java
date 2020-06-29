package ca.bc.gov.educ.api.student.profile.saga.struct.base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DigitalIdSagaData {
  private String digitalID;
  private String studentID;
  private String updateUser;
  private String identityTypeCode;
  private String identityValue;
  private String lastAccessDate;
  private String lastAccessChannelCode;
}
