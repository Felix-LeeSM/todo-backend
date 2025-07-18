package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.TodoDTO;

public record FullGroupDetailsResponseDTO(
    long id,
    String name,
    String description,
    List<MemberDTO> members,
    long memberCount,
    GroupRole myRole,
    List<TodoDTO> todos) {

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
