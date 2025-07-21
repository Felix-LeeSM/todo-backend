package rest.felix.back.todo.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.CreateTodoDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
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
      Assertions.assertTrue(
          todoDTOs.stream().map(TodoDTO::getStatus).allMatch(status -> status == TodoStatus.TO_DO));
      Assertions.assertTrue(
          todoDTOs.stream()
              .map(TodoDTO::getGroupId)
              .allMatch(groupId -> groupId.equals(group.getId())));
      Assertions.assertTrue(
          todoDTOs.stream()
              .map(TodoDTO::getAuthorId)
              .allMatch(authorId -> authorId.equals(user.getId())));
      Assertions.assertTrue(
          todoDTOs.stream()
              .map(TodoDTO::getTitle)
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
          new CreateTodoDTO("todo title", "todo description", user.getId(), group.getId());

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertEquals("todo title", todoDTO.getTitle());
      Assertions.assertEquals("todo description", todoDTO.getDescription());
      Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.getStatus());
      Assertions.assertEquals(user.getId(), todoDTO.getAuthorId());
      Assertions.assertEquals(group.getId(), todoDTO.getGroupId());
      Assertions.assertNotNull(todoDTO.getOrder());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 유저 ID로 생성 시 예외가 발생한다")
    void fail_whenUserNotFound() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      th.delete(user);
      CreateTodoDTO createTodoDTO =
          new CreateTodoDTO("todo title", "todo description", user.getId(), group.getId());

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
          new CreateTodoDTO("todo title", "todo description", user.getId(), group.getId());

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
          new CreateTodoDTO("todo title", "todo description", user.getId(), group.getId());

      // When
      TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

      // Then
      Assertions.assertNotNull(todoDTO.getOrder());
      Assertions.assertFalse(todoDTO.getOrder().isEmpty());
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
      Assertions.assertEquals(todo.getId(), todoDTO.getId());
      Assertions.assertEquals(todo.getTodoStatus(), todoDTO.getStatus());
      Assertions.assertEquals(todo.getDescription(), todoDTO.getDescription());
      Assertions.assertEquals(todo.getTitle(), todoDTO.getTitle());
      Assertions.assertEquals(todo.getGroup().getId(), todoDTO.getGroupId());
      Assertions.assertEquals(todo.getAuthor().getId(), todoDTO.getAuthorId());
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
      Assertions.assertTrue(todoRepository.findById(todo.getId()).isEmpty());
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
      Assertions.assertTrue(todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.deleteTodo(todo.getId());

      // Then
      Assertions.assertTrue(todoRepository.findById(todo.getId()).isEmpty());
      Assertions.assertFalse(todoRepository.starExistsById(user.getId(), todo.getId()));
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
      Assertions.assertEquals(todo.getId(), todoDTO.getId());
      Assertions.assertEquals("todo updated title", todoDTO.getTitle());
      Assertions.assertEquals("todo updated description", todoDTO.getDescription());
      Assertions.assertEquals("todo updated title", updatedTodo.getTitle());
      Assertions.assertEquals("todo updated description", updatedTodo.getDescription());
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
      Assertions.assertEquals(todo.getId(), todoDTO.getId());
      Assertions.assertEquals("updated title", todoDTO.getTitle());
      Assertions.assertEquals("original description", todoDTO.getDescription());
      Assertions.assertEquals("updated title", updatedTodo.getTitle());
      Assertions.assertEquals("original description", updatedTodo.getDescription());
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
      Assertions.assertEquals(todo.getId(), todoDTO.getId());
      Assertions.assertEquals("original title", todoDTO.getTitle());
      Assertions.assertEquals("updated description", todoDTO.getDescription());
      Assertions.assertEquals("original title", updatedTodo.getTitle());
      Assertions.assertEquals("updated description", updatedTodo.getDescription());
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
                    (role, t) -> t.getAuthorId() == member.getId());

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
                    (role, t) -> t.getAuthorId() == member.getId());

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

      // 실패하는 역할 조합을 제공하는 Stream
      private static Stream<Arguments> provideInsufficientRoles() {
        return Stream.of(
            Arguments.of(GroupRole.MEMBER, GroupRole.MANAGER),
            Arguments.of(GroupRole.MEMBER, GroupRole.OWNER),
            Arguments.of(GroupRole.VIEWER, GroupRole.MEMBER));
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
  @DisplayName("Todo 위치 및 상태 이동 (moveTodo)")
  class MoveTodo {

    @Test
    @DisplayName("성공: Todo를 다른 상태(column)의 가장 끝으로 이동시킨다")
    void success_whenMovingTodoToEndOfAnotherStatus() {
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
      entityFactory.insertTodo(
          user.getId(),
          user.getId(),
          group.getId(),
          "other",
          "d",
          TodoStatus.IN_PROGRESS,
          "b",
          false);

      // When
      todoService.moveTodo(targetTodo.getId(), null, TodoStatus.IN_PROGRESS);

      // Then
      TodoDTO movedTodo = todoRepository.findById(targetTodo.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.IN_PROGRESS, movedTodo.getStatus());
      Assertions.assertTrue(movedTodo.getOrder().compareTo("b") > 0);
    }

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

      // When
      todoService.moveTodo(targetTodo.getId(), destTodo.getId(), TodoStatus.IN_PROGRESS);

      // Then
      TodoDTO movedTodo = todoRepository.findById(targetTodo.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.IN_PROGRESS, movedTodo.getStatus());
      Assertions.assertTrue(movedTodo.getOrder().compareTo("c") < 0);
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
      Todo destTodo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "dest", "d", TodoStatus.TO_DO, "a", false);

      // When
      todoService.moveTodo(targetTodo.getId(), destTodo.getId(), TodoStatus.TO_DO);

      // Then
      TodoDTO movedTodo = todoRepository.findById(targetTodo.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.TO_DO, movedTodo.getStatus());
      Assertions.assertTrue(movedTodo.getOrder().compareTo("a") < 0);
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

      // When
      Runnable lambda = () -> todoService.moveTodo(todo.getId(), null, TodoStatus.IN_PROGRESS);

      // Then
      Assertions.assertThrows(TodoNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 목표 위치(destination) Todo를 찾을 수 없으면 예외가 발생한다")
    void fail_whenDestinationTodoNotFound() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo targetTodo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, "a", false);
      Todo destTodo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "t2",
              "d2",
              TodoStatus.IN_PROGRESS,
              "b",
              false);
      th.delete(destTodo);

      // When
      Runnable lambda =
          () -> todoService.moveTodo(targetTodo.getId(), destTodo.getId(), TodoStatus.IN_PROGRESS);

      // Then
      Assertions.assertThrows(
          rest.felix.back.todo.exception.DestinationNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 대상과 목표 위치의 그룹이 다르면 예외가 발생한다")
    void fail_whenTargetAndDestinationInDifferentGroups() {
      // Given
      var trio1 = entityFactory.insertUserGroup();
      User user1 = trio1.first();
      Group group1 = trio1.second();
      var trio2 = entityFactory.insertUserGroup();
      User user2 = trio2.first();
      Group group2 = trio2.second();

      Todo targetTodo =
          entityFactory.insertTodo(
              user1.getId(),
              user1.getId(),
              group1.getId(),
              "t1",
              "d1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo destTodo =
          entityFactory.insertTodo(
              user2.getId(),
              user2.getId(),
              group2.getId(),
              "t2",
              "d2",
              TodoStatus.TO_DO,
              "b",
              false);

      // When
      Runnable lambda =
          () -> todoService.moveTodo(targetTodo.getId(), destTodo.getId(), TodoStatus.TO_DO);

      // Then
      Assertions.assertThrows(
          rest.felix.back.todo.exception.InvalidDestinationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 목표 위치의 상태가 이동하려는 상태와 다르면 예외가 발생한다")
    void fail_whenDestinationStatusIsIncorrect() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo targetTodo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t1", "d1", TodoStatus.TO_DO, "a", false);
      Todo destTodo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t2", "d2", TodoStatus.DONE, "b", false);

      // When
      Runnable lambda =
          () -> todoService.moveTodo(targetTodo.getId(), destTodo.getId(), TodoStatus.IN_PROGRESS);

      // Then
      Assertions.assertThrows(
          rest.felix.back.todo.exception.InvalidDestinationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패: 대상과 목표 위치가 동일하면 예외가 발생한다")
    void fail_whenTargetAndDestinationAreSame() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t", "d", TodoStatus.TO_DO, "a", false);

      // When
      Runnable lambda = () -> todoService.moveTodo(todo.getId(), todo.getId(), TodoStatus.TO_DO);

      // Then
      Assertions.assertThrows(
          rest.felix.back.todo.exception.InvalidDestinationException.class, lambda::run);
    }

    @Test
    @DisplayName("성공: 같은 상태 내에서 Todo를 가장 끝으로 이동시킨다")
    void success_whenMovingTodoToEndOfSameStatus() {
      // Given
      var trio = entityFactory.insertUserGroup();
      User user = trio.first();
      Group group = trio.second();
      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t1", "d1", TodoStatus.TO_DO, "a", false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(), user.getId(), group.getId(), "t2", "d2", TodoStatus.TO_DO, "c", false);

      // When
      todoService.moveTodo(todo1.getId(), null, TodoStatus.TO_DO);

      // Then
      TodoDTO movedTodo = todoRepository.findById(todo1.getId()).orElseThrow();
      Assertions.assertEquals(TodoStatus.TO_DO, movedTodo.getStatus());
      Assertions.assertTrue(movedTodo.getOrder().compareTo(todo2.getOrder()) > 0);
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

      // When
      Runnable lambda = () -> todoService.moveTodo(todo.getId(), null, TodoStatus.IN_PROGRESS);

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
      Assertions.assertFalse(todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertTrue(todoRepository.starExistsById(user.getId(), todo.getId()));
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
      Assertions.assertTrue(todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.starTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertTrue(todoRepository.starExistsById(user.getId(), todo.getId()));
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
      Assertions.assertTrue(todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.unstarTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertFalse(todoRepository.starExistsById(user.getId(), todo.getId()));
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
      Assertions.assertFalse(todoRepository.starExistsById(user.getId(), todo.getId()));

      // When
      todoService.unstarTodo(user.getId(), todo.getId());

      // Then
      Assertions.assertFalse(todoRepository.starExistsById(user.getId(), todo.getId()));
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
