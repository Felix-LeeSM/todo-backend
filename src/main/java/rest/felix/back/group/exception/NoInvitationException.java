package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.notFound.NotFoundException;

public class NoInvitationException extends NotFoundException {
  public NoInvitationException() {
    super("초대가 존재하지 않습니다.");
  }
}
