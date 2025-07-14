package rest.felix.back.common.exception.throwable.badrequest;

import lombok.Getter;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class BadRequestException extends RuntimeException implements RequestExceptionInterface {

  private final int statusCode = 400;
  private String message = "Bad Request Couldn't be handled";

  public BadRequestException(String message) {
    this.message = message;
  }
}
