package rest.felix.back.todo.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.repository.TodoRepository;

@Service
@AllArgsConstructor
public class TodoService {

  private final TodoRepository todoRepository;

  @Transactional(readOnly = true)
  public List<TodoDTO> getTodosInGroup(long groupId) {

    return todoRepository.getTodosInGroup(groupId);
  }

  @Transactional(readOnly = true)
  public TodoDTO getTodoInGroup(long groupId, long todoId) {

    return todoRepository
        .getTodoInGroup(groupId, todoId)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @Transactional
  public TodoDTO createTodo(CreateTodoDTO createTodoDTO) {

    return todoRepository.createTodo(createTodoDTO);
  }

  @Transactional
  public void deleteTodo(long todoId) {

    todoRepository.deleteTodo(todoId);
  }

  @Transactional
  public TodoDTO updateTodo(UpdateTodoDTO updateTodoDTO) {

    return todoRepository.updateTodo(updateTodoDTO);
  }
}
