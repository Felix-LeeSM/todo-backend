package rest.felix.back.common.exception.throwable.badrequest;

public class ConfirmPasswordMismatchException extends BadRequestException {

  public ConfirmPasswordMismatchException() {
    super("password and confirm Password do not match.");
  }
}
