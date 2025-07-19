package rest.felix.back.common.util;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.GroupInvitation;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.entity.Todo;
import rest.felix.back.todo.entity.UserTodoStar;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;

@Component
@Transactional
public class EntityFactory {
  @Autowired private PasswordService passwordService;
  @Autowired private EntityManager entityManager;

  public User insertUser(String username, String password, String nickname) {

    User user = new User();

    user.setHashedPassword(passwordService.hashPassword(password));
    user.setUsername(username);
    user.setNickname(nickname);

    entityManager.persist(user);

    return user;
  }

  public Group insertGroup(String name, String description) {

    Group group = new Group();

    group.setName(name);
    group.setDescription(description);

    entityManager.persist(group);

    return group;
  }

  public Trio<User, Group, UserGroup> insertUserGroup() {
    User user =
        insertUser(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    Group group = insertGroup(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    UserGroup userGroup = new UserGroup();

    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(GroupRole.OWNER);

    entityManager.persist(userGroup);

    return new Trio<>(user, group, userGroup);
  }

  public UserGroup insertUserGroup(Long userId, Long groupId, GroupRole groupRole) {
    User user = entityManager.getReference(User.class, userId);
    Group group = entityManager.getReference(Group.class, groupId);

    UserGroup userGroup = new UserGroup();

    userGroup.setUser(user);
    userGroup.setGroup(group);
    userGroup.setGroupRole(groupRole);

    entityManager.persist(userGroup);

    return userGroup;
  }

  public Todo insertTodo(
      Long authorId,
      Long assigneeId,
      Long groupId,
      String title,
      String description,
      TodoStatus todoStatus,
      String order,
      boolean isImportant) {
    User author = entityManager.getReference(User.class, authorId);
    User assignee = entityManager.getReference(User.class, assigneeId);
    Group group = entityManager.getReference(Group.class, groupId);

    Todo todo = new Todo();

    todo.setAuthor(author);
    todo.setAssignee(assignee);
    todo.setGroup(group);
    todo.setTitle(title);
    todo.setDescription(description);
    todo.setTodoStatus(todoStatus);
    todo.setOrder(order);
    todo.setImportant(isImportant);

    entityManager.persist(todo);

    return todo;
  }

  public Todo insertTodo(
      Long authorId,
      Long assigneeId,
      Long groupId,
      String title,
      String description,
      TodoStatus todoStatus,
      boolean isImportant) {
    User author = entityManager.getReference(User.class, authorId);
    User assignee = assigneeId != null ? entityManager.getReference(User.class, assigneeId) : null;
    Group group = entityManager.getReference(Group.class, groupId);

    Todo todo = new Todo();

    todo.setAuthor(author);
    todo.setAssignee(assignee);
    todo.setGroup(group);
    todo.setTitle(title);
    todo.setDescription(description);
    todo.setTodoStatus(todoStatus);
    todo.setOrder(UUID.randomUUID().toString());
    todo.setImportant(isImportant);

    entityManager.persist(todo);

    return todo;
  }

  public GroupInvitation insertGroupInvitation(Long groupId, Long issuerId, String token) {
    User issuer = entityManager.getReference(User.class, issuerId);
    Group group = entityManager.getReference(Group.class, groupId);

    GroupInvitation groupInvitation = new GroupInvitation();

    groupInvitation.setGroup(group);
    groupInvitation.setIssuer(issuer);
    groupInvitation.setToken(token);

    entityManager.persist(groupInvitation);

    return groupInvitation;
  }

  public UserTodoStar insertUserTodoStar(Long userId, Long todoId) {
    User user = entityManager.getReference(User.class, userId);
    Todo todo = entityManager.getReference(Todo.class, todoId);

    UserTodoStar userTodoStar = new UserTodoStar();

    userTodoStar.setUser(user);
    userTodoStar.setTodo(todo);

    entityManager.persist(userTodoStar);

    return userTodoStar;
  }
}
