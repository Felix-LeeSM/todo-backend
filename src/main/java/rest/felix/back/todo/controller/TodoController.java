package rest.felix.back.todo.controller;

import java.security.Principal;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.service.GroupService;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.CreateTodoRequestDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.TodoResponseDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.service.TodoService;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UserAccessDeniedException;
import rest.felix.back.user.service.UserService;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TodoController {

  private final GroupService groupService;
  private final TodoService todoService;
  private final UserService userService;

  @GetMapping("/group/{groupId}/todo")
  public ResponseEntity<List<TodoResponseDTO>> getTodos(
      Principal principal, @PathVariable(name = "groupId") long groupId) {
    String username = principal.getName();
    UserDTO userDTO = userService.getByUsername(username).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    groupService.getUserRoleInGroup(userId, groupId);

    List<TodoResponseDTO> todoResponseDTOs =
        todoService.getTodosInGroup(groupId).stream().map(TodoResponseDTO::of).toList();

    return ResponseEntity.ok().body(todoResponseDTOs);
  }

  @PostMapping("/group/{groupId}/todo")
  public ResponseEntity<TodoResponseDTO> createTodo(
      Principal principal,
      @PathVariable(name = "groupId") long groupId,
      @RequestBody CreateTodoRequestDTO createTodoRequestDTO) {
    String username = principal.getName();
    UserDTO userDTO = userService.getByUsername(username).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    GroupRole groupRole = groupService.getUserRoleInGroup(userId, groupId);
    if (groupRole == GroupRole.VIEWER) {
      throw new UserAccessDeniedException();
    }

    CreateTodoDTO createTodoDTO =
        new CreateTodoDTO(
            createTodoRequestDTO.getTitle(),
            createTodoRequestDTO.getDescription(),
            createTodoRequestDTO.getOrder(),
            userId,
            groupId);

    TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

    TodoResponseDTO todoResponseDTO = TodoResponseDTO.of(todoDTO);

    return ResponseEntity.status(HttpStatus.CREATED).body(todoResponseDTO);
  }

  @DeleteMapping("/group/{groupId}/todo/{todoId}")
  public ResponseEntity<Void> deleteTodo(
      Principal principal,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {
    String username = principal.getName();
    UserDTO userDTO = userService.getByUsername(username).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    GroupRole groupRole = groupService.getUserRoleInGroup(userId, groupId);

    if (groupRole == GroupRole.VIEWER) {
      throw new UserAccessDeniedException();
    }

    TodoDTO todoDTO = todoService.getTodoInGroup(groupId, todoId);

    if (groupRole == GroupRole.MEMBER && userId != todoDTO.getAuthorId()) {
      throw new UserAccessDeniedException();
    }

    todoService.deleteTodo(todoDTO.getId());

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/group/{groupId}/todo/{todoId}")
  public ResponseEntity<TodoDTO> updateTodo(
      Principal principal,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId,
      @RequestBody UpdateTodoRequestDTO updateTodoRequestDTO) {
    String username = principal.getName();
    UserDTO userDTO = userService.getByUsername(username).orElseThrow(NoMatchingUserException::new);
    long userId = userDTO.getId();

    GroupRole groupRole = groupService.getUserRoleInGroup(userId, groupId);

    if (groupRole == GroupRole.VIEWER) {
      throw new UserAccessDeniedException();
    }

    TodoDTO todoDTO = todoService.getTodoInGroup(groupId, todoId);

    if (groupRole == GroupRole.MEMBER && userId != todoDTO.getAuthorId()) {
      throw new UserAccessDeniedException();
    }

    UpdateTodoDTO updateTodoDTO =
        new UpdateTodoDTO(
            todoId,
            updateTodoRequestDTO.getTitle(),
            updateTodoRequestDTO.getDescription(),
            updateTodoRequestDTO.getOrder(),
            updateTodoRequestDTO.getStatus());

    TodoDTO updatedTodoDTO = todoService.updateTodo(updateTodoDTO);

    return ResponseEntity.ok().body(updatedTodoDTO);
  }
}
