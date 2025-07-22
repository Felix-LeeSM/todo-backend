package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class TooManyInvitationsException extends BadRequestException {
  public TooManyInvitationsException() {
    super("Too many invitations.");
  }
}
