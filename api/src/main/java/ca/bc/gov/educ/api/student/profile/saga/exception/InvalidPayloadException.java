package ca.bc.gov.educ.api.student.profile.saga.exception;

import ca.bc.gov.educ.api.student.profile.saga.exception.errors.ApiError;
import lombok.Getter;

@SuppressWarnings("squid:S1948")
public class InvalidPayloadException extends RuntimeException {

  private static final long serialVersionUID = 621453241303770042L;
  @Getter
  private final ApiError error;

  public InvalidPayloadException(final ApiError error) {
    super(error.getMessage());
    this.error = error;
  }
}
