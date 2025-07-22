package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.GroupInvitation;

@Getter
@AllArgsConstructor
public class GroupInvitationDTO {
  private long issuerId;
  private long groupId;
  private String token;
  private ZonedDateTime expiresAt;

  public static GroupInvitationDTO of(GroupInvitation groupInvitation) {
    return new GroupInvitationDTO(
        groupInvitation.getIssuer().getId(),
        groupInvitation.getGroup().getId(),
        groupInvitation.getToken(),
        groupInvitation.getExpiresAt());
  }
}
