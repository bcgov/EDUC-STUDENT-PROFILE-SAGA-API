package ca.bc.gov.educ.api.student.profile.saga.struct.ump;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StudentProfileComments {
  String studentRequestID;
  String staffMemberIDIRGUID;
  String staffMemberName;
  String commentContent;
  String commentTimestamp;
  String createUser;
  String updateUser;
}
