package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record GroupInvitationInfoDTOResponse(
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    long memberCount,
    MemberResponseDTO issuer,
    List<MemberResponseDTO> members,
    ZonedDateTime expiresAt) {

  public static GroupInvitationInfoDTOResponse of(
      GroupInvitationInfoDTO groupInvitationInfoDetails) {
    return new GroupInvitationInfoDTOResponse(
        groupInvitationInfoDetails.getName(),
        groupInvitationInfoDetails.getDescription(),
        groupInvitationInfoDetails.getTodoCount(),
        groupInvitationInfoDetails.getCompletedTodoCount(),
        groupInvitationInfoDetails.getMemberCount(),
        MemberResponseDTO.of(groupInvitationInfoDetails.getIssuer()),
        groupInvitationInfoDetails.getMembers().stream().map(MemberResponseDTO::of).toList(),
        groupInvitationInfoDetails.getExpiresAt());
  }
}
