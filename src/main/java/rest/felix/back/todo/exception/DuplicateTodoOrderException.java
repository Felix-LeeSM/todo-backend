package rest.felix.back.todo.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class DuplicateTodoOrderException extends BadRequestException {
  public DuplicateTodoOrderException() {
    super("A todo with the same order value already exists within this status.");
  }
}
