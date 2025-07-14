package rest.felix.back.common.exception.throwable.unauthorized;

import lombok.Getter;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class UnauthorizedException extends RuntimeException implements RequestExceptionInterface {

  private final int statusCode = 401;
  private String message = "Unauthorized Request.";

  public UnauthorizedException(String message) {
    this.message = message;
  }
}
