package rest.felix.back.group.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.TodoDTO;

@Getter
@AllArgsConstructor
public class FullGroupDetailsDTO {
  private final long id;
  private final String name;
  private final String description;
  private final List<MemberDTO> members;
  private final long memberCount;
  private final GroupRole myRole;
  private final List<TodoDTO> todos;
}
