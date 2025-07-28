package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class CannotRemoveSelfException extends BadRequestException {
  public CannotRemoveSelfException() {
    super("You cannot remove yourself from the group.");
  }
}
