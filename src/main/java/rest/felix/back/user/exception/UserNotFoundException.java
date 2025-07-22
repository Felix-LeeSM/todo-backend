package rest.felix.back.user.exception;

import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;

public class UserNotFoundException extends ResourceNotFoundException {

  public UserNotFoundException() {
    super();
  }
}
