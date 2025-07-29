package rest.felix.back.group.service;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.exception.throwable.notFound.ResourceNotFoundException;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.dto.*;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.exception.GroupNotFoundException;
import rest.felix.back.group.repository.GroupRepository;
import rest.felix.back.group.repository.UserGroupRepository;
import rest.felix.back.todo.entity.enumerated.TodoStatus;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UserAccessDeniedException;

@SpringBootTest
@ActiveProfiles("test")
class GroupServiceTest {

  @Autowired private GroupService groupService;
  @Autowired private GroupRepository groupRepository;
  @Autowired private UserGroupRepository userGroupRepository;
  @Autowired private EntityFactory entityFactory;
  @Autowired private TestHelper th;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Nested
  @DisplayName("그룹 생성 테스트")
  class CreateGroup {
    @Test
    void HappyPath() {
      // Given

      User user = entityFactory.insertUser("username", "some password", "nickname");

      CreateGroupDTO createGroupDTO =
          new CreateGroupDTO(user.getId(), "groupName", "group description");

      // When

      GroupDTO groupDTO = groupService.createGroup(createGroupDTO);

      // Then

      Assertions.assertEquals("groupName", groupDTO.name());

      GroupDTO createdGroup = groupRepository.findById(groupDTO.id()).get();

      Assertions.assertEquals(createdGroup.id(), groupDTO.id());
      Assertions.assertEquals("group description", createdGroup.description());
    }

