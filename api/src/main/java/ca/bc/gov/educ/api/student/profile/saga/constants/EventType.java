package ca.bc.gov.educ.api.student.profile.saga.constants;

public enum EventType {
  GET_STUDENT,
  CREATE_STUDENT,
  UPDATE_STUDENT,
  UPDATE_DIGITAL_ID,
  UPDATE_STUDENT_PROFILE,
  NOTIFY_STUDENT_PROFILE_REQUEST_COMPLETE,
  MARK_SAGA_COMPLETE,
  INITIATED,
  ADD_STUDENT_PROFILE_COMMENT,
  GET_DIGITAL_ID,
  GET_STUDENT_PROFILE,
  NOTIFY_STUDENT_PROFILE_REQUEST_REJECT,
  NOTIFY_STUDENT_PROFILE_REQUEST_RETURN,
  UPDATE_PEN_REQUEST,
  NOTIFY_STUDENT_PEN_REQUEST_COMPLETE,
  NOTIFY_STUDENT_PEN_REQUEST_RETURN,
  NOTIFY_STUDENT_PEN_REQUEST_REJECT,
  ADD_PEN_REQUEST_COMMENT,
  GET_PEN_REQUEST,
  UNLINK_PEN_REQUEST
}
