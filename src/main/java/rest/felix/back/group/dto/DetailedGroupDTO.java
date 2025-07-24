package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;

public record DetailedGroupDTO(
    long id,
    String name,
    String description,
    long todoCount,
    long completedTodoCount,
    List<MemberDTO> members,
    long memberCount,
    GroupRole myRole) {}
