package rest.felix.back.common.exception.throwable.gone;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class GoneException extends RuntimeException implements RequestExceptionInterface {
  private final int statusCode = HttpStatus.GONE.value();
  private String message = "Gone.";

  public GoneException(String message) {
    this.message = message;
  }
}
