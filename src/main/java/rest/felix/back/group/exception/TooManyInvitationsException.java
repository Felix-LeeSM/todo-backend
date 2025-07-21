package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class TooManyInvitationsException extends BadRequestException {
  public TooManyInvitationsException() {
    super("You have made too many invitation codes. Pleas try again later.");
  }
}
