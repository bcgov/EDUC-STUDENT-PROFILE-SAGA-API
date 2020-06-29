package ca.bc.gov.educ.api.student.profile.saga.struct.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CompleteEvent extends Event{
  private String sagaStatus;
}
