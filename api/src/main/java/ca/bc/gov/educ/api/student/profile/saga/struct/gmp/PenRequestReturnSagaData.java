package ca.bc.gov.educ.api.student.profile.saga.struct.gmp;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@EqualsAndHashCode(callSuper = true)
@Data
public class PenRequestReturnSagaData extends PenRequestActionsSagaData{
    @NotNull(message = "Comment content can not be null")
    String commentContent;
    @Pattern(regexp = "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})")
    @Size(max = 19)
    @NotNull(message = "commentTimestamp can not be null")
    String commentTimestamp;
}
