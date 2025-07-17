package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;

@Getter
@AllArgsConstructor
public class MemberDTO {
  private final long id;
  private final String username;
  private final String nickname;
  private final long groupId;
  private final GroupRole role;
}
