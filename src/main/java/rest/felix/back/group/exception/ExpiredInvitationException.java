package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.gone.GoneException;

public class ExpiredInvitationException extends GoneException {
  public ExpiredInvitationException() {
    super("만료된 초대입니다. 더 이상 사용할 수 없습니다.");
  }
}
