package rest.felix.back.todo.service;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.exception.TodoNotFoundException;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.exception.UserAccessDeniedException;

@Service
@AllArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;
  private final UserGroupRepository userGroupRepository;

  @Transactional(readOnly = true)
  public List<TodoDTO> getTodosInGroup(long groupId) {

    return todoRepository.findByGroupId(groupId);
  }

  @Transactional(readOnly = true)
  public TodoDTO getTodoInGroup(long groupId, long todoId) {

    return todoRepository
        .findByIdAndGroupId(groupId, todoId)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @Transactional
  public TodoDTO createTodo(CreateTodoDTO createTodoDTO) {

    return todoRepository.createTodo(createTodoDTO);
  }

  @Transactional
  public void deleteTodo(long todoId) {

    todoRepository.deleteById(todoId);
  }

  @Transactional
  public TodoDTO updateTodo(UpdateTodoDTO updateTodoDTO) {

    return todoRepository.updateTodo(updateTodoDTO);
  }

  @Transactional
  public void assertTodoAuthority(
      long userId, long groupId, long todoId, Predicate<TodoDTO> authorityChecker) {

    userGroupRepository
        .findByUserIdAndGroupId(userId, groupId)
        .map(dto -> dto.getGroupRole())
        .orElseThrow(UserAccessDeniedException::new);
    TodoDTO todo = todoRepository.findById(groupId, todoId).orElseThrow(TodoNotFoundException::new);

    if (!authorityChecker.test(todo)) {
      throw new UserAccessDeniedException();
    }
  }

  @Transactional
  public void assertTodoAuthority(
      long userId, long groupId, long todoId, BiPredicate<GroupRole, TodoDTO> authorityChecker) {
    GroupRole role =
        userGroupRepository
            .findByUserIdAndGroupId(userId, groupId)
            .map(dto -> dto.getGroupRole())
            .orElseThrow(UserAccessDeniedException::new);
    TodoDTO todo = todoRepository.findById(groupId, todoId).orElseThrow(TodoNotFoundException::new);

    if (!authorityChecker.test(role, todo)) {
      throw new UserAccessDeniedException();
    }
  }

  @Transactional
  public void assertTodoAuthority(long userId, long groupId, long todoId, GroupRole groupRole) {

    userGroupRepository
        .findByUserIdAndGroupId(userId, groupId)
        .map(dto -> dto.getGroupRole())
        .filter(role -> role.gte(groupRole))
        .orElseThrow(UserAccessDeniedException::new);
    todoRepository.findById(groupId, todoId).orElseThrow(TodoNotFoundException::new);
  }

  @Transactional
  public TodoDTO moveTodo(long targetId, Long destinationId, TodoStatus todoStatus) {
    return todoRepository.moveTodo(targetId, destinationId, todoStatus);
  }

  @Transactional
  public void starTodo(long userId, long todoId) {

    if (todoRepository.starExistsById(userId, todoId)) return;

    todoRepository.starTodo(userId, todoId);
  }

  @Transactional
  public void unstarTodo(long userId, long todoId) {

    if (!todoRepository.starExistsById(userId, todoId)) return;

    todoRepository.unstarTodo(userId, todoId);
  }
}
