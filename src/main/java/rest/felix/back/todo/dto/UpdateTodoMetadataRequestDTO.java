package rest.felix.back.todo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UpdateTodoMetadataRequestDTO {

  @NotNull private Boolean isImportant;

  private LocalDate dueDate;

  private Long assigneeId;
}
