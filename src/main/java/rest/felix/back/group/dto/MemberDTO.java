package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;

@Getter
@AllArgsConstructor
public class MemberDTO {
  private final long userId;
  private final String username;
  private final long groupId;
  private final GroupRole role;
}
