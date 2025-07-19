package rest.felix.back.todo.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class InvalidDestinationException extends BadRequestException {
  public InvalidDestinationException() {
    super("Destination must be another todo in the same group with the same todo status.");
  }
}
