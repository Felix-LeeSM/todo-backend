package rest.felix.back.common.exception.throwable.notFound;

public class ResourceNotFoundException extends NotFoundException {

  public ResourceNotFoundException() {
    super("Resource Not Found.");
  }
}
