package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.forbidden.ForbiddenException;

public class ForbiddenMemberRemoveException extends ForbiddenException {
  public ForbiddenMemberRemoveException() {
    super("You do not have permission to remove members from this group.");
  }
}
