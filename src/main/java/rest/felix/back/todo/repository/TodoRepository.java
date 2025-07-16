package rest.felix.back.todo.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.group.entity.Group;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.user.entity.User;

@Repository
@AllArgsConstructor
public class TodoRepository {

  private final EntityManager em;

  public List<TodoDTO> getTodosInGroup(long groupId) {
    return em
        .createQuery(
            """
                                SELECT
                                    t
                                FROM
                                    Group g
                                JOIN
                                    g.todos t
                                JOIN FETCH
                                    t.author
                                WHERE
                                    g.id = :groupId
                                ORDER BY
                                    t.order ASC
                                """,
            Todo.class)
        .setParameter("groupId", groupId)
        .getResultList()
        .stream()
        .map(TodoDTO::of)
        .toList();
  }

  public Optional<TodoDTO> getTodoInGroup(long groupId, long todoId) {
    return em
        .createQuery(
            """
                                SELECT
                                    t
                                FROM
                                    Group g
                                JOIN
                                    g.todos t
                                WHERE
                                    g.id = :groupId AND
                                    t.id = :todoId
                                ORDER BY
                                    t.order ASC
                                """,
            Todo.class)
        .setParameter("groupId", groupId)
        .setParameter("todoId", todoId)
        .getResultList()
        .stream()
        .findFirst()
        .map(TodoDTO::of);
  }

  public TodoDTO createTodo(CreateTodoDTO createTodoDTO) {
    Todo todo = new Todo();

    User author = em.getReference(User.class, createTodoDTO.getAuthorId());
    Group group = em.getReference(Group.class, createTodoDTO.getGroupId());

    todo.setAuthor(author);
    todo.setGroup(group);
    todo.setTitle(createTodoDTO.getTitle());
    todo.setDescription(createTodoDTO.getDescription());
    todo.setOrder(createTodoDTO.getOrder());

    em.persist(todo);

    return TodoDTO.of(todo);
  }

  public void deleteTodo(long todoId) {
    em.createQuery(
            """
                        DELETE
                        FROM Todo t
                        WHERE t.id = :todoId
                        """)
        .setParameter("todoId", todoId)
        .executeUpdate();
  }

  public TodoDTO updateTodo(UpdateTodoDTO updateTodoDTO) {
    return em
        .createQuery(
            """
                                SELECT
                                    t
                                FROM
                                    Todo t
                                WHERE
                                    t.id = :todoId
                                """,
            Todo.class)
        .setParameter("todoId", updateTodoDTO.getId())
        .getResultList()
        .stream()
        .findFirst()
        .map(
            todo -> {
              todo.setTodoStatus(updateTodoDTO.getStatus());
              todo.setDescription(updateTodoDTO.getDescription());
              todo.setTitle(updateTodoDTO.getTitle());
              todo.setOrder(updateTodoDTO.getOrder());
              em.flush();
              return todo;
            })
        .map(TodoDTO::of)
        .orElseThrow(ResourceNotFoundException::new);
  }

  public void deleteByGroupId(long groupId) {
    em.createQuery(
            """
                        DELETE
                        FROM
                          Todo t
                        WHERE
                          t.group.id =:groupId
                        """)
        .setParameter("groupId", groupId)
        .executeUpdate();
  }
}
