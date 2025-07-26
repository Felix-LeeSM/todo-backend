package rest.felix.back.todo.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.*;
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
import rest.felix.back.common.security.JwtTokenProvider;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.common.util.Trio;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.dto.CreateTodoRequestDTO;
import rest.felix.back.todo.dto.MoveTodoRequestDTO;
import rest.felix.back.todo.dto.TodoDTO;
import rest.felix.back.todo.dto.UpdateTodoRequestDTO;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.todo.repository.TodoRepository;
import rest.felix.back.user.dto.AuthUserDTO;
import rest.felix.back.user.entity.User;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TodoControllerWebTest {

  @Autowired EntityManager em;
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtTokenProvider jwtTokenProvider;
  @Autowired private EntityFactory entityFactory;
  @Autowired private TodoRepository todoRepository;
  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  private Cookie userCookie(User user) {
    return new Cookie("accessToken", jwtTokenProvider.generateToken(AuthUserDTO.of(user)));
  }

  @Nested
  @DisplayName("투두 조회 테스트")
  class GetTodos {

    @Test
    void HappyPath() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

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

      Cookie cookie = userCookie(user);

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
    void HappyPath_NoTodo() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Cookie cookie = userCookie(user);

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
    void Failure_NoUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user);

      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    void Failure_NoGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);

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
    void Failure_NoUserGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);

      Cookie cookie = userCookie(user);

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
  }

  @Nested
  @DisplayName("투두 생성 테스트")
  class CreateTodo {
    @Test
    void HappyPath() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
      result.andExpect(jsonPath("$.order", notNullValue()));
      result.andExpect(jsonPath("$.isImportant", equalTo(false)));
      result.andExpect(jsonPath("$.dueDate").doesNotExist());
      result.andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    @Test
    void HappyPath_WithDueDateAndAssignee() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      User assignee =
          entityFactory.insertUser("assigneeUser", "hashedPassword", "assigneeNickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      Cookie cookie = userCookie(user);

      LocalDate dueDate = LocalDate.now().plusDays(7);
      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", dueDate, assignee.getId());

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
      result.andExpect(jsonPath("$.order", notNullValue()));
      result.andExpect(jsonPath("$.isImportant", equalTo(false)));
      result.andExpect(jsonPath("$.dueDate", equalTo(dueDate.toString())));
      result.andExpect(jsonPath("$.assigneeId", equalTo(assignee.getId().intValue())));
    }

    @Test
    void HappyPath_WithDueDateOnly() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Cookie cookie = userCookie(user);

      LocalDate dueDate = LocalDate.now().plusDays(7);
      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", dueDate, null);

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
      result.andExpect(jsonPath("$.order", notNullValue()));
      result.andExpect(jsonPath("$.isImportant", equalTo(false)));
      result.andExpect(jsonPath("$.dueDate", equalTo(dueDate.toString())));
      result.andExpect(jsonPath("$.assigneeId").doesNotExist());
    }

    @Test
    void HappyPath_WithAssigneeOnly() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      User assignee =
          entityFactory.insertUser("assigneeUser", "hashedPassword", "assigneeNickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, assignee.getId());

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
      result.andExpect(jsonPath("$.order", notNullValue()));
      result.andExpect(jsonPath("$.isImportant", equalTo(false)));
      result.andExpect(jsonPath("$.dueDate").doesNotExist());
      result.andExpect(jsonPath("$.assigneeId", equalTo(assignee.getId().intValue())));
    }

    @Test
    void Failure_InvalidAssignee() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Cookie cookie = userCookie(user);

      // 존재하지 않는 assigneeId
      long invalidAssigneeId = 9999L;
      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, invalidAssigneeId);

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

      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    void Failure_NoCookie() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
    void Failure_NoUser() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(user);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    void Failure_NoGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
    void Failure_NoUserGroup() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
    void Failure_GroupRoleViewer() throws Exception {

      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);

      CreateTodoRequestDTO createTodoRequestDTO =
          new CreateTodoRequestDTO("todo title", "todo description", null, null);

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
  }

  @Nested
  @DisplayName("투두 삭제 테스트")
  class DeleteTodo {
    @Test
    void HappyPath_1() throws Exception {
      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

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

      Cookie cookie = userCookie(user);

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

      Assertions.assertEquals(true, todoRepository.findById(todo.getId()).isEmpty());

      Assertions.assertEquals(false, todoRepository.starExistsById(todo.getId()));
    }

    @Test
    void HappyPath_2() throws Exception {
      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

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

      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      Cookie cookie = userCookie(user);

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

      Assertions.assertEquals(true, todoRepository.findById(todo.getId()).isEmpty());

      Assertions.assertEquals(false, todoRepository.starExistsById(todo.getId()));
    }

    @Test
    void Failure_NoUser() throws Exception {
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

      th.delete(todo);
      th.delete(userGroup);
      th.delete(user);

      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    void Failure_NoGroupUser() throws Exception {
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

      th.delete(userGroup);

      Cookie cookie = userCookie(user);

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
    void Failure_ImproperGroupRole1() throws Exception {
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

      th.delete(userGroup);

      Cookie cookie = userCookie(user);

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
    void Failure_ImproperGroupRole2() throws Exception {
      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      User author =
          entityFactory.insertUser("username123_author", "hashedPassword", "nickname_author");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

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

      Cookie cookie = userCookie(user);

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
    void Failure_NoGroup() throws Exception {
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

      th.delete(todo);
      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);

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
    void Failure_NoTodo() throws Exception {
      // Given

      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

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

      Cookie cookie = userCookie(user);

      String path = String.format("/api/v1/group/%d/todo/%d", group.getId(), todo.getId());

      // When

      ResultActions result =
          mvc.perform(
              delete(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON));

      // Then

      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }
  }

  @Nested
  @DisplayName("투두 업데이트 테스트")
  class UpdateTodo {

    @Test
    void HappyPath() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.authorId", equalTo(user.getId().intValue())));
      result.andExpect(jsonPath("$.groupId", equalTo(group.getId().intValue())));

      Todo updatedTodo =
          em.createQuery(
                  """
                SELECT t
                FROM Todo t
                WHERE t.id = :todoId
                """,
                  Todo.class)
              .setParameter("todoId", todo.getId())
              .getSingleResult();

      Assertions.assertEquals(todo.getId(), updatedTodo.getId());
      Assertions.assertEquals("updated todo title", updatedTodo.getTitle());
      Assertions.assertEquals("updated todo description", updatedTodo.getDescription());
      Assertions.assertEquals(user.getId(), updatedTodo.getAuthor().getId());
      Assertions.assertEquals(group.getId(), updatedTodo.getGroup().getId());
    }

    @Test
    void Failure_NoUser() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      th.delete(todo);
      th.delete(userGroup);
      th.delete(user);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    void Failure_NoUserGroup() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MANAGER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      th.delete(userGroup);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
    void Failure_ImproperGroupRole1() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
    void Failure_ImproperGroupRole2() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      User author =
          entityFactory.insertUser("username123_author", "hashedPassword", "nickname_author");

      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(author.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              author.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
    void Failure_NoGroup() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      th.delete(todo);
      th.delete(userGroup);
      th.delete(group);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
    void Failure_NoTodo() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      th.delete(todo);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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

      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    void Failure_TodoInAnotherGorup() throws Exception {
      // Given

      User user = entityFactory.insertUser("uesrname123", "hashedPassword", "nickname");

      Group group = entityFactory.insertGroup("group name", "group description");
      Group group2 = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.VIEWER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);

      UpdateTodoRequestDTO updateTodoRequestDTO =
          new UpdateTodoRequestDTO("updated todo title", "updated todo description");

      Cookie cookie = userCookie(user);

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
  }

  @Nested
  @DisplayName("투두 메타데이터 업데이트 테스트")
  class UpdateTodoMetadata {
    @Test
    @DisplayName("성공 - 모든 필드 업데이트")
    void HappyPath_1() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee = entityFactory.insertUser("assignee", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      requestBody.put("dueDate", LocalDate.now().plusDays(1));
      requestBody.put("assigneeId", assignee.getId());

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.isImportant", equalTo(true)));
      result.andExpect(jsonPath("$.dueDate", equalTo(LocalDate.now().plusDays(1).toString())));
      result.andExpect(jsonPath("$.assigneeId", equalTo(assignee.getId().intValue())));

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertTrue(fetchedTodo.isImportant());
      Assertions.assertEquals(LocalDate.now().plusDays(1), fetchedTodo.dueDate());
      Assertions.assertEquals(assignee.getId(), fetchedTodo.assigneeId());
    }

    @ParameterizedTest
    @MethodSource("isImportantParameter")
    @DisplayName("성공 - isImportant만 업데이트")
    void HappyPath_2(Boolean current, Boolean toUpdate) throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              current != null && current);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", toUpdate);

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.isImportant", equalTo(toUpdate)));
      result.andExpect(jsonPath("$.dueDate").doesNotExist());
      result.andExpect(jsonPath("$.assigneeId").doesNotExist());

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertEquals(toUpdate, fetchedTodo.isImportant());
    }

    private static Stream<Arguments> isImportantParameter() {
      return Stream.of(
          Arguments.of(true, false),
          Arguments.of(true, true),
          Arguments.of(false, false),
          Arguments.of(false, true));
    }

    @ParameterizedTest
    @MethodSource("dueDateParameter")
    @DisplayName("성공 - dueDate만 업데이트")
    void HappyPath_3(LocalDate current, LocalDate toUpdate) throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              current,
              false);

      todo.setDueDate(current);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("dueDate", toUpdate);

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      if (toUpdate == null) {
        result.andExpect(jsonPath("$.dueDate").doesNotExist());
      } else {
        result.andExpect(jsonPath("$.dueDate", equalTo(toUpdate.toString())));
      }

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertEquals(toUpdate, fetchedTodo.dueDate());
    }

    private static Stream<Arguments> dueDateParameter() {
      LocalDate now = LocalDate.now();
      return Stream.of(
          Arguments.of(now, now.plusDays(3)),
          Arguments.of(null, now.minusDays(2)),
          Arguments.of(now.minusDays(20), null),
          Arguments.of(null, now.minusDays(10)));
    }

    @Test
    @DisplayName("성공 - assigneeId를 설정하여 담당자 설정")
    void HappyPath_4() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee = entityFactory.insertUser("assignee", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("assigneeId", assignee.getId());

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.assigneeId", equalTo(assignee.getId().intValue())));

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertEquals(assignee.getId(), fetchedTodo.assigneeId());
    }

    @Test
    @DisplayName("성공 - assigneeId를 설정하여 담당자 변경")
    void HappyPath_5() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User oldAssignee =
          entityFactory.insertUser("oldAssignee", "hashedPassword", "oldAssigneeNickname");
      User newAssignee =
          entityFactory.insertUser("newAssignee", "hashedPassword", "newAssigneeNickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(oldAssignee.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(newAssignee.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              oldAssignee.getId(),
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("assigneeId", newAssignee.getId());

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.assigneeId", equalTo(newAssignee.getId().intValue())));

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertEquals(newAssignee.getId(), fetchedTodo.assigneeId());
    }

    @Test
    @DisplayName("성공 - assigneeId를 null로 설정하여 담당자 해제")
    void HappyPath_6() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee = entityFactory.insertUser("assignee", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(assignee.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              assignee.getId(),
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("assigneeId", null);

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isOk());
      result.andExpect(jsonPath("$.assigneeId").doesNotExist());

      TodoDTO fetchedTodo = todoRepository.findById(todo.getId()).orElseThrow();
      Assertions.assertNull(fetchedTodo.assigneeId());
    }

    @Test
    @DisplayName("실패 - isImportant를 null로 변경하려 함.")
    void Failure_IsImportantIsNotNull() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", null);

      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isBadRequest());
      result.andExpect(jsonPath("$.message", equalTo("Bad Request, please check parameters.")));
    }

    @Test
    @DisplayName("실패 - 토큰 없음")
    void Failure_NoToken() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 유저 없음")
    void Failure_NoUser() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User author =
          entityFactory.insertUser("username_author", "hashedPassword", "nickname_author");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(author.getId(), group.getId(), GroupRole.MANAGER);
      Todo todo =
          entityFactory.insertTodo(
              author.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      th.delete(userGroup);
      th.delete(user);

      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 유저-그룹 관계 없음")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);
      th.delete(userGroup);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 부적절한 그룹 역할")
    void Failure_ImproperGroupRole() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER); // Not MANAGER

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 그룹 없음")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      long deletedGroupId = group.getId();
      th.delete(todo);
      th.delete(userGroup);
      th.delete(group);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path =
          String.format("/api/v1/group/%d/todo/%d/metadata", deletedGroupId, todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - Todo 없음")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);
      th.delete(todo);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("isImportant", true);
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    @DisplayName("실패 - 할당된 유저가 그룹에 없음")
    void Failure_AssigneeNotInGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username", "hashedPassword", "nickname");
      User assignee = entityFactory.insertUser("assignee", "hashedPassword", "assigneeNickname");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      // assignee는 그룹에 추가하지 않음

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.TO_DO,
              false);

      Cookie cookie = userCookie(user);
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("assigneeId", assignee.getId());
      String body = objectMapper.writeValueAsString(requestBody);
      String path = String.format("/api/v1/group/%d/todo/%d/metadata", group.getId(), todo.getId());

      // When
      ResultActions result =
          mvc.perform(
              patch(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }
  }

  @Nested
  @DisplayName("투두 순서 및 상태 변경 테스트")
  class MoveTodo {
    @Test
    @DisplayName("성공 - 상태만 변경 (맨 뒤로 이동)")
    void HappyPath_1() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);

      entityFactory.insertTodo(
          user.getId(),
          user.getId(),
          group.getId(),
          "todo2",
          "desc2",
          TodoStatus.TO_DO,
          "b",
          false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo1.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.id", equalTo(todo1.getId().intValue())));
      result.andExpect(jsonPath("$.status", equalTo("IN_PROGRESS")));
      result.andExpect(jsonPath("$.order", notNullValue()));
    }

    @Test
    @DisplayName("성공 - 상태 및 순서 변경")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo2",
              "desc2",
              TodoStatus.IN_PROGRESS,
              "b",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo1.getId());
      MoveTodoRequestDTO moveTodoRequestDTO =
          new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, todo2.getId());
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
      result.andExpect(jsonPath("$.id", equalTo(todo1.getId().intValue())));
      result.andExpect(jsonPath("$.status", equalTo("IN_PROGRESS")));
      result.andExpect(jsonPath("$.order", lessThan(todo2.getOrder())));
    }

    @Test
    @DisplayName("성공 - 순서를 여러회 변경 후 정렬 순서")
    void HappyPath_3() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo2",
              "desc2",
              TodoStatus.TO_DO,
              "b",
              false);
      Todo todo3 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo3",
              "desc3",
              TodoStatus.TO_DO,
              "c",
              false);

      // todo1 todo2 todo3 순
      Cookie cookie = userCookie(user);

      // todo2를 todo1 앞으로 -> todo2 todo1 todo3 순
      mvc.perform(
          put(String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo2.getId()))
              .cookie(cookie)
              .accept(MediaType.APPLICATION_JSON)
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  objectMapper.writeValueAsString(
                      new MoveTodoRequestDTO(TodoStatus.TO_DO, todo1.getId()))));
      // todo3을 todo1 앞으로 -> todo2 todo3 todo1순
      mvc.perform(
          put(String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo3.getId()))
              .cookie(cookie)
              .accept(MediaType.APPLICATION_JSON)
              .contentType(MediaType.APPLICATION_JSON)
              .content(
                  objectMapper.writeValueAsString(
                      new MoveTodoRequestDTO(TodoStatus.TO_DO, todo1.getId()))));
      // When
      ResultActions result =
          mvc.perform(
              get(String.format("/api/v1/group/%d/todo", group.getId()))
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON));

      // Then
      result.andExpect(status().isOk());

      List<Long> actualIds =
          objectMapper
              .readValue(
                  result.andReturn().getResponse().getContentAsString(),
                  new TypeReference<List<TodoDTO>>() {})
              .stream()
              .sorted(Comparator.comparing(TodoDTO::order))
              .map(TodoDTO::id)
              .collect(Collectors.toList());

      Assertions.assertIterableEquals(
          List.of(todo2.getId(), todo3.getId(), todo1.getId()), actualIds);
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 상태")
    void Failure_Token() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);

      entityFactory.insertTodo(
          user.getId(),
          user.getId(),
          group.getId(),
          "todo2",
          "desc2",
          TodoStatus.TO_DO,
          "b",
          false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo1.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);

      // When
      ResultActions result =
          mvc.perform(
              put(path)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    void Failure_NoUser() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      Cookie cookie = userCookie(user);
      th.delete(userGroup);
      th.delete(todo);
      th.delete(user);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);

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
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 권한 부족 - VIEWER")
    void Failure_NoAuthority() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.VIEWER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group name", "group description");
      Group group2 = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group1.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(group2);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group2.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
    @DisplayName("실패 - 없는 Todo를 이동하기")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "todo title",
              "todo description",
              TodoStatus.ON_HOLD,
              false);
      th.delete(todo);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

      // When
      ResultActions result =
          mvc.perform(
              put(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo를 옮기기")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path =
          String.format("/api/v1/group/%d/todo/%d/move", group1.getId(), todoInGroup2.getId());
      MoveTodoRequestDTO moveTodoRequestDTO = new MoveTodoRequestDTO(TodoStatus.IN_PROGRESS, null);
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

      // When
      ResultActions result =
          mvc.perform(
              put(path)
                  .cookie(cookie)
                  .accept(MediaType.APPLICATION_JSON)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    @DisplayName("실패 - 대상과 destination이 일치하는 경우")
    void Failure_InvalidDestination_1() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo.getId());
      MoveTodoRequestDTO moveTodoRequestDTO =
          new MoveTodoRequestDTO(TodoStatus.TO_DO, todo.getId());
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
      result.andExpect(
          jsonPath(
              "$.message",
              equalTo(
                  "Destination must be another todo in the same group with the same todo status.")));
    }

    @Test
    @DisplayName("실패 - destination이 다른 그룹에 속한 경우")
    void Failure_InvalidDestination_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group1.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo2",
              "desc2",
              TodoStatus.TO_DO,
              "b",
              false);

      String path =
          String.format("/api/v1/group/%d/todo/%d/move", group1.getId(), todoInGroup1.getId());
      MoveTodoRequestDTO moveTodoRequestDTO =
          new MoveTodoRequestDTO(TodoStatus.TO_DO, todoInGroup2.getId());
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
      result.andExpect(
          jsonPath(
              "$.message",
              equalTo(
                  "Destination must be another todo in the same group with the same todo status.")));
    }

    @Test
    @DisplayName("실패 - destination이 다른 todoStatus에 속한 경우")
    void Failure_InvalidDestination_3() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo1 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo1",
              "desc1",
              TodoStatus.TO_DO,
              "a",
              false);
      Todo todo2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo2",
              "desc2",
              TodoStatus.IN_PROGRESS,
              "b",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/move", group.getId(), todo1.getId());
      MoveTodoRequestDTO moveTodoRequestDTO =
          new MoveTodoRequestDTO(TodoStatus.TO_DO, todo2.getId());
      String body = objectMapper.writeValueAsString(moveTodoRequestDTO);
      Cookie cookie = userCookie(user);

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
      result.andExpect(
          jsonPath(
              "$.message",
              equalTo(
                  "Destination must be another todo in the same group with the same todo status.")));
    }
  }

  @Nested
  @DisplayName("투두 Star 추가 테스트")
  class StarTodo {
    @ParameterizedTest
    @EnumSource(GroupRole.class)
    @DisplayName("성공 - 서로 다른 role에 대해 성공")
    void HappyPath_1(GroupRole role) throws Exception {
      // Given
      User user =
          entityFactory.insertUser("username" + role.toString(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("성공 - 이미 star 표기된 케이스")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isCreated());
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 경우")
    void Failure_NoToken() throws Exception {
      // Given
      String path = String.format("/api/v1/group/%d/todo/%d/star", 1L, 1L);

      // When
      ResultActions result = mvc.perform(post(path));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 유저가 없는 경우")
    void Failure_NoUser() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      Cookie cookie = userCookie(user);
      th.delete(userGroup);
      th.delete(todo);
      th.delete(user);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 그룹이 없는 경우")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      Group wrongGroup = entityFactory.insertGroup("group name", "group description");
      th.delete(wrongGroup);

      String path =
          String.format("/api/v1/group/%d/todo/%d/star", wrongGroup.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 없는 Todo")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo worngTodo =
          entityFactory.insertTodo(
              user.getId(),
              null,
              group.getId(),
              "worngTodo title",
              "todo description",
              TodoStatus.IN_PROGRESS,
              false);
      th.delete(worngTodo);

      String path =
          String.format("/api/v1/group/%d/todo/%d/star", group.getId(), worngTodo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path =
          String.format("/api/v1/group/%d/todo/%d/star", group1.getId(), todoInGroup2.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(post(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }
  }

  @Nested
  @DisplayName("투두 Star 제거 테스트")
  class UnStarTodo {
    @ParameterizedTest
    @EnumSource(GroupRole.class)
    @DisplayName("성공 - 서로 다른 role에 대해 성공")
    void HappyPath_1(GroupRole role) throws Exception {
      // Given
      User user =
          entityFactory.insertUser("username" + role.toString(), "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), role);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("성공 - star가 없는 케이스")
    void HappyPath_2() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패 - 로그인 하지 않은 경우")
    void Failure_NoToken() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);
      entityFactory.insertUserTodoStar(user.getId(), todo.getId());

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());

      // When
      ResultActions result = mvc.perform(delete(path));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 유저가 없는 경우")
    void Failure_NoUser() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      Cookie cookie = userCookie(user);

      th.delete(todo);
      th.delete(userGroup);
      th.delete(user);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("허가되지 않은 접근입니다.")));
    }

    @Test
    @DisplayName("실패 - 그룹이 없는 경우")
    void Failure_NoGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      long wrongGroupId = group.getId() + 1;
      String path = String.format("/api/v1/group/%d/todo/%d/star", wrongGroupId, todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 가입하지 않은 그룹")
    void Failure_NoUserGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      Todo todo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path = String.format("/api/v1/group/%d/todo/%d/star", group.getId(), todo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isForbidden());
      result.andExpect(jsonPath("$.message", equalTo("No permission to perform this action.")));
    }

    @Test
    @DisplayName("실패 - 없는 Todo")
    void Failure_NoTodo() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group = entityFactory.insertGroup("group name", "group description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      Todo wrongTodo =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      th.delete(wrongTodo);
      String path =
          String.format("/api/v1/group/%d/todo/%d/star", group.getId(), wrongTodo.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }

    @Test
    @DisplayName("실패 - 다른 그룹의 Todo")
    void Failure_TodoInAnotherGroup() throws Exception {
      // Given
      User user = entityFactory.insertUser("username123", "hashedPassword", "nickname");
      Group group1 = entityFactory.insertGroup("group1", "desc1");
      Group group2 = entityFactory.insertGroup("group2", "desc2");
      entityFactory.insertUserGroup(user.getId(), group1.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(user.getId(), group2.getId(), GroupRole.MEMBER);
      Todo todoInGroup2 =
          entityFactory.insertTodo(
              user.getId(),
              user.getId(),
              group2.getId(),
              "todo",
              "desc",
              TodoStatus.TO_DO,
              "a",
              false);

      String path =
          String.format("/api/v1/group/%d/todo/%d/star", group1.getId(), todoInGroup2.getId());
      Cookie cookie = userCookie(user);

      // When
      ResultActions result = mvc.perform(delete(path).cookie(cookie));

      // Then
      result.andExpect(status().isNotFound());
      result.andExpect(jsonPath("$.message", equalTo("Resource Not Found.")));
    }
  }
}
