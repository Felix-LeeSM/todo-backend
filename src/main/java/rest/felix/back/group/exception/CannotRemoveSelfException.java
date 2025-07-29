package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.forbidden.ForbiddenException;

public class CannotRemoveSelfException extends ForbiddenException {
  public CannotRemoveSelfException() {
    super("You cannot remove yourself from the group.");
  }
}
