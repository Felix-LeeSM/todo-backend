package rest.felix.back.group.service;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import rest.felix.back.common.config.GroupConfig;
import rest.felix.back.common.util.EntityFactory;
import rest.felix.back.common.util.TestHelper;
import rest.felix.back.group.dto.CreateGroupInvitationDTO;
import rest.felix.back.group.dto.GroupInvitationDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.group.exception.ExpiredInvitationException;
import rest.felix.back.group.exception.NoInvitationException;
import rest.felix.back.group.exception.TooManyInvitationsException;
import rest.felix.back.group.repository.GroupInvitationRepository;
import rest.felix.back.user.entity.User;

@SpringBootTest
@ActiveProfiles("test")
class GroupInvitationServiceTest {

  @Autowired private GroupInvitationService groupInvitationService;
  @Autowired private GroupInvitationRepository groupInvitationRepository;
  @Autowired private EntityFactory entityFactory;
  @Autowired private TestHelper th;
  @Autowired private GroupConfig groupConfig;

  @BeforeEach
  void setUp() {
    th.cleanUp();
  }

  @Nested
  @DisplayName("초대 토큰 생성 테스트")
  class CreateInvitationToken {
    @Test
    @DisplayName("성공")
    void HappyPath() {
      // Given

      // When
      String token = groupInvitationService.createInvitationToken();

      // Then
      Assertions.assertNotNull(token);
      Assertions.assertFalse(token.isEmpty());
      // Basic UUID format check (e.g., length, presence of hyphens)
      Assertions.assertTrue(
          token.matches(
              "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
    }
  }

  @Nested
  @DisplayName("초대 횟수 제한 확인 테스트")
  class AssertInvitationCountLimitation {
    @Test
    @DisplayName("성공 - 제한 미만")
    void HappyPath_BelowLimit() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      // Insert invitations below the limit
      for (int i = 0; i < groupConfig.getLimit() - 1; i++) {
        entityFactory.insertGroupInvitation(
            issuer.getId(), group.getId(), "token" + i, ZonedDateTime.now().plusDays(1));
      }

      // When & Then
      Assertions.assertDoesNotThrow(
          () ->
              groupInvitationService.assertInvitationCountLimitation(
                  issuer.getId(), group.getId(), groupConfig.getLimit(), ZonedDateTime.now()));
    }

    @Test
    @DisplayName("실패 - 제한 초과")
    void Failure_AtLimit() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      // Insert invitations up to the limit
      for (int i = 0; i < groupConfig.getLimit(); i++) {
        entityFactory.insertGroupInvitation(
            issuer.getId(), group.getId(), "token" + i, ZonedDateTime.now().plusDays(1));
      }

      // When
      Runnable lambda =
          () ->
              groupInvitationService.assertInvitationCountLimitation(
                  issuer.getId(), group.getId(), groupConfig.getLimit(), ZonedDateTime.now());

      // Then
      Assertions.assertThrows(TooManyInvitationsException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 제한 초과 (이미 초과)")
    void Failure_AboveLimit() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      // Insert invitations above the limit
      for (int i = 0; i < groupConfig.getLimit() + 1; i++) {
        entityFactory.insertGroupInvitation(
            issuer.getId(), group.getId(), "token" + i, ZonedDateTime.now().plusDays(1));
      }

      // When
      Runnable lambda =
          () ->
              groupInvitationService.assertInvitationCountLimitation(
                  issuer.getId(), group.getId(), groupConfig.getLimit(), ZonedDateTime.now());

      // Then
      Assertions.assertThrows(TooManyInvitationsException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("그룹 초대 생성 테스트")
  class CreateGroupInvitationService {
    @Test
    @DisplayName("성공")
    void HappyPath() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = UUID.randomUUID().toString();
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      CreateGroupInvitationDTO createGroupInvitationDTO =
          new CreateGroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      GroupInvitationDTO createdInvitation =
          groupInvitationService.createGroupInvitation(createGroupInvitationDTO);

      // Then
      Assertions.assertNotNull(createdInvitation);
      Assertions.assertEquals(issuer.getId(), createdInvitation.issuerId());
      Assertions.assertEquals(group.getId(), createdInvitation.groupId());
      Assertions.assertEquals(token, createdInvitation.token());
      Assertions.assertEquals(
          expiresAt.toEpochSecond(), createdInvitation.expiresAt().toEpochSecond());

      GroupInvitationDTO foundInvitation = groupInvitationRepository.findByToken(token).get();

      Assertions.assertNotNull(foundInvitation);
      Assertions.assertEquals(issuer.getId(), foundInvitation.issuerId());
      Assertions.assertEquals(group.getId(), foundInvitation.groupId());
      Assertions.assertEquals(token, foundInvitation.token());
      Assertions.assertEquals(
          expiresAt.toEpochSecond(), foundInvitation.expiresAt().toEpochSecond());
    }

    @Test
    @DisplayName("실패 - 없는 발행자")
    void Failure_NoSuchIssuer() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      th.delete(userGroup);
      th.delete(issuer);

      String token = UUID.randomUUID().toString();
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      CreateGroupInvitationDTO createGroupInvitationDTO =
          new CreateGroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      Runnable lambda =
          () -> groupInvitationService.createGroupInvitation(createGroupInvitationDTO);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 없는 그룹")
    void Failure_NoSuchGroup() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      UserGroup userGroup =
          entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);
      th.delete(userGroup);
      th.delete(group);

      String token = UUID.randomUUID().toString();
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      CreateGroupInvitationDTO createGroupInvitationDTO =
          new CreateGroupInvitationDTO(issuer.getId(), group.getId(), token, expiresAt);

      // When
      Runnable lambda =
          () -> groupInvitationService.createGroupInvitation(createGroupInvitationDTO);

      // Then
      Assertions.assertThrows(DataIntegrityViolationException.class, lambda::run);
    }
  }

