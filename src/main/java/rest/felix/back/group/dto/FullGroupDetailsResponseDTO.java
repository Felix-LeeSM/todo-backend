package rest.felix.back.group.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.TodoDTO;

@Getter
@AllArgsConstructor
public class FullGroupDetailsResponseDTO {
  private final long id;
  private final String name;
  private final String description;
  private final List<MemberDTO> members;
  private final long memberCount;
  private final GroupRole myRole;
  private final List<TodoDTO> todos;

  public static FullGroupDetailsResponseDTO of(FullGroupDetailsDTO dto) {
    return new FullGroupDetailsResponseDTO(
        dto.getId(),
        dto.getName(),
        dto.getDescription(),
        dto.getMembers(),
        dto.getMemberCount(),
        dto.getMyRole(),
        dto.getTodos());
  }
}
