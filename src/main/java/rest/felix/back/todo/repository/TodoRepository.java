package rest.felix.back.todo.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.group.entity.Group;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoCountDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.UserTodoStar;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.service.OrderGenerator;
import rest.felix.back.user.entity.User;

@Repository
@AllArgsConstructor
public class TodoRepository {

  private final EntityManager em;

  public List<TodoDTO> getTodosInGroup(long groupId) {
    return em
        .createQuery(
            """
            SELECT t
            FROM Group g
            JOIN g.todos t
            JOIN FETCH t.author
            WHERE g.id = :groupId
            ORDER BY t.order ASC
                """,
            Todo.class)
        .setParameter("groupId", groupId)
        .getResultList()
        .stream()
        .map(TodoDTO::of)
        .toList();
  }

  public Map<Long, TodoCountDTO> findTodoCountsByGroupIds(List<Long> groupIds) {
    return em
        .createQuery(
            """
            SELECT new rest.felix.back.todo.dto.TodoCountDTO(
                g.id,
                COUNT(t),
                SUM(CASE WHEN t.todoStatus = TodoStatus.DONE THEN 1 ELSE 0 END)
            )
            FROM Group g
            LEFT JOIN g.todos t
            WHERE g.id IN :groupIds
            GROUP BY g.id
            """,
            TodoCountDTO.class)
        .setParameter("groupIds", groupIds)
        .getResultList()
        .stream()
        .collect(Collectors.toMap(TodoCountDTO::getGroupId, dto -> dto));
  }

  public Optional<TodoDTO> getTodoInGroup(long groupId, long todoId) {
    return em
        .createQuery(
            """
                                SELECT t
                                FROM Group g
                                JOIN g.todos t
                                WHERE g.id = :groupId AND t.id = :todoId
                                ORDER BY t.order ASC
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

  @Transactional()
  public void deleteTodo(long todoId) {
    em.createQuery(
            """
                        DELETE FROM UserTodoStar uts
                        WHERE uts.todo.id = :todoId
                        """)
        .setParameter("todoId", todoId)
        .executeUpdate();
    em.createQuery(
            """
                        DELETE FROM Todo t
                        WHERE t.id = :todoId
                        """)
        .setParameter("todoId", todoId)
        .executeUpdate();
  }

  @Transactional()
  public TodoDTO updateTodo(UpdateTodoDTO updateTodoDTO) {
    return em
        .createQuery(
            """
                                SELECT t
                                FROM Todo t
                                WHERE t.id = :todoId
                                """,
            Todo.class)
        .setParameter("todoId", updateTodoDTO.getId())
        .getResultList()
        .stream()
        .findFirst()
        .map(
            todo -> {
              todo.setDescription(updateTodoDTO.getDescription());
              todo.setTitle(updateTodoDTO.getTitle());
              em.flush();
              return todo;
            })
        .map(TodoDTO::of)
        .orElseThrow(ResourceNotFoundException::new);
  }

  @Transactional()
  public void deleteByGroupId(long groupId) {
    em.createQuery(
            """
                        DELETE FROM UserTodoStar uts
                        WHERE uts.todo.id IN (
                            SELECT t.id FROM Todo t WHERE t.group.id = :groupId
                        )
                        """)
        .setParameter("groupId", groupId)
        .executeUpdate();
    em.createQuery(
            """
                        DELETE FROM Todo t WHERE t.group.id = :groupId
                        """)
        .setParameter("groupId", groupId)
        .executeUpdate();
  }

  @Transactional(readOnly = true)
  public Optional<TodoDTO> findById(long todoId) {
    try {
      Todo todo =
          em.createQuery("SELECT t FROM Todo t WHERE t.id = :todoId", Todo.class)
              .setParameter("todoId", todoId)
              .getSingleResult();

      return Optional.of(TodoDTO.of(todo));

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Transactional
  public void moveTodo(long targetId, Long destinationId, TodoStatus todoStatus) {
    Todo targetTodo = em.find(Todo.class, targetId);
    targetTodo.setTodoStatus(todoStatus);

    String newOrder;
    if (destinationId == null) {
      // 맨 뒤로 보내는 로직
      String maxOrder =
          em.createQuery(
                  "SELECT MAX(t.order) FROM Todo t WHERE t.group = :group AND t.todoStatus = :todoStatus",
                  String.class)
              .setParameter("group", targetTodo.getGroup())
              .setParameter("todoStatus", todoStatus)
              .getSingleResult();
      newOrder = OrderGenerator.generate(maxOrder, null);
    } else {
      Todo destinationTodo = em.find(Todo.class, destinationId);
      String destinationOrder = destinationTodo.getOrder();

      // destinationTodo의 이전 todo를 찾는다.
      String prevOrder =
          em.createQuery(
                  "SELECT MAX(t.order) FROM Todo t WHERE t.group = :group AND t.order < :destinationOrder AND t.todoStatus = :todoStatus",
                  String.class)
              .setParameter("group", targetTodo.getGroup())
              .setParameter("destinationOrder", destinationOrder)
              .setParameter("todoStatus", todoStatus)
              .getSingleResult();

      newOrder = OrderGenerator.generate(prevOrder, destinationOrder);
    }

    targetTodo.setOrder(newOrder);
    em.flush();
  }

  @Transactional
  public void starTodo(long userId, long todoId) {
    User user = em.getReference(User.class, userId);
    Todo todo = em.getReference(Todo.class, todoId);

    UserTodoStar userTodoStar = new UserTodoStar();
    userTodoStar.setUser(user);
    userTodoStar.setTodo(todo);

    em.persist(userTodoStar);
  }

  @Transactional
  public void unstarTodo(long userId, long todoId) {
    em.createQuery(
            "DELETE FROM UserTodoStar uts WHERE uts.user.id = :userId AND uts.todo.id = :todoId")
        .setParameter("userId", userId)
        .setParameter("todoId", todoId)
        .executeUpdate();
  }
}
