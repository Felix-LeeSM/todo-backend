package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;

public record DetailedGroupResponseDTO(
    Long id,
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    List<MemberDTO> members,
    long memberCount,
    GroupRole myRole) {

  public static DetailedGroupResponseDTO of(DetailedGroupDTO dto) {
    return new DetailedGroupResponseDTO(
        dto.getId(),
        dto.getName(),
        dto.getDescription(),
        dto.getTodoCount(),
        dto.getCompletedTodoCount(),
        dto.getMembers(),
        dto.getMemberCount(),
        dto.getMyRole());
  }
}
