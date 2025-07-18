package rest.felix.back.group.controller;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GroupControllerWebTest {

  @Autowired private EntityManager em;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private PasswordService passwordService;
  private EntityFactory entityFactory;

  @BeforeEach
  void setUp() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  private Cookie userCookie(User user) {
    return new Cookie("accessToken", jwtTokenProvider.generateToken(AuthUserDTO.of(user)));
  }

  @Nested
  @DisplayName("그룹 생성 테스트")
  class CreateGroupTest {

    @Test
    public void HappyPath() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      em.flush();
      Cookie cookie = userCookie(user);

      String path = "/api/v1/group";

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");
      String body = objectMapper.writeValueAsString(createGroupRequestDTO);

      // When

      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .content(body)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isCreated());
      result.andExpect(jsonPath("$.id").isNotEmpty());
      result.andExpect(jsonPath("$.name").value("groupName"));
      result.andExpect(jsonPath("$.description").value("group description"));
    }

    @Test
    public void Failure_NoSuchUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      em.flush();
      em.remove(user);
      em.flush();
      Cookie cookie = userCookie(user);

      String path = "/api/v1/group";

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "description");
      String body = objectMapper.writeValueAsString(createGroupRequestDTO);

      // When

      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .content(body)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
    }

    @Test
    public void Failure_NoCookie() throws Exception {

      // Given

      String path = "/api/v1/group";

      CreateGroupRequestDTO createGroupRequestDTO =
          new CreateGroupRequestDTO("groupName", "group description");
      String body = objectMapper.writeValueAsString(createGroupRequestDTO);

      // When

      ResultActions result =
          mvc.perform(
              post(path)
                  .content(body)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
    }

    @Test
    public void Failure_InvalidArgument() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      em.flush();
      Cookie cookie = userCookie(user);

      String path = "/api/v1/group";

      for (String[] row : new String[][] {{"groupName", null}, {null, "group description"}}) {
        CreateGroupRequestDTO createGroupRequestDTO = new CreateGroupRequestDTO(row[0], row[1]);
        String body = objectMapper.writeValueAsString(createGroupRequestDTO);

        // When

        ResultActions result =
            mvc.perform(
                post(path)
                    .cookie(cookie)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON));

        // Then

        result.andExpect(status().isBadRequest());
      }
    }
  }

  @Nested
  @DisplayName("유저 전체 그룹 조회 테스트")
  class GetMyDetailedGroupsTest {
    @Test
    @DisplayName("Happy Path - 2 groups")
    public void HappyPath_1() throws Exception {

      // Given

      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      User otherUser1 = entityFactory.insertUser("otherUser1", "password", "otherUser1Nick");
      User otherUser2 = entityFactory.insertUser("otherUser2", "password", "otherUser2Nick");

      Group group1 = entityFactory.insertGroup("Group 1", "Description 1");
      Group group2 = entityFactory.insertGroup("Group 2", "Description 2");
      entityFactory.insertGroup("Group 3", "Description 3");

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

      Cookie cookie = userCookie(mainUser);
      String path = "/api/v1/group/my";

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then

      String group1Key = String.format("$[?(@.id == %d)]", group1.getId());
      String group2Key = String.format("$[?(@.id == %d)]", group2.getId());
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath(group1Key + ".name").value("Group 1"))
          .andExpect(jsonPath(group1Key + ".description").value("Description 1"))
          .andExpect(jsonPath(group1Key + ".todoCount").value(2))
          .andExpect(jsonPath(group1Key + ".completedTodoCount").value(1))
          .andExpect(jsonPath(group1Key + ".memberCount").value(2))
          .andExpect(jsonPath(group1Key + ".myRole").value("OWNER"))
          .andExpect(jsonPath(group2Key + ".name").value("Group 2"))
          .andExpect(jsonPath(group2Key + ".description").value("Description 2"))
          .andExpect(jsonPath(group2Key + ".todoCount").value(3))
          .andExpect(jsonPath(group2Key + ".completedTodoCount").value(2))
          .andExpect(jsonPath(group2Key + ".memberCount").value(3))
          .andExpect(jsonPath(group2Key + ".myRole").value("MEMBER"));
    }

    @Test
    @DisplayName("Happy Path - 1 group")
    public void HappyPath_2() throws Exception {
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

      Cookie cookie = userCookie(mainUser);
      String path = "/api/v1/group/my";

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].name").value("Group 1"))
          .andExpect(jsonPath("$[0].description").value("Description 1"))
          .andExpect(jsonPath("$[0].todoCount").value(1))
          .andExpect(jsonPath("$[0].completedTodoCount").value(0))
          .andExpect(jsonPath("$[0].memberCount").value(2))
          .andExpect(jsonPath("$[0].myRole").value("OWNER"));
    }

    @Test
    @DisplayName("Happy Path - No groups")
    public void HappyPath_3() throws Exception {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      entityFactory.insertGroup("Group 1", "Description 1");

      em.flush();

      Cookie cookie = userCookie(mainUser);
      String path = "/api/v1/group/my";

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Failure - No such user")
    public void Failure_NoSuchUser() throws Exception {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      Cookie cookie = userCookie(mainUser);

      em.remove(mainUser);
      em.flush();

      String path = "/api/v1/group/my";

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("유저 단일 그룹 조회 테스트")
  class GetUserGroupTest {
    @Test
    @DisplayName("Happy Path - User is OWNER with multiple members and todos")
    public void HappyPath() throws Exception {

      // Given
      User ownerUser = entityFactory.insertUser("ownerUser", "hashedPassword", "ownerNick");
      User memberUser1 = entityFactory.insertUser("memberUser1", "hashedPassword", "memberNick1");
      User memberUser2 = entityFactory.insertUser("memberUser2", "hashedPassword", "memberNick2");

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

      em.flush();

      Cookie cookie = userCookie(ownerUser);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              get(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.id", notNullValue()));
      result.andExpect(jsonPath("$.name", equalTo("Complex Group 1")));
      result.andExpect(jsonPath("$.description", equalTo("Description for complex group 1")));
      result.andExpect(jsonPath("$.myRole", equalTo("OWNER")));
      result.andExpect(jsonPath("$.memberCount", equalTo(3)));

      result.andExpect(
          jsonPath(
              "$.members[0].nickname",
              anyOf(equalTo("ownerNick"), equalTo("memberNick1"), equalTo("memberNick2"))));
      result.andExpect(
          jsonPath(
              "$.members[1].nickname",
              anyOf(equalTo("ownerNick"), equalTo("memberNick1"), equalTo("memberNick2"))));
      result.andExpect(
          jsonPath(
              "$.members[2].nickname",
              anyOf(equalTo("ownerNick"), equalTo("memberNick1"), equalTo("memberNick2"))));

      result.andExpect(jsonPath("$.todos.length()", equalTo(4)));
      result.andExpect(
          jsonPath(
              "$.todos[0].title",
              anyOf(
                  equalTo("Owner Todo 1"),
                  equalTo("Member1 Todo 1"),
                  equalTo("Member2 Todo 1"),
                  equalTo("Owner Todo 2"))));
      result.andExpect(
          jsonPath(
              "$.todos[1].title",
              anyOf(
                  equalTo("Owner Todo 1"),
                  equalTo("Member1 Todo 1"),
                  equalTo("Member2 Todo 1"),
                  equalTo("Owner Todo 2"))));
      result.andExpect(
          jsonPath(
              "$.todos[2].title",
              anyOf(
                  equalTo("Owner Todo 1"),
                  equalTo("Member1 Todo 1"),
                  equalTo("Member2 Todo 1"),
                  equalTo("Owner Todo 2"))));
      result.andExpect(
          jsonPath(
              "$.todos[3].title",
              anyOf(
                  equalTo("Owner Todo 1"),
                  equalTo("Member1 Todo 1"),
                  equalTo("Member2 Todo 1"),
                  equalTo("Owner Todo 2"))));
    }

    @Test
    public void Failure_NoCookie() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              get(path).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
    }

    @Test
    public void Failure_NoUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(user);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              get(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    public void Failure_NoGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(group);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              get(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("Failure - User not in group")
    public void Failure_NoUserGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      // User is not associated with the group

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              get(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("Failure - Group not found")
    public void Failure_GroupNotFound() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      long groupId = group.getId();

      em.remove(userGroup);
      em.flush();
      em.remove(group);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", groupId);

      // When

      ResultActions result =
          mvc.perform(
              get(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }
  }

  @Nested
  @DisplayName("그룹 삭제 테스트 테스트")
  class DeleteGroupTest {

    @Test
    public void HappyPath() throws Exception {

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
          TodoStatus.IN_PROGRESS,
          "todo order",
          false);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              delete(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isNoContent());

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
    public void Failure_NoUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(user);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              delete(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    public void Failure_NoUserGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              delete(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    public void Failure_ImproperGroupRole() throws Exception {

      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      for (GroupRole role : List.of(GroupRole.MANAGER, GroupRole.MEMBER, GroupRole.VIEWER)) {

        User user = entityFactory.insertUser("username123" + role, "hashedPassword", "nickname");

        entityFactory.insertUserGroup(user.getId(), group.getId(), role);

        em.flush();

        Cookie cookie = userCookie(user);

        String path = String.format("/api/v1/group/%d", group.getId());

        // When

        ResultActions result =
            mvc.perform(
                delete(path)
                    .cookie(cookie)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON));

        // Then

        result.andExpect(status().isForbidden());
        result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
      }
    }

    @Test
    public void Failure_NoGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      em.flush();

      em.remove(userGroup);
      em.remove(group);

      em.flush();

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d", group.getId());

      // When

      ResultActions result =
          mvc.perform(
              delete(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }
  }
}
