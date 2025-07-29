package rest.felix.back.todo.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import rest.felix.back.common.util.NullableField;
import rest.felix.back.common.util.NullableField.Absent;
import rest.felix.back.common.util.NullableField.Present;

class UpdateTodoMetadataRequestDTOTest {
  private static Validator validator;

  // 테스트 클래스가 로드될 때 Validator를 한 번만 설정
  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  private static Stream<Arguments> atLeastOneExists() {
    return Stream.of(
        Arguments.of(new Present<>(true), new Absent<>(), new Absent<>()),
        Arguments.of(new Absent<>(), new Present<>(LocalDate.now()), new Absent<>()),
        Arguments.of(new Present<>(false), new Absent<>(), new Present<>(3)));
  }

  @ParameterizedTest
  @MethodSource("atLeastOneExists")
  void Happy_Path(
      NullableField<Boolean> isImportant,
      NullableField<LocalDate> dueDate,
      NullableField<Long> assigneeId) {

    // Given
    UpdateTodoMetadataRequestDTO dto =
        new UpdateTodoMetadataRequestDTO(isImportant, dueDate, assigneeId);

    // When
    Set<ConstraintViolation<UpdateTodoMetadataRequestDTO>> violations = validator.validate(dto);

    // Then
    Assertions.assertEquals(true, violations.isEmpty());
  }

  @Test
  void Failure_isImportantIsNotNull() {
    // Given
    UpdateTodoMetadataRequestDTO dto =
        new UpdateTodoMetadataRequestDTO(new Present<>(null), new Absent<>(), new Absent<>());

    // When
    Set<ConstraintViolation<UpdateTodoMetadataRequestDTO>> violations = validator.validate(dto);

    // Then
    Assertions.assertEquals(1, violations.size());
    ConstraintViolation<UpdateTodoMetadataRequestDTO> violation = violations.iterator().next();

    Assertions.assertEquals("The isImportant field cannot be set to null.", violation.getMessage());
  }

  @Test
  void Failure_atLeastOneFieldIsRequired() {
    // Given
    UpdateTodoMetadataRequestDTO dto =
        new UpdateTodoMetadataRequestDTO(new Absent<>(), new Absent<>(), new Absent<>());

    // When
    Set<ConstraintViolation<UpdateTodoMetadataRequestDTO>> violations = validator.validate(dto);

    // Then
    Assertions.assertEquals(1, violations.size());
    ConstraintViolation<UpdateTodoMetadataRequestDTO> violation = violations.iterator().next();

    Assertions.assertEquals("At least one field is required.", violation.getMessage());
  }
}
