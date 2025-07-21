package rest.felix.back.common.exception.throwable.conflict;

import lombok.Getter;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class ConflictException extends RuntimeException implements RequestExceptionInterface {
  private final int statusCode = 409;
  private String message = "Conflict.";

  public ConflictException(String message) {
    this.message = message;
  }
}
