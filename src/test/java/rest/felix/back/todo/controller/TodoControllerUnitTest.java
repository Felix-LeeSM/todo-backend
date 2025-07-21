package rest.felix.back.todo.controller;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.common.util.Trio;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.CreateTodoRequestDTO;
import rest.felix.back.todo.dto.MoveTodoRequestDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.TodoResponseDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.exception.TodoNotFoundException;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
public class TodoControllerUnitTest {

  @Autowired private EntityManager em;
  @Autowired private TodoController todoController;
  @Autowired private EntityFactory entityFactory;
  @Autowired private TodoRepository todoRepository;
  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Test
  void getTodos_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    ResponseEntity<List<TodoResponseDTO>> responseEntity =
        todoController.getTodos(authUser, group.getId());

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

    entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    ResponseEntity<List<TodoResponseDTO>> responseEntity =
        todoController.getTodos(authUser, group.getId());

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

    th.delete(user);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.getTodos(authUser, group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void getTodos_Failure_Group() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    th.delete(userGroup);
    th.delete(group);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.getTodos(authUser, group.getId());

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

    th.delete(userGroup);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.getTodos(authUser, group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    ResponseEntity<TodoResponseDTO> responseEntity =
        todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

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

    entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

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

    th.delete(userGroup);
    th.delete(user);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    th.delete(userGroup);
    th.delete(group);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

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

    th.delete(userGroup);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_Failure_Duplicated_Order_Status_In_Group() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    entityFactory.insertTodo(
        user.getId(),
        user.getId(),
        group.getId(),
        "todo title",
        "todo description",
        TodoStatus.TO_DO,
        "todo order",
        false);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    // When

    Runnable lambda =
        () -> todoController.createTodo(authUser, group.getId(), createTodoRequestDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }

  @Test
  void deleteTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    ResponseEntity<Void> responseEntity =
        todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());

    Assertions.assertTrue(todoRepository.findById(todo.getId()).isEmpty());
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

    th.delete(todo);
    th.delete(userGroup);
    th.delete(user);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
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

    th.delete(userGroup);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole1() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole2() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    User author = entityFactory.insertUser("username_author", "hashedPassword", "nickname_author");

    Group group = entityFactory.insertGroup("group", "description");

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

    AuthUserDTO authUser = AuthUserDTO.of(author);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

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

    th.delete(todo);
    th.delete(userGroup);
    th.delete(group);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void deleteTodo_Failure_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    th.delete(todo);

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda = () -> todoController.deleteTodo(authUser, group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
  }

  @Test
  void updateTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    ResponseEntity<TodoDTO> responseEntity =
        todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

    TodoDTO todoDTO = responseEntity.getBody();

    Assertions.assertEquals(user.getId(), todoDTO.getAuthorId());
    Assertions.assertEquals(group.getId(), todoDTO.getGroupId());
    Assertions.assertEquals("updated todo title", todoDTO.getTitle());
    Assertions.assertEquals("updated todo description", todoDTO.getDescription());

    TodoDTO updatedTodo = todoRepository.findById(todo.getId()).orElseThrow();

    Assertions.assertEquals(updatedTodo.getId(), updatedTodo.getId());
    Assertions.assertEquals(user.getId(), updatedTodo.getAuthorId());
    Assertions.assertEquals(group.getId(), updatedTodo.getGroupId());
    Assertions.assertEquals("updated todo title", updatedTodo.getTitle());
    Assertions.assertEquals("updated todo description", updatedTodo.getDescription());
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

    th.delete(todo);
    th.delete(userGroup);
    th.delete(user);

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
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

    th.delete(userGroup);

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole1() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group", "description");

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

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole2() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    User author = entityFactory.insertUser("username_author", "hashedPassword", "nickname_author");

    Group group = entityFactory.insertGroup("group", "description");

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

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(author);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "password", "nickname");
    Group group = entityFactory.insertGroup("group", "description");
    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            null,
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            false);

    th.delete(todo);
    th.delete(userGroup);
    th.delete(group);

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void updateTodo_Failure_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "password", "nickname");
    Group group = entityFactory.insertGroup("group", "description");
    entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            null,
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            false);

    th.delete(todo);

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO("updated todo title", "updated todo description");

    AuthUserDTO authUser = AuthUserDTO.of(user);

    // When

    Runnable lambda =
        () ->
            todoController.updateTodo(authUser, group.getId(), todo.getId(), updateTodoRequestDTO);

    // Then

    Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
  }

  @Nested
  @DisplayName("투두 순서 및 상태 변경 테스트")
  class MoveTodo {
    @Test
    @DisplayName("성공 - 상태만 변경 (맨 뒤로 이동)")
    void HappyPath_1() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);

      entityFactory.insertTodo(
          user.getId(),
          user.getId(),
          group.getId(),
          "todo2",
          "desc2",
          TodoStatus.TO_DO,
          "b",
          false);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      ResponseEntity<TodoResponseDTO> responseEntity =
          todoController.moveTodo(authUser, group.getId(), todo1.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      TodoResponseDTO todoResponseDTO = responseEntity.getBody();
      Assertions.assertEquals(todo1.getId(), todoResponseDTO.id());
      Assertions.assertEquals(TodoStatus.IN_PROGRESS, todoResponseDTO.status());
      Assertions.assertNotNull(todoResponseDTO.order());
    }

    @Test
    @DisplayName("성공 - 상태 및 순서 변경")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo2",
              "desc2",
              TodoStatus.IN_PROGRESS,
              "b",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO =
          new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, todo2.getId());

      // When
      ResponseEntity<TodoResponseDTO> responseEntity =
          todoController.moveTodo(authUser, group.getId(), todo1.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      TodoResponseDTO todoResponseDTO = responseEntity.getBody();
      Assertions.assertEquals(todo1.getId(), todoResponseDTO.id());
      Assertions.assertEquals(TodoStatus.IN_PROGRESS, todoResponseDTO.status());
      Assertions.assertTrue(todoResponseDTO.order().compareTo(todo2.getOrder()) < 0);
    }

    @Test
    @DisplayName("성공 - 순서를 여러회 변경 후 정렬 순서")
    void HappyPath_3() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo2",
              "desc2",
              TodoStatus.TO_DO,
              "b",
              false);
      Todo todo3 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo3",
              "desc3",
              TodoStatus.TO_DO,
              "c",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // todo2를 todo1 앞으로 -> todo2 todo1 todo3 순
      todoController.moveTodo(
          authUser,
          group.getId(),
          todo2.getId(),
          new MoveTodoRequestDTO(TodoStatus.TO_DO, todo1.getId()));
      // todo3을 todo1 앞으로 -> todo2 todo3 todo1순
      todoController.moveTodo(
          authUser,
          group.getId(),
          todo3.getId(),
          new MoveTodoRequestDTO(TodoStatus.TO_DO, todo1.getId()));

      // When
      ResponseEntity<List<TodoResponseDTO>> responseEntity =
          todoController.getTodos(authUser, group.getId());

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      List<Long> actualIds =
          responseEntity.getBody().stream()
              .sorted(Comparator.comparing(TodoResponseDTO::order))
              .map(TodoResponseDTO::id)
              .collect(Collectors.toList());

      Assertions.assertIterableEquals(
          List.of(todo2.getId(), todo3.getId(), todo1.getId()), actualIds);
    }

    @Test
    @DisplayName("실패 - 권한 부족 - VIEWER")
    void Failure_NoAuthority() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      Runnable lambda =
          () -> todoController.moveTodo(authUser, group.getId(), todo.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(userGroup);
      th.delete(todo);
      th.delete(group);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      Runnable lambda =
          () -> todoController.moveTodo(authUser, group.getId(), todo.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      Runnable lambda =
          () -> todoController.moveTodo(authUser, group.getId(), todo.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 Todo를 이동하기")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      th.delete(todo);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      Runnable lambda =
          () -> todoController.moveTodo(authUser, group.getId(), todo.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo를 옮기기")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);

      // When
      Runnable lambda =
          () ->
              todoController.moveTodo(
                  authUser, group1.getId(), todoInGroup2.getId(), moveTodoRequestDTO);

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("투두 Star 추가 테스트")
  class StarTodo {
    @ParameterizedTest
    @EnumSource(GroupRole.class)
    @DisplayName("성공 - 서로 다른 role에 대해 성공")
    void HappyPath_1(GroupRole role) throws Exception {
      // Given
      User user =
          entityFactory.insertUser("username" + role.toString(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> responseEntity =
          todoController.starTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Test
    @DisplayName("성공 - 이미 star 표기된 케이스")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> responseEntity =
          todoController.starTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }

    @Test
    @DisplayName("실패 - 그룹이 없는 경우")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(userGroup);
      th.delete(todo);
      th.delete(group);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.starTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.starTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 Todo")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(todo);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.starTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda =
          () -> todoController.starTodo(authUser, group1.getId(), todoInGroup2.getId());

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("투두 Star 제거 테스트")
  class UnStarTodo {
    @ParameterizedTest
    @EnumSource(GroupRole.class)
    @DisplayName("성공 - 서로 다른 role에 대해 성공")
    void HappyPath_1(GroupRole role) throws Exception {
      // Given
      User user =
          entityFactory.insertUser("username" + role.toString(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> responseEntity =
          todoController.unstarTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    @DisplayName("성공 - star가 없는 케이스")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> responseEntity =
          todoController.unstarTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }

    @Test
    @DisplayName("실패 - 그룹이 없는 경우")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      long wrongGroupId = group.getId() + 1;
      th.delete(userGroup);
      th.delete(todo);
      th.delete(group);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.unstarTodo(authUser, wrongGroupId, todo.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.unstarTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 Todo")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(todo);

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> todoController.unstarTodo(authUser, group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      entityFactory.insertUserTodoStar(user.getId(), todoInGroup2.getId());

      AuthUserDTO authUser = AuthUserDTO.of(user);

      // When
      Runnable lambda =
          () -> todoController.unstarTodo(authUser, group1.getId(), todoInGroup2.getId());

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }
  }
}
