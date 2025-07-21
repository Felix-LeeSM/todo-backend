package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.conflict.ConflictException;

public class AlreadyGroupMemberException extends ConflictException {
  public AlreadyGroupMemberException() {
    super("해당 그룹의 멤버로 이미 등록되어 있습니다.");
  }
}
