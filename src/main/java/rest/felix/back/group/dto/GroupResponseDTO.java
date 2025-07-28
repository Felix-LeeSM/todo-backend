package rest.felix.back.group.dto;

public record GroupResponseDTO(Long id, String name, String description) {
  public static GroupResponseDTO of(GroupDTO groupDTO) {
    return new GroupResponseDTO(groupDTO.id(), groupDTO.name(), groupDTO.description());
  }
}
