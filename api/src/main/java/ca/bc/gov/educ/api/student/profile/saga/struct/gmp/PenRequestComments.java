package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

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
public class PenRequestComments {
  String penRetrievalRequestID;
  String staffMemberIDIRGUID;
  String staffMemberName;
  String commentContent;
  String commentTimestamp;
  String createUser;
  String updateUser;
}
