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
import rest.felix.back.common.util.Pair;
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

  @Test
  void getTodosInGroup_HappyPath() {
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
        todoDTOs.stream()
            .map(TodoDTO::getStatus)
            .map(status -> status == TodoStatus.TO_DO)
            .reduce(true, (one, another) -> one && another));

    Assertions.assertTrue(
        todoDTOs.stream()
            .map(TodoDTO::getGroupId)
            .map(groupId -> groupId.equals(group.getId()))
            .reduce(true, (one, another) -> one && another));

    Assertions.assertTrue(
        todoDTOs.stream()
            .map(TodoDTO::getAuthorId)
            .map(authorId -> authorId.equals(user.getId()))
            .reduce(true, (one, another) -> one && another));

    Assertions.assertTrue(
        todoDTOs.stream()
            .map(TodoDTO::getTitle)
            .toList()
            .containsAll(List.of("todo 1", "todo 2", "todo 3")));

    Assertions.assertTrue(
        todoDTOs.stream()
            .map(TodoDTO::getDescription)
            .toList()
            .containsAll(
                List.of("todo 1 description", "todo 2 description", "todo 3 description")));

    Assertions.assertTrue(
        todoDTOs.stream()
            .map(TodoDTO::getOrder)
            .toList()
            .containsAll(List.of("todo order 1", "todo order 2", "todo order 3")));
  }

  @Test
  void getTodosInGroup_HappyPath_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    // When

    List<TodoDTO> todoDTOs = todoService.getTodosInGroup(group.getId());

    // Then

    Assertions.assertEquals(0, todoDTOs.size());
  }

  @Test
  void getTodosInGroup_HappyPath_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    th.delete(group);

    // When

    List<TodoDTO> todoDTOs = todoService.getTodosInGroup(group.getId());

    // Then

    Assertions.assertEquals(0, todoDTOs.size());
  }

  @Test
  void createTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    CreateTodoDTO createTodoDTO =
        new CreateTodoDTO(
            "todo title", "todo description", "todo order", user.getId(), group.getId());

    // When

    TodoDTO todoDTO = todoService.createTodo(createTodoDTO);

    // Then

    Assertions.assertEquals("todo title", todoDTO.getTitle());
    Assertions.assertEquals("todo description", todoDTO.getDescription());
    Assertions.assertEquals(TodoStatus.TO_DO, todoDTO.getStatus());
    Assertions.assertEquals(user.getId(), todoDTO.getAuthorId());
    Assertions.assertEquals(group.getId(), todoDTO.getGroupId());
    Assertions.assertEquals("todo order", todoDTO.getOrder());
  }

  @Test
  void createTodo_Failure_NoUser() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    th.delete(user);

    CreateTodoDTO createTodoDTO =
        new CreateTodoDTO(
            "todo title", "todo description", "todo order", user.getId(), group.getId());

    // When

    Runnable lambda = () -> todoService.createTodo(createTodoDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
    Group group = entityFactory.insertGroup("group name", "group description");

    th.delete(group);

    CreateTodoDTO createTodoDTO =
        new CreateTodoDTO(
            "todo title", "todo description", "todo order", user.getId(), group.getId());

    // When

    Runnable lambda = () -> todoService.createTodo(createTodoDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }

  @Test
  void createTodo_Failure_Duplicated_Order_Status_In_Group() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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

    CreateTodoDTO createTodoDTO =
        new CreateTodoDTO(
            "new todo title", "new todo description", "todo order", user.getId(), group.getId());

    // When

    Runnable lambda = () -> todoService.createTodo(createTodoDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
  }

  @Test
  void getTodoInGroup_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    List<Todo> todos =
        Stream.of(
                new Pair<>(TodoStatus.TO_DO, 1),
                new Pair<>(TodoStatus.IN_PROGRESS, 2),
                new Pair<>(TodoStatus.DONE, 3),
                new Pair<>(TodoStatus.ON_HOLD, 4))
            .map(
                pair -> {
                  TodoStatus todoStatus = pair.first();
                  int idx = pair.second();

                  Todo todo =
                      entityFactory.insertTodo(
                          user.getId(),
                          user.getId(),
                          group.getId(),
                          String.format("todo %d", idx),
                          String.format("todo %d description", idx),
                          todoStatus,
                          String.format("todo %d order", idx),
                          false);
                  return todo;
                })
            .toList();

    todos.forEach(
        todo -> {

          // When

          TodoDTO todoDTO = todoService.getTodoInGroup(group.getId(), todo.getId());

          // Then

          Assertions.assertEquals(todo.getId(), todoDTO.getId());
          Assertions.assertEquals(todo.getTodoStatus(), todoDTO.getStatus());
          Assertions.assertEquals(todo.getDescription(), todoDTO.getDescription());
          Assertions.assertEquals(todo.getTitle(), todoDTO.getTitle());
          Assertions.assertEquals(todo.getGroup().getId(), todoDTO.getGroupId());
          Assertions.assertEquals(todo.getAuthor().getId(), todoDTO.getAuthorId());
        });
  }

  @Test
  void getTodoInGroup_Failure_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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

    // When

    Runnable lambda = () -> todoService.getTodoInGroup(group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void getTodoInGroup_Failure_NoGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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
    th.delete(group);

    // When

    Runnable lambda = () -> todoService.getTodoInGroup(group.getId(), todo.getId());

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void getTodoInGroup_Failure_WrongGroup() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group1 = entityFactory.insertGroup("group1 name", "group1 description");

    Group group2 = entityFactory.insertGroup("group2 name", "group2 description");

    Todo todo =
        entityFactory.insertTodo(
            user.getId(),
            user.getId(),
            group1.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    // When

    Runnable lambda = () -> todoService.getTodoInGroup(group2.getId(), todo.getId());

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void deleteTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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

    // When

    todoService.deleteTodo(todo.getId());

    // Then

    Assertions.assertTrue(todoRepository.findById(todo.getId()).isEmpty());
  }

  @Test
  void deleteTodo_HappyPath_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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

    // When

    Runnable lambda = () -> todoService.deleteTodo(todo.getId());

    // Then

    Assertions.assertDoesNotThrow(lambda::run);
  }

  @Test
  void updateTodo_HappyPath() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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
  void updateTodo_Failure_NoTodo() {
    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

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

    UpdateTodoDTO updateTodoDTO =
        new UpdateTodoDTO(todo.getId(), "updated todo title", "updated todo description");

    // When

    Runnable lambda = () -> todoService.updateTodo(updateTodoDTO);

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
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
        // Given: 유저가 그룹에 속해 있고, Todo가 존재하며, Predicate는 true를 반환
        // When: assertTodoAuthority 호출
        // Then: 예외가 발생하지 않음
      }

      @Test
      @DisplayName("유저가 그룹에 속해있지 않으면 UserAccessDeniedException이 발생한다")
      void fail_whenUserNotInGroup() {
        // Given: userGroupRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound() {
        // Given: todoRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: TodoNotFoundException 예외 발생
      }

      @Test
      @DisplayName("모든 조건이 유효하더라도 Predicate가 false를 반환하면 UserAccessDeniedException이 발생한다")
      void fail_whenPredicateIsFalse() {
        // Given: 유저, 그룹, Todo 모두 유효
        // When: Predicate가 false를 반환하도록 assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
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
        // Given: 역할(role)이 OWNER 또는 MANAGER이고, Predicate는 true를 반환
        // When: assertTodoAuthority 호출
        // Then: 예외가 발생하지 않음
      }

      @Test
      @DisplayName("멤버(MEMBER)는 자신이 작성한 Todo에 한해 통과한다")
      void success_forMember_whenIsAuthor() {
        // Given: 역할이 MEMBER, userId와 todo.authorId가 동일, Predicate는 true 반환
        // When: assertTodoAuthority 호출
        // Then: 예외가 발생하지 않음
      }

      @DisplayName("뷰어(VIEWER)는 어떤 조건이든 UserAccessDeniedException이 발생한다")
      @ParameterizedTest
      @EnumSource(
          value = GroupRole.class,
          names = {"VIEWER"})
      void fail_forViewerRole(GroupRole viewerRole) {
        // Given: 역할(role)이 VIEWER, Predicate는 false를 반환
        // When: assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
      }

      @Test
      @DisplayName("멤버(MEMBER)가 타인이 작성한 Todo에 접근하면 UserAccessDeniedException이 발생한다")
      void fail_forMember_whenIsNotAuthor() {
        // Given: 역할이 MEMBER, userId와 todo.authorId가 다름, Predicate는 false 반환
        // When: assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
      }

      @Test
      @DisplayName("유저가 그룹에 속해있지 않으면 UserAccessDeniedException이 발생한다")
      void fail_whenUserNotInGroup() {
        // Given: userGroupRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound() {
        // Given: todoRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: TodoNotFoundException 예외 발생
      }
    }

    @Nested
    @DisplayName("Todo 접급 권한 (GroupRole 요구사항 버전)")
    class AssertTodoAuthorityRole {

      @DisplayName("요구되는 역할보다 유저의 실제 역할이 같거나 높으면 예외가 발생하지 않는다")
      @ParameterizedTest
      @MethodSource("provideSufficientRoles") // 구현이 필요함.
      void success_whenUserRoleIsGteRequiredRole(GroupRole userActualRole, GroupRole requiredRole) {
        // Given: 유저의 실제 역할(userActualRole)과 Todo가 존재하도록 설정
        // When: 요구되는 역할(requiredRole)을 인자로 assertTodoAuthority 호출
        // Then: 예외가 발생하지 않음
      }

      @DisplayName("요구되는 역할보다 유저의 실제 역할이 낮으면 UserAccessDeniedException이 발생한다")
      @ParameterizedTest
      @MethodSource("provideInsufficientRoles")
      void fail_whenUserRoleIsLowerThanRequiredRole(
          GroupRole userActualRole, GroupRole requiredRole) {
        // Given: 유저의 실제 역할(userActualRole)과 Todo가 존재하도록 설정
        // When: 요구되는 역할(requiredRole)을 인자로 assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
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
      void fail_whenUserNotInGroup() {
        // Given: userGroupRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: UserAccessDeniedException 예외 발생
      }

      @Test
      @DisplayName("Todo가 존재하지 않으면 TodoNotFoundException이 발생한다")
      void fail_whenTodoNotFound() {
        // Given: 유저 정보는 유효하지만 todoRepository가 Optional.empty() 반환
        // When: assertTodoAuthority 호출
        // Then: TodoNotFoundException 예외 발생
      }
    }
  }

  @Nested
  @DisplayName("Todo 위치 및 상태 이동")
  class MoveTodo {

    @Test
    @DisplayName("관리자(MANAGER)가 Todo를 다른 상태(column)의 가장 끝으로 이동시킨다")
    void success_whenManagerMovesTodoToEndOfAnotherStatus() {
      // Given: MANAGER 역할의 유저, 이동 대상 Todo, 목표 상태(status)
      // When: destinationId를 null로 하여 moveTodo 서비스 호출
      // Then: Todo의 상태와 순서가 올바르게 변경되고, 예외가 발생하지 않음
    }

    @Test
    @DisplayName("관리자(MANAGER)가 Todo를 다른 상태(column)의 특정 위치로 이동시킨다")
    void success_whenManagerMovesTodoToSpecificPositionInAnotherStatus() {
      // Given: MANAGER 역할의 유저, 이동 대상 Todo, 목표 상태(status), 목표 위치(destination) Todo
      // When: destinationId를 지정하여 moveTodo 서비스 호출
      // Then: Todo의 상태와 순서가 올바르게 변경되고, 예외가 발생하지 않음
    }

    @Test
    @DisplayName("멤버(MEMBER)가 자신의 Todo 순서를 같은 상태 내에서 변경한다")
    void success_whenMemberReordersOwnTodoWithinSameStatus() {
      // Given: MEMBER 역할의 유저, 자신이 작성한 이동 대상 Todo와 목표 위치 Todo (모두 같은 상태)
      // When: moveTodo 서비스 호출
      // Then: Todo의 순서가 올바르게 변경되고, 상태는 그대로이며, 예외가 발생하지 않음
    }

    @DisplayName("뷰어(VIEWER) 등급은 Todo를 이동시킬 수 없어 UserAccessDeniedException이 발생한다")
    @ParameterizedTest
    @EnumSource(
        value = GroupRole.class,
        names = {"VIEWER"})
    void fail_whenRoleIsInsufficient(GroupRole insufficientRole) {
      // Given: 권한이 부족한 역할(role)의 유저
      // When: moveTodo 서비스 호출
      // Then: UserAccessDeniedException 예외 발생
    }

    @Test
    @DisplayName("멤버(MEMBER)가 다른 사람의 Todo를 이동시키려 하면 UserAccessDeniedException이 발생한다")
    void fail_whenMemberMovesOthersTodo() {
      // Given: MEMBER 역할의 유저, 다른 사람이 작성한 Todo
      // When: moveTodo 서비스 호출
      // Then: UserAccessDeniedException 예외 발생
    }

    @Test
    @DisplayName("그룹에 속하지 않은 유저는 Todo를 이동시킬 수 없어 UserAccessDeniedException이 발생한다")
    void fail_whenUserIsNotInGroup() {
      // Given: 그룹에 속하지 않은 유저
      // When: moveTodo 서비스 호출
      // Then: UserAccessDeniedException 예외 발생
    }

    @Test
    @DisplayName("이동 대상 Todo를 찾을 수 없으면 TodoNotFoundException이 발생한다")
    void fail_whenTargetTodoNotFound() {
      // Given: 존재하지 않는 targetId
      // When: moveTodo 서비스 호출
      // Then: TodoNotFoundException 예외 발생
    }

    @Test
    @DisplayName("목표 위치(destination) Todo를 찾을 수 없으면 DestinationNotFoundException이 발생한다")
    void fail_whenDestinationTodoNotFound() {
      // Given: 유효한 targetId, 존재하지 않는 destinationId
      // When: moveTodo 서비스 호출
      // Then: DestinationNotFoundException 예외 발생
    }

    @Test
    @DisplayName("대상과 목표 위치의 그룹이 다르면 InvalidDestinationException이 발생한다")
    void fail_whenTargetAndDestinationInDifferentGroups() {
      // Given: 서로 다른 그룹에 속한 target Todo와 destination Todo
      // When: moveTodo 서비스 호출
      // Then: InvalidDestinationException 예외 발생
    }

    @Test
    @DisplayName("목표 위치의 상태가 이동하려는 상태와 다르면 InvalidDestinationException이 발생한다")
    void fail_whenDestinationStatusIsIncorrect() {
      // Given: target Todo를 'IN_PROGRESS' 상태로 옮기려 하지만, destination Todo는 'DONE' 상태임
      // When: moveTodo 서비스 호출
      // Then: InvalidDestinationException 예외 발생
    }

    @Test
    @DisplayName("대상과 목표 위치가 동일하면 InvalidDestinationException이 발생한다")
    void fail_whenTargetAndDestinationAreSame() {
      // Given: targetId와 destinationId가 동일함
      // When: moveTodo 서비스 호출
      // Then: InvalidDestinationException 예외 발생
    }
  }

  @Nested
  @DisplayName("Todo star 표기")
  class StarTodo {

    @Test
    @DisplayName("성공: 아직 Star(좋아요)하지 않은 Todo에 Star를 추가한다")
    void success_whenStarringANonStarredTodo() {
      // Given: todoRepository.starExistsById()가 false를 반환하도록 설정
      // When: starTodo 서비스 호출
      // Then: todoRepository.starTodo()가 1번 호출되었는지 검증
    }

    @Test
    @DisplayName("성공(멱등성): 이미 Star(좋아요)한 Todo에 다시 Star를 요청해도 아무 일이 일어나지 않는다")
    void success_whenStarringAnAlreadyStarredTodo() {
      // Given: todoRepository.starExistsById()가 true를 반환하도록 설정
      // When: starTodo 서비스 호출
      // Then: todoRepository.starTodo()가 호출되지 않았는지 검증
    }

    @Test
    @DisplayName("실패: 존재하지 않는 Todo에 Star를 요청하면 예외가 발생한다")
    void fail_whenTodoNotFound() {
      // Given: todoRepository.starTodo() 호출 시 예외(DataIntegrityViolationException 등)를 발생시키도록 설정
      // When: starTodo 서비스 호출
      // Then: 적절한 예외가 발생하는지 검증
    }
  }

  @Nested
  @DisplayName("Todo star 제거")
  class UnstarTodo {

    @Test
    @DisplayName("성공: Star(좋아요)한 Todo의 Star를 제거한다")
    void success_whenUnstarringAStarredTodo() {
      // Given: todoRepository.starExistsById()가 true를 반환하도록 설정
      // When: unstarTodo 서비스 호출
      // Then: todoRepository.unstarTodo()가 1번 호출되었는지 검증
    }

    @Test
    @DisplayName("성공(멱등성): Star(좋아요)하지 않은 Todo의 Star를 제거해도 아무 일이 일어나지 않는다")
    void success_whenUnstarringANonStarredTodo() {
      // Given: todoRepository.starExistsById()가 false를 반환하도록 설정
      // When: unstarTodo 서비스 호출
      // Then: todoRepository.unstarTodo()가 호출되지 않았는지 검증
    }
  }
}
