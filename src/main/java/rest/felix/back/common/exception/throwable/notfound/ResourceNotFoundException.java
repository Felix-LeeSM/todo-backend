package rest.felix.back.common.exception.throwable.notfound;

public class ResourceNotFoundException extends NotFoundException {

  public ResourceNotFoundException() {
    super("Resource Not Found.");
  }
}