    @Test
    void Failure_NoSuchUser() {
      // Given

      User user = entityFactory.insertUser("username", "some password", "nickname");

      th.delete(user);

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

      // When

      List<GroupDTO> user1GroupDTOs = groupService.findGroupsByUserId(user1.getId());
      List<GroupDTO> user2GroupDTOs = groupService.findGroupsByUserId(user2.getId());

      // Then

      Assertions.assertEquals(3, user1GroupDTOs.size());
      Assertions.assertEquals(3, user2GroupDTOs.size());

      Assertions.assertEquals(
          true,
          user1GroupDTOs.stream()
              .map(GroupDTO::name)
              .toList()
              .containsAll(List.of("user1 group1", "user1 group2", "user1 group3")));

      Assertions.assertEquals(
          true,
          user1GroupDTOs.stream()
              .map(GroupDTO::description)
              .toList()
              .containsAll(
                  List.of(
                      "user1 group1 description",
                      "user1 group2 description",
                      "user1 group3 description")));

      Assertions.assertEquals(
          true,
          user2GroupDTOs.stream()
              .map(GroupDTO::name)
              .toList()
              .containsAll(List.of("user2 group1", "user2 group2", "user2 group3")));

      Assertions.assertEquals(
          true,
          user2GroupDTOs.stream()
              .map(GroupDTO::description)
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

      th.delete(user);

      // When

      List<GroupDTO> userGroupDTOs = groupService.findGroupsByUserId(user.getId());

      // Then

      Assertions.assertEquals(0, userGroupDTOs.size());
    }

    @Test
    void HappyPath_NoGroup() {

      // Given

      User user = entityFactory.insertUser("usernaem1", "some password", "nickname1");

      // When

      List<GroupDTO> userGroupDTOs = groupService.findGroupsByUserId(user.getId());

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

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(2, detailedGroups.size());

      List<DetailedGroupDTO> sortedGroups = new java.util.ArrayList<>(detailedGroups);
      sortedGroups.sort((a, b) -> a.name().compareTo(b.name()));

      DetailedGroupDTO group1DTO = sortedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(2, group1DTO.todoCount());
      Assertions.assertEquals(1, group1DTO.completedTodoCount());
      Assertions.assertEquals(2, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick"),
          group1DTO.members().stream().map(MemberDTO::nickname).sorted().toList());

      DetailedGroupDTO group2DTO = detailedGroups.get(1);
      Assertions.assertEquals(group2.getId(), group2DTO.id());
      Assertions.assertEquals("Group 2", group2DTO.name());
      Assertions.assertEquals("Description 2", group2DTO.description());
      Assertions.assertEquals(3, group2DTO.todoCount());
      Assertions.assertEquals(2, group2DTO.completedTodoCount());
      Assertions.assertEquals(3, group2DTO.memberCount());
      Assertions.assertEquals(GroupRole.MEMBER, group2DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick", "otherUser2Nick"),
          group2DTO.members().stream().map(MemberDTO::nickname).sorted().toList());
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

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(1, detailedGroups.size());

      DetailedGroupDTO group1DTO = detailedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(1, group1DTO.todoCount());
      Assertions.assertEquals(0, group1DTO.completedTodoCount());
      Assertions.assertEquals(2, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      Assertions.assertEquals(
          List.of("mainUserNick", "otherUser1Nick"),
          group1DTO.members().stream().map(MemberDTO::nickname).sorted().toList());
    }

    @Test
    @DisplayName("Happy Path - No todos, no other members")
    void HappyPath_3() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      Group group1 = entityFactory.insertGroup("Group 1", "Description 1");
      entityFactory.insertUserGroup(mainUser.getId(), group1.getId(), GroupRole.OWNER);

      // When
      List<DetailedGroupDTO> detailedGroups =
          groupService.findDetailedGroupsByUserId(mainUser.getId());

      // Then
      Assertions.assertEquals(1, detailedGroups.size());

      DetailedGroupDTO group1DTO = detailedGroups.get(0);
      Assertions.assertEquals(group1.getId(), group1DTO.id());
      Assertions.assertEquals("Group 1", group1DTO.name());
      Assertions.assertEquals("Description 1", group1DTO.description());
      Assertions.assertEquals(0, group1DTO.todoCount());
      Assertions.assertEquals(0, group1DTO.completedTodoCount());
      Assertions.assertEquals(1, group1DTO.memberCount());
      Assertions.assertEquals(GroupRole.OWNER, group1DTO.myRole());

      List<String> group1MemberNicknames =
          group1DTO.members().stream().map(MemberDTO::nickname).sorted().toList();
      Assertions.assertEquals(List.of("mainUserNick"), group1MemberNicknames);
    }

    @Test
    @DisplayName("Happy Path - No such user")
    void HappyPath_NoUser() {
      // Given
      User mainUser = entityFactory.insertUser("mainUser", "password", "mainUserNick");
      long userId = mainUser.getId();
      th.delete(mainUser);

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

      for (GroupRole groupRole :
          new GroupRole[] {
            GroupRole.VIEWER, GroupRole.MEMBER, GroupRole.MANAGER, GroupRole.OWNER
          }) {

        UserGroup userGroup = entityFactory.insertUserGroup(user.getId(), group.getId(), groupRole);

        // When

        Assertions.assertDoesNotThrow(
            () -> {
              GroupRole foundGroupRole =
                  groupService
                      .findUserRole(user.getId(), group.getId())
                      .orElseThrow(ResourceNotFoundException::new);

              // Then

              Assertions.assertEquals(groupRole, foundGroupRole);

              th.delete(userGroup);
            });
      }
    }

    @Test
    void Empty_NotInGroup() {

      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      // When

      Optional<GroupRole> userGroup = groupService.findUserRole(user.getId(), group.getId());

      // Then

      Assertions.assertEquals(true, userGroup.isEmpty());
    }

    @Test
    void Empty_NoUser() {
      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      th.delete(user);

      // When

      Optional<GroupRole> userGroup = groupService.findUserRole(user.getId(), group.getId());

      // Then

      Assertions.assertEquals(true, userGroup.isEmpty());
    }

    @Test
    void Empty_NoGroup() {
      // Given

      User user = entityFactory.insertUser("username1", "some password", "nickname1");
      Group group = entityFactory.insertGroup("group name", "group description");

      th.delete(group);

      // When

      Optional<GroupRole> userGroup = groupService.findUserRole(user.getId(), group.getId());

      // Then

      Assertions.assertEquals(true, userGroup.isEmpty());
    }
  }

  @Nested
  @DisplayName("단일 그룹 조회")
  class GetGroupById {
    @Test
    void HappyPath() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      // When

