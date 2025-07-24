package rest.felix.back.group.dto;

import rest.felix.back.group.entity.enumerated.GroupRole;

public record MemberResponseDTO(long id, String nickname, long groupId, GroupRole role) {

  public static MemberResponseDTO of(MemberDTO dto) {
    if (dto == null) return null;
    return new MemberResponseDTO(dto.id(), dto.nickname(), dto.groupId(), dto.role());
  }
}
