package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import jakarta.validation.constraints.NotNull;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class PenRequestRejectSagaData extends PenRequestActionsSagaData {
  @NotNull
  String rejectionReason;
}
