package ca.bc.gov.educ.api.student.profile.saga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "SAGA_EVENT")
@DynamicUpdate
public class SagaEvent {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "saga_event_id", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sagaEventId;

  @ManyToOne
  @JoinColumn(name = "saga_id", updatable = false, columnDefinition = "BINARY(16)")
  private Saga saga;

  @NotNull(message = "saga_event_state cannot be null")
  @Column(name = "saga_event_state")
  private String sagaEventState;

  @NotNull(message = "saga_event_outcome cannot be null")
  @Column(name = "saga_event_outcome")
  private String sagaEventOutcome;

  @NotNull(message = "saga_step_number cannot be null")
  @Column(name = "saga_step_number")
  private Integer sagaStepNumber;

  @Column(name = "saga_event_response")
  private String sagaEventResponse;

  @NotNull(message = "create user cannot be null")
  @Column(name = "create_user", updatable = false)
  @Size(max = 32)
  private String createUser;

  @NotNull(message = "update user cannot be null")
  @Column(name = "update_user")
  @Size(max = 32)
  private String updateUser;

  @PastOrPresent
  @Column(name = "create_date")
  private LocalDateTime createDate;

  @PastOrPresent
  @Column(name = "update_date")
  private LocalDateTime updateDate;
}
