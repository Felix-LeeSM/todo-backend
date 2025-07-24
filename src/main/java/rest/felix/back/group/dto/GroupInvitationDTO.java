package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import rest.felix.back.group.entity.GroupInvitation;

public record GroupInvitationDTO(
    long issuerId, long groupId, String token, ZonedDateTime expiresAt) {

  public static GroupInvitationDTO of(GroupInvitation groupInvitation) {
    return new GroupInvitationDTO(
        groupInvitation.getIssuer().getId(),
        groupInvitation.getGroup().getId(),
        groupInvitation.getToken(),
        groupInvitation.getExpiresAt());
  }
}
