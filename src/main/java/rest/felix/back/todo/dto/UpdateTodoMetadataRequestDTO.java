package rest.felix.back.todo.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDate;
import rest.felix.back.common.util.NullableField;
import rest.felix.back.common.util.NullableFieldDeserializer;

public record UpdateTodoMetadataRequestDTO(
    @JsonDeserialize(using = NullableFieldDeserializer.class) NullableField<Boolean> isImportant,
    @JsonDeserialize(using = NullableFieldDeserializer.class) NullableField<LocalDate> dueDate,
    @JsonDeserialize(using = NullableFieldDeserializer.class) NullableField<Long> assigneeId) {

  @AssertTrue(message = "At least one field is required.")
  private boolean isAnyFieldPresent() {
    return isImportant.isPresent() || dueDate.isPresent() || assigneeId.isPresent();
  }

  @AssertTrue(message = "The isImportant field cannot be set to null.")
  private boolean isIsImportantValid() {
    return isImportant.isAbsent() || isImportant.getValue() != null;
  }
}
