package rest.felix.back.todo.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.service.GroupService;
import rest.felix.back.todo.dto.*;
import rest.felix.back.todo.service.TodoService;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.exception.UserNotFoundException;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TodoController {

  private final GroupService groupService;
  private final TodoService todoService;

  @GetMapping("/group/{groupId}/todo")
  public ResponseEntity<List<TodoResponseDTO>> getTodos(
      @AuthenticationPrincipal AuthUserDTO authUser, @PathVariable(name = "groupId") long groupId) {

    groupService.assertGroupAuthority(authUser.getUserId(), groupId, GroupRole.VIEWER);

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

    if (createTodoRequestDTO.assigneeId() != null)
      groupService
          .findUserRole(createTodoRequestDTO.assigneeId(), groupId)
          .orElseThrow(UserNotFoundException::new);

    CreateTodoDTO createTodoDTO = CreateTodoDTO.of(createTodoRequestDTO, userId, groupId);

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
                || (role.eq(GroupRole.MEMBER) && todo.authorId() == userId));

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
                || (role.eq(GroupRole.MEMBER) && todo.authorId() == userId));

    UpdateTodoDTO updateTodoDTO =
        new UpdateTodoDTO(todoId, updateTodoRequestDTO.title(), updateTodoRequestDTO.description());

    TodoDTO updatedTodoDTO = todoService.updateTodo(updateTodoDTO);

    return ResponseEntity.ok().body(updatedTodoDTO);
  }

  @PatchMapping("/group/{groupId}/todo/{todoId}/metadata")
  public ResponseEntity<TodoDTO> updateTodoMetadata(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId,
      @RequestBody @Valid UpdateTodoMetadataRequestDTO dto) {

    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.MANAGER);

    if (dto.assigneeId().isPresent() && dto.assigneeId().getValue() != null) {
      long assigneeId = dto.assigneeId().getValue();
      groupService.findUserRole(assigneeId, groupId).orElseThrow(UserNotFoundException::new);
    }

    dto.isImportant()
        .ifPresent(
            isImportant ->
                Optional.ofNullable(isImportant).orElseThrow(IllegalArgumentException::new));

    TodoDTO updatedTodoDTO = todoService.updateTodoMetadata(UpdateTodoMetadataDTO.of(todoId, dto));

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
            todoId, moveTodoRequestDTO.destinationId(), moveTodoRequestDTO.todoStatus());

    return ResponseEntity.ok().body(TodoResponseDTO.of(todo));
  }

  @PostMapping("/group/{groupId}/todo/{todoId}/star")
  public ResponseEntity<Void> starTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {
    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.VIEWER);

    todoService.starTodo(userId, todoId);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @DeleteMapping("/group/{groupId}/todo/{todoId}/star")
  public ResponseEntity<Void> unstarTodo(
      @AuthenticationPrincipal AuthUserDTO authUser,
      @PathVariable(name = "groupId") long groupId,
      @PathVariable(name = "todoId") long todoId) {
    long userId = authUser.getUserId();

    todoService.assertTodoAuthority(userId, groupId, todoId, GroupRole.VIEWER);

    todoService.unstarTodo(userId, todoId);
    return ResponseEntity.noContent().build();
  }
}
