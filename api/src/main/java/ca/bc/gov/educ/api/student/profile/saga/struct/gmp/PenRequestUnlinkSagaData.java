package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenRequestUnlinkSagaData {
  @NotNull(message = "penRetrievalRequestID can not be null")
  private String penRetrievalRequestID;
  @NotNull(message = "digitalID can not be null")
  private String digitalID;
  @NotNull(message = "reviewer can not be null")
  private String reviewer;
  @NotNull(message = "penRequestStatusCode can not be null")
  private String penRequestStatusCode;
  @NotNull(message = "createUser can not be null")
  private String createUser;
  @NotNull(message = "updateUser can not be null")
  private String updateUser;
}
