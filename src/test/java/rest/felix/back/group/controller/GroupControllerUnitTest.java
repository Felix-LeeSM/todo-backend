package rest.felix.back.group.controller;

import jakarta.persistence.EntityManager;
import java.security.Principal;
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
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.Pair;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.dto.DetailedGroupResponseDTO;
import rest.felix.back.group.dto.GroupResponseDTO;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.NoMatchingUserException;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class GroupControllerUnitTest {

  @Autowired private GroupController groupController;
  @Autowired private PasswordService passwordService;
  @Autowired private EntityManager em;
  private EntityFactory entityFactory;

  @BeforeEach
  void setUp() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  @SpringBootTest
  @Transactional
  @ActiveProfiles("test")
  @DisplayName("그룹 생성 테스트")
  class CreateGroupTest {

    @Test
    public void createGroup_HappyPath() {
      // Given

      User user = entityFactory.insertUser("username", "some_password", "nickname");

      Principal principal = user::getUsername;

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");

      // When

      ResponseEntity<GroupResponseDTO> responseEntity =
          groupController.createGroup(principal, createGroupRequestDTO);

      // Then

      Assertions.assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

      GroupResponseDTO groupResponseDTO = responseEntity.getBody();

      Group createdGroup =
          em.createQuery(
                  """
            SELECT
                g
            FROM
                Group g
            WHERE
                g.id = :groupId

            """,
                  Group.class)
              .setParameter("groupId", groupResponseDTO.id())
              .getSingleResult();

      Assertions.assertEquals("groupName", createdGroup.getName());
      Assertions.assertEquals("group description", createdGroup.getDescription());

      UserGroup userGroup =
          em.createQuery(
                  """
            SELECT
                ug
            FROM
                UserGroup ug
            WHERE
                ug.group.id = :groupId AND
                ug.user.id = :userId
            """,
                  UserGroup.class)
              .setParameter("groupId", groupResponseDTO.id())
              .setParameter("userId", user.getId())
              .getSingleResult();

      Assertions.assertEquals(GroupRole.OWNER, userGroup.getGroupRole());
    }

    @Test
    public void createGroup_Failure_NoSuchUser() {
      // Given

      User user = entityFactory.insertUser("username", "some_password", "nickname");

      em.flush();
      em.remove(user);
      em.flush();

      Principal principal = user::getUsername;

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");

      // When

      Runnable lambda = () -> groupController.createGroup(principal, createGroupRequestDTO);

      // Then

      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
    }
  }

  @SpringBootTest
  @Transactional
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

      em.flush();

      Principal principal = mainUser::getUsername;

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(principal);

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
          List.of("mainUser", "otherUser1"),
          group1DTO.members().stream().map(MemberDTO::getUsername).sorted().toList());

      DetailedGroupResponseDTO group2DTO = body.get(1);
      Assertions.assertEquals(group2.getId(), group2DTO.id());
      Assertions.assertEquals("Group 2", group2DTO.name());
      Assertions.assertEquals("Description 2", group2DTO.description());
      Assertions.assertEquals(3, group2DTO.todoCount());
      Assertions.assertEquals(2, group2DTO.completedTodoCount());
      Assertions.assertEquals(3, group2DTO.memberCount());
      Assertions.assertEquals(GroupRole.MEMBER, group2DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUser", "otherUser1", "otherUser2"),
          group2DTO.members().stream().map(MemberDTO::getUsername).sorted().toList());
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

      em.flush();

      Principal principal = mainUser::getUsername;

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(principal);

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
          List.of("mainUser", "otherUser1"),
          group1DTO.members().stream().map(MemberDTO::getUsername).sorted().toList());
    }

    @Test
    @DisplayName("Happy Path - No groups")
    public void Happy_Path_NoGroup() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      entityFactory.insertGroup("Group 1", "Description 1");

      em.flush();

      Principal principal = mainUser::getUsername;

      // When
      ResponseEntity<List<DetailedGroupResponseDTO>> responseEntity =
          groupController.getMyDetailedGroups(principal);

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
      Principal principal = mainUser::getUsername;

      em.remove(mainUser);
      em.flush();

      // When
      Runnable lambda = () -> groupController.getMyDetailedGroups(principal);

      // Then
      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
    }
  }

  @SpringBootTest
  @Transactional
  @DisplayName("유저 단일 그룹 조회 테스트")
  class GetUserGroupTest {

    @Test
    public void HappyPath() {

      // Given

      User user = entityFactory.insertUser("username", "some_password", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      Principal principal = user::getUsername;

      // When

      ResponseEntity<GroupResponseDTO> responseEntity =
          groupController.getUserGroup(principal, group.getId());

      // Then

      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

      GroupResponseDTO groupResponseDTO = responseEntity.getBody();
      Assertions.assertNotNull(groupResponseDTO.id());
      Assertions.assertEquals("group name", groupResponseDTO.name());
      Assertions.assertEquals("group description", groupResponseDTO.description());
    }

    @Test
    public void Failure_NoGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(group);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.getUserGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    public void Failure_NoUser() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(user);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.getUserGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);
    }

    @Test
    public void Failure_NoUserGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.getUserGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @SpringBootTest
  @Transactional
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

      em.flush();

      Principal principal = user::getUsername;

      // When

      groupController.deleteGroup(principal, group.getId());

      // Then

      Assertions.assertTrue(
          em.createQuery(
                  """
                  SELECT g
                  FROM Group g
                  WHERE g.id = :groupId
                  """,
                  Group.class)
              .setParameter("groupId", group.getId())
              .getResultStream()
              .findFirst()
              .isEmpty());

      Assertions.assertTrue(
          em.createQuery(
                  """
                  SELECT t
                  FROM Todo t
                  WHERE t.group.id = :groupId
                  """,
                  Todo.class)
              .setParameter("groupId", group.getId())
              .getResultStream()
              .findFirst()
              .isEmpty());

      Assertions.assertTrue(
          em.createQuery(
                  """
                  SELECT ug
                  FROM UserGroup ug
                  WHERE ug.group.id = :groupId
                  """,
                  UserGroup.class)
              .setParameter("groupId", group.getId())
              .getResultStream()
              .findFirst()
              .isEmpty());
    }

    @Test
    public void Failure_Failure_NoAuthority() {

      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

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

                em.flush();

                Principal principal = user::getUsername;

                // When

                Runnable lambda = () -> groupController.deleteGroup(principal, group.getId());

                // Then

                Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

                Assertions.assertTrue(
                    em.createQuery(
                            """
                            SELECT g
                            FROM Group g
                            WHERE g.id = :groupId
                            """,
                            Group.class)
                        .setParameter("groupId", group.getId())
                        .getResultStream()
                        .findFirst()
                        .isPresent());

                Assertions.assertTrue(
                    em.createQuery(
                            """
                            SELECT t
                            FROM Todo t
                            WHERE t.group.id = :groupId
                            """,
                            Todo.class)
                        .setParameter("groupId", group.getId())
                        .getResultStream()
                        .findFirst()
                        .isPresent());

                Assertions.assertTrue(
                    em.createQuery(
                            """
                            SELECT ug
                            FROM UserGroup ug
                            WHERE ug.group.id = :groupId
                            """,
                            UserGroup.class)
                        .setParameter("groupId", group.getId())
                        .getResultStream()
                        .findFirst()
                        .isPresent());
              });
    }

    @Test
    public void Failure_Failure_NoUser() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(user);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.deleteGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(NoMatchingUserException.class, lambda::run);

      Assertions.assertTrue(
          em.createQuery(
                  """
                  SELECT g
                  FROM Group g
                  WHERE g.id = :groupId
                  """,
                  Group.class)
              .setParameter("groupId", group.getId())
              .getResultStream()
              .findFirst()
              .isPresent());
    }

    @Test
    public void Failure_Failure_NoUserGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.deleteGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);

      Assertions.assertTrue(
          em.createQuery(
                  """
                  SELECT g
                  FROM Group g
                  WHERE g.id = :groupId
                  """,
                  Group.class)
              .setParameter("groupId", group.getId())
              .getResultStream()
              .findFirst()
              .isPresent());
    }

    @Test
    public void Failure_Failure_NoGroup() {

      // Given

      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(group);

      em.flush();

      Principal principal = user::getUsername;

      // When

      Runnable lambda = () -> groupController.getUserGroup(principal, group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }
}
