package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record GroupInvitationInfoDTO(
    long groupId,
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    long memberCount,
    MemberDTO issuer,
    List<MemberDTO> members,
    ZonedDateTime expiresAt) {}
