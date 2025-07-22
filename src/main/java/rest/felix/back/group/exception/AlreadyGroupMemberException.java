package rest.felix.back.group.exception;

import rest.felix.back.common.exception.throwable.conflict.ConflictException;

public class AlreadyGroupMemberException extends ConflictException {
  public AlreadyGroupMemberException() {
    super("User is already a member of the group.");
  }
}
