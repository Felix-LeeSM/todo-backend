package rest.felix.back.common.exception.throwable.tooManyRequests;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import rest.felix.back.common.exception.throwable.RequestExceptionInterface;

@Getter
public class TooManyRequestsException extends RuntimeException
    implements RequestExceptionInterface {
  private final int statusCode = HttpStatus.TOO_MANY_REQUESTS.value();
  private String message = "Too Many Requests. Please Try Again Later.";

  public TooManyRequestsException(String message) {
    this.message = message;
  }
}
