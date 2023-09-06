package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PenRequestCommentsSagaData {
  @NotNull(message = "penRetrievalRequestID content can not be null")
  String penRetrievalRequestID;
  @NotNull(message = "Comment content can not be null")
  String commentContent;
  @Pattern(regexp = "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})")
  @Size(max = 19)
  @NotNull(message = "commentTimestamp can not be null")
  String commentTimestamp;
  @NotNull(message = "penRequestStatusCode can not be null")
  String penRequestStatusCode;
  @NotNull(message = "createUser can not be null")
  String createUser;
  @NotNull(message = "updateUser can not be null")
  String updateUser;
}
