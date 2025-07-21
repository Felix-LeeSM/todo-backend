package rest.felix.back.group.repository;

import jakarta.persistence.EntityManager;
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
    Group group = em.getReference(Group.class, createGroupInvitationDTO.getGroupId());
    User issuer = em.getReference(User.class, createGroupInvitationDTO.getIssuerId());

    GroupInvitation groupInvitation = new GroupInvitation();

    groupInvitation.setGroup(group);
    groupInvitation.setIssuer(issuer);
    groupInvitation.setToken(createGroupInvitationDTO.getToken());
    groupInvitation.setExpiresAt(createGroupInvitationDTO.getExpiresAt());

    em.persist(groupInvitation);

    return GroupInvitationDTO.of(groupInvitation);
  }

  @Transactional(readOnly = true)
  public Integer countActiveUntil(long issuerId, long groupId, ZonedDateTime now) {
    return em.createQuery(
            """
          SELECT COUNT(*)
          FROM GroupInvitation gi
          WHERE gi.issuer.id = :issuerId
            AND gi.group.id = :groupId
            AND gi.expiresAt < :now
          """,
            Integer.class)
        .setParameter("issuerId", issuerId)
        .setParameter("groupId", groupId)
        .setParameter("now", now)
        .getSingleResult();
  }

  @Transactional(readOnly = true)
  public Optional<GroupInvitationDTO> findByToken(String token) {
    return em.createQuery(
            """
          SELECT gi
          FROM GroupInvitation gi
          LEFT JOIN FETCH gi.group g
          LEFT JOIN FETCH gi.issuer i
          WHERE gi.token = :token
          """,
            GroupInvitation.class)
        .setParameter("token", token)
        .getResultStream()
        .findFirst()
        .map(GroupInvitationDTO::of);
  }

  @Transactional
  public void delete(GroupInvitation groupInvitation) {
    em.remove(groupInvitation);
  }
}
