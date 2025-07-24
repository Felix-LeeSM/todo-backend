package rest.felix.back.group.dto;

import java.time.ZonedDateTime;

public record CreateGroupInvitationDTO(
    long issuerId, long groupId, String token, ZonedDateTime expiresAt) {}
