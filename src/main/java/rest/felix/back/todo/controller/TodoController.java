package rest.felix.back.todo.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import rest.felix.back.todo.dto.MoveTodoRequestDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.TodoResponseDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.service.TodoService;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.exception.UserAccessDeniedException;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TodoController {

  private final GroupService groupService;
  private final TodoService todoService;

  @GetMapping("/group/{groupId}/todo")
  public ResponseEntity<List<TodoResponseDTO>> getTodos(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {

    long userId = authUser.getUserId();

    groupService.findUserRole(userId, groupId).orElseThrow(UserAccessDeniedException::new);

    List<TodoResponseDTO> todoResponseDTOs =
        todoService.getTodosInGroup(groupId).stream().map(TodoResponseDTO::of).toList();

    return ResponseEntity.ok().body(todoResponseDTOs);
  }

  @PostMapping("/group/{groupId}/todo")
  public ResponseEntity<TodoResponseDTO> createTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @RequestBody CreateTodoRequestDTO createTodoRequestDTO) {

    long userId = authUser.getUserId();

    groupService.assertGroupAuthority(userId, groupId, GroupRole.MEMBER);

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
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {

    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(
        userId,
        groupId,
        todoId,
        (role, todo) ->
            role.gte(GroupRole.MANAGER)
                || (role.eq(GroupRole.MEMBER) && todo.getAuthorId() == userId));

    todoService.deleteTodo(todoId);

    return ResponseEntity.noContent().build();
  }

  @PutMapping("/group/{groupId}/todo/{todoId}")
  public ResponseEntity<TodoDTO> updateTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId,
      @RequestBody UpdateTodoRequestDTO updateTodoRequestDTO) {

    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(
        userId,
        groupId,
        todoId,
        (role, todo) ->
            role.gte(GroupRole.MANAGER)
                || (role.eq(GroupRole.MEMBER) && todo.getAuthorId() == userId));

    UpdateTodoDTO updateTodoDTO =
        new UpdateTodoDTO(
            todoId, updateTodoRequestDTO.getTitle(), updateTodoRequestDTO.getDescription());

    TodoDTO updatedTodoDTO = todoService.updateTodo(updateTodoDTO);

    return ResponseEntity.ok().body(updatedTodoDTO);
  }

  @PutMapping("/group/{groupId}/todo/{todoId}/move")
  public ResponseEntity<TodoResponseDTO> moveTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId,
      @RequestBody MoveTodoRequestDTO moveTodoRequestDTO) {
    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.MEMBER);

    TodoDTO todo =
        todoService.moveTodo(
            todoId, moveTodoRequestDTO.getDestinationId(), moveTodoRequestDTO.getTodoStatus());

    return ResponseEntity.ok().body(TodoResponseDTO.of(todo));
  }

  @PostMapping("/group/{groupId}/todo/{todoId}/star")
  public ResponseEntity<Void> starTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {
    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.VIEWER);

    todoService.starTodo(userId, groupId, todoId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/group/{groupId}/todo/{todoId}/star")
  public ResponseEntity<Void> unstarTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {
    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.VIEWER);

    todoService.unstarTodo(userId, groupId, todoId);
    return ResponseEntity.noContent().build();
  }
}
