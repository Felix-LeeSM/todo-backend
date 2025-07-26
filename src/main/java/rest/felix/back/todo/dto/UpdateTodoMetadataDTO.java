package rest.felix.back.todo.dto;

import java.time.LocalDate;
import rest.felix.back.common.util.NullableField;

public record UpdateTodoMetadataDTO(
    long todoId,
    NullableField<Boolean> isImportant,
    NullableField<LocalDate> dueDate,
    NullableField<Long> assigneeId) {
  public static UpdateTodoMetadataDTO of(long todoId, UpdateTodoMetadataRequestDTO dto) {
    return new UpdateTodoMetadataDTO(todoId, dto.isImportant(), dto.dueDate(), dto.assigneeId());
  }
}
