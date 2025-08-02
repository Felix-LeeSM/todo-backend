package rest.felix.back.group.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import rest.felix.back.common.config.GroupConfig;
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.dto.*;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GroupControllerWebTest {

  @Autowired private TodoRepository todoRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserGroupRepository userGroupRepository;
  @Autowired private MockMvc mvc;
  @Autowired private GroupConfig groupConfig;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private EntityFactory entityFactory;
  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
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

      th.delete(user);

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
          null,
          false);

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

      th.delete(mainUser);

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
          null,
          false);
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
          false);
      entityFactory.insertTodo(
          ownerUser.getId(),
          ownerUser.getId(),
          group.getId(),
          "Owner Todo 2",
          "Desc",
          TodoStatus.DONE,
          "d",
          null,
          false);

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

      th.delete(userGroup);
      th.delete(user);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    public void Failure_NoGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

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

      long groupId = group.getId();

      th.delete(userGroup);

      th.delete(group);

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
          null,
          false);

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

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isEmpty());

      Assertions.assertEquals(true, todoRepository.findByGroupId(group.getId()).isEmpty());

      Assertions.assertEquals(true, userGroupRepository.findByGroupId(group.getId()).isEmpty());
    }

    @Test
    public void Failure_NoUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user);

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

      th.delete(userGroup);

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

      th.delete(userGroup);
      th.delete(group);

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

  @Nested
  @DisplayName("그룹 초대 생성 테스트")
  class CreateGroupInvitation {
    @ParameterizedTest
    @EnumSource(
        value = GroupRole.class,
        names = {"OWNER", "MANAGER"})
    @DisplayName("성공")
    public void HappyPath(GroupRole role) throws Exception {
      // Given
      User user = entityFactory.insertUser("username" + role.name(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/invitation", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isCreated());
      result.andExpect(jsonPath("$.token").isNotEmpty());
      result.andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 상태")
    public void Failure_NoCookie() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      String path = String.format("/api/v1/group/%d/invitation", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              post(path)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    public void Failure_NoSuchUser() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/invitation", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    public void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/invitation", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @ParameterizedTest
    @EnumSource(
        value = GroupRole.class,
        names = {"MEMBER", "VIEWER"})
    @DisplayName("실패 - 권한 부족")
    public void Failure_ImproperGroupRole(GroupRole role) throws Exception {
      // Given
      User user = entityFactory.insertUser("username" + role.name(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/invitation", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              post(path)
                  .cookie(cookie)
                  .contentType(MediaType.APPLICATION_JSON)
                  .accept(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }
  }

  @Nested
  @DisplayName("그룹 초대 정보 테스트")
  class GetInvitationInfo {
    @Test
    @DisplayName("성공")
    public void HappyPath_1() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      User member1 = entityFactory.insertUser("member1", "hashedPassword", "memberNick1");
      User member2 = entityFactory.insertUser("member2", "hashedPassword", "memberNick2");
      User member3 = entityFactory.insertUser("member3", "hashedPassword", "memberNick3");

      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(member1.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(member2.getId(), group.getId(), GroupRole.MANAGER);
      entityFactory.insertUserGroup(member3.getId(), group.getId(), GroupRole.VIEWER);

      entityFactory.insertTodo(
          issuer.getId(),
          issuer.getId(),
          group.getId(),
          "Todo A",
          "Desc A",
          TodoStatus.IN_PROGRESS,
          "a",
          null,
          false);
      entityFactory.insertTodo(
          member1.getId(),
          member1.getId(),
          group.getId(),
          "Todo B",
          "Desc B",
          TodoStatus.DONE,
          "b",
          null,
          false);
      entityFactory.insertTodo(
          member2.getId(),
          member2.getId(),
          group.getId(),
          "Todo C",
          "Desc C",
          TodoStatus.IN_PROGRESS,
          "c",
          null,
          false);
      entityFactory.insertTodo(
          member3.getId(),
          member3.getId(),
          group.getId(),
          "Todo D",
          "Desc D",
          TodoStatus.DONE,
          "d",
          null,
          false);
      entityFactory.insertTodo(
          issuer.getId(),
          issuer.getId(),
          group.getId(),
          "Todo E",
          "Desc E",
          TodoStatus.IN_PROGRESS,
          "e",
          null,
          false);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result =
          mvc.perform(get(path).cookie(cookie)).andDo(MockMvcResultHandlers.print());

      // Then
      result.andExpect(status().isOk());

      String responseString = result.andReturn().getResponse().getContentAsString();
      GroupInvitationInfoDTOResponse responseDTO =
          objectMapper.readValue(responseString, GroupInvitationInfoDTOResponse.class);

      // Verify basic group details
      Assertions.assertEquals(group.getName(), responseDTO.name());
      Assertions.assertEquals(group.getDescription(), responseDTO.description());
      Assertions.assertNotNull(responseDTO.expiresAt());

      // Verify issuer details
      Assertions.assertEquals(issuer.getId(), responseDTO.issuer().id());
      Assertions.assertEquals(issuer.getNickname(), responseDTO.issuer().nickname());
      Assertions.assertEquals(GroupRole.OWNER, responseDTO.issuer().role());

      // Verify member count
      Assertions.assertEquals(4, responseDTO.memberCount());

      // Verify members list
      List<MemberResponseDTO> expectedMembers =
          Stream.of(
                  new MemberResponseDTO(
                      issuer.getId(), issuer.getNickname(), group.getId(), GroupRole.OWNER),
                  new MemberResponseDTO(
                      member1.getId(), member1.getNickname(), group.getId(), GroupRole.MEMBER),
                  new MemberResponseDTO(
                      member2.getId(), member2.getNickname(), group.getId(), GroupRole.MANAGER),
                  new MemberResponseDTO(
                      member3.getId(), member3.getNickname(), group.getId(), GroupRole.VIEWER))
              .sorted(Comparator.comparing(MemberResponseDTO::id))
              .toList();

      List<MemberResponseDTO> actualMembers =
          responseDTO.members().stream()
              .sorted(Comparator.comparing(MemberResponseDTO::id))
              .collect(Collectors.toList());

      org.assertj.core.api.Assertions.assertThat(actualMembers)
          .usingRecursiveComparison()
          .isEqualTo(expectedMembers);

      // Verify todo counts
      Assertions.assertEquals(5, responseDTO.todoCount());
      Assertions.assertEquals(2, responseDTO.completedTodoCount());

      Assertions.assertEquals(false, responseDTO.isMember());
    }

    @Test
    @DisplayName("성공 - 이미 그룹 멤버")
    public void HappyPath_2() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isOk());

      String responseString = result.andReturn().getResponse().getContentAsString();
      GroupInvitationInfoDTOResponse responseDTO =
          objectMapper.readValue(responseString, GroupInvitationInfoDTOResponse.class);
      Assertions.assertEquals(true, responseDTO.isMember());
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 상태")
    public void Failure_NoCookie() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result = mvc.perform(get(path));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    public void Failure_NoSuchUser() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      th.delete(user);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 초대")
    public void Failure_NoInvitation() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");

      String token = "invalidToken";

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("초대가 존재하지 않습니다.")));
    }

    @Test
    @DisplayName("실패 - 만료된 초대")
    public void Failure_ExpiredInvitation() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "expiredToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().minusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s", token);

      // When
      ResultActions result = mvc.perform(get(path).cookie(cookie));

      // Then
      result.andExpect(status().isGone());
      result.andExpect(jsonPath("$.message", equalTo("만료된 초대입니다. 더 이상 사용할 수 없습니다.")));
    }
  }

  @Nested
  @DisplayName("그룹 초대 수락 테스트")
  class AcceptInvitation {
    @Test
    @DisplayName("성공")
    public void HappyPath() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isCreated());
      Assertions.assertEquals(
          true,
          userGroupRepository.findByUserIdAndGroupId(user.getId(), group.getId()).isPresent());
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 상태")
    public void Failure_NoCookie() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    public void Failure_NoSuchUser() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      th.delete(user);

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 초대")
    public void Failure_NoInvitation() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");

      String token = "invalidToken";

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("초대가 존재하지 않습니다.")));
    }

    @Test
    @DisplayName("실패 - 만료된 초대")
    public void Failure_ExpiredInvitation() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = "expiredToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().minusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isGone());
      result.andExpect(jsonPath("$.message", equalTo("만료된 초대입니다. 더 이상 사용할 수 없습니다.")));
    }

    @Test
    @DisplayName("실패 - 이미 그룹 멤버")
    public void Failure_AlreadyGroupMember() throws Exception {
      // Given
      User issuer = entityFactory.insertUser("issuer", "hashedPassword", "issuerNick");
      User user = entityFactory.insertUser("user", "hashedPassword", "userNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      String token = "validToken";
      entityFactory.insertGroupInvitation(
          issuer.getId(), group.getId(), token, ZonedDateTime.now().plusDays(1));

      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/invitation/%s/accept", token);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isConflict());
      result.andExpect(jsonPath("$.message", equalTo("User is already a member of the group.")));
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
    void happyPath(GroupRole role) throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              put(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.name").value("new name"));
      result.andExpect(jsonPath("$.description").value("new desc"));
    }

    @ParameterizedTest
    @MethodSource("FailingAuthorities")
    @DisplayName("실패 - 권한 없음 (MEMBER, VIEWER)")
    void failure_forbidden(GroupRole role) throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              put(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 그룹")
    void failure_groupNotFound() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO("new name", "new desc");
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      long groupId = group.getId();

      th.delete(userGroup);
      th.delete(group);

      String path = String.format("/api/v1/group/%d", groupId);

      // When
      ResultActions result =
          mvc.perform(
              put(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 유효성 검사 실패 (이름 누락)")
    void failure_validationFailed() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      UpdateGroupRequestDTO requestDTO = new UpdateGroupRequestDTO(null, "new desc");
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d", group.getId());

      // When
      ResultActions result =
          mvc.perform(
              put(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("그룹 멤버 추방 테스트")
  class DeleteUserGroupTest {

    private static Stream<Arguments> SuccessfulAuthoritiesDelete() {
      return Stream.of(
          Arguments.of(GroupRole.OWNER, GroupRole.MANAGER),
          Arguments.of(GroupRole.OWNER, GroupRole.MEMBER),
          Arguments.of(GroupRole.OWNER, GroupRole.VIEWER));
    }

    private static Stream<Arguments> FailingAuthoritiesDelete() {
      return Stream.of(
          Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER),
          Arguments.of(GroupRole.MEMBER, GroupRole.MEMBER),
          Arguments.of(GroupRole.MEMBER, GroupRole.VIEWER),
          Arguments.of(GroupRole.VIEWER, GroupRole.MEMBER),
          Arguments.of(GroupRole.VIEWER, GroupRole.VIEWER));
    }

    @ParameterizedTest
    @MethodSource("SuccessfulAuthoritiesDelete")
    @DisplayName("성공")
    void happyPath(GroupRole userRole, GroupRole targetRole) throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNoContent());
      Assertions.assertEquals(
          true,
          userGroupRepository.findByUserIdAndGroupId(target.getId(), group.getId()).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthoritiesDelete")
    @DisplayName("실패 - 권한 없음")
    void failure_forbidden(GroupRole userRole, GroupRole targetRole) throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 자기 자신을 추방")
    void failure_cannotRemoveSelf() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), user.getId());

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버")
    void failure_memberNotFound() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
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
    void happyPath(GroupRole userRole, GroupRole targetRole, GroupRole newRole) throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthoritiesUpdate")
    @DisplayName("실패 - 권한 없음")
    void failure_forbidden(GroupRole userRole, GroupRole targetRole, GroupRole newRole)
        throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 자기 자신의 역할 변경 시도")
    void failure_cannotUpdateSelf() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(GroupRole.MANAGER);
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), user.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버")
    void failure_memberNotFound() throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(GroupRole.MANAGER);
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("FailingAuthoritiesToOwnerUpdate")
    @DisplayName("실패 - OWNER로 역할 변경 시도")
    void failure_cannotPromoteToOwner(GroupRole userRole, GroupRole targetRole, GroupRole newRole)
        throws Exception {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      User target = entityFactory.insertUser("target", "pass", "targetNick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);
      entityFactory.insertUserGroup(target.getId(), group.getId(), targetRole);
      UpdateMemberRequestDTO requestDTO = new UpdateMemberRequestDTO(newRole);
      String body = objectMapper.writeValueAsString(requestDTO);
      Cookie cookie = userCookie(user);
      String path = String.format("/api/v1/group/%d/member/%d", group.getId(), target.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path).cookie(cookie).content(body).contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isForbidden());
    }
  }
}
