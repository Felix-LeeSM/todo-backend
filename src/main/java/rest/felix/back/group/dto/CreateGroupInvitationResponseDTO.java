package rest.felix.back.group.dto;

import java.time.ZonedDateTime;

public record CreateGroupInvitationResponseDTO(String token, ZonedDateTime expiresAt) {
  public static CreateGroupInvitationResponseDTO of(GroupInvitationDTO groupInvitationDTO) {
    return new CreateGroupInvitationResponseDTO(
        groupInvitationDTO.token(), groupInvitationDTO.expiresAt());
  }
}
