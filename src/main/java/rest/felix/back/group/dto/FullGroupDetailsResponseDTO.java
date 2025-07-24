package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.TodoWithStarredStatusResponseDTO;

public record FullGroupDetailsResponseDTO(
    long id,
    String name,
    String description,
    List<MemberResponseDTO> members,
    long memberCount,
    GroupRole myRole,
    List<TodoWithStarredStatusResponseDTO> todos) {

  public static FullGroupDetailsResponseDTO of(FullGroupDetailsDTO dto) {
    return new FullGroupDetailsResponseDTO(
        dto.id(),
        dto.name(),
        dto.description(),
        dto.members().stream().map(MemberResponseDTO::of).toList(),
        dto.memberCount(),
        dto.myRole(),
        dto.todos().stream().map(TodoWithStarredStatusResponseDTO::of).toList());
  }
}
