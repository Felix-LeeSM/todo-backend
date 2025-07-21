package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.GroupInvitation;

@Getter
@AllArgsConstructor
public class GroupInvitationDTO {
  private long groupId;
  private long issuerId;
  private String token;
  private ZonedDateTime expiresAt;

  public static GroupInvitationDTO of(GroupInvitation groupInvitation) {
    return new GroupInvitationDTO(
        groupInvitation.getGroup().getId(),
        groupInvitation.getIssuer().getId(),
        groupInvitation.getToken(),
        groupInvitation.getExpiresAt());
  }
}
