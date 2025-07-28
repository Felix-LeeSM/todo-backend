package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.forbidden.ForbiddenException;

public class ForbiddenRoleChangeException extends ForbiddenException {
  public ForbiddenRoleChangeException() {
    super("You do not have permission to change the role of the member.");
  }
}
