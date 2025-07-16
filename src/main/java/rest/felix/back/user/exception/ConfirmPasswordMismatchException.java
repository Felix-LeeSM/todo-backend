package rest.felix.back.user.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class ConfirmPasswordMismatchException extends BadRequestException {

  public ConfirmPasswordMismatchException() {
    super("password and confirm Password do not match.");
  }
}
