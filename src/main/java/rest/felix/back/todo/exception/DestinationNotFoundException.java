package rest.felix.back.todo.exception;

import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;

public class DestinationNotFoundException extends BadRequestException {
  public DestinationNotFoundException() {
    super("Destination does not exist.");
  }
}
