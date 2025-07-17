package rest.felix.back.group.controller;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.security.Principal;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.Pair;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.dto.GroupResponseDTO;
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

  @Test
  public void getUserGroup_HappyPath() {

    // Given

    User user = entityFactory.insertUser("username", "some_password", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
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
  public void getUserGroup_NoGroup() {

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
  public void getUserGroup_NoUser() {

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
  public void getUserGroup_NoUserGroup() {

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

  @Test
  public void deleteGroup_HappyPath() {

    // Given

    User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
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
                SELECT
                  g
                FROM
                  Group g
                WHERE
                  g.id = :groupId
                """,
                Group.class)
            .setParameter("groupId", group.getId())
            .getResultStream()
            .findFirst()
            .isEmpty());

    Assertions.assertTrue(
        em.createQuery(
                """
                SELECT
                  t
                FROM
                  Todo t
                WHERE
                  t.group.id = :groupId
                """,
                Todo.class)
            .setParameter("groupId", group.getId())
            .getResultStream()
            .findFirst()
            .isEmpty());

    Assertions.assertTrue(
        em.createQuery(
                """
                SELECT
                  ug
                FROM
                  UserGroup ug
                WHERE
                  ug.group.id = :groupId
                """,
                UserGroup.class)
            .setParameter("groupId", group.getId())
            .getResultStream()
            .findFirst()
            .isEmpty());
  }

  @Test
  public void deleteGroup_Failure_NoAuthority() {

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

              User user = entityFactory.insertUser("username" + role, "hashedPassword", "nickname");

              UserGroup userGroup =
                  entityFactory.insertUserGroup(user.getId(), group.getId(), role);

              Todo todo =
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
                          SELECT
                            g
                          FROM
                            Group g
                          WHERE
                            g.id = :groupId
                          """,
                          Group.class)
                      .setParameter("groupId", group.getId())
                      .getResultStream()
                      .findFirst()
                      .isPresent());

              Assertions.assertTrue(
                  em.createQuery(
                          """
                          SELECT
                            t
                          FROM
                            Todo t
                          WHERE
                            t.group.id = :groupId
                          """,
                          Todo.class)
                      .setParameter("groupId", group.getId())
                      .getResultStream()
                      .findFirst()
                      .isPresent());

              Assertions.assertTrue(
                  em.createQuery(
                          """
                          SELECT
                            ug
                          FROM
                            UserGroup ug
                          WHERE
                            ug.group.id = :groupId
                          """,
                          UserGroup.class)
                      .setParameter("groupId", group.getId())
                      .getResultStream()
                      .findFirst()
                      .isPresent());
            });
  }

  @Test
  public void deleteGroup_Failure_NoUser() {

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
                SELECT
                  g
                FROM
                  Group g
                WHERE
                  g.id = :groupId
                """,
                Group.class)
            .setParameter("groupId", group.getId())
            .getResultStream()
            .findFirst()
            .isPresent());
  }

  @Test
  public void deleteGroup_Failure_NoUserGroup() {

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
                SELECT
                  g
                FROM
                  Group g
                WHERE
                  g.id = :groupId
                """,
                Group.class)
            .setParameter("groupId", group.getId())
            .getResultStream()
            .findFirst()
            .isPresent());
  }

  @Test
  public void deleteGroup_Failure_NoGroup() {

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
