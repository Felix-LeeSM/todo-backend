package rest.felix.back.todo.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.NullableField;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.*;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.exception.DuplicateTodoOrderException;
import rest.felix.back.todo.exception.TodoNotFoundException;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.entity.User;

@SpringBootTest
@ActiveProfiles("test")
class TodoServiceTest {

  @Autowired private TodoService todoService;
  @Autowired private TodoRepository todoRepository;
  @Autowired private EntityFactory entityFactory;

  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Nested
  @DisplayName("그룹의 모든 Todo 조회 (getTodosInGroup)")
  class GetTodosInGroup {
    @Test
    @DisplayName("성공: 그룹에 속한 모든 Todo 목록을 반환한다")
    void success_whenTodosExistInGroup() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Arrays.stream(new int[] {1, 2, 3})
          .forEach(
              idx -> {
                entityFactory.insertTodo(
                    user.getId(),
                    user.getId(),
                    group.getId(),
                    String.format("todo %d", idx),
                    String.format("todo %d description", idx),
                    TodoStatus.TO_DO,
                    String.format("todo order %d", idx),
                    false);
              });

      // When
      List<TodoDTO> todoDTOs = todoService.getTodosInGroup(group.getId());

      // Then
      Assertions.assertEquals(3, todoDTOs.size());
      Assertions.assertEquals(
          true,
          todoDTOs.stream().map(TodoDTO::status).allMatch(status -> status == TodoStatus.TO_DO));
      Assertions.assertEquals(
          true,
          todoDTOs.stream()
              .map(TodoDTO::groupId)
              .allMatch(groupId -> groupId.equals(group.getId())));
      Assertions.assertEquals(
          true,
          todoDTOs.stream()
              .map(TodoDTO::authorId)
              .allMatch(authorId -> authorId.equals(user.getId())));
      Assertions.assertEquals(
          true,
          todoDTOs.stream()
              .map(TodoDTO::title)
              .toList()
              .containsAll(List.of("todo 1", "todo 2", "todo 3")));
    }

    @Test
    @DisplayName("성공: 그룹에 Todo가 하나도 없을 때 빈 목록을 반환한다")
    void success_whenNoTodosInGroup() {
      // Given
      entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");

      // When
      List<TodoDTO> todoDTOs = todoService.getTodosInGroup(group.getId());

      // Then
      Assertions.assertEquals(0, todoDTOs.size());
    }

