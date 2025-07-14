package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;

@Getter
@AllArgsConstructor
public class UserGroupDTO {

  private final GroupRole groupRole;
  private final long userId;
  private final long groupId;
}
