package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record GroupInvitationInfoDTOResponse(
    long groupId,
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    long memberCount,
    boolean isMember,
    boolean isExpired,
    MemberResponseDTO issuer,
    List<MemberResponseDTO> members,
    ZonedDateTime expiresAt) {

  public static GroupInvitationInfoDTOResponse of(
      boolean isMember, boolean isExpired, GroupInvitationInfoDTO groupInvitationInfoDetails) {
    return new GroupInvitationInfoDTOResponse(
        groupInvitationInfoDetails.groupId(),
        groupInvitationInfoDetails.name(),
        groupInvitationInfoDetails.description(),
        groupInvitationInfoDetails.todoCount(),
        groupInvitationInfoDetails.completedTodoCount(),
        groupInvitationInfoDetails.memberCount(),
        isMember,
        isExpired,
        MemberResponseDTO.of(groupInvitationInfoDetails.issuer()),
        groupInvitationInfoDetails.members().stream().map(MemberResponseDTO::of).toList(),
        groupInvitationInfoDetails.expiresAt());
  }
}
