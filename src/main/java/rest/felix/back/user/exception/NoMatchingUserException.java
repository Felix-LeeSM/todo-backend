package rest.felix.back.user.exception;

import rest.felix.back.common.exception.throwable.unauthorized.UnauthorizedException;

public class NoMatchingUserException extends UnauthorizedException {

  public NoMatchingUserException() {
    super("There is no user with given conditions.");
  }
}
