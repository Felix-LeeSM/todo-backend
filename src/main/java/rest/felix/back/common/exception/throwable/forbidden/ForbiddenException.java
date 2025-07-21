package rest.felix.back.common.exception.throwable.forbidden;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class ForbiddenException extends RuntimeException implements RequestExceptionInterface {

  private final int statusCode = HttpStatus.FORBIDDEN.value();
  private String message = "Forbidden Request.";

  public ForbiddenException(String message) {
    this.message = message;
  }
}
