package rest.felix.back.group.controller;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.Pair;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.dto.DetailedGroupResponseDTO;
import rest.felix.back.group.dto.FullGroupDetailsResponseDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.GroupResponseDTO;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.group.dto.UserGroupDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@ActiveProfiles("test")
public class GroupControllerUnitTest {

  @Autowired private GroupController groupController;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserGroupRepository userGroupRepository;
  @Autowired private TodoRepository todoRepository;
  @Autowired private EntityFactory entityFactory;

  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @SpringBootTest
  @ActiveProfiles("test")
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

      Assertions.assertEquals("groupName", createdGroup.getName());
      Assertions.assertEquals("group description", createdGroup.getDescription());

      UserGroupDTO userGroup =
          userGroupRepository.findByUserIdAndGroupId(user.getId(), groupResponseDTO.id()).get();

      Assertions.assertEquals(GroupRole.OWNER, userGroup.getGroupRole());
    }

    @Test
    public void createGroup_Failure_NoSuchUser() {
      // Given

      User user = entityFactory.insertUser("username", "some_password", "nickname");

      th.delete(user);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");

      // When

      Runnable lambda = () -> groupController.createGroup(authUserDTO, createGroupRequestDTO);

      // Then

      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
    }
  }

  @SpringBootTest
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
          false);
      entityFactory.insertTodo(
          otherUser1.getId(),
          otherUser1.getId(),
          group1.getId(),
          "Todo 1-2",
          "Desc",
          TodoStatus.DONE,
          "b",
          false);

      entityFactory.insertTodo(
          mainUser.getId(),
          mainUser.getId(),
          group2.getId(),
          "Todo 2-1",
          "Desc",
          TodoStatus.DONE,
          "a",
          false);
      entityFactory.insertTodo(
          otherUser1.getId(),
          otherUser1.getId(),
          group2.getId(),
          "Todo 2-2",
          "Desc",
          TodoStatus.DONE,
          "b",
          false);
      entityFactory.insertTodo(
          otherUser2.getId(),
          otherUser2.getId(),
          group2.getId(),
          "Todo 2-3",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
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

      body.sort((a, b) -> a.name().compareTo(b.name()));

      DetailedGroupResponseDTO group1DTO = body.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(2, group1DTO.todoCount());
      Assertions.assertEquals(1, group1DTO.completedTodoCount());
      Assertions.assertEquals(2, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick"),
          group1DTO.members().stream().map(MemberDTO::getNickname).sorted().toList());

      DetailedGroupResponseDTO group2DTO = body.get(1);
      Assertions.assertEquals(group2.getId(), group2DTO.id());
      Assertions.assertEquals("Group 2", group2DTO.name());
      Assertions.assertEquals("Description 2", group2DTO.description());
      Assertions.assertEquals(3, group2DTO.todoCount());
      Assertions.assertEquals(2, group2DTO.completedTodoCount());
      Assertions.assertEquals(3, group2DTO.memberCount());
      Assertions.assertEquals(GroupRole.MEMBER, group2DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick", "otherUser2Nick"),
          group2DTO.members().stream().map(MemberDTO::getNickname).sorted().toList());
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
          group1DTO.members().stream().map(MemberDTO::getNickname).sorted().toList());
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

    @Test
    @DisplayName("Failure - No such user")
    public void Failure_NoSuchUser() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      AuthUserDTO authUserDTO = AuthUserDTO.of(mainUser);

      th.delete(mainUser);

      // When
      Runnable lambda = () -> groupController.getMyDetailedGroups(authUserDTO);

      // Then
      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
    }
  }

  @SpringBootTest
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

      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "a",
          false);
      entityFactory.insertTodo(
          memberUser1.getId(),
          memberUser1.getId(),
          group.getId(),
          "Member1 Todo 1",
          "Desc",
          TodoStatus.DONE,
          "b",
          false);
      entityFactory.insertTodo(
          memberUser2.getId(),
          memberUser2.getId(),
          group.getId(),
          "Member2 Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
          false);
      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 2",
          "Desc",
          TodoStatus.DONE,
          "d",
          false);

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
          body.members().stream().map(MemberDTO::getNickname).sorted().toList());
      Assertions.assertEquals(4, body.todos().size());
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Owner Todo 1")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Member1 Todo 1")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Member2 Todo 1")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Owner Todo 2")));
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
          false);
      entityFactory.insertTodo(
          memberUser1.getId(),
          memberUser1.getId(),
          group.getId(),
          "Member3 Todo 1",
          "Desc",
          TodoStatus.DONE,
          "b",
          false);
      entityFactory.insertTodo(
          memberUser2.getId(),
          memberUser2.getId(),
          group.getId(),
          "Member4 Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
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
          body.members().stream().map(MemberDTO::getNickname).sorted().toList());
      Assertions.assertEquals(3, body.todos().size());
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Owner Todo 3")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Member3 Todo 1")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Member4 Todo 1")));
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
          false);
      entityFactory.insertTodo(
          memberUser1.getId(),
          memberUser1.getId(),
          group.getId(),
          "Member5 Todo 1",
          "Desc",
          TodoStatus.DONE,
          "b",
          false);
      entityFactory.insertTodo(
          managerUser.getId(),
          managerUser.getId(),
          group.getId(),
          "Manager Todo 1",
          "Desc",
          TodoStatus.IN_PROGRESS,
          "c",
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
          body.members().stream().map(MemberDTO::getNickname).sorted().toList());
      Assertions.assertEquals(3, body.todos().size());
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Owner Todo 4")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Member5 Todo 1")));
      Assertions.assertTrue(
          body.todos().stream().anyMatch(todo -> todo.getTitle().equals("Manager Todo 1")));
    }

    @Test
    @DisplayName("Failure - No such group")
    public void Failure_NoGroup() {
      // Given
      User user = entityFactory.insertUser("testUser4", "password", "nick4");

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);
      Long nonExistentGroupId = 999L; // A group ID that does not exist

      // When
      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, nonExistentGroupId);

      // Then
      Assertions.assertThrows(
          rest.felix.back.group.exception.GroupNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("Failure - No such user")
    public void Failure_NoUser() {
      // Given
      User user = entityFactory.insertUser("testUser5", "password", "nick5");
      Group group = entityFactory.insertGroup("Test Group 5", "Description 5");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(user); // Remove the user to simulate no such user

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When
      Runnable lambda = () -> groupController.getUserGroup(authUserDTO, group.getId());

      // Then
      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
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

  @SpringBootTest
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
          false);

      AuthUserDTO authUserDTO = AuthUserDTO.of(user);

      // When

      groupController.deleteGroup(authUserDTO, group.getId());

      // Then

      Assertions.assertTrue(groupRepository.findById(group.getId()).isEmpty());

      Assertions.assertTrue(todoRepository.findByGroupId(group.getId()).isEmpty());

      Assertions.assertTrue(userGroupRepository.findByGroupId(group.getId()).isEmpty());
    }

    @Test
    public void Failure_Failure_NoAuthority() {

      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      ;
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
                    false);

                AuthUserDTO authUserDTO = AuthUserDTO.of(user);

                // When

                Runnable lambda = () -> groupController.deleteGroup(authUserDTO, group.getId());

                // Then

                Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

                Assertions.assertTrue(groupRepository.findById(group.getId()).isPresent());

                Assertions.assertTrue(0 < todoRepository.findByGroupId(group.getId()).size());

                Assertions.assertTrue(0 < userGroupRepository.findByGroupId(group.getId()).size());
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

      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);

      Assertions.assertTrue(groupRepository.findById(group.getId()).isPresent());
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

      Assertions.assertTrue(groupRepository.findById(group.getId()).isPresent());
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
}
