package rest.felix.back.common.exception.throwable.badrequest;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class BadRequestException extends RuntimeException implements RequestExceptionInterface {

  private final int statusCode = HttpStatus.BAD_REQUEST.value();
  private String message = "Bad Request Couldn't be handled";

  public BadRequestException(String message) {
    this.message = message;
  }
}
