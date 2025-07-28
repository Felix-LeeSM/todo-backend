package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.notFound.NotFoundException;

public class MembershipNotFoundException extends NotFoundException {
  public MembershipNotFoundException() {
    super("The specified user is not a member of this group.");
  }
}
