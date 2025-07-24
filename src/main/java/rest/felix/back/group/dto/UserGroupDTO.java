package rest.felix.back.group.dto;

import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;

public record UserGroupDTO(long userId, long groupId, GroupRole groupRole) {

  public static UserGroupDTO of(UserGroup userGroup) {
    return new UserGroupDTO(
        userGroup.getUser().getId(), userGroup.getGroup().getId(), userGroup.getGroupRole());
  }
}
