package ca.bc.gov.educ.api.student.profile.saga.exception;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class EntityNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 4413979549737000974L;

  public EntityNotFoundException(Class<?> clazz, String... searchParamsMap) {
    super(EntityNotFoundException.generateMessage(clazz.getSimpleName(),
      ExceptionUtils.toMap(String.class, String.class, (Object[]) searchParamsMap)));
  }

  private static String generateMessage(String entity, Map<String, String> searchParams) {
    return StringUtils.capitalize(entity) +
      " was not found for parameters " +
      searchParams;
  }
}
