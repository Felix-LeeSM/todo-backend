package rest.felix.back.group.dto;

import rest.felix.back.group.entity.Group;

public record GroupDTO(long id, String name, String description) {
  public static GroupDTO of(Group group) {
    return new GroupDTO(group.getId(), group.getName(), group.getDescription());
  }
}
