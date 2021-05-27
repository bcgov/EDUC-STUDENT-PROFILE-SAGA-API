package ca.bc.gov.educ.api.student.profile.saga.filter;

import org.springframework.data.jpa.domain.Specification;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;

/**
 * this is the generic class to support all kind of filter specifications for different entities
 *
 * @param <R> the entity type.
 * @author Om
 */
public abstract class BaseFilterSpecs<R> {

  private final FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications;
  private final FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final FilterSpecifications<R, Integer> integerFilterSpecifications;
  private final FilterSpecifications<R, String> stringFilterSpecifications;
  private final FilterSpecifications<R, Long> longFilterSpecifications;
  private final FilterSpecifications<R, UUID> uuidFilterSpecifications;
  private final Converters converters;

  /**
   * Instantiates a new Base filter specs.
   *
   * @param dateFilterSpecifications     the date filter specifications
   * @param dateTimeFilterSpecifications the date time filter specifications
   * @param integerFilterSpecifications  the integer filter specifications
   * @param stringFilterSpecifications   the string filter specifications
   * @param longFilterSpecifications     the long filter specifications
   * @param uuidFilterSpecifications     the uuid filter specifications
   * @param converters                   the converters
   */
  protected BaseFilterSpecs(final FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications, final FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, final FilterSpecifications<R, Integer> integerFilterSpecifications, final FilterSpecifications<R, String> stringFilterSpecifications, final FilterSpecifications<R, Long> longFilterSpecifications, final FilterSpecifications<R, UUID> uuidFilterSpecifications, final Converters converters) {
    this.dateFilterSpecifications = dateFilterSpecifications;
    this.dateTimeFilterSpecifications = dateTimeFilterSpecifications;
    this.integerFilterSpecifications = integerFilterSpecifications;
    this.stringFilterSpecifications = stringFilterSpecifications;
    this.longFilterSpecifications = longFilterSpecifications;
    this.uuidFilterSpecifications = uuidFilterSpecifications;
    this.converters = converters;
  }

  /**
   * Gets date type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date type specification
   */
  public Specification<R> getDateTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(ChronoLocalDate.class), this.dateFilterSpecifications);
  }

  /**
   * Gets date time type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date time type specification
   */
  public Specification<R> getDateTimeTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(ChronoLocalDateTime.class), this.dateTimeFilterSpecifications);
  }

  /**
   * Gets integer type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the integer type specification
   */
  public Specification<R> getIntegerTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(Integer.class), this.integerFilterSpecifications);
  }

  /**
   * Gets long type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the long type specification
   */
  public Specification<R> getLongTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(Long.class), this.longFilterSpecifications);
  }

  /**
   * Gets string type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the string type specification
   */
  public Specification<R> getStringTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(String.class), this.stringFilterSpecifications);
  }

  /**
   * Gets uuid type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the uuid type specification
   */
  public Specification<R> getUUIDTypeSpecification(final String fieldName, final String filterValue, final FilterOperation filterOperation) {
    return this.getSpecification(fieldName, filterValue, filterOperation, this.converters.getFunction(UUID.class), this.uuidFilterSpecifications);
  }

  private <T extends Comparable<T>> Specification<R> getSpecification(final String fieldName,
                                                                      final String filterValue,
                                                                      final FilterOperation filterOperation,
                                                                      final Function<String, T> converter,
                                                                      final FilterSpecifications<R, T> specifications) {
    final FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
