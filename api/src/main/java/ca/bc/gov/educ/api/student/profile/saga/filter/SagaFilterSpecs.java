package ca.bc.gov.educ.api.student.profile.saga.filter;

import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

/**
 * The type Student filter specs.
 */
@Service
@Slf4j
public class SagaFilterSpecs extends BaseFilterSpecs<Saga> {

  /**
   * Instantiates a new Student filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  public SagaFilterSpecs(final FilterSpecifications<Saga, ChronoLocalDate> dateFilterSpecifications, final FilterSpecifications<Saga, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, final FilterSpecifications<Saga, Integer> integerFilterSpecifications, final FilterSpecifications<Saga, String> stringFilterSpecifications, final FilterSpecifications<Saga, Long> longFilterSpecifications, final FilterSpecifications<Saga, UUID> uuidFilterSpecifications, final Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, converters);
  }
}
