package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class StudentProfileComments {
  String penRetrievalReqCommentID;
  String penRetrievalRequestID;
  String staffMemberIDIRGUID;
  String staffMemberName;
  String commentContent;
  String commentTimestamp;
  String createUser;
  String updateUser;
}