    @Test
    @DisplayName("성공: 존재하지 않는 그룹 ID로 조회 시 빈 목록을 반환한다")
    void success_whenGroupNotFound() {
      // Given
      Group group = entityFactory.insertGroup("g", "d");
      th.delete(group);

      // When
      List<TodoDTO> todoDTOs = todoService.getTodosInGroup(group.getId());

      // Then
      Assertions.assertEquals(0, todoDTOs.size());
    }
  }

  @Nested
  @DisplayName("Todo 생성 (createTodo)")
  class CreateTodo {
    @Test
    @DisplayName("성공: 새로운 Todo를 생성한다")
    void success_whenCreatingNewTodo() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title", "todo description", null, user.getId(), group.getId(), null);

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertEquals("todo title", todoDTO.title());
      Assertions.assertEquals("todo description", todoDTO.description());
      Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.status());
      Assertions.assertEquals(user.getId(), todoDTO.authorId());
      Assertions.assertEquals(group.getId(), todoDTO.groupId());
      Assertions.assertNotNull(todoDTO.order());
      Assertions.assertNull(todoDTO.dueDate());
      Assertions.assertNull(todoDTO.assigneeId());
    }

    @Test
    @DisplayName("성공: dueDate와 assigneeId를 포함하여 새로운 Todo를 생성한다")
    void success_whenCreatingNewTodoWithDueDateAndAssignee() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee =
          entityFactory.insertUser("assigneeUser", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      LocalDate dueDate = LocalDate.now().plusDays(7);
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title",
              "todo description",
              dueDate,
              user.getId(),
              group.getId(),
              assignee.getId());

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertEquals("todo title", todoDTO.title());
      Assertions.assertEquals("todo description", todoDTO.description());
      Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.status());
      Assertions.assertEquals(user.getId(), todoDTO.authorId());
      Assertions.assertEquals(group.getId(), todoDTO.groupId());
      Assertions.assertNotNull(todoDTO.order());
      Assertions.assertEquals(dueDate, todoDTO.dueDate());
      Assertions.assertEquals(assignee.getId(), todoDTO.assigneeId());
    }

    @Test
    @DisplayName("성공: dueDate만 포함하여 새로운 Todo를 생성한다")
    void success_whenCreatingNewTodoWithDueDateOnly() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");

      LocalDate dueDate = LocalDate.now().plusDays(7);
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title", "todo description", dueDate, user.getId(), group.getId(), null);

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertEquals("todo title", todoDTO.title());
      Assertions.assertEquals("todo description", todoDTO.description());
      Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.status());
      Assertions.assertEquals(user.getId(), todoDTO.authorId());
      Assertions.assertEquals(group.getId(), todoDTO.groupId());
      Assertions.assertNotNull(todoDTO.order());
      Assertions.assertEquals(dueDate, todoDTO.dueDate());
      Assertions.assertNull(todoDTO.assigneeId());
    }

    @Test
    @DisplayName("성공: assigneeId만 포함하여 새로운 Todo를 생성한다")
    void success_whenCreatingNewTodoWithAssigneeOnly() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee =
          entityFactory.insertUser("assigneeUser", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title",
              "todo description",
              null,
              user.getId(),
              group.getId(),
              assignee.getId());

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertEquals("todo title", todoDTO.title());
      Assertions.assertEquals("todo description", todoDTO.description());
      Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.status());
      Assertions.assertEquals(user.getId(), todoDTO.authorId());
      Assertions.assertEquals(group.getId(), todoDTO.groupId());
      Assertions.assertNotNull(todoDTO.order());
      Assertions.assertNull(todoDTO.dueDate());
      Assertions.assertEquals(assignee.getId(), todoDTO.assigneeId());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 유저 ID로 생성 시 예외가 발생한다")
    void fail_whenUserNotFound() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      th.delete(user);
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title", "todo description", null, user.getId(), group.getId(), null);

      // When
      Runnable lambda = () -> todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 그룹 ID로 생성 시 예외가 발생한다")
    void fail_whenGroupNotFound() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      th.delete(group);
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title", "todo description", null, user.getId(), group.getId(), null);

      // When
      Runnable lambda = () -> todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 assigneeId로 생성 시 예외가 발생한다")
    void fail_whenAssigneeNotFound() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      long invalidAssigneeId = 9999L;
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title",
              "todo description",
              null,
              user.getId(),
              group.getId(),
              invalidAssigneeId);

      // When
      Runnable lambda = () -> todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("성공: order가 null일 때 자동으로 생성된다")
    void success_whenOrderIsNull() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO(
              "todo title", "todo description", null, user.getId(), group.getId(), null);

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertNotNull(todoDTO.order());
      Assertions.assertEquals(false, todoDTO.order().isEmpty());
    }
  }

  @Nested
  @DisplayName("특정 Todo 조회 (getTodoInGroup)")
  class GetTodoInGroup {
    @Test
    @DisplayName("성공: 특정 Todo의 정보를 조회한다")
    void success_whenTodoExists() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.IN_PROGRESS,
              "o",
              false);

      // When
      TodoDTO todoDTO = todoService.getTodoInGroup(group.getId(), todo.getId());

      // Then
      Assertions.assertEquals(todo.getId(), todoDTO.id());
      Assertions.assertEquals(todo.getTodoStatus(), todoDTO.status());
      Assertions.assertEquals(todo.getDescription(), todoDTO.description());
      Assertions.assertEquals(todo.getTitle(), todoDTO.title());
      Assertions.assertEquals(todo.getGroup().getId(), todoDTO.groupId());
      Assertions.assertEquals(todo.getAuthor().getId(), todoDTO.authorId());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Todo ID로 조회 시 예외가 발생한다")
    void fail_whenTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);
      th.delete(todo);

      // When
      Runnable lambda = () -> todoService.getTodoInGroup(group.getId(), todo.getId());

      // Then
      Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: Todo가 요청한 그룹에 속해있지 않으면 예외가 발생한다")
    void fail_whenTodoInDifferentGroup() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1 name", "group1 description");
      Group group2 = entityFactory.insertGroup("group2 name", "group2 description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group1.getId(),
              "t",
              "d",
              TodoStatus.IN_PROGRESS,
              "o",
              false);

      // When
      Runnable lambda = () -> todoService.getTodoInGroup(group2.getId(), todo.getId());

      // Then
      Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("Todo 삭제 (deleteTodo)")
  class DeleteTodo {
    @Test
    @DisplayName("성공: 특정 Todo를 삭제한다")
    void success_whenDeletingTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);

      // When
      todoService.deleteTodo(todo.getId());

      // Then
      Assertions.assertEquals(true, todoRepository.findById(todo.getId()).isEmpty());
    }

    @Test
    @DisplayName("성공(멱등성): 존재하지 않는 Todo ID로 삭제 요청 시 아무 일도 일어나지 않는다")
    void success_whenDeletingNonExistentTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);
      th.delete(todo);

      // When
      Runnable lambda = () -> todoService.deleteTodo(todo.getId());

      // Then
      Assertions.assertDoesNotThrow(lambda::run);
    }

    @Test
    @DisplayName("성공: Todo 삭제 시 관련된 Star 정보도 함께 삭제된다")
    void success_whenTodoIsDeleted_relatedStarsAreAlsoDeleted() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), trio.second().getId(), "t", "d", TodoStatus.TO_DO, false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());
      Assertions.assertEquals(true, todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.deleteTodo(todo.getId());

      // Then
      Assertions.assertEquals(true, todoRepository.findById(todo.getId()).isEmpty());
      Assertions.assertEquals(false, todoRepository.starExistsById(user.getId(), todo.getId()));
    }
  }

  @Nested
  @DisplayName("Todo 수정 (updateTodo)")
  class UpdateTodo {
    @Test
    @DisplayName("성공: Todo의 제목과 설명을 수정한다")
    void success_whenUpdatingTitleAndDescription() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);
      UpdateTodoDTO updateTodoDTO =
          new UpdateTodoDTO(todo.getId(), "todo updated title", "todo updated description");

      // When
      TodoDTO todoDTO = todoService.updateTodo(updateTodoDTO);
      TodoDTO updatedTodo =
          todoRepository.findById(todo.getId()).orElseThrow(TodoNotFoundException::new);

      // Then
      Assertions.assertEquals(todo.getId(), todoDTO.id());
      Assertions.assertEquals("todo updated title", todoDTO.title());
      Assertions.assertEquals("todo updated description", todoDTO.description());
      Assertions.assertEquals("todo updated title", updatedTodo.title());
      Assertions.assertEquals("todo updated description", updatedTodo.description());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Todo ID로 수정 요청 시 예외가 발생한다")
    void fail_whenTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);
      th.delete(todo);
      UpdateTodoDTO updateTodoDTO =
          new UpdateTodoDTO(todo.getId(), "updated todo title", "updated todo description");

      // When
      Runnable lambda = () -> todoService.updateTodo(updateTodoDTO);

      // Then
      Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("성공: 제목만 업데이트하고 설명은 그대로 둔다")
    void success_whenUpdatingOnlyTitle() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "original title",
              "original description",
              TodoStatus.TO_DO,
              false);
      UpdateTodoDTO updateTodoDTO = new UpdateTodoDTO(todo.getId(), "updated title", null);

      // When
      TodoDTO todoDTO = todoService.updateTodo(updateTodoDTO);
      TodoDTO updatedTodo =
          todoRepository.findById(todo.getId()).orElseThrow(TodoNotFoundException::new);

      // Then
      Assertions.assertEquals(todo.getId(), todoDTO.id());
      Assertions.assertEquals("updated title", todoDTO.title());
      Assertions.assertEquals("original description", todoDTO.description());
      Assertions.assertEquals("updated title", updatedTodo.title());
      Assertions.assertEquals("original description", updatedTodo.description());
    }

    @Test
    @DisplayName("성공: 설명만 업데이트하고 제목은 그대로 둔다")
    void success_whenUpdatingOnlyDescription() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "original title",
              "original description",
              TodoStatus.TO_DO,
              false);
      UpdateTodoDTO updateTodoDTO = new UpdateTodoDTO(todo.getId(), null, "updated description");

      // When
      TodoDTO todoDTO = todoService.updateTodo(updateTodoDTO);
      TodoDTO updatedTodo =
          todoRepository.findById(todo.getId()).orElseThrow(TodoNotFoundException::new);

      // Then
      Assertions.assertEquals(todo.getId(), todoDTO.id());
      Assertions.assertEquals("original title", todoDTO.title());
      Assertions.assertEquals("updated description", todoDTO.description());
      Assertions.assertEquals("original title", updatedTodo.title());
      Assertions.assertEquals("updated description", updatedTodo.description());
    }
  }

  @Nested
  @DisplayName("권한 검증 (assertTodoAuthority) 테스트")
  class AssertTodoAuthority {
    @Nested
    @DisplayName("Todo 접급 권한 (Predicate<TodoDTO> 버전)")
    class AssertTodoAuthorityPredicate {

      @Test
      @DisplayName("모든 조건(유저, 그룹, Todo)이 유효하고 Predicate가 true를 반환하면 예외가 발생하지 않는다")
      void success_whenAllConditionsMetAndPredicateIsTrue() {
        // Given
        var trio = entityFactory.insertUserGroup();
        User user = trio.first();
        Group group = trio.second();
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), t -> true);

        // Then
        Assertions.assertDoesNotThrow(lambda::run);
      }

      @Test
      @DisplayName("유저가 그룹에 속해있지 않으면 UserAccessDeniedException이 발생한다")
      void fail_whenUserNotInGroup() {
        // Given
        User user = entityFactory.insertUser("u", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), t -> true);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound() {
        // Given
        var trio = entityFactory.insertUserGroup();
        User user = trio.first();
        Group group = trio.second();
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
        th.delete(todo);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), t -> true);

        // Then
        Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
      }

      @Test
      @DisplayName("모든 조건이 유효하더라도 Predicate가 false를 반환하면 UserAccessDeniedException이 발생한다")
      void fail_whenPredicateIsFalse() {
        // Given
        var trio = entityFactory.insertUserGroup();
        User user = trio.first();
        Group group = trio.second();
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), t -> false);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }
    }

    @Nested
    @DisplayName("Todo 접급 권한 (BiPredicate<GroupRole, TodoDTO> 버전)")
    class AssertTodoAuthorityBiPredicate {
      @DisplayName("관리자(OWNER, MANAGER)는 작성자와 상관없이 항상 통과한다")
      @ParameterizedTest
      @EnumSource(
          value = GroupRole.class,
          names = {"OWNER", "MANAGER"})
      void success_forAdminRoles_regardlessOfAuthor(GroupRole adminRole) {
        // Given
        User admin = entityFactory.insertUser("admin", "p", "n");
        User author = entityFactory.insertUser("author", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(admin.getId(), group.getId(), adminRole);
        Todo todo =
            entityFactory.insertTodo(
                author.getId(), author.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    admin.getId(), group.getId(), todo.getId(), (role, t) -> true);

        // Then
        Assertions.assertDoesNotThrow(lambda::run);
      }

      @Test
      @DisplayName("멤버(MEMBER)는 자신이 작성한 Todo에 한해 통과한다")
      void success_forMember_whenIsAuthor() {
        // Given
        User member = entityFactory.insertUser("member", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(member.getId(), group.getId(), GroupRole.MEMBER);
        Todo todo =
            entityFactory.insertTodo(
                member.getId(), member.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    member.getId(),
                    group.getId(),
                    todo.getId(),
                    (role, t) -> t.authorId() == member.getId());

        // Then
        Assertions.assertDoesNotThrow(lambda::run);
      }

      @DisplayName("뷰어(VIEWER)는 어떤 조건이든 UserAccessDeniedException이 발생한다")
      @ParameterizedTest
      @EnumSource(
          value = GroupRole.class,
          names = {"VIEWER"})
      void fail_forViewerRole(GroupRole viewerRole) {
        // Given
        User viewer = entityFactory.insertUser("viewer", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(viewer.getId(), group.getId(), viewerRole);
        Todo todo =
            entityFactory.insertTodo(
                viewer.getId(), viewer.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    viewer.getId(), group.getId(), todo.getId(), (role, t) -> false);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("멤버(MEMBER)가 타인이 작성한 Todo에 접근하면 UserAccessDeniedException이 발생한다")
      void fail_forMember_whenIsNotAuthor() {
        // Given
        User member = entityFactory.insertUser("member", "p", "n");
        User author = entityFactory.insertUser("author", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(member.getId(), group.getId(), GroupRole.MEMBER);
        Todo todo =
            entityFactory.insertTodo(
                author.getId(), author.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    member.getId(),
                    group.getId(),
                    todo.getId(),
                    (role, t) -> t.authorId() == member.getId());

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("유저가 그룹에 속해있지 않으면 UserAccessDeniedException이 발생한다")
      void fail_whenUserNotInGroup_BiPredicate() {
        // Given
        User user = entityFactory.insertUser("u", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), (role, t) -> true);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound_BiPredicate() {
        // Given
        var trio = entityFactory.insertUserGroup();
        User user = trio.first();
        Group group = trio.second();
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
        th.delete(todo);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), (role, t) -> true);

        // Then
        Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
      }
    }

    @Nested
    @DisplayName("Todo 접급 권한 (GroupRole 요구사항 버전)")
    class AssertTodoAuthorityRole {

      private static Stream<Arguments> provideSufficientRoles() {
        return Stream.of(
            Arguments.of(GroupRole.OWNER, GroupRole.OWNER),
            Arguments.of(GroupRole.OWNER, GroupRole.MANAGER),
            Arguments.of(GroupRole.OWNER, GroupRole.MEMBER),
            Arguments.of(GroupRole.OWNER, GroupRole.VIEWER),
            Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER),
            Arguments.of(GroupRole.MANAGER, GroupRole.MEMBER),
            Arguments.of(GroupRole.MANAGER, GroupRole.VIEWER),
            Arguments.of(GroupRole.MEMBER, GroupRole.MEMBER),
            Arguments.of(GroupRole.MEMBER, GroupRole.VIEWER),
            Arguments.of(GroupRole.VIEWER, GroupRole.VIEWER));
      }

      private static Stream<Arguments> provideInsufficientRoles() {
        return Stream.of(
            Arguments.of(GroupRole.MEMBER, GroupRole.MANAGER),
            Arguments.of(GroupRole.MEMBER, GroupRole.OWNER),
            Arguments.of(GroupRole.VIEWER, GroupRole.MEMBER));
      }

      @DisplayName("요구되는 역할보다 유저의 실제 역할이 같거나 높으면 예외가 발생하지 않는다")
      @ParameterizedTest
      @MethodSource("provideSufficientRoles")
      void success_whenUserRoleIsGteRequiredRole(GroupRole userActualRole, GroupRole requiredRole) {
        // Given
        User user = entityFactory.insertUser("u", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(user.getId(), group.getId(), userActualRole);
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), requiredRole);

        // Then
        Assertions.assertDoesNotThrow(lambda::run);
      }

      @DisplayName("요구되는 역할보다 유저의 실제 역할이 낮으면 UserAccessDeniedException이 발생한다")
      @ParameterizedTest
      @MethodSource("provideInsufficientRoles")
      void fail_whenUserRoleIsLowerThanRequiredRole(
          GroupRole userActualRole, GroupRole requiredRole) {
        // Given
        User user = entityFactory.insertUser("u", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        entityFactory.insertUserGroup(user.getId(), group.getId(), userActualRole);
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), requiredRole);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("유저가 그룹에 속해있지 않으면 UserAccessDeniedException이 발생한다")
      void fail_whenUserNotInGroup_Role() {
        // Given
        User user = entityFactory.insertUser("u", "p", "n");
        Group group = entityFactory.insertGroup("g", "d");
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), GroupRole.VIEWER);

        // Then
        Assertions.assertThrows(
            rest.felix.back.user.exception.UserAccessDeniedException.class, lambda::run);
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound_Role() {
        // Given
        var trio = entityFactory.insertUserGroup();
        User user = trio.first();
        Group group = trio.second();
        Todo todo =
            entityFactory.insertTodo(
                user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
        th.delete(todo);

        // When
        Runnable lambda =
            () ->
                todoService.assertTodoAuthority(
                    user.getId(), group.getId(), todo.getId(), GroupRole.VIEWER);

        // Then
        Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
      }
    }
  }

  @Nested
  @DisplayName("Todo 메타데이터 수정 (updateTodoMetadata)")
  class UpdateTodoMetadata {
    private static Stream<Arguments> isImportantParameter() {
      return Stream.of(
          Arguments.of(true, false),
          Arguments.of(false, true),
          Arguments.of(true, true),
          Arguments.of(false, false));
    }

    private static Stream<Arguments> dueDateParameter() {
      LocalDate now = LocalDate.now();
      return Stream.of(
          Arguments.of(now, now.plusDays(3)),
          Arguments.of(null, now.minusDays(2)),
          Arguments.of(now.minusDays(20), null),
          Arguments.of(null, now.plusDays(30)));
    }

    @Test
    @DisplayName("성공: Todo의 메타데이터(중요도, 마감일, 담당자)를 수정한다")
    void success_whenUpdatingAllMetadataFields() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      User assignee = entityFactory.insertUser("assignee", "p", "n");
      Group group = trio.second();
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Present<>(true),
              new NullableField.Present<>(LocalDate.now()),
              new NullableField.Present<>(assignee.getId()));

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertNotNull(result);
      Assertions.assertEquals(true, result.isImportant());
      Assertions.assertEquals(LocalDate.now(), result.dueDate());
      Assertions.assertEquals(assignee.getId(), result.assigneeId());
    }

    @ParameterizedTest
    @MethodSource("isImportantParameter")
    @DisplayName("성공 - isImportant만 업데이트")
    void success_whenUpdatingOnlyIsImportant(Boolean current, Boolean toUpdate) {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, current);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Present<>(toUpdate),
              new NullableField.Absent<>(),
              new NullableField.Absent<>());

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertEquals(toUpdate, result.isImportant());
    }

    @ParameterizedTest
    @MethodSource("dueDateParameter")
    @DisplayName("성공: dueDate만 수정한다")
    void success_whenUpdatingOnlyDueDate(LocalDate current, LocalDate toUpdate) {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              current,
              false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Absent<>(),
              new NullableField.Present<>(toUpdate),
              new NullableField.Absent<>());

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertEquals(toUpdate, result.dueDate());
    }

    @Test
    @DisplayName("성공: assigneeId만 추가한다")
    void success_whenSettingOnlyAssigneeId() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      User assignee = entityFactory.insertUser("assignee", "p", "n");
      Group group = trio.second();
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Absent<>(),
              new NullableField.Absent<>(),
              new NullableField.Present<>(assignee.getId()));

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertEquals(assignee.getId(), result.assigneeId());
    }

    @Test
    @DisplayName("성공: assigneeId만 수정한다")
    void success_whenUpdatingOnlyAssigneeId() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      User originalAssignee = entityFactory.insertUser("original", "p", "n");
      User newAssignee = entityFactory.insertUser("new", "p", "n");
      Group group = trio.second();
      entityFactory.insertUserGroup(originalAssignee.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(newAssignee.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              originalAssignee.getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              "a",
              false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Absent<>(),
              new NullableField.Absent<>(),
              new NullableField.Present<>(newAssignee.getId()));

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertEquals(newAssignee.getId(), result.assigneeId());
    }

    @Test
    @DisplayName("성공: assigneeId를 null로 설정하여 담당자를 해제한다")
    void success_whenSettingAssigneeIdToNull() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      User assignee = entityFactory.insertUser("assignee", "p", "n");
      Group group = trio.second();
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              assignee.getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              "a",
              false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Absent<>(),
              new NullableField.Absent<>(),
              new NullableField.Present<>(null));

      // When
      TodoDTO result = todoService.updateTodoMetadata(requestDTO);

      // Then
      Assertions.assertNull(result.assigneeId());
    }

    @Test
    @DisplayName("실패 - isImportant를 null로 변경하려 함.")
    void Failure_IsImportantIsNotNull() {
      // Given

      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      User assignee = entityFactory.insertUser("assignee", "p", "n");
      Group group = trio.second();
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              assignee.getId(),
              group.getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              "a",
              false);

      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Present<>(null),
              new NullableField.Absent<>(),
              new NullableField.Absent<>());

      // When
      Runnable lambda = () -> todoService.updateTodoMetadata(requestDTO);

      // Then

      Assertions.assertThrows(NullPointerException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Todo ID로 수정 요청 시 예외가 발생한다")
    void fail_whenTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              999L,
              new NullableField.Present<>(true),
              new NullableField.Absent<>(),
              new NullableField.Absent<>());

      // When / Then
      Assertions.assertThrows(
          TodoNotFoundException.class,
          () -> {
            todoService.updateTodoMetadata(requestDTO);
          });
    }

    @Test
    @DisplayName("실패: 존재하지 않는 assigneeId로 수정 요청 시 예외가 발생한다")
    void fail_whenAssigneeNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      UpdateTodoMetadataDTO requestDTO =
          new UpdateTodoMetadataDTO(
              todo.getId(),
              new NullableField.Absent<>(),
              new NullableField.Absent<>(),
              new NullableField.Present<>(999L));

      // When / Then
      Assertions.assertThrows(
          DataIntegrityViolationException.class,
          () -> {
            todoService.updateTodoMetadata(requestDTO);
          });
    }
  }

  @Nested
  @DisplayName("Todo 위치 및 상태 이동 (moveTodo)")
  class MoveTodo {
    @Test
    @DisplayName("성공: Todo를 다른 상태(column)의 특정 위치로 이동시킨다")
    void success_whenMovingTodoToSpecificPositionInAnotherStatus() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo targetTodo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "target",
              "d",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo destTodo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "dest",
              "d",
              TodoStatus.IN_PROGRESS,
              "c",
              false);

      MoveTodoDTO moveTodoDTO = new MoveTodoDTO(targetTodo.getId(), TodoStatus.IN_PROGRESS, "b");

      // When
      todoService.moveTodo(moveTodoDTO);

      // Then
      TodoDTO movedTodo = todoRepository.findById(targetTodo.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.IN_PROGRESS, movedTodo.status());
      Assertions.assertEquals("b", movedTodo.order());
    }

    @Test
    @DisplayName("성공: Todo 순서를 같은 상태 내에서 변경한다")
    void success_whenReorderingTodoWithinSameStatus() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo targetTodo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "target",
              "d",
              TodoStatus.TO_DO,
              "c",
              false);

      MoveTodoDTO moveTodoDTO = new MoveTodoDTO(targetTodo.getId(), TodoStatus.TO_DO, "d");

      // When
      todoService.moveTodo(moveTodoDTO);

      // Then
      TodoDTO movedTodo = todoRepository.findById(targetTodo.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.TO_DO, movedTodo.status());
      Assertions.assertEquals("d", movedTodo.order());
    }

    @Test
    @DisplayName("실패: 이동 대상 Todo를 찾을 수 없으면 예외가 발생한다")
    void fail_whenTargetTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);
      th.delete(todo);

      MoveTodoDTO moveTodoDTO = new MoveTodoDTO(todo.getId(), TodoStatus.IN_PROGRESS, "b");

      // When
      Runnable lambda = () -> todoService.moveTodo(moveTodoDTO);

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 이동 대상 Todo를 찾을 수 없으면 예외가 발생한다")
    void fail_whenDuplicatedOrderAndStatus() {
      // Given
      var trio = entityFactory.insertUserGroup();
      Todo todo1 =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);

      Todo todo2 =
          entityFactory.insertTodo(
              trio.first().getId(),
              trio.first().getId(),
              trio.second().getId(),
              "t",
              "d",
              TodoStatus.TO_DO,
              false);

      MoveTodoDTO moveTodoDTO = new MoveTodoDTO(todo2.getId(), TodoStatus.TO_DO, todo1.getOrder());

      // When
      Runnable lambda = () -> todoService.moveTodo(moveTodoDTO);

      // Then
      Assertions.assertThrows(DuplicateTodoOrderException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 삭제된 Todo를 이동하려 할 때 예외가 발생한다")
    void fail_whenMovingDeletedTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, "a", false);
      todoService.deleteTodo(todo.getId());

      MoveTodoDTO moveTodoDTO = new MoveTodoDTO(todo.getId(), TodoStatus.TO_DO, "asdf");

      // When
      Runnable lambda = () -> todoService.moveTodo(moveTodoDTO);

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("Todo star 표기 (starTodo)")
  class StarTodo {

    @Test
    @DisplayName("성공: 아직 Star(좋아요)하지 않은 Todo에 Star를 추가한다")
    void success_whenStarringANonStarredTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      Assertions.assertEquals(false, todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertEquals(true, todoRepository.starExistsById(user.getId(), todo.getId()));
    }

    @Test
    @DisplayName("성공(멱등성): 이미 Star(좋아요)한 Todo에 다시 Star를 요청해도 아무 일이 일어나지 않는다")
    void success_whenStarringAnAlreadyStarredTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());
      Assertions.assertEquals(true, todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertEquals(true, todoRepository.starExistsById(user.getId(), todo.getId()));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Todo에 Star를 요청하면 예외가 발생한다")
    void fail_whenTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), trio.second().getId(), "t", "d", TodoStatus.TO_DO, false);
      th.delete(todo);

      // When
      Runnable lambda = () -> todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 삭제된 Todo에 Star를 요청하면 예외가 발생한다")
    void fail_whenStarringDeletedTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);

      todoService.deleteTodo(todo.getId());

      // When
      Runnable lambda = () -> todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("Todo star 제거 (unstarTodo)")
  class UnstarTodo {

    @Test
    @DisplayName("성공: Star(좋아요)한 Todo의 Star를 제거한다")
    void success_whenUnstarringAStarredTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());
      Assertions.assertEquals(true, todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.unstarTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertEquals(false, todoRepository.starExistsById(user.getId(), todo.getId()));
    }

    @Test
    @DisplayName("성공(멱등성): Star(좋아요)하지 않은 Todo의 Star를 제거해도 아무 일이 일어나지 않는다")
    void success_whenUnstarringANonStarredTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, false);
      Assertions.assertEquals(false, todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.unstarTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertEquals(false, todoRepository.starExistsById(user.getId(), todo.getId()));
    }

    @Test
    @DisplayName("성공(멱등성): 삭제된 Todo의 Star를 제거해도 아무 일이 일어나지 않는다")
    void success_whenUnstarringDeletedTodo() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), trio.second().getId(), "t", "d", TodoStatus.TO_DO, false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());
      todoService.deleteTodo(todo.getId());

      // When
      Runnable lambda = () -> todoService.unstarTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertDoesNotThrow(lambda::run);
    }
  }
}
