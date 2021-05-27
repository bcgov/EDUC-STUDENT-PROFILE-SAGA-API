package ca.bc.gov.educ.api.student.profile.saga.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
  public static final ObjectMapper objectMapper = new ObjectMapper();

  private JsonUtil() {
  }

  public static String getJsonStringFromObject(final Object payload) throws JsonProcessingException {
    return objectMapper.writeValueAsString(payload);
  }

  public static <T> T getJsonObjectFromString(final Class<T> clazz, final String payload) throws JsonProcessingException {
    return objectMapper.readValue(payload, clazz);
  }
}
