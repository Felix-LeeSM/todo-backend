package rest.felix.back.user.exception;

import lombok.Getter;
import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

@Getter
public class UsernameTakenException extends BadRequestException {

  public UsernameTakenException() {
    super("해당 username은 이미 사용 중입니다.");
  }
}
