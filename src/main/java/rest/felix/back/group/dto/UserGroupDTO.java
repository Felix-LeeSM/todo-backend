package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;

@Getter
@AllArgsConstructor
public class UserGroupDTO {

  private final long userId;
  private final long groupId;
  private final GroupRole groupRole;

  public static UserGroupDTO of(UserGroup userGroup) {
    return new UserGroupDTO(
        userGroup.getUser().getId(), userGroup.getGroup().getId(), userGroup.getGroupRole());
  }
}
