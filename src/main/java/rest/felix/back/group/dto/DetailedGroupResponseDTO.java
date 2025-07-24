package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;

public record DetailedGroupResponseDTO(
    Long id,
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    List<MemberResponseDTO> members,
    long memberCount,
    GroupRole myRole) {

  public static DetailedGroupResponseDTO of(DetailedGroupDTO dto) {
    return new DetailedGroupResponseDTO(
        dto.id(),
        dto.name(),
        dto.description(),
        dto.todoCount(),
        dto.completedTodoCount(),
        dto.members().stream().map(MemberResponseDTO::of).toList(),
        dto.memberCount(),
        dto.myRole());
  }
}
