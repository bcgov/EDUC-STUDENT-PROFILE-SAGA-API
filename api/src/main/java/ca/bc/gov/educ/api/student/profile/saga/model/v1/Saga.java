package ca.bc.gov.educ.api.student.profile.saga.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "STUDENT_PROFILE_SAGA")
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class Saga {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
    @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SAGA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sagaId;

  @NotNull(message = "saga name cannot be null")
  @Column(name = "SAGA_NAME")
  private String sagaName;

  @NotNull(message = "saga state cannot be null")
  @Column(name = "SAGA_STATE")
  private String sagaState;

  @NotNull(message = "payload cannot be null")
  @Column(name = "PAYLOAD", length = 10485760)
  private String payload;

  @NotNull(message = "status cannot be null")
  @Column(name = "STATUS")
  private String status;

  @NotNull(message = "sagaCompensated cannot be null")
  @Column(name = "SAGA_COMPENSATED")
  private Boolean sagaCompensated;

  @NotNull(message = "create user cannot be null")
  @Column(name = "CREATE_USER", updatable = false)
  @Size(max = 100)
  private String createUser;

  @NotNull(message = "update user cannot be null")
  @Column(name = "UPDATE_USER")
  @Size(max = 100)
  private String updateUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  private LocalDateTime updateDate;

  @Column(name = "PROFILE_REQUEST_ID", columnDefinition = "BINARY(16)")
  private UUID profileRequestId;

  @Column(name = "PEN_REQUEST_ID", columnDefinition = "BINARY(16)")
  private UUID penRequestId;

  @Column(name = "RETRY_COUNT")
  private Integer retryCount;
}
