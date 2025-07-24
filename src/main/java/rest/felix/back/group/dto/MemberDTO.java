package rest.felix.back.group.dto;

import rest.felix.back.group.entity.enumerated.GroupRole;

public record MemberDTO(long id, String nickname, long groupId, GroupRole role) {}
