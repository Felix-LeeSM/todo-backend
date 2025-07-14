package rest.felix.back.group.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupDTO {

  private final long id;
  private final String name;
  private final String description;
}
