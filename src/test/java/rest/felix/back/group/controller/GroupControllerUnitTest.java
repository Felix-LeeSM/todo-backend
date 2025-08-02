package rest.felix.back.group.controller;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.config.GroupConfig;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.Pair;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.dto.*;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.exception.*;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.dto.TodoWithStarredStatusResponseDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@ActiveProfiles("test")
public class GroupControllerUnitTest {

  @Autowired private GroupController groupController;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserGroupRepository userGroupRepository;
  @Autowired private TodoRepository todoRepository;
  @Autowired private EntityFactory entityFactory;
  @Autowired private GroupConfig groupConfig;

  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Nested
  @DisplayName("그룹 생성 테스트")
  class CreateGroupTest {

    @Test
    public void createGroup_HappyPath() {
      // Given

      User user = entityFactory.insertUser("username", "some_password", "nickname");

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");

      // When

      ResponseEntity<GroupResponseDTO> responseEntity =
          groupController.createGroup(authUserDTO, createGroupRequestDTO);

      // Then

      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

      GroupResponseDTO groupResponseDTO = responseEntity.getBody();

      GroupDTO createdGroup = groupRepository.findById(groupResponseDTO.id()).get();

      Assertions.assertEquals("groupName", createdGroup.name());
      Assertions.assertEquals("group description", createdGroup.description());

      UserGroupDTO userGroup =
          userGroupRepository.findByUserIdAndGroupId(user.getId(), groupResponseDTO.id()).get();

      Assertions.assertEquals(GroupRole.OWNER, userGroup.groupRole());
    }
  }

  @Nested
  @DisplayName("유저 전체 그룹 조회 테스트")
  class GetMyDetailedGroupsTest {

