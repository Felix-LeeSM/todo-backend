package rest.felix.back.group.dto;

import rest.felix.back.group.entity.enumerated.GroupRole;

public record UpdateMemberDTO(long userId, long groupId, GroupRole role) {
  public static UpdateMemberDTO of(long userId, long groupId, UpdateMemberRequestDTO dto) {
    return new UpdateMemberDTO(userId, groupId, dto.role());
  }
}
