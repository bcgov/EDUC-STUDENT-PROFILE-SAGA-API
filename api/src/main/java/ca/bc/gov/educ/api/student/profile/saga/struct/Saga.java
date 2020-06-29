package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.Data;



@Data
public class Saga {
  private String sagaId;
  private String sagaName;
  private String sagaState;
  private String payload;
  private String status;
  private Boolean sagaCompensated;
}
