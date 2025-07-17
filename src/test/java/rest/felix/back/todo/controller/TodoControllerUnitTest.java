package rest.felix.back.todo.controller;

import jakarta.persistence.EntityManager;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.Trio;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.CreateTodoRequestDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.TodoResponseDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@Transactional
public class TodoControllerUnitTest {

  @Autowired private EntityManager em;
  @Autowired private TodoController todoController;
  @Autowired private PasswordService passwordService;
  private EntityFactory entityFactory;

  @BeforeEach
  void setUp() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  @Test
  void getTodos_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    List<Trio<TodoStatus, String, Integer>> list =
        Arrays.asList(
            new Trio<>(TodoStatus.TO_DO, "c", 1),
            new Trio<>(TodoStatus.IN_PROGRESS, "a", 2),
            new Trio<>(TodoStatus.DONE, "b", 3),
            new Trio<>(TodoStatus.ON_HOLD, "d", 4));

    list.forEach(
        trio -> {
          TodoStatus todoStatus = trio.first();
          String order = trio.second();
          int idx = trio.third();

          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              String.format("todo %d", idx),
              String.format("todo %d description", idx),
              todoStatus,
              order,
              false);
        });

    em.flush();

    Principal principal = user::getUsername;

    // When

    ResponseEntity<List<TodoResponseDTO>> responseEntity =
        todoController.getTodos(principal, group.getId());

    // Then

    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    List<TodoResponseDTO> todoResponseDTOs = responseEntity.getBody();
    Assertions.assertEquals(4, todoResponseDTOs.size());

    // todo with order "a" is todo 2
    Assertions.assertEquals("todo 2", todoResponseDTOs.get(0).title());
    Assertions.assertEquals("todo 2 description", todoResponseDTOs.get(0).description());
    Assertions.assertEquals(TodoStatus.IN_PROGRESS, todoResponseDTOs.get(0).status());
    Assertions.assertEquals("a", todoResponseDTOs.get(0).order());

    // todo with order "b" is todo 3
    Assertions.assertEquals("todo 3", todoResponseDTOs.get(1).title());
    Assertions.assertEquals("todo 3 description", todoResponseDTOs.get(1).description());
    Assertions.assertEquals(TodoStatus.DONE, todoResponseDTOs.get(1).status());
    Assertions.assertEquals("b", todoResponseDTOs.get(1).order());

    // todo with order "c" is todo 1
    Assertions.assertEquals("todo 1", todoResponseDTOs.get(2).title());
    Assertions.assertEquals("todo 1 description", todoResponseDTOs.get(2).description());
    Assertions.assertEquals(TodoStatus.TO_DO, todoResponseDTOs.get(2).status());
    Assertions.assertEquals("c", todoResponseDTOs.get(2).order());

    // todo with order "d" is todo 4
    Assertions.assertEquals("todo 4", todoResponseDTOs.get(3).title());
    Assertions.assertEquals("todo 4 description", todoResponseDTOs.get(3).description());
    Assertions.assertEquals(TodoStatus.ON_HOLD, todoResponseDTOs.get(3).status());
    Assertions.assertEquals("d", todoResponseDTOs.get(3).order());

    Assertions.assertTrue(
        todoResponseDTOs.stream()
            .map(TodoResponseDTO::authorId)
            .allMatch(authorId -> authorId.equals(user.getId())));

    Assertions.assertTrue(
        todoResponseDTOs.stream()
            .map(TodoResponseDTO::groupId)
            .allMatch(groupId -> groupId.equals(group.getId())));
  }

  @Test
  void getTodos_HappyPath_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    Principal principal = user::getUsername;

    // When

    ResponseEntity<List<TodoResponseDTO>> responseEntity =
        todoController.getTodos(principal, group.getId());

    // Then

    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    List<TodoResponseDTO> todoResponseDTOs = responseEntity.getBody();
    Assertions.assertEquals(0, todoResponseDTOs.size());
  }

  @Test
  void getTodos_Failure_NoUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    em.flush();

    em.remove(user);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.getTodos(principal, group.getId());

    // Then

    Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
  }

  @Test
  void getTodos_Failure_Group() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(group);
    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.getTodos(principal, group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void getTodos_Failure_NoUserGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.getTodos(principal, group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    ResponseEntity<TodoResponseDTO> responseEntity =
        todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

    TodoResponseDTO todoResponseDTO = responseEntity.getBody();

    Assertions.assertEquals("todo title", todoResponseDTO.title());
    Assertions.assertEquals("todo description", todoResponseDTO.description());
    Assertions.assertEquals(TodoStatus.TO_DO, todoResponseDTO.status());
    Assertions.assertEquals(user.getId(), todoResponseDTO.authorId());
    Assertions.assertEquals(group.getId(), todoResponseDTO.groupId());
    Assertions.assertEquals("todo order", todoResponseDTO.order());
  }

  @Test
  void createTodo_Failure_NoAuthority() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_NoUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(user);
    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(group);
    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_NoUserGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_Failure_Duplicated_Order_Status_In_Group() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.TO_DO,
            "todo order",
            false);

    em.flush();

    Principal principal = user::getUsername;

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(principal, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }

  @Test
  void deleteTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    Principal principal = user::getUsername;

    // When

    ResponseEntity<Void> responseEntity =
        todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    Assertions.assertTrue(
        em.createQuery(
                """
                SELECT
                  t
                FROM
                  Todo t
                WHERE
                  t.id = :todoId
                """,
                Todo.class)
            .setParameter("todoId", todo.getId())
            .getResultStream()
            .findFirst()
            .isEmpty());
  }

  @Test
  void deleteTodo_Failure_NoUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(user);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_NoGroupUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(userGroup);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole1() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole2() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    User author = entityFactory.insertUser("username_author", "hashedPassword", "nickname_author");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

    Todo todo =
        entityFactory.insertTodo(
            author.getId(),
            author.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    Principal principal = author::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(group);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(todo);

    em.flush();

    Principal principal = user::getUsername;

    // When

    Runnable lambda = () -> todoController.deleteTodo(principal, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void updateTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    ResponseEntity<TodoDTO> responseEntity =
        todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    TodoDTO todoDTO = responseEntity.getBody();

    Assertions.assertEquals(user.getId(), todoDTO.getAuthorId());
    Assertions.assertEquals(group.getId(), todoDTO.getGroupId());
    Assertions.assertEquals("updated todo title", todoDTO.getTitle());
    Assertions.assertEquals("updated todo description", todoDTO.getDescription());
    Assertions.assertEquals(TodoStatus.DONE, todoDTO.getStatus());
    Assertions.assertEquals("updated todo order", todoDTO.getOrder());

    Todo updatedTodo =
        em.createQuery(
                """
            SELECT
              t
            FROM
              Todo t
            WHERE
              t.id = :todoId
            """,
                Todo.class)
            .setParameter("todoId", todo.getId())
            .getSingleResult();

    Assertions.assertEquals(updatedTodo.getId(), updatedTodo.getId());
    Assertions.assertEquals(user.getId(), updatedTodo.getAuthor().getId());
    Assertions.assertEquals(group.getId(), updatedTodo.getGroup().getId());
    Assertions.assertEquals("updated todo title", updatedTodo.getTitle());
    Assertions.assertEquals("updated todo description", updatedTodo.getDescription());
    Assertions.assertEquals(TodoStatus.DONE, updatedTodo.getTodoStatus());
    Assertions.assertEquals("updated todo order", updatedTodo.getOrder());
  }

  @Test
  void updateTodo_Failure_NoUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(user);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_NoUserGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    em.remove(userGroup);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole1() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole2() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    User author = entityFactory.insertUser("username_author", "hashedPassword", "nickname_author");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

    Todo todo =
        entityFactory.insertTodo(
            author.getId(),
            author.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();
    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = author::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_NoGroup() {
    // Given

    User user = new User();
    user.setUsername("username");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(GroupRole.OWNER);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(group);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_NoTodo() {
    // Given

    User user = new User();
    user.setUsername("username");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(GroupRole.OWNER);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.DONE,
            "updated todo order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(principal, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_Duplicated_Order_Status_In_Group() {
    // Given

    User user = new User();
    user.setUsername("username");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(GroupRole.OWNER);

    em.persist(userGroup);

    Todo todo1 = new Todo();
    todo1.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo1.setTitle("todo title");
    todo1.setDescription("todo description");
    todo1.setOrder("todo1 order");
    todo1.setAuthor(user);
    todo1.setGroup(group);

    Todo todo2 = new Todo();
    todo2.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo2.setTitle("todo title");
    todo2.setDescription("todo description");
    todo2.setOrder("todo2 order");
    todo2.setAuthor(user);
    todo2.setGroup(group);

    em.persist(todo1);
    em.persist(todo2);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.IN_PROGRESS,
            "todo1 order");

    Principal principal = user::getUsername;

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(
                principal, group.getId(), todo2.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }
}
