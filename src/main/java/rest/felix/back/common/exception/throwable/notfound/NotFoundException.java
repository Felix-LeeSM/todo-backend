package rest.felix.back.common.exception.throwable.notfound;

import lombok.Getter;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class NotFoundException extends RuntimeException implements RequestExceptionInterface {

  private final int statusCode = 404;
  private String message = "Not Found.";

  public NotFoundException(String message) {
    this.message = message;
  }
}
