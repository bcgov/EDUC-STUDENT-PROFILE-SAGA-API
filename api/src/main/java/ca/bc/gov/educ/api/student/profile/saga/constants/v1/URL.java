package ca.bc.gov.educ.api.student.profile.saga.constants.v1;

public final class URL {
  public static final String PEN_REQUEST_COMPLETE_SAGA = "/pen-request-complete-saga";
  public static final String PEN_REQUEST_COMMENT_SAGA = "/pen-request-comment-saga";
  public static final String PEN_REQUEST_RETURN_SAGA = "/pen-request-return-saga";
  public static final String PEN_REQUEST_REJECT_SAGA = "/pen-request-reject-saga";
  public static final String PEN_REQUEST_UNLINK_SAGA = "/pen-request-unlink-saga";
  public static final String STUDENT_PROFILE_COMPLETE_SAGA = "/student-profile-complete-saga";
  public static final String STUDENT_PROFILE_COMMENT_SAGA = "/student-profile-comment-saga";
  public static final String STUDENT_PROFILE_REJECT_SAGA = "/student-profile-reject-saga";
  public static final String STUDENT_PROFILE_RETURN_SAGA = "/student-profile-return-saga";
  public static final String SAGA_ID = "/{sagaID}";
  public static final String SAGA_EVENTS = "/events";
  public static final String PAGINATED = "/paginated";
  public static final String BASE_URL = "/api/v1/student-profile-saga";

  private URL() {

  }
}
