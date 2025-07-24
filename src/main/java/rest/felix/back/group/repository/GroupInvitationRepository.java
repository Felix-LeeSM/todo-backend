package rest.felix.back.group.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.group.dto.CreateGroupInvitationDTO;
import rest.felix.back.group.dto.GroupInvitationDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.GroupInvitation;
import rest.felix.back.user.entity.User;

@Repository
@RequiredArgsConstructor
public class GroupInvitationRepository {

  private final EntityManager em;

  @Transactional
  public GroupInvitationDTO createGroupInvitation(
      CreateGroupInvitationDTO createGroupInvitationDTO) {
    Group group = em.getReference(Group.class, createGroupInvitationDTO.groupId());
    User issuer = em.getReference(User.class, createGroupInvitationDTO.issuerId());

    GroupInvitation groupInvitation = new GroupInvitation();

    groupInvitation.setGroup(group);
    groupInvitation.setIssuer(issuer);
    groupInvitation.setToken(createGroupInvitationDTO.token());
    groupInvitation.setExpiresAt(createGroupInvitationDTO.expiresAt());

    em.persist(groupInvitation);

    return GroupInvitationDTO.of(groupInvitation);
  }

  @Transactional(readOnly = true)
  public Long countActiveUntil(long issuerId, long groupId, ZonedDateTime now) {
    return em.createQuery(
            """
          SELECT COUNT(*)
          FROM GroupInvitation gi
          WHERE gi.issuer.id = :issuerId
            AND gi.group.id = :groupId
            AND :now < gi.expiresAt
          """,
            Long.class)
        .setParameter("issuerId", issuerId)
        .setParameter("groupId", groupId)
        .setParameter("now", now)
        .getSingleResult();
  }

  @Transactional(readOnly = true)
  public Optional<GroupInvitationDTO> findByToken(String token) {
    try {
      return Optional.of(
              em.createQuery(
                      """
          SELECT gi
          FROM GroupInvitation gi
          WHERE gi.token = :token
          """,
                      GroupInvitation.class)
                  .setParameter("token", token)
                  .getSingleResult())
          .map(GroupInvitationDTO::of);
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  @Transactional
  public void delete(GroupInvitation groupInvitation) {
    em.remove(groupInvitation);
  }
}
