package rest.felix.back.group.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import rest.felix.back.common.exception.throwable.notfound.ResourceNotFoundException;
import rest.felix.back.group.dto.CreateGroupDTO;
import rest.felix.back.group.dto.GroupDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UserAccessDeniedException;

@Transactional
@SpringBootTest
class GroupServiceTest {

  @Autowired private EntityManager em;
  @Autowired private GroupService groupService;

  @Test
  void createGroup_HappyPath() {
    // Given

    User user = new User();
    user.setNickname("nickname");
    user.setUsername("username");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    CreateGroupDTO createGroupDTO =
        new CreateGroupDTO(user.getId(), "groupName", "group description");

    em.flush();

    // When

    GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

    // Then

    Assertions.assertEquals("groupName", groupDTO.getName());

    Group createdGroup =
        em.createQuery("SELECT g FROM Group g WHERE g.name = :groupName", Group.class)
            .setParameter("groupName", "groupName")
            .getSingleResult();

    Assertions.assertEquals(createdGroup.getId(), groupDTO.getId());
    Assertions.assertEquals("group description", createdGroup.getDescription());
  }

  @Test
  void createGroup_Failure_NoSuchUser() {
    // Given

    User user = new User();
    user.setNickname("nickname");
    user.setUsername("username");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    CreateGroupDTO createGroupDTO =
        new CreateGroupDTO(user.getId(), "groupName", "group description");

    em.remove(user);
    em.flush();

    // When

    Runnable lambda = () -> groupService.createGroup(createGroupDTO);

    // Then

    Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);

    // DataIntegrityViolationException
  }

  @Test
  void getGroupsByUserId_HappyPath() {

    // Given

    User user1 = new User();
    user1.setNickname("nickname1");
    user1.setUsername("username1");
    user1.setHashedPassword("hashedPassword");

    User user2 = new User();
    user2.setNickname("nickname2");
    user2.setUsername("username2");
    user2.setHashedPassword("hashedPassword");

    em.persist(user1);
    em.persist(user2);

    Arrays.stream(new int[] {1, 2, 3})
        .forEach(
            idx -> {
              groupService.createGroup(
                  new CreateGroupDTO(
                      user1.getId(),
                      String.format("user1 group%d", idx),
                      String.format("user1 group%d description", idx)));
              groupService.createGroup(
                  new CreateGroupDTO(
                      user2.getId(),
                      String.format("user2 group%d", idx),
                      String.format("user2 group%d description", idx)));
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
  void getGroupsByUserId_HappyPath_NoUser() {

    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);
    em.remove(user);
    em.flush();

    // When

    List<GroupDTO> userGroupDTOs = groupService.getGroupsByUserId(user.getId());

    // Then

    Assertions.assertEquals(0, userGroupDTOs.size());
  }

  @Test
  void getGroupsByUserId_HappyPath_NoGroup() {

    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    em.flush();

    // When

    List<GroupDTO> userGroupDTOs = groupService.getGroupsByUserId(user.getId());

    // Then

    Assertions.assertEquals(0, userGroupDTOs.size());
  }

  // GroupRole getUserRoleInGroup(long userId, long groupId)

  @Test
  void getUserRoleInGroup_HappyPath() {

    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("test group");

    em.persist(group);

    for (GroupRole groupRole :
        new GroupRole[] {GroupRole.VIEWER, GroupRole.MEMBER, GroupRole.MANAGER, GroupRole.OWNER}) {

      UserGroup userGroup = new UserGroup();
      userGroup.setGroupRole(groupRole);
      userGroup.setUser(user);
      userGroup.setGroup(group);

      em.persist(userGroup);

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
  void getUserRoleInGroup_Failure_NotInGroup() {

    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("test group");

    em.persist(group);

    em.flush();

    // When

    Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void getUserRoleInGroup_Failure_NoUser() {
    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("test group");

    em.persist(group);

    em.flush();

    em.remove(user);
    em.flush();

    // When

    Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void getUserRoleInGroup_Failure_NoGroup() {
    // Given

    User user = new User();
    user.setNickname("nickname1");
    user.setUsername("username1");
    user.setHashedPassword("hashedPassword");

    em.persist(user);

    Group group = new Group();
    group.setName("group");
    group.setDescription("test group");

    em.persist(group);

    em.flush();

    em.remove(group);
    em.flush();

    // When

    Runnable lambda = () -> groupService.getUserRoleInGroup(user.getId(), group.getId());

    // Then

    Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
  }

  @Test
  void getGroupById_HappyPath() {
    // Given

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);
    em.flush();

    // When

    GroupDTO groupDTO = groupService.getGroupById(group.getId());

    // Then

    Assertions.assertNotNull(groupDTO.getId());
    Assertions.assertEquals("group name", groupDTO.getName());
    Assertions.assertEquals("group description", groupDTO.getDescription());
  }

  @Test
  void getGroupById_Failure_NoGroup() {
    // Given

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);
    em.flush();

    em.remove(group);
    em.flush();

    // When

    Runnable lambda = () -> groupService.getGroupById(group.getId());

    // Then

    Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
  }

  @Test
  void deleteGroupById_HappyPath() {
    // Given

    Group group = new Group();
    group.setName("group name");
    group.setDescription("group description");

    em.persist(group);
    em.flush();

    // When

    groupService.deleteGroupById(group.getId());

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
  }
}
