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
        groupInvitationInfoDetails.name(),
        groupInvitationInfoDetails.description(),
        groupInvitationInfoDetails.todoCount(),
        groupInvitationInfoDetails.completedTodoCount(),
        groupInvitationInfoDetails.memberCount(),
        MemberResponseDTO.of(groupInvitationInfoDetails.issuer()),
        groupInvitationInfoDetails.members().stream().map(MemberResponseDTO::of).toList(),
        groupInvitationInfoDetails.expiresAt());
  }
}
