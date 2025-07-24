package rest.felix.back.group.dto;

import java.util.List;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.TodoWithStarredStatusDTO;

public record FullGroupDetailsDTO(
    long id,
    String name,
    String description,
    List<MemberDTO> members,
    long memberCount,
    GroupRole myRole,
    List<TodoWithStarredStatusDTO> todos) {}
