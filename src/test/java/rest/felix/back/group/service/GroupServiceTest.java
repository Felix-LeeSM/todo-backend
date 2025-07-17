package rest.felix.back.group.service;

import jakarta.persistence.EntityManager;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.common.security.PasswordService;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.DetailedGroupDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GroupServiceTest {

  @Autowired private EntityManager em;
  @Autowired private GroupService groupService;
  @Autowired private PasswordService passwordService;

  private EntityFactory entityFactory;

  @BeforeEach
  void setup() {
    entityFactory = new EntityFactory(passwordService, em);
  }

  @Nested
  @DisplayName("그룹 생성 테스트")
  class CreateGroup {
    @Test
    void HappyPath() {
      // Given

      User user = entityFactory.insertUser("username", "some password", "nickname");

      em.flush();

      CreateGroupDTO createGroupDTO =
          new CreateGroupDTO(user.getId(), "groupName", "group description");

      // When

      GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

      // Then

      Assertions.assertEquals("groupName", groupDTO.getName());

      Group createdGroup =
          em.createQuery(
                  """
            SELECT g
            FROM Group g
            WHERE g.name = :groupName
            """,
                  Group.class)
              .setParameter("groupName", "groupName")
              .getSingleResult();

      Assertions.assertEquals(createdGroup.getId(), groupDTO.getId());
      Assertions.assertEquals("group description", createdGroup.getDescription());
    }

    @Test
    void Failure_NoSuchUser() {
      // Given

      User user = entityFactory.insertUser("username", "some password", "nickname");

      em.remove(user);

      em.flush();

      CreateGroupDTO createGroupDTO =
          new CreateGroupDTO(user.getId(), "groupName", "group description");

      // When

      Runnable lambda = () -> groupService.createGroup(createGroupDTO);

      // Then

      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("유저 전체 그룹 조회 테스트")
  class GetGroupsByUserId {
    @Test
    void HappyPath() {

      // Given

      User user1 = entityFactory.insertUser("usernaem1", "some password", "nickname1");
      User user2 = entityFactory.insertUser("usernaem2", "some password", "nickname2");

      Arrays.stream(new int[] {1, 2, 3})
          .forEach(
              idx -> {
                Group group1 =
                    entityFactory.insertGroup(
                        String.format("user1 group%d", idx),
                        String.format("user1 group%d description", idx));
                entityFactory.insertUserGroup(user1.getId(), group1.getId(), GroupRole.OWNER);
                Group group2 =
                    entityFactory.insertGroup(
                        String.format("user2 group%d", idx),
                        String.format("user2 group%d description", idx));
                entityFactory.insertUserGroup(user2.getId(), group2.getId(), GroupRole.OWNER);
              });

      em.flush();

      // When

      List<GroupDTO> user1GroupDTOs = groupService.getGroupsByUserId(user1.getId());
      List<GroupDTO> user2GroupDTOs = groupService.getGroupsByUserId(user2.getId());

      // Then

      Assertions.assertEquals(3, user1GroupDTOs.size());
      Assertions.assertEquals(3, user2GroupDTOs.size());

      Assertions.assertTrue(
          user1GroupDTOs.stream()
              .map(GroupDTO::getName)
              .toList()
              .containsAll(List.of("user1 group1", "user1 group2", "user1 group3")));

      Assertions.assertTrue(
          user1GroupDTOs.stream()
              .map(GroupDTO::getDescription)
              .toList()
              .containsAll(
                  List.of(
                      "user1 group1 description",
                      "user1 group2 description",
                      "user1 group3 description")));

      Assertions.assertTrue(
          user2GroupDTOs.stream()
              .map(GroupDTO::getName)
              .toList()
              .containsAll(List.of("user2 group1", "user2 group2", "user2 group3")));

      Assertions.assertTrue(
          user2GroupDTOs.stream()
              .map(GroupDTO::getDescription)
              .toList()
              .containsAll(
                  List.of(
                      "user2 group1 description",
                      "user2 group2 description",
                      "user2 group3 description")));
    }

    @Test
    void HappyPath_NoUser() {

      // Given

      User user = entityFactory.insertUser("usernaem1", "some password", "nickname1");

      em.remove(user);
      em.flush();

      // When

      List<GroupDTO> userGroupDTOs = groupService.getGroupsByUserId(user.getId());

      // Then

      Assertions.assertEquals(0, userGroupDTOs.size());
    }

    @Test
    void HappyPath_NoGroup() {

      // Given

      User user = entityFactory.insertUser("usernaem1", "some password", "nickname1");

      em.flush();

      // When

      List<GroupDTO> userGroupDTOs = groupService.getGroupsByUserId(user.getId());

      // Then

      Assertions.assertEquals(0, userGroupDTOs.size());
    }
  }

  @Nested
  @DisplayName("유저 전체 그룹 자세히 조회 테스트")
  class FindDetailedGroupsByUserId {

    @Test
    @DisplayName("Happy Path - 2 groups")
    void HappyPath_1() {
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

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(2, detailedGroups.size());

      List<DetailedGroupDTO> sortedGroups = new java.util.ArrayList<>(detailedGroups);
      sortedGroups.sort((a, b) -> a.getName().compareTo(b.getName()));

      DetailedGroupDTO group1DTO = sortedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.getId());
      Assertions.assertEquals("Group 1", group1DTO.getName());
      Assertions.assertEquals("Description 1", group1DTO.getDescription());
      Assertions.assertEquals(2, group1DTO.getTodoCount());
      Assertions.assertEquals(1, group1DTO.getCompletedTodoCount());
      Assertions.assertEquals(2, group1DTO.getMemberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.getMyRole());

      List<String> group1MemberNames =
          group1DTO.getMembers().stream().map(MemberDTO::getUsername).sorted().toList();
      Assertions.assertEquals(List.of("mainUser", "otherUser1"), group1MemberNames);

      DetailedGroupDTO group2DTO = detailedGroups.get(1);
      Assertions.assertEquals(group2.getId(), group2DTO.getId());
      Assertions.assertEquals("Group 2", group2DTO.getName());
      Assertions.assertEquals("Description 2", group2DTO.getDescription());
      Assertions.assertEquals(3, group2DTO.getTodoCount());
      Assertions.assertEquals(2, group2DTO.getCompletedTodoCount());
      Assertions.assertEquals(3, group2DTO.getMemberCount());
      Assertions.assertEquals(GroupRole.MEMBER, group2DTO.getMyRole());

      List<String> group2MemberNames =
          group2DTO.getMembers().stream().map(MemberDTO::getUsername).sorted().toList();
      Assertions.assertEquals(List.of("mainUser", "otherUser1", "otherUser2"), group2MemberNames);
    }

    @Test
    @DisplayName("Happy Path - 1 group")
    void HappyPath_2() {
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

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(1, detailedGroups.size());

      DetailedGroupDTO group1DTO = detailedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.getId());
      Assertions.assertEquals("Group 1", group1DTO.getName());
      Assertions.assertEquals("Description 1", group1DTO.getDescription());
      Assertions.assertEquals(1, group1DTO.getTodoCount());
      Assertions.assertEquals(0, group1DTO.getCompletedTodoCount());
      Assertions.assertEquals(2, group1DTO.getMemberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.getMyRole());

      List<String> group1MemberNames =
          group1DTO.getMembers().stream().map(MemberDTO::getUsername).sorted().toList();
      Assertions.assertEquals(List.of("mainUser", "otherUser1"), group1MemberNames);
    }

    @Test
    @DisplayName("Happy Path - No todos, no other members")
    void HappyPath_3() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      Group group1 = entityFactory.insertGroup("Group 1", "Description 1");
      entityFactory.insertUserGroup(mainUser.getId(), group1.getId(), GroupRole.OWNER);

      em.flush();

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(1, detailedGroups.size());

      DetailedGroupDTO group1DTO = detailedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.getId());
      Assertions.assertEquals("Group 1", group1DTO.getName());
      Assertions.assertEquals("Description 1", group1DTO.getDescription());
      Assertions.assertEquals(0, group1DTO.getTodoCount());
      Assertions.assertEquals(0, group1DTO.getCompletedTodoCount());
      Assertions.assertEquals(1, group1DTO.getMemberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.getMyRole());

      List<String> group1MemberNames =
          group1DTO.getMembers().stream().map(MemberDTO::getUsername).sorted().toList();
      Assertions.assertEquals(List.of("mainUser"), group1MemberNames);
    }

    @Test
    @DisplayName("Happy Path - No such user")
    void HappyPath_NoUser() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      long userId = mainUser.getId();
      em.remove(mainUser);
      em.flush();

      // When
      List<DetailedGroupDTO> detailedGroups = groupService.findDetailedGroupsByUserId(userId);

      // Then
      Assertions.assertEquals(0, detailedGroups.size());
    }

    @Test
    @DisplayName("Happy Path - No groups for user")
    void HappyPath_NoGroup() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      entityFactory.insertGroup("Group 1", "Description 1");
      em.flush();

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(0, detailedGroups.size());
    }
  }

  @Nested
  @DisplayName("그룹 내 역할 조회")
  class GetUserRoleInGroup {

    @Test
    void HappyPath() {

      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      for (GroupRole groupRole :
          new GroupRole[] {
            GroupRole.VIEWER, GroupRole.MEMBER, GroupRole.MANAGER, GroupRole.OWNER
          }) {

        UserGroup userGroup = entityFactory.insertUserGroup(user.getId(), group.getId(), groupRole);

        em.flush();

        // When

        GroupRole foundGroupRole = groupService.getUserRoleInGroup(user.getId(), group.getId());

        // Then

        Assertions.assertEquals(groupRole, foundGroupRole);

        em.remove(userGroup);
        em.flush();
      }
    }

    @Test
    void Failure_NotInGroup() {

      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      // When

      Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    void Failure_NoUser() {
      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      em.remove(user);
      em.flush();

      // When

      Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    void Failure_NoGroup() {
      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      em.remove(group);
      em.flush();

      // When

      Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

      // Then

      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("단일 그룹 조회")
  class GetGroupById {
    @Test
    void HappyPath() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      // When

      GroupDTO groupDTO = groupService.getGroupById(group.getId());

      // Then

      Assertions.assertNotNull(groupDTO.getId());
      Assertions.assertEquals("group name", groupDTO.getName());
      Assertions.assertEquals("group description", groupDTO.getDescription());
    }

    @Test
    void Failure_NoGroup() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      em.remove(group);
      em.flush();

      // When

      Runnable lambda = () -> groupService.getGroupById(group.getId());

      // Then

      Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 삭제")
  class DeleteGroupById {

    @Test
    void HappyPath() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      em.flush();

      // When

      groupService.deleteGroupById(group.getId());

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
    }

    @Test
    void HappyPath_NotCheckGroupExistence() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");
      em.flush();

      em.remove(group);
      em.flush();

      // When

      groupService.deleteGroupById(group.getId());

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
    }
  }
}
