package com.mipt.team4.cloud_storage_backend.utils.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import org.junit.jupiter.api.Test;

class ValidatorsTest {
  @Test
  public void notNull_ShouldInvalid_WhenStringIsNull() {
    assertInvalid(Validators.notNull("String", null));
  }

  @Test
  public void notNull_ShouldValid_WhenStringIsNotNull() {
    assertValid(Validators.notNull("String", "some string"));
  }

  @Test
  public void notBlank_ShouldInvalid_WhenStringIsNull() {
    assertInvalid(Validators.notBlank("String", (String) null));
  }

  @Test
  public void notBlank_ShouldInvalid_WhenStringIsEmpty() {
    assertInvalid(Validators.notBlank("String", ""));
  }

  @Test
  public void notBlank_ShouldValid_WhenStringIsNotBlank() {
    assertValid(Validators.notBlank("String", "some string"));
  }

  @Test
  public void notBlank_ShouldInvalid_WhenListIsNull() {
    assertInvalid(Validators.notBlank("List", (List<String>) null));
  }

  @Test
  public void notBlank_ShouldInvalid_WhenListIsEmpty() {
    assertInvalid(Validators.notBlank("List", new ArrayList<>()));
  }

  @Test
  public void notBlank_ShouldValid_WhenListIsNotBlank() {
    assertValid(Validators.notBlank("List", Arrays.asList("some string")));
  }

  @Test
  public void lengthRange_ShouldThrowException_WhenMinLengthMoreThanMaxLength() {
    assertThrows(
        IllegalArgumentException.class, () -> Validators.lengthRange("String", "1234", 100, 1));
  }

  @Test
  public void lengthRange_ShouldInvalid_WhenStringIsTooLarge() {
    assertInvalid(Validators.lengthRange("String", "1234", 2, 3));
  }

  @Test
  public void lengthRange_ShouldInvalid_WhenStringIsTooSmall() {
    assertInvalid(Validators.lengthRange("String", "1", 2, 5));
  }

  @Test
  public void lengthRange_ShouldValid_WhenStringSizeInRange() {
    assertValid(Validators.lengthRange("String", "123", 1, 3));
  }

  @Test
  public void maxLength_ShouldInvalid_WhenStringIsTooLarge() {
    assertInvalid(Validators.maxLength("String", "1234", 3));
  }

  @Test
  public void maxLength_ShouldValid_WhenStringSizeLessThanMax() {
    assertValid(Validators.maxLength("String", "1234", 5));
  }

  @Test
  public void minLength_ShouldInvalid_WhenStringIsTooSmall() {
    assertInvalid(Validators.minLength("String", "1", 2));
  }

  @Test
  public void minLength_ShouldValid_WhenStringSizeGreaterThanMin() {
    assertValid(Validators.minLength("String", "1234", 3));
  }

  @Test
  public void pattern_ShouldInvalid_WhenStringNotMatchPattern() {
    assertInvalid(Validators.pattern("String", "A", "\\d"));
  }

  @Test
  public void pattern_ShouldValid_WhenStringMatchPattern() {
    assertValid(Validators.pattern("String", "1", "\\d"));
  }

  @Test
  public void numberRange_ShouldThrowException_WhenMinNumberMoreThanMaxNumber() {
    assertThrows(IllegalArgumentException.class, () -> Validators.numberRange("Int", 123, 100, 1));
  }

  @Test
  public void numberRange_ShouldInvalid_WhenIntIsTooLarge() {
    assertInvalid(Validators.numberRange("Int", 5, 1, 2));
  }

  @Test
  public void numberRange_ShouldInvalid_WhenIntIsTooSmall() {
    assertInvalid(Validators.numberRange("Int", 1, 2, 5));
  }

  @Test
  public void numberRange_ShouldValid_WhenIntInRange() {
    assertValid(Validators.numberRange("Int", 2, 1, 3));
  }

  @Test
  public void numberMax_ShouldInvalid_WhenIntIsTooLarge() {
    assertInvalid(Validators.numberMax("Int", 100, 1));
  }

  @Test
  public void numberMax_ShouldInvalid_WhenIntLessThanMax() {
    assertValid(Validators.numberMax("Int", 1, 100));
  }

  @Test
  public void numberMin_ShouldInvalid_WhenIntIsTooSmall() {
    assertInvalid(Validators.numberMin("Int", 1, 100));
  }

  @Test
  public void numberMin_ShouldInvalid_WhenIntGreaterThanMin() {
    assertValid(Validators.numberMin("Int", 100, 1));
  }

  @Test
  public void cannotBeNegative_ShouldInvalid_WhenIntIsNegative() {
    assertInvalid(Validators.cannotBeNegative("Int", -1));
  }

  @Test
  public void cannotBeNegative_ShouldValid_WhenIntIsPositive() {
    assertValid(Validators.cannotBeNegative("Int", 1));
  }

  @Test
  public void cannotBeNegative_ShouldValid_WhenIntIsZero() {
    assertValid(Validators.cannotBeNegative("Int", 0));
  }

  @Test
  public void mustBePositive_ShouldInvalid_WhenIntIsNegative() {
    assertInvalid(Validators.mustBePositive("Int", -1));
  }

  @Test
  public void mustBePositive_ShouldInvalid_WhenIntIsZero() {
    assertInvalid(Validators.mustBePositive("Int", 0));
  }

  @Test
  public void mustBePositive_ShouldValid_WhenIntIsPositive() {
    assertValid(Validators.mustBePositive("Int", 1));
  }

  @Test
  public void isUuid_ShouldInvalid_WhenStringIsNotUuid() {
    assertInvalid(Validators.isUuid("String", "asdsad"));
  }

  @Test
  public void isUuid_ShouldValid_WhenStringIsUuid() {
    assertValid(Validators.isUuid("String", UUID.randomUUID().toString()));
  }

  @Test
  public void shouldMergeSeveralValidators() {
    ValidationResult result = Validators.all(
            Validators.notNull("2", ""),
            Validators.notNull("1", null),
            Validators.notBlank("3", "")
    );

    assertFalse(result.isValid());
    assertEquals(2, result.getErrors().size());
  }

  @Test
  public void shouldThrowException_WhenNotValid() {
    assertThrows(
        ValidationFailedException.class,
        () -> Validators.throwExceptionIfNotValid(new ValidationResult(false, List.of())));
  }

  private void assertValid(ValidationResult result) {
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
  }

  private void assertInvalid(ValidationResult result) {
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());
  }
}
