package rest.felix.back.group.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvitationTokenDTO {
  String token;
  ZonedDateTime expiresAt;
}
