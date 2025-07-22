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
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.group.entity.Group;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoCountDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.UserTodoStar;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.exception.DestinationNotFoundException;
import rest.felix.back.todo.exception.InvalidDestinationException;
import rest.felix.back.todo.exception.TodoNotFoundException;
import rest.felix.back.todo.service.OrderGenerator;
import rest.felix.back.user.entity.User;

@Repository
@AllArgsConstructor
public class TodoRepository {

  private final EntityManager em;

  @Transactional(readOnly = true)
  public List<TodoDTO> findByGroupId(long groupId) {
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

  @Transactional(readOnly = true)
  public Optional<TodoCountDTO> findTodoCountsByGroupId(Long groupId) {
    try {
      return Optional.of(
          em.createQuery(
                  """
              SELECT new rest.felix.back.todo.dto.TodoCountDTO(
                  g.id,
                  COUNT(t),
                  SUM(CASE WHEN t.todoStatus = TodoStatus.DONE THEN 1 ELSE 0 END)
              )
              FROM Group g
              LEFT JOIN g.todos t
              WHERE g.id = :groupId
              GROUP BY g.id
              """,
                  TodoCountDTO.class)
              .setParameter("groupId", groupId)
              .getSingleResult());

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Transactional(readOnly = true)
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

  @Transactional(readOnly = true)
  public Optional<TodoDTO> findByIdAndGroupId(long groupId, long todoId) {
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

  @Transactional
  public TodoDTO createTodo(CreateTodoDTO createTodoDTO) {
    Todo todo = new Todo();

    User author = em.getReference(User.class, createTodoDTO.getAuthorId());
    Group group = em.getReference(Group.class, createTodoDTO.getGroupId());

    TodoStatus defaultTodoStatus = TodoStatus.TO_DO;

    todo.setAuthor(author);
    todo.setGroup(group);
    todo.setTitle(createTodoDTO.getTitle());
    todo.setDescription(createTodoDTO.getDescription());
    todo.setTodoStatus(defaultTodoStatus);

    String maxOrder =
        em.createQuery(
                "SELECT MAX(t.order) FROM Todo t WHERE t.group = :group AND t.todoStatus = :todoStatus",
                String.class)
            .setParameter("group", group)
            .setParameter("todoStatus", defaultTodoStatus)
            .getSingleResult();

    String newOrder = OrderGenerator.generate(maxOrder, null);

    todo.setOrder(newOrder);

    em.persist(todo);

    return TodoDTO.of(todo);
  }

  @Transactional
  public void deleteById(long todoId) {
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

  @Transactional
  public TodoDTO updateTodo(UpdateTodoDTO updateTodoDTO) {
    Todo todo =
        em
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
            .orElseThrow(ResourceNotFoundException::new);

    if (updateTodoDTO.getTitle() != null) todo.setTitle(updateTodoDTO.getTitle());

    if (updateTodoDTO.getDescription() != null) todo.setDescription(updateTodoDTO.getDescription());

    return TodoDTO.of(todo);
  }

  @Transactional
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
    return Optional.ofNullable(em.find(Todo.class, todoId)).map(TodoDTO::of);
  }

  @Transactional(readOnly = true)
  public Optional<Todo> findEntityById(long todoId) {
    try {
      Todo todo =
          em.createQuery("SELECT t FROM Todo t WHERE t.id = :todoId", Todo.class)
              .setParameter("todoId", todoId)
              .getSingleResult();

      return Optional.of(todo);

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Transactional(readOnly = true)
  public Optional<TodoDTO> findById(long groupId, long todoId) {
    try {
      Todo todo =
          em.createQuery(
                  "SELECT t FROM Todo t JOIN Group g ON t.group.id = :groupId WHERE t.id = :todoId",
                  Todo.class)
              .setParameter("todoId", todoId)
              .setParameter("groupId", groupId)
              .getSingleResult();

      return Optional.of(TodoDTO.of(todo));

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Transactional
  public TodoDTO moveTodo(long targetId, Long destinationId, TodoStatus todoStatus) {
    Todo targetTodo = findEntityById(targetId).orElseThrow(TodoNotFoundException::new);

    // destinationId가 있을 경우 검증 수행
    if (destinationId != null) {
      Todo destinationTodo = em.find(Todo.class, destinationId);
      if (destinationTodo == null) throw new DestinationNotFoundException();

      if (!destinationTodo.getGroup().equals(targetTodo.getGroup()))
        throw new InvalidDestinationException();

      if (!destinationTodo.getTodoStatus().equals(todoStatus))
        throw new InvalidDestinationException();

      if (destinationId.equals(targetId)) throw new InvalidDestinationException();
    }

    targetTodo.setTodoStatus(todoStatus);

    String newOrder;
    if (destinationId == null) {
      // 리스트 끝으로 이동
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
    return TodoDTO.of(targetTodo);
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

  @Transactional(readOnly = true)
  public boolean starExistsById(long userId, long todoId) {
    return !em.createQuery(
            """
            SELECT 1L
            FROM UserTodoStar uts
            WHERE uts.user.id = :userId AND uts.todo.id = :todoId
            """,
            Long.class)
        .setParameter("userId", userId)
        .setParameter("todoId", todoId)
        .setMaxResults(1) // 1개만 찾으면 조회를 멈추도록 설정
        .getResultList()
        .isEmpty();
  }

  @Transactional(readOnly = true)
  public boolean starExistsById(long todoId) {
    return !em.createQuery(
            """
            SELECT 1L
            FROM UserTodoStar uts
            WHERE uts.todo.id = :todoId
            """,
            Long.class)
        .setParameter("todoId", todoId)
        .setMaxResults(1)
        .getResultList()
        .isEmpty();
  }
}
