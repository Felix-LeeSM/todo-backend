package rest.felix.back.group.controller;

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
import jakarta.transaction.Transactional;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.group.dto.CreateGroupRequestDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
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

  private Cookie userCookie(String username) {
    return new Cookie("accessToken", jwtTokenProvider.generateToken(username));
  }

  @Test
  public void createGroup_HappyPath() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
    em.flush();
    Cookie cookie = userCookie(user.getUsername());

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
  public void createGroup_Failure_NoSuchUser() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
    em.flush();
    em.remove(user);
    em.flush();
    Cookie cookie = userCookie(user.getUsername());

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

    result.andExpect(status().isUnauthorized());
  }

  @Test
  public void createGroup_Failure_NoCookie() throws Exception {

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
  public void createGroup_Failure_InvalidArgument() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
    em.flush();
    Cookie cookie = userCookie(user.getUsername());

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

  @Test
  public void getUserGroup_HappyPath() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

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
    result.andExpect(jsonPath("$.name", equalTo("group name")));
    result.andExpect(jsonPath("$.description", equalTo("group description")));
  }

  @Test
  public void getUserGroup_Failure_NoCookie() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
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
  public void getUserGroup_Failure_NoUser() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(user);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  public void getUserGroup_Failure_NoGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(group);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

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
  public void getUserGroup_Failure_NoUserGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

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
  public void deleteGroup_HappyPath() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

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

    Cookie cookie = userCookie(user.getUsername());

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
  public void deleteGroup_Failure_NoUser() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(user);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  public void deleteGroup_Failure_NoUserGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

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
  public void deleteGroup_Failure_ImproperGroupRole() throws Exception {

    // Given

    Group group = entityFactory.insertGroup("group name", "group description");

    for (GroupRole role : List.of(GroupRole.MANAGER, GroupRole.MEMBER, GroupRole.VIEWER)) {

      User user = entityFactory.insertUser("username123" + role, "hashedPassword", "nickname");

      UserGroup userGroup = entityFactory.insertUserGroup(user.getId(), group.getId(), role);

      em.flush();

      Cookie cookie = userCookie(user.getUsername());

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
  public void deleteGroup_Failure_NoGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.remove(group);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

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
