package ca.bc.gov.educ.api.student.profile.saga.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Filter Criteria Holder
 *
 * @param <T> is the java type of the DB table column
 * @author om
 */
public class FilterCriteria<T extends Comparable<T>> {

  /**
   * Holds the operation {@link FilterOperation}
   */
  private final FilterOperation operation;

  /**
   * Table column name
   */
  private final String fieldName;

  /**
   * Holds the Function to convertString to <T>
   */
  private final Function<String, T> converterFunction;
  /**
   * Holds the filter criteria
   */
  private final Collection<String> originalValues;
  /**
   * Holds the filter criteria as type <T>
   */
  private final Collection<T> convertedValues;
  /**
   * Converted value
   */
  private T convertedSingleValue;
  /**
   * minimum value - application only for {@link FilterOperation#BETWEEN}
   */
  private T minValue;
  /**
   * maximum value - application only for {@link FilterOperation#BETWEEN}
   */
  private T maxValue;

  /**
   * Instantiates a new Filter criteria.
   *
   * @param fieldName         the field name
   * @param fieldValue        the field value
   * @param filterOperation   the filter operation
   * @param converterFunction the converter function
   */
  public FilterCriteria(@NonNull final String fieldName, final String fieldValue, @NonNull final FilterOperation filterOperation, final Function<String, T> converterFunction) {

    this.fieldName = fieldName;
    this.converterFunction = converterFunction;

    final String[] operationValues;

    if (fieldValue != null) {
      // Split the fieldValue value as comma separated.
      operationValues = StringUtils.split(fieldValue, ",");
    } else {
      operationValues = new String[]{null};
    }

    if (operationValues.length < 1) {
      throw new IllegalArgumentException("field value can't be empty");
    }
    this.operation = filterOperation;
    this.originalValues = Arrays.asList(operationValues);
    this.convertedValues = new ArrayList<>();

    // Validate other conditions
    this.validateAndAssign(operationValues);

  }

  private void validateAndAssign(final String[] operationValues) {

    //For operation 'btn'
    if (FilterOperation.BETWEEN == this.operation) {
      if (operationValues.length != 2) {
        throw new IllegalArgumentException("For 'btn' operation two values are expected");
      } else {

        //Convert
        final T value1 = this.converterFunction.apply(operationValues[0]);
        final T value2 = this.converterFunction.apply(operationValues[1]);

        //Set min and max values
        if (value1.compareTo(value2) > 0) {
          this.minValue = value2;
          this.maxValue = value1;
        } else {
          this.minValue = value1;
          this.maxValue = value2;
        }
      }

      //For 'in' or 'nin' operation
    } else if (FilterOperation.IN == this.operation || FilterOperation.NOT_IN == this.operation) {
      this.convertedValues.addAll(this.originalValues.stream().map(this.converterFunction).collect(Collectors.toList()));
    } else {
      //All other operation
      this.convertedSingleValue = this.converterFunction.apply(operationValues[0]);
    }

  }

  /**
   * Gets converted single value.
   *
   * @return the converted single value
   */
  public T getConvertedSingleValue() {
    return this.convertedSingleValue;
  }

  /**
   * Gets min value.
   *
   * @return the min value
   */
  public T getMinValue() {
    return this.minValue;
  }

  /**
   * Gets max value.
   *
   * @return the max value
   */
  public T getMaxValue() {
    return this.maxValue;
  }

  /**
   * Gets operation.
   *
   * @return the operation
   */
  public FilterOperation getOperation() {
    return this.operation;
  }

  /**
   * Gets field name.
   *
   * @return the field name
   */
  public String getFieldName() {
    return this.fieldName;
  }

  /**
   * Gets converter function.
   *
   * @return the converter function
   */
  public Function<String, T> getConverterFunction() {
    return this.converterFunction;
  }

  /**
   * Gets original values.
   *
   * @return the original values
   */
  public Collection<String> getOriginalValues() {
    return this.originalValues;
  }

  /**
   * Gets converted values.
   *
   * @return the converted values
   */
  public Collection<T> getConvertedValues() {
    return this.convertedValues;
  }

}
