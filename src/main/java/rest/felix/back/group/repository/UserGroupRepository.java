package rest.felix.back.group.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import rest.felix.back.group.dto.UserGroupDTO;
import rest.felix.back.group.entity.Group;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.user.entity.User;

@Repository
@AllArgsConstructor
public class UserGroupRepository {

  private final EntityManager em;

  public Optional<UserGroupDTO> findByUserIdAndGroupId(long userId, long groupId) {
    try {
      return Optional.of(
              em.createQuery(
                      """
              SELECT
                  ug
              FROM
                  UserGroup ug
              WHERE
                  ug.user.id = :userId AND
                  ug.group.id = :groupId
              """,
                      UserGroup.class)
                  .setParameter("userId", userId)
                  .setParameter("groupId", groupId)
                  .getSingleResult())
          .map(UserGroupDTO::of);

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void registerUserToGroup(long userId, long groupId, GroupRole role) {
    User userRef = em.getReference(User.class, userId);
    Group groupRef = em.getReference(Group.class, groupId);

    UserGroup userGroup = new UserGroup();
    userGroup.setUser(userRef);
    userGroup.setGroup(groupRef);
    userGroup.setGroupRole(role);

    em.persist(userGroup);
  }

  public void deleteByGroupId(long groupId) {
    em.createQuery(
            """
        DELETE
        FROM
          UserGroup ug
        WHERE
          ug.group.id = :groupId
        """)
        .setParameter("groupId", groupId)
        .executeUpdate();
  }

  public Map<Long, GroupRole> findUserRolesByGroupIds(Long userId, List<Long> groupIds) {
    return em
        .createQuery(
            """
      SELECT
        ug
      FROM
        UserGroup ug
      WHERE
        ug.group.id in :groupIds
        AND
        ug.user.id = :userId
  """,
            UserGroup.class)
        .setParameter("groupIds", groupIds)
        .setParameter("userId", userId)
        .getResultList()
        .stream()
        .map(UserGroupDTO::of)
        .collect(Collectors.toMap(UserGroupDTO::getGroupId, dto -> dto.getGroupRole()));
  }
}