    @Test
    @DisplayName("Happy Path - 2 groups")
    public void Happy_Path_1() {

      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      User otherUser1 = entityFactory.insertUser("otherUser1", "password", "otherUser1Nick");
      User otherUser2 = entityFactory.insertUser("otherUser2", "password", "otherUser2Nick");

      Group group1 = entityFactory.insertGroup("Group 1", "Description 1");
      Group group2 = entityFactory.insertGroup("Group 2", "Description 2");
      entityFactory.insertGroup("Group 3", "Description 3"); // mainUser is not in this group

      entityFactory.insertUserGroup(mainUser.getId(), group1.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(otherUser1.getId(), group1.getId(), GroupRole.MEMBER);

      entityFactory.insertUserGroup(mainUser.getId(), group2.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(otherUser1.getId(), group2.getId(), GroupRole.MANAGER);
      entityFactory.insertUserGroup(otherUser2.getId(), group2.getId(), GroupRole.MEMBER);

      entityFactory.insertTodo(
          mainUser.getId(),
          mainUser.getId(),
          group1.getId(),
          "Todo 1-1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "a",
          null,
          false);
      entityFactory.insertTodo(
          otherUser1.getId(),
          otherUser1.getId(),
          group1.getId(),
          "Todo 1-2",
          "Desc",
          TodoStatus.DONE,
          "b",
          null,
          false);

      entityFactory.insertTodo(
          mainUser.getId(),
          mainUser.getId(),
          group2.getId(),
          "Todo 2-1",
          "Desc",
          TodoStatus.DONE,
          "a",
          null,
          false);
      entityFactory.insertTodo(
          otherUser1.getId(),
          otherUser1.getId(),
          group2.getId(),
          "Todo 2-2",
          "Desc",
          TodoStatus.DONE,
          "b",
          null,
          false);
      entityFactory.insertTodo(
          otherUser2.getId(),
          otherUser2.getId(),
          group2.getId(),
          "Todo 2-3",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
          null,
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(mainUser);

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(authUserDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      List<DetailedGroupResponseDTO> body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(2, body.size());

      List<DetailedGroupResponseDTO> data =
          body.stream().sorted(Comparator.comparing(DetailedGroupResponseDTO::name)).toList();

      DetailedGroupResponseDTO group1DTO = data.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(2, group1DTO.todoCount());
      Assertions.assertEquals(1, group1DTO.completedTodoCount());
      Assertions.assertEquals(2, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick"),
          group1DTO.members().stream().map(MemberResponseDTO::nickname).sorted().toList());

      DetailedGroupResponseDTO group2DTO = data.get(1);
      Assertions.assertEquals(group2.getId(), group2DTO.id());
      Assertions.assertEquals("Group 2", group2DTO.name());
      Assertions.assertEquals("Description 2", group2DTO.description());
      Assertions.assertEquals(3, group2DTO.todoCount());
      Assertions.assertEquals(2, group2DTO.completedTodoCount());
      Assertions.assertEquals(3, group2DTO.memberCount());
      Assertions.assertEquals(GroupRole.MEMBER, group2DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick", "otherUser2Nick"),
          group2DTO.members().stream().map(MemberResponseDTO::nickname).sorted().toList());
    }

    @Test
    @DisplayName("Happy Path - 1 group")
    public void Happy_Path_2() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      User otherUser1 = entityFactory.insertUser("otherUser1", "password", "otherUser1Nick");

      Group group1 = entityFactory.insertGroup("Group 1", "Description 1");

      entityFactory.insertUserGroup(mainUser.getId(), group1.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(otherUser1.getId(), group1.getId(), GroupRole.MEMBER);

      entityFactory.insertTodo(
          mainUser.getId(),
          mainUser.getId(),
          group1.getId(),
          "Todo 1-1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "a",
          null,
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(mainUser);

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(authUserDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      List<DetailedGroupResponseDTO> body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(1, body.size());

      DetailedGroupResponseDTO group1DTO = body.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(1, group1DTO.todoCount());
      Assertions.assertEquals(0, group1DTO.completedTodoCount());
      Assertions.assertEquals(2, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick"),
          group1DTO.members().stream().map(MemberResponseDTO::nickname).sorted().toList());
    }

    @Test
    @DisplayName("Happy Path - No groups")
    public void Happy_Path_NoGroup() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      entityFactory.insertGroup("Group 1", "Description 1");

      AuthUserDTO authUserDTO = AuthUserDTO.of(mainUser);

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(authUserDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      List<DetailedGroupResponseDTO> body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(0, body.size());
    }
  }

  @Nested
  @DisplayName("유저 단일 그룹 조회 테스트")
  class GetUserGroupTest {

    @Test
    @DisplayName("Happy Path - User is OWNER with multiple members and todos")
    public void HappyPath_1() {
      // Given
      User ownerUser = entityFactory.insertUser("ownerUser", "password", "ownerNick");
      User memberUser1 = entityFactory.insertUser("memberUser1", "password", "memberNick1");
      User memberUser2 = entityFactory.insertUser("memberUser2", "password", "memberNick2");

      Group group = entityFactory.insertGroup("Complex Group 1", "Description for complex group 1");

      entityFactory.insertUserGroup(ownerUser.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(memberUser1.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(memberUser2.getId(), group.getId(), GroupRole.VIEWER);

      Todo todo1 =
          entityFactory.insertTodo(
              ownerUser.getId(),
              ownerUser.getId(),
              group.getId(),
              "Owner Todo 1",
              "Desc",
              TodoStatus.IN_PROGRESS,
              "a",
              null,
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              memberUser1.getId(),
              memberUser1.getId(),
              group.getId(),
              "Member1 Todo 1",
              "Desc",
              TodoStatus.DONE,
              "b",
              null,
              false);
      entityFactory.insertTodo(
          memberUser2.getId(),
          memberUser2.getId(),
          group.getId(),
          "Member2 Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
          null,
          true);
      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 2",
          "Desc",
          TodoStatus.DONE,
          null,
          LocalDate.of(2023, 12, 10),
          false);

      entityFactory.insertUserTodoStar(memberUser1.getId(), todo1.getId());
      entityFactory.insertUserTodoStar(ownerUser.getId(), todo2.getId());
      entityFactory.insertUserTodoStar(memberUser2.getId(), todo2.getId());

      AuthUserDTO authUserDTO = AuthUserDTO.of(ownerUser);

      // When
      ResponseEntity<FullGroupDetailsResponseDTO> responseEntity =
          groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      FullGroupDetailsResponseDTO body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(group.getId(), body.id());
      Assertions.assertEquals("Complex Group 1", body.name());
      Assertions.assertEquals("Description for complex group 1", body.description());
      Assertions.assertEquals(GroupRole.OWNER, body.myRole());
      Assertions.assertEquals(3, body.memberCount());
      Assertions.assertEquals(
          List.of("memberNick1", "memberNick2", "ownerNick"),
          body.members().stream().map(MemberResponseDTO::nickname).sorted().toList());

      Assertions.assertEquals(4, body.todos().size());

      List<TodoWithStarredStatusResponseDTO> todos =
          body.todos().stream()
              .sorted(Comparator.comparing(TodoWithStarredStatusResponseDTO::id))
              .toList();

      Assertions.assertEquals(
          List.of("Owner Todo 1", "Member1 Todo 1", "Member2 Todo 1", "Owner Todo 2"),
          todos.stream().map(TodoWithStarredStatusResponseDTO::title).toList());

      Assertions.assertEquals(
          List.of(false, true, false, false),
          todos.stream().map(TodoWithStarredStatusResponseDTO::isStarred).toList());

      Assertions.assertEquals(
          List.of(false, false, true, false),
          todos.stream().map(TodoWithStarredStatusResponseDTO::isImportant).toList());

      Assertions.assertEquals(
          Arrays.asList(null, null, null, LocalDate.of(2023, 12, 10)),
          todos.stream().map(TodoWithStarredStatusResponseDTO::dueDate).toList());
    }

    @Test
    @DisplayName("Happy Path - User is MEMBER with multiple members and todos")
    public void HappyPath_2() {
      // Given
      User ownerUser = entityFactory.insertUser("ownerUser2", "password", "ownerNick2");
      User memberUser1 = entityFactory.insertUser("memberUser3", "password", "memberNick3");
      User memberUser2 = entityFactory.insertUser("memberUser4", "password", "memberNick4");

      Group group = entityFactory.insertGroup("Complex Group 2", "Description for complex group 2");

      entityFactory.insertUserGroup(ownerUser.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(memberUser1.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(memberUser2.getId(), group.getId(), GroupRole.MANAGER);

      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 3",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "a",
          null,
          false);
      entityFactory.insertTodo(
          memberUser1.getId(),
          memberUser1.getId(),
          group.getId(),
          "Member3 Todo 1",
          "Desc",
          TodoStatus.DONE,
          "b",
          null,
          false);
      entityFactory.insertTodo(
          memberUser2.getId(),
          memberUser2.getId(),
          group.getId(),
          "Member4 Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
          null,
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(memberUser1);

      // When
      ResponseEntity<FullGroupDetailsResponseDTO> responseEntity =
          groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      FullGroupDetailsResponseDTO body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(group.getId(), body.id());
      Assertions.assertEquals("Complex Group 2", body.name());
      Assertions.assertEquals("Description for complex group 2", body.description());
      Assertions.assertEquals(GroupRole.MEMBER, body.myRole());
      Assertions.assertEquals(3, body.memberCount());
      Assertions.assertEquals(
          List.of("memberNick3", "memberNick4", "ownerNick2"),
          body.members().stream().map(MemberResponseDTO::nickname).sorted().toList());
      Assertions.assertEquals(3, body.todos().size());
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Owner Todo 3")));
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Member3 Todo 1")));
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Member4 Todo 1")));
    }

    @Test
    @DisplayName("Happy Path - User is MANAGER with multiple members and todos")
    public void HappyPath_3() {
      // Given
      User ownerUser = entityFactory.insertUser("ownerUser3", "password", "ownerNick3");
      User memberUser1 = entityFactory.insertUser("memberUser5", "password", "memberNick5");
      User managerUser = entityFactory.insertUser("managerUser", "password", "managerNick");

      Group group = entityFactory.insertGroup("Complex Group 3", "Description for complex group 3");

      entityFactory.insertUserGroup(ownerUser.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(memberUser1.getId(), group.getId(), GroupRole.VIEWER);
      entityFactory.insertUserGroup(managerUser.getId(), group.getId(), GroupRole.MANAGER);

      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 4",
          "Desc",
          TodoStatus.DONE,
          "a",
          null,
          false);
      entityFactory.insertTodo(
          memberUser1.getId(),
          memberUser1.getId(),
          group.getId(),
          "Member5 Todo 1",
          "Desc",
          TodoStatus.DONE,
          "b",
          null,
          false);
      entityFactory.insertTodo(
          managerUser.getId(),
          managerUser.getId(),
          group.getId(),
          "Manager Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
          null,
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(managerUser);

      // When
      ResponseEntity<FullGroupDetailsResponseDTO> responseEntity =
          groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      FullGroupDetailsResponseDTO body = responseEntity.getBody();
      Assertions.assertNotNull(body);
      Assertions.assertEquals(group.getId(), body.id());
      Assertions.assertEquals("Complex Group 3", body.name());
      Assertions.assertEquals("Description for complex group 3", body.description());
      Assertions.assertEquals(GroupRole.MANAGER, body.myRole());
      Assertions.assertEquals(3, body.memberCount());
      Assertions.assertEquals(
          List.of("managerNick", "memberNick5", "ownerNick3"),
          body.members().stream().map(MemberResponseDTO::nickname).sorted().toList());
      Assertions.assertEquals(3, body.todos().size());
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Owner Todo 4")));
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Member5 Todo 1")));
      Assertions.assertEquals(
          true, body.todos().stream().anyMatch(todo -> todo.title().equals("Manager Todo 1")));
    }

    @Test
    @DisplayName("Failure - No such group")
    public void Failure_NoGroup() {
      // Given
      User user = entityFactory.insertUser("testUser4", "password", "nick4");

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      Group wrongGroup = entityFactory.insertGroup("wrong grop", "desc");
      th.delete(wrongGroup);

      // When
      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, wrongGroup.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("Failure - No such user")
    public void Failure_NoUser() {
      // Given
      User user = entityFactory.insertUser("testUser5", "password", "nick5");
      Group group = entityFactory.insertGroup("Test Group 5", "Description 5");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user); // Remove the user to simulate no such user

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("Failure - User not in group")
    public void Failure_NoUserGroup() {
      // Given
      User user = entityFactory.insertUser("testUser6", "password", "nick6");
      Group group = entityFactory.insertGroup("Test Group 6", "Description 6");
      // User is not associated with the group

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 삭제 테스트 테스트")
  class DeleteGroupTest {

    @Test
    public void HappyPath() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      entityFactory.insertTodo(
          user.getId(),
          user.getId(),
          group.getId(),
          "todo title",
          "todo description",
          TodoStatus.IN_PROGRESS,
          "todo order",
          null,
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When

      groupController.deleteGroup(authUserDTO, group.getId());

      // Then

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isEmpty());

      Assertions.assertEquals(true, todoRepository.findByGroupId(group.getId()).isEmpty());

      Assertions.assertEquals(true, userGroupRepository.findByGroupId(group.getId()).isEmpty());
    }

    @Test
    public void Failure_Failure_NoAuthority() {

      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      Stream.of(
              Pair.of(GroupRole.MANAGER, 1),
              Pair.of(GroupRole.VIEWER, 2),
              Pair.of(GroupRole.MEMBER, 3))
          .forEach(
              pair -> {
                GroupRole role = pair.first();
                Integer idx = pair.second();

                User user =
                    entityFactory.insertUser("username" + role, "hashedPassword", "nickname");

                entityFactory.insertUserGroup(user.getId(), group.getId(), role);

                entityFactory.insertTodo(
                    user.getId(),
                    user.getId(),
                    group.getId(),
                    "todo title",
                    "todo description",
                    TodoStatus.IN_PROGRESS,
                    String.format("todo order %d", idx),
                    null,
                    false);

                AuthUserDTO authUserDTO = AuthUserDTO.of(user);

                // When

                Runnable lambda = () -> groupController.deleteGroup(authUserDTO, group.getId());

                // Then

                Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

                Assertions.assertEquals(true, groupRepository.findById(group.getId()).isPresent());

                Assertions.assertEquals(
                    true, 0 < todoRepository.findByGroupId(group.getId()).size());

                Assertions.assertEquals(
                    true, 0 < userGroupRepository.findByGroupId(group.getId()).size());
              });
    }

    @Test
    public void Failure_Failure_NoUser() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When

      Runnable lambda = () -> groupController.deleteGroup(authUserDTO, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isPresent());
    }

    @Test
    public void Failure_Failure_NoUserGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When

      Runnable lambda = () -> groupController.deleteGroup(authUserDTO, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isPresent());
    }

    @Test
    public void Failure_Failure_NoGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When

      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 초대 생성 테스트")
  class CreateGroupInvitationTest {

    @Test
    @DisplayName("성공")
    public void HappyPath() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<CreateGroupInvitationResponseDTO> responseEntity =
          groupController.createGroupInvitation(authUserDTO, group.getId());

      // Then
      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
      Assertions.assertNotNull(responseEntity.getBody());
      Assertions.assertNotNull(responseEntity.getBody().token());
      Assertions.assertNotNull(responseEntity.getBody().expiresAt());
    }

    @Test
    @DisplayName("실패 - 권한 부족")
    public void Failure_ImproperGroupRole() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(
          user.getId(), group.getId(), GroupRole.MEMBER); // Not MANAGER or OWNER

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.createGroupInvitation(authUserDTO, group.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    public void Failure_NoGroup() {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.createGroupInvitation(authUserDTO, group.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 초대 정보 테스트")
  class GetInvitationInfoTest {

    @Test
    @DisplayName("성공")
    public void HappyPath_1() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<GroupInvitationInfoDTOResponse> responseEntity =
          groupController.getInvitationInfo(authUserDTO, token);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      GroupInvitationInfoDTOResponse response = responseEntity.getBody();
      Assertions.assertNotNull(response);

      Assertions.assertEquals(group.getName(), response.name());
      Assertions.assertEquals(group.getDescription(), response.description());
      Assertions.assertNotNull(response.expiresAt());
      Assertions.assertEquals(issuer.getId(), response.issuer().id());
      Assertions.assertEquals(false, response.isMember());
    }

    @Test
    @DisplayName("성공 - 이미 그룹 멤버")
    public void HappyPath_2() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<GroupInvitationInfoDTOResponse> responseEntity =
          groupController.getInvitationInfo(authUserDTO, token);

      // Then
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      GroupInvitationInfoDTOResponse response = responseEntity.getBody();
      Assertions.assertNotNull(response);

      Assertions.assertEquals(group.getName(), response.name());
      Assertions.assertEquals(group.getDescription(), response.description());
      Assertions.assertNotNull(response.expiresAt());
      Assertions.assertEquals(issuer.getId(), response.issuer().id());
      Assertions.assertEquals(true, response.isMember());
    }

    @Test
    @DisplayName("실패 - 없는 초대")
    public void Failure_NoInvitation() {
      // Given
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      String invalidToken = "nonExistentToken";

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.getInvitationInfo(authUserDTO, invalidToken);

      // Then
      Assertions.assertThrows(NoInvitationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 만료된 초대")
    public void Failure_ExpiredInvitation() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "expiredToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().minusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.getInvitationInfo(authUserDTO, token);

      // Then
      Assertions.assertThrows(ExpiredInvitationException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 초대 수락 테스트")
  class AcceptInvitationTest {

    @Test
    @DisplayName("성공")
    public void HappyPath() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> responseEntity = groupController.acceptInvitation(authUserDTO, token);

      // Then
      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
      Assertions.assertEquals(
          true,
          userGroupRepository.findByUserIdAndGroupId(user.getId(), group.getId()).isPresent());
    }

    @Test
    @DisplayName("실패 - 없는 초대")
    public void Failure_NoInvitation() {
      // Given
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      String invalidToken = "nonExistentToken";

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.acceptInvitation(authUserDTO, invalidToken);

      // Then
      Assertions.assertThrows(NoInvitationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 만료된 초대")
    public void Failure_ExpiredInvitation() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "expiredToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().minusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.acceptInvitation(authUserDTO, token);

      // Then
      Assertions.assertThrows(ExpiredInvitationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 이미 그룹 멤버")
    public void Failure_AlreadyGroupMember() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.acceptInvitation(authUserDTO, token);

      // Then
      Assertions.assertThrows(AlreadyGroupMemberException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 정보 수정 테스트")
  class UpdateGroupTest {

    private static Stream<Arguments> SuccessfulAuthorities() {
      return Stream.of(Arguments.of(GroupRole.OWNER), Arguments.of(GroupRole.MANAGER));
    }

    private static Stream<Arguments> FailingAuthorities() {
      return Stream.of(Arguments.of(GroupRole.MEMBER), Arguments.of(GroupRole.VIEWER));
    }

    @ParameterizedTest
    @MethodSource("SuccessfulAuthorities")
    @DisplayName("성공")
    void happyPath(GroupRole role) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<GroupResponseDTO> response =
          groupController.updateGroup(authUserDTO, group.getId(), requestDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      GroupDTO updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
      Assertions.assertEquals("new name", updatedGroup.name());
      Assertions.assertEquals("new desc", updatedGroup.description());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthorities")
    @DisplayName("실패 - 권한 없음 (MEMBER, VIEWER)")
    void failure_forbidden(GroupRole role) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.updateGroup(authUserDTO, group.getId(), requestDTO);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 그룹")
    void failure_groupNotFound() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      th.delete(userGroup);
      th.delete(group);

      // When
      Runnable lambda = () -> groupController.updateGroup(authUserDTO, group.getId(), requestDTO);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 멤버 추방 테스트")
  class DeleteUserGroupTest {

    private static Stream<Arguments> SuccessfulAuthorities() {
      return Stream.of(Arguments.of(GroupRole.OWNER));
    }

    private static Stream<Arguments> FailingAuthorities() {
      return Stream.of(
          Arguments.of(GroupRole.MANAGER),
          Arguments.of(GroupRole.MEMBER),
          Arguments.of(GroupRole.VIEWER));
    }

    @ParameterizedTest
    @MethodSource("SuccessfulAuthorities")
    @DisplayName("성공")
    void happyPath(GroupRole userRole) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), GroupRole.MANAGER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      ResponseEntity<Void> response =
          groupController.deleteUserGroup(authUserDTO, group.getId(), target.getId());

      // Then
      Assertions.assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
      Assertions.assertEquals(
          true,
          userGroupRepository.findByUserIdAndGroupId(target.getId(), group.getId()).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthorities")
    @DisplayName("실패 - 권한 없음")
    void failure_forbidden(GroupRole userRole) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), GroupRole.MEMBER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda =
          () -> groupController.deleteUserGroup(authUserDTO, group.getId(), target.getId());

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 자기 자신을 추방")
    void failure_cannotRemoveSelf() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda =
          () -> groupController.deleteUserGroup(authUserDTO, group.getId(), user.getId());

      // Then
      Assertions.assertThrows(CannotRemoveSelfException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버")
    void failure_memberNotFound() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda =
          () -> groupController.deleteUserGroup(authUserDTO, group.getId(), target.getId());

      // Then
      Assertions.assertThrows(MembershipNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 멤버 역할 변경 테스트")
  class UpdateUserGroupTest {

    private static Stream<Arguments> SuccessfulAuthoritiesUpdate() {
      return Stream.of(
          Arguments.of(GroupRole.OWNER, GroupRole.MANAGER, GroupRole.MEMBER),
          Arguments.of(GroupRole.OWNER, GroupRole.MANAGER, GroupRole.VIEWER),
          Arguments.of(GroupRole.OWNER, GroupRole.MEMBER, GroupRole.MANAGER),
          Arguments.of(GroupRole.OWNER, GroupRole.MEMBER, GroupRole.VIEWER),
          Arguments.of(GroupRole.OWNER, GroupRole.VIEWER, GroupRole.MANAGER),
          Arguments.of(GroupRole.OWNER, GroupRole.VIEWER, GroupRole.MEMBER),
          Arguments.of(GroupRole.MANAGER, GroupRole.MEMBER, GroupRole.VIEWER),
          Arguments.of(GroupRole.MANAGER, GroupRole.VIEWER, GroupRole.MEMBER));
    }

    private static Stream<Arguments> FailingAuthoritiesUpdate() {
      return Stream.of(
          Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER, GroupRole.MEMBER),
          Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER, GroupRole.VIEWER));
    }

    private static Stream<Arguments> FailingAuthoritiesToOwnerUpdate() {
      return Stream.of(
          Arguments.of(GroupRole.OWNER, GroupRole.MANAGER, GroupRole.OWNER),
          Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER, GroupRole.OWNER));
    }

    @ParameterizedTest
    @MethodSource("SuccessfulAuthoritiesUpdate")
    @DisplayName("성공")
    void happyPath(GroupRole userRole, GroupRole targetRole, GroupRole newRole) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);

      // When
      ResponseEntity<Void> response =
          groupController.updateUserGroup(authUserDTO, group.getId(), target.getId(), requestDTO);

      // Then
      Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
      UserGroupDTO updatedUserGroup =
          userGroupRepository.findByUserIdAndGroupId(target.getId(), group.getId()).orElseThrow();
      Assertions.assertEquals(newRole, updatedUserGroup.groupRole());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthoritiesUpdate")
    @DisplayName("실패 - 권한 없음")
    void failure_forbidden(GroupRole userRole, GroupRole targetRole, GroupRole newRole) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);

      // When
      Runnable lambda =
          () ->
              groupController.updateUserGroup(
                  authUserDTO, group.getId(), target.getId(), requestDTO);

      // Then
      Assertions.assertThrows(ForbiddenRoleChangeException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 자기 자신의 역할 변경 시도")
    void failure_cannotUpdateSelf() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(GroupRole.MANAGER);

      // When
      Runnable lambda =
          () ->
              groupController.updateUserGroup(authUserDTO, group.getId(), user.getId(), requestDTO);

      // Then
      Assertions.assertThrows(ForbiddenRoleChangeException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버")
    void failure_memberNotFound() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(GroupRole.MANAGER);

      // When
      Runnable lambda =
          () ->
              groupController.updateUserGroup(
                  authUserDTO, group.getId(), target.getId(), requestDTO);

      // Then
      Assertions.assertThrows(MembershipNotFoundException.class, lambda::run);
    }

    @ParameterizedTest
    @MethodSource("FailingAuthoritiesToOwnerUpdate")
    @DisplayName("실패 - OWNER로 역할 변경 시도")
    void failure_cannotPromoteToOwner(GroupRole userRole, GroupRole targetRole, GroupRole newRole) {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);

      // When
      Runnable lambda =
          () ->
              groupController.updateUserGroup(
                  authUserDTO, group.getId(), target.getId(), requestDTO);

      // Then
      Assertions.assertThrows(ForbiddenRoleChangeException.class, lambda::run);
    }
  }
}
