package ca.bc.gov.educ.api.student.profile.saga.struct;

import lombok.Data;
import lombok.Getter;


@Data
@Getter
public class Saga {
  private String sagaId;
  private String sagaName;
  private String sagaState;
  private String payload;
  private String status;
  private Boolean sagaCompensated;
  private String profileRequestId;
  private String penRequestId;
  private Integer retryCount;
  String createUser;
  String updateUser;
  String createDate;
  String updateDate;
}
