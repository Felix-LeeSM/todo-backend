package rest.felix.back.common.exception.handler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import rest.felix.back.common.exception.ErrorResponseDTO;
import rest.felix.back.common.exception.throwable.badrequest.BadRequestException;
import rest.felix.back.common.exception.throwable.conflict.ConflictException;
import rest.felix.back.common.exception.throwable.gone.GoneException;
import rest.felix.back.common.exception.throwable.notFound.NotFoundException;
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.common.exception.throwable.tooManyRequests.TooManyRequestsException;
import rest.felix.back.common.exception.throwable.unauthorized.UnauthorizedException;
import rest.felix.back.user.exception.UserAccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponseDTO> handleBadRequestException(BadRequestException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(GoneException.class)
  public ResponseEntity<ErrorResponseDTO> handleGoneException(GoneException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponseDTO> handleConflictException(ConflictException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<ErrorResponseDTO> handleTooManyRequestsException(
      TooManyRequestsException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(
      UnauthorizedException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(UserAccessDeniedException.class)
  public ResponseEntity<ErrorResponseDTO> handleUnauthorizedException(
      UserAccessDeniedException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(
      ResourceNotFoundException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponseDTO> handleNotFoundException(NotFoundException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorResponseDTO(exception.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDTO> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDTO("Bad Request, please try again later."));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponseDTO> handleConstraintViolationException(
      ConstraintViolationException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDTO("Something went wrong, please try again later."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponseDTO("Bad Request, please check parameters."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDTO> handleException(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponseDTO("Something went wrong, please try  later."));
  }
}
