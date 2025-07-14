package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CreateGroupDTO {

  private final long userId;
  private final String groupName;
  private final String description;
}