  @Nested
  @DisplayName("유효한 초대 찾기 테스트")
  class FindValidInvitation {
    @Test
    @DisplayName("성공")
    void HappyPath() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = UUID.randomUUID().toString();
      ZonedDateTime expiresAt = ZonedDateTime.now().plusDays(1);
      entityFactory.insertGroupInvitation(issuer.getId(), group.getId(), token, expiresAt);

      // When
      GroupInvitationDTO foundInvitation =
          groupInvitationService.findValidInvitation(token, ZonedDateTime.now());

      // Then
      Assertions.assertNotNull(foundInvitation);
      Assertions.assertEquals(issuer.getId(), foundInvitation.issuerId());
      Assertions.assertEquals(group.getId(), foundInvitation.groupId());
      Assertions.assertEquals(token, foundInvitation.token());
      Assertions.assertEquals(
          expiresAt.toEpochSecond(), foundInvitation.expiresAt().toEpochSecond());
    }

    @Test
    @DisplayName("실패 - 없는 초대")
    void Failure_NoInvitation() {
      // Given
      String invalidToken = "nonExistentToken";

      // When
      Runnable lambda =
          () -> groupInvitationService.findValidInvitation(invalidToken, ZonedDateTime.now());

      // Then
      Assertions.assertThrows(NoInvitationException.class, lambda::run);
    }

    @Test
    @DisplayName("실패 - 만료된 초대")
    void Failure_ExpiredInvitation() {
      // Given
      User issuer = entityFactory.insertUser("issuer", "password", "issuerNick");
      Group group = entityFactory.insertGroup("group", "description");
      entityFactory.insertUserGroup(issuer.getId(), group.getId(), GroupRole.OWNER);

      String token = UUID.randomUUID().toString();
      ZonedDateTime expiresAt = ZonedDateTime.now().minusDays(1); // Expired
      entityFactory.insertGroupInvitation(issuer.getId(), group.getId(), token, expiresAt);

      // When
      Runnable lambda =
          () -> groupInvitationService.findValidInvitation(token, ZonedDateTime.now());

      // Then
      Assertions.assertThrows(ExpiredInvitationException.class, lambda::run);
    }
  }
}
