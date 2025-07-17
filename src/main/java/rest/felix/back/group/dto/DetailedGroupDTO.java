package rest.felix.back.group.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import rest.felix.back.group.entity.enumerated.GroupRole;

@Getter
@AllArgsConstructor
public class DetailedGroupDTO {
  private final long id;
  private final String name;
  private final String description;
  private final long todoCount;
  private final long completedTodoCount;
  private final List<MemberDTO> members;
  private final long memberCount;
  private final GroupRole myRole;
}
