package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import java.util.List;

public record GroupInvitationInfoDTOResponse(
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    long memberCount,
    MemberDTO issuer,
    List<MemberDTO> members,
    ZonedDateTime activeUntil) {

  public static GroupInvitationInfoDTOResponse of(
      GroupInvitationInfoDTO groupInvitationInfoDetails) {
    return new GroupInvitationInfoDTOResponse(
        groupInvitationInfoDetails.getName(),
        groupInvitationInfoDetails.getDescription(),
        groupInvitationInfoDetails.getTodoCount(),
        groupInvitationInfoDetails.getCompletedTodoCount(),
        groupInvitationInfoDetails.getMemberCount(),
        groupInvitationInfoDetails.getIssuer(),
        groupInvitationInfoDetails.getMembers(),
        groupInvitationInfoDetails.getActiveUntil());
  }
}