      Assertions.assertDoesNotThrow(
          () -> {
            GroupDTO groupDTO =
                groupService.findById(group.getId()).orElseThrow(ResourceNotFoundException::new);

            // Then

            Assertions.assertNotNull(groupDTO.id());
            Assertions.assertEquals("group name", groupDTO.name());
            Assertions.assertEquals("group description", groupDTO.description());
          });
    }

    @Test
    void Empty_NoGroup() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      th.delete(group);

      // When

      Optional<GroupDTO> groupDto = groupService.findById(group.getId());

      // Then

      Assertions.assertEquals(true, groupDto.isEmpty());
    }
  }

  @Nested
  @DisplayName("그룹 삭제")
  class DeleteGroupById {

    @Test
    void HappyPath() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      // When

      groupService.deleteGroupById(group.getId());

      // Then

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isEmpty());
    }

    @Test
    void HappyPath_NotCheckGroupExistence() {
      // Given

      Group group = entityFactory.insertGroup("group name", "group description");

      th.delete(group);

      // When

      groupService.deleteGroupById(group.getId());

      // Then

      Assertions.assertEquals(true, groupRepository.findById(group.getId()).isEmpty());
    }
  }

  @Nested
  @DisplayName("그룹 권한 확인 테스트")
  class AssertGroupAuthority {

    private static Stream<Arguments> sufficientAuthorityTestCases() {
      return Stream.of(
          Arguments.of(GroupRole.OWNER, GroupRole.OWNER),
          Arguments.of(GroupRole.OWNER, GroupRole.MANAGER),
          Arguments.of(GroupRole.OWNER, GroupRole.MEMBER),
          Arguments.of(GroupRole.OWNER, GroupRole.VIEWER),
          Arguments.of(GroupRole.MANAGER, GroupRole.MANAGER),
          Arguments.of(GroupRole.MANAGER, GroupRole.MEMBER),
          Arguments.of(GroupRole.MANAGER, GroupRole.VIEWER),
          Arguments.of(GroupRole.MEMBER, GroupRole.MEMBER),
          Arguments.of(GroupRole.MEMBER, GroupRole.VIEWER),
          Arguments.of(GroupRole.VIEWER, GroupRole.VIEWER));
    }

    private static Stream<Arguments> insufficientAuthorityTestCases() {
      return Stream.of(
          Arguments.of(GroupRole.MANAGER, GroupRole.OWNER),
          Arguments.of(GroupRole.MEMBER, GroupRole.OWNER),
          Arguments.of(GroupRole.MEMBER, GroupRole.MANAGER),
          Arguments.of(GroupRole.VIEWER, GroupRole.OWNER),
          Arguments.of(GroupRole.VIEWER, GroupRole.MANAGER),
          Arguments.of(GroupRole.VIEWER, GroupRole.MEMBER));
    }

    @ParameterizedTest(name = "성공 - 유저 역할: {0}, 요구 역할: {1}")
    @MethodSource("sufficientAuthorityTestCases")
    void HappyPath_SufficientAuthority(GroupRole userRole, GroupRole requiredRole) {
      // Given
      User user = entityFactory.insertUser("user" + userRole.name(), "password", "userNick");
      Group group = entityFactory.insertGroup("group" + userRole.name(), "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);

      // When & Then
      Assertions.assertDoesNotThrow(
          () -> groupService.assertGroupAuthority(user.getId(), group.getId(), requiredRole));
    }

    @ParameterizedTest(name = "실패 - 유저 역할: {0}, 요구 역할: {1}")
    @MethodSource("insufficientAuthorityTestCases")
    void Failure_InsufficientAuthority(GroupRole userRole, GroupRole requiredRole) {
      // Given
      User user = entityFactory.insertUser("user" + userRole.name(), "password", "userNick");
      Group group = entityFactory.insertGroup("group" + userRole.name(), "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), userRole);

      // When
      Runnable lambda =
          () -> groupService.assertGroupAuthority(user.getId(), group.getId(), requiredRole);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    void Failure_NoSuchUser() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      th.delete(userGroup);
      th.delete(user);

      // When
      Runnable lambda =
          () -> groupService.assertGroupAuthority(user.getId(), group.getId(), GroupRole.OWNER);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoSuchGroup() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);
      th.delete(userGroup);
      th.delete(group);

      // When
      Runnable lambda =
          () -> groupService.assertGroupAuthority(user.getId(), group.getId(), GroupRole.OWNER);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 유저가 그룹에 속해있지 않음")
    void Failure_UserNotInGroup() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      // User is not associated with the group

      // When
      Runnable lambda =
          () -> groupService.assertGroupAuthority(user.getId(), group.getId(), GroupRole.OWNER);

      // Then
      Assertions.assertThrows(UserAccessDeniedException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 초대 정보 조회 테스트")
  class FindGroupInvitationInfo {

    @Test
    @DisplayName("성공")
    void HappyPath() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      User member1 = entityFactory.insertUser("member1", "password", "memberNick1");
      User member2 = entityFactory.insertUser("member2", "password", "memberNick2");
      Group group = entityFactory.insertGroup("group name", "group description");

      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(member1.getId(), group.getId(), GroupRole.MEMBER);
      entityFactory.insertUserGroup(member2.getId(), group.getId(), GroupRole.VIEWER);

      entityFactory.insertTodo(
          issuer.getId(),
          issuer.getId(),
          group.getId(),
          "Todo 1",
          "Desc 1",
          TodoStatus.IN_PROGRESS,
          "a",
          false);
      entityFactory.insertTodo(
          member1.getId(),
          member1.getId(),
          group.getId(),
          "Todo 2",
          "Desc 2",
          TodoStatus.DONE,
          "b",
          false);

      String token = "testToken";
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      GroupInvitationDTO groupInvitationDTO =
          new GroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      GroupInvitationInfoDTO info = groupService.findGroupInvitationInfo(groupInvitationDTO);

      // Then
      Assertions.assertNotNull(info);
      Assertions.assertEquals(group.getName(), info.name());
      Assertions.assertEquals(group.getDescription(), info.description());
      Assertions.assertEquals(2, info.todoCount());
      Assertions.assertEquals(1, info.completedTodoCount());
      Assertions.assertEquals(3, info.memberCount());
      Assertions.assertEquals(issuer.getId(), info.issuer().id());
      Assertions.assertEquals(issuer.getNickname(), info.issuer().nickname());
      Assertions.assertEquals(expiresAt.toEpochSecond(), info.expiresAt().toEpochSecond());

      // Verify members
      Assertions.assertEquals(3, info.members().size());
      Assertions.assertEquals(
          true, info.members().stream().anyMatch(m -> m.id() == issuer.getId()));
      Assertions.assertEquals(
          true, info.members().stream().anyMatch(m -> m.id() == member1.getId()));
      Assertions.assertEquals(
          true, info.members().stream().anyMatch(m -> m.id() == member2.getId()));
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoSuchGroup() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group name", "group description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      th.delete(userGroup);
      th.delete(group);

      String token = "testToken";
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      GroupInvitationDTO groupInvitationDTO =
          new GroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      Runnable lambda = () -> groupService.findGroupInvitationInfo(groupInvitationDTO);

      // Then
      Assertions.assertThrows(GroupNotFoundException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 발행자")
    void Failure_NoSuchIssuer() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      User member1 = entityFactory.insertUser("member1", "password", "memberNick1");
      Group group = entityFactory.insertGroup("group name", "group description");

      UserGroup userGroup =
          entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      entityFactory.insertUserGroup(member1.getId(), group.getId(), GroupRole.MEMBER);

      th.delete(userGroup);
      th.delete(issuer);

      String token = "testToken";
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      GroupInvitationDTO groupInvitationDTO =
          new GroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      Runnable lambda = () -> groupService.findGroupInvitationInfo(groupInvitationDTO);

      // Then
      Assertions.assertThrows(ResourceNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("유저 그룹 등록 테스트")
  class RegisterUserToGroup {

    @Test
    @DisplayName("성공")
    void HappyPath() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");

      // When
      groupService.registerUserToGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      // Then
      UserGroupDTO userGroup =
          userGroupRepository.findByUserIdAndGroupId(user.getId(), group.getId()).get();

      Assertions.assertEquals(GroupRole.MEMBER, userGroup.groupRole());
    }

    @Test
    @DisplayName("실패 - 없는 유저")
    void Failure_NoSuchUser() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      th.delete(user);

      // When
      Runnable lambda =
          () -> groupService.registerUserToGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoSuchGroup() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      th.delete(group);

      // When
      Runnable lambda =
          () -> groupService.registerUserToGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 이미 그룹 멤버")
    void Failure_AlreadyGroupMember() {
      // Given
      User user = entityFactory.insertUser("user", "password", "userNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.OWNER);

      // When
      Runnable lambda =
          () -> groupService.registerUserToGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 정보 수정 테스트")
  class UpdateGroupTest {

    @Test
    @DisplayName("성공")
    void happyPath() {
      // Given
      Group group = entityFactory.insertGroup("Original Name", "Original Desc");
      UpdateGroupDTO updateGroupDTO =
          new UpdateGroupDTO(group.getId(), "Updated Name", "Updated Desc");

      // When
      groupService.updateGroup(updateGroupDTO);

      // Then
      GroupDTO updatedGroup = groupRepository.findById(group.getId()).orElseThrow();
      Assertions.assertEquals("Updated Name", updatedGroup.name());
      Assertions.assertEquals("Updated Desc", updatedGroup.description());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 그룹")
    void failure_groupNotFound() {
      // Given
      Group group = entityFactory.insertGroup("group", "desc");
      long groupId = group.getId();
      th.delete(group);
      UpdateGroupDTO updateGroupDTO =
          new UpdateGroupDTO(group.getId(), "Updated Name", "Updated Desc");

      // When
      Runnable lambda = () -> groupService.updateGroup(updateGroupDTO);

      // Then
      Assertions.assertThrows(GroupNotFoundException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 멤버 추방 테스트")
  class DeleteUserGroupTest {

    @Test
    @DisplayName("성공")
    void happyPath() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);

      // When
      groupService.deleteUserGroupById(user.getId(), group.getId());

      // Then
      Assertions.assertEquals(
          true, userGroupRepository.findByUserIdAndGroupId(user.getId(), group.getId()).isEmpty());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버십")
    void failure_membershipNotFound() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      // User is not in the group

      // When
      Runnable lambda = () -> groupService.deleteUserGroupById(user.getId(), group.getId());

      // Then
      Assertions.assertDoesNotThrow(lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 멤버 역할 변경 테스트")
  class UpdateUserGroupTest {

    @Test
    @DisplayName("성공")
    void happyPath() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      entityFactory.insertUserGroup(user.getId(), group.getId(), GroupRole.MEMBER);
      UpdateMemberDTO updateMemberDTO =
          new UpdateMemberDTO(user.getId(), group.getId(), GroupRole.MANAGER);

      // When
      groupService.updateUserGroup(updateMemberDTO);

      // Then
      UserGroupDTO updatedUserGroup =
          userGroupRepository.findByUserIdAndGroupId(user.getId(), group.getId()).orElseThrow();
      Assertions.assertEquals(GroupRole.MANAGER, updatedUserGroup.groupRole());
    }

    @Test
    @DisplayName("실패 - 존재하지 않는 멤버십")
    void failure_membershipNotFound() {
      // Given
      User user = entityFactory.insertUser("user", "pass", "nick");
      Group group = entityFactory.insertGroup("group", "desc");
      UpdateMemberDTO updateMemberDTO =
          new UpdateMemberDTO(user.getId(), group.getId(), GroupRole.MANAGER);
      // User is not in the group

      // When
      Runnable lambda = () -> groupService.updateUserGroup(updateMemberDTO);

      // Then
      Assertions.assertDoesNotThrow(lambda::run);
    }
  }
}
