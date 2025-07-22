package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupInvitationInfoDTO {
  private final String name;
  private final String description;
  private final long todoCount;
  private final long completedTodoCount;
  private final long memberCount;
  private final MemberDTO issuer;
  private final List<MemberDTO> members;
  private final ZonedDateTime expiresAt;
}
