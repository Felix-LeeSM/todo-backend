package rest.felix.back.todo.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import java.util.Arrays;
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
import rest.felix.back.common.util.Trio;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.CreateTodoRequestDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TodoControllerWebTest {

  @Autowired EntityManager em;
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
  void getTodos_HappyPath() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    List<Trio<TodoStatus, String, Integer>> list =
        Arrays.asList(
            new Trio<>(TodoStatus.TO_DO, "c", 1),
            new Trio<>(TodoStatus.IN_PROGRESS, "a", 2),
            new Trio<>(TodoStatus.DONE, "b", 3),
            new Trio<>(TodoStatus.ON_HOLD, "d", 4));

    list.forEach(
        trio -> {
          TodoStatus todoStatus = trio.first();
          String order = trio.second();
          int idx = trio.third();

          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              String.format("todo %d", idx),
              String.format("todo %d description", idx),
              todoStatus,
              order,
              false);
        });

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isOk());
    result.andExpect(jsonPath("$", hasSize(4)));
    result.andExpect(jsonPath("$[0].title", equalTo("todo 2")));
    result.andExpect(jsonPath("$[0].description", equalTo("todo 2 description")));
    result.andExpect(jsonPath("$[0].status", equalTo("IN_PROGRESS")));
    result.andExpect(jsonPath("$[0].order", equalTo("a")));
    result.andExpect(jsonPath("$[1].title", equalTo("todo 3")));
    result.andExpect(jsonPath("$[1].description", equalTo("todo 3 description")));
    result.andExpect(jsonPath("$[1].status", equalTo("DONE")));
    result.andExpect(jsonPath("$[1].order", equalTo("b")));
    result.andExpect(jsonPath("$[2].title", equalTo("todo 1")));
    result.andExpect(jsonPath("$[2].description", equalTo("todo 1 description")));
    result.andExpect(jsonPath("$[2].status", equalTo("TO_DO")));
    result.andExpect(jsonPath("$[2].order", equalTo("c")));
    result.andExpect(jsonPath("$[3].title", equalTo("todo 4")));
    result.andExpect(jsonPath("$[3].description", equalTo("todo 4 description")));
    result.andExpect(jsonPath("$[3].status", equalTo("ON_HOLD")));
    result.andExpect(jsonPath("$[3].order", equalTo("d")));
  }

  @Test
  void getTodos_HappyPath_NoTodo() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isOk());
    result.andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void getTodos_Failure_NoUser() throws Exception {

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

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  void getTodos_Failure_NoGroup() throws Exception {

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

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void getTodos_Failure_NoUserGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            get(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void createTodo_HappyPath() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isCreated());
    result.andExpect(jsonPath("$.id", notNullValue()));
    result.andExpect(jsonPath("$.authorId", equalTo(user.getId().intValue())));
    result.andExpect(jsonPath("$.groupId", equalTo(group.getId().intValue())));
    result.andExpect(jsonPath("$.title", equalTo("todo title")));
    result.andExpect(jsonPath("$.description", equalTo("todo description")));
    result.andExpect(jsonPath("$.status", equalTo("TO_DO")));
    result.andExpect(jsonPath("$.order", equalTo("todo order")));
  }

  @Test
  void createTodo_Failure_NoCookie() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
  }

  @Test
  void createTodo_Failure_NoUser() throws Exception {

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

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  void createTodo_Failure_NoGroup() throws Exception {

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

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void createTodo_Failure_NoUserGroup() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

    em.flush();

    em.remove(userGroup);
    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void createTodo_Failure_GroupRoleViewer() throws Exception {

    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

    em.flush();

    em.remove(userGroup);
    em.remove(group);
    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void createTodo_Failure_Failure_Duplicated_Order_Status_In_Group() throws Exception {

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
            TodoStatus.TO_DO,
            "todo order",
            false);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    CreateTodoRequestDTO createTodoRequestDTO =
        new CreateTodoRequestDTO("todo title", "todo description", "todo order");

    String body = objectMapper.writeValueAsString(createTodoRequestDTO);

    String path = String.format("/api/v1/group/%d/todo", group.getId());

    // When

    ResultActions result =
        mvc.perform(
            post(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isBadRequest());
    result.andExpect(jsonPath("$.message", equalTo("Bad Request, please try again later.")));
  }

  @Test
  void deleteTodo_HappyPath() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MANAGER);

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

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isNoContent());

    Assertions.assertTrue(
        em.createQuery(
                """
                SELECT
                  t
                FROM
                  Todo t
                WHERE
                  t.id = :todoId
                """,
                Todo.class)
            .setParameter("todoId", todo.getId())
            .getResultStream()
            .findFirst()
            .isEmpty());
  }

  @Test
  void deleteTodo_Failure_NoUser() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MANAGER);

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

    em.remove(todo);
    em.remove(userGroup);
    em.remove(user);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  void deleteTodo_Failure_NoGroupUser() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MANAGER);

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

    em.remove(userGroup);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole1() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

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

    em.remove(userGroup);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void deleteTodo_Failure_ImproperGroupRole2() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    User author =
        entityFactory.insertUser("username123_author", "hashedPassword", "nickname_author");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

    UserGroup authorUserGroup =
        entityFactory.insertUserGroup(author.getId(), group.getId(), GroupRole.MEMBER);

    Todo todo =
        entityFactory.insertTodo(
            author.getId(),
            author.getId(),
            group.getId(),
            "todo title",
            "todo description",
            TodoStatus.IN_PROGRESS,
            "todo order",
            false);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void deleteTodo_Failure_NoGroup() throws Exception {
    // Given

    User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

    Group group = entityFactory.insertGroup("group name", "group description");

    UserGroup userGroup =
        entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

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

    em.remove(todo);
    em.remove(userGroup);
    em.remove(group);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void deleteTodo_Failure_NoTodo() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.VIEWER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);

    em.flush();

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    // When

    ResultActions result =
        mvc.perform(
            delete(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_HappyPath() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.MEMBER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isOk());
    result.andExpect(jsonPath("$.id", equalTo(todo.getId().intValue())));
    result.andExpect(jsonPath("$.title", equalTo("updated todo title")));
    result.andExpect(jsonPath("$.description", equalTo("updated todo description")));
    result.andExpect(jsonPath("$.status", equalTo("ON_HOLD")));
    result.andExpect(jsonPath("$.order", equalTo("someOrder")));
    result.andExpect(jsonPath("$.authorId", equalTo(user.getId().intValue())));
    result.andExpect(jsonPath("$.groupId", equalTo(group.getId().intValue())));

    Todo updatedTodo =
        em.createQuery(
                """
            SELECT
              t
            FROM
              Todo t
            WHERE
              t.id = :todoId
            """,
                Todo.class)
            .setParameter("todoId", todo.getId())
            .getSingleResult();

    Assertions.assertEquals(todo.getId(), updatedTodo.getId());
    Assertions.assertEquals("updated todo title", updatedTodo.getTitle());
    Assertions.assertEquals("updated todo description", updatedTodo.getDescription());
    Assertions.assertEquals(TodoStatus.ON_HOLD, updatedTodo.getTodoStatus());
    Assertions.assertEquals(user.getId(), updatedTodo.getAuthor().getId());
    Assertions.assertEquals(group.getId(), updatedTodo.getGroup().getId());
  }

  @Test
  void updateTodo_Failure_NoUser() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.MEMBER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(user);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isUnauthorized());
    result.andExpect(jsonPath("$.message", equalTo("There is no user with given conditions.")));
  }

  @Test
  void updateTodo_Failure_NoUserGroup() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.MANAGER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(userGroup);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole1() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.VIEWER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_Failure_ImproperGroupRole2() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    User author = new User();
    author.setUsername("username123_author");
    author.setNickname("nickname_author");
    author.setHashedPassword("hashedPassword");

    em.persist(author);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.MEMBER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    UserGroup authorUserGroup = new UserGroup();
    authorUserGroup.setGroupRole(GroupRole.MEMBER);
    authorUserGroup.setUser(author);
    authorUserGroup.setGroup(group);

    em.persist(authorUserGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(author);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_Failure_NoGroup() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.VIEWER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);
    em.remove(userGroup);
    em.remove(group);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_Failure_NoTodo() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username123");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setGroupRole(GroupRole.VIEWER);
    userGroup.setUser(user);
    userGroup.setGroup(group);

    em.persist(userGroup);

    Todo todo = new Todo();
    todo.setTitle("todo title");
    todo.setDescription("todo description");
    todo.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo.setOrder("todo order");
    todo.setAuthor(user);
    todo.setGroup(group);

    em.persist(todo);

    em.flush();

    em.remove(todo);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title", "updated todo description", TodoStatus.ON_HOLD, "someOrder");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isForbidden());
    result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
  }

  @Test
  void updateTodo_Failure_Duplicated_Order_Status_In_Group() throws Exception {
    // Given

    User user = new User();
    user.setUsername("username");
    user.setNickname("nickname");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("description");

    em.persist(group);

    UserGroup userGroup = new UserGroup();
    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(GroupRole.OWNER);

    em.persist(userGroup);

    Todo todo1 = new Todo();
    todo1.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo1.setTitle("todo title");
    todo1.setDescription("todo description");
    todo1.setOrder("todo1 order");
    todo1.setAuthor(user);
    todo1.setGroup(group);

    Todo todo2 = new Todo();
    todo2.setTodoStatus(TodoStatus.IN_PROGRESS);
    todo2.setTitle("todo title");
    todo2.setDescription("todo description");
    todo2.setOrder("todo2 order");
    todo2.setAuthor(user);
    todo2.setGroup(group);

    em.persist(todo1);
    em.persist(todo2);

    em.flush();

    UpdateTodoRequestDTO updateTodoRequestDTO =
        new UpdateTodoRequestDTO(
            "updated todo title",
            "updated todo description",
            TodoStatus.IN_PROGRESS,
            "todo1 order");

    Cookie cookie = userCookie(user.getUsername());

    String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo2.getId());

    String body = objectMapper.writeValueAsString(updateTodoRequestDTO);

    // When

    ResultActions result =
        mvc.perform(
            put(path)
                .cookie(cookie)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

    // Then

    result.andExpect(status().isBadRequest());
    result.andExpect(jsonPath("$.message", equalTo("Bad Request, please try again later.")));
  }
}
