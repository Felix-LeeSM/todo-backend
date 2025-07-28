package rest.felix.back.group.dto;

public record UpdateGroupDTO(long groupId, String name, String description) {
  public static UpdateGroupDTO of(long groupId, UpdateGroupRequestDTO dto) {
    return new UpdateGroupDTO(groupId, dto.name(), dto.description());
  }
}
