package rest.felix.back.user.exception;

import rest.felix.back.common.exception.throwable.forbidden.ForbiddenException;

public class UserAccessDeniedException extends ForbiddenException {

  public UserAccessDeniedException() {
    super("No permission to perform this action.");
  }
}
