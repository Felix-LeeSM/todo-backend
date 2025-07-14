package rest.felix.back.common.exception.throwable.unauthorized;

public class NoMatchingUserException extends UnauthorizedException {

  public NoMatchingUserException() {
    super("There is no user with given conditions.");
  }
}
