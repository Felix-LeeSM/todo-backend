package rest.felix.back.user.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import rest.felix.back.group.dto.MemberDTO;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.dto.UserDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UsernameTakenException;

@Repository
@AllArgsConstructor
public class UserRepository {

  private final EntityManager em;

  @Transactional
  public User createUser(SignupDTO signupDTO) throws UsernameTakenException {
    try {
      User user = new User();

      user.setHashedPassword(signupDTO.hashedPassword());
      user.setNickname(signupDTO.nickname());
      user.setUsername(signupDTO.username());

      em.persist(user);

      return user;
    } catch (DataIntegrityViolationException e) {

      throw new UsernameTakenException();
    }
  }

  @Transactional(readOnly = true)
  public Optional<UserDTO> findByUsername(String username) {

    return em
        .createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
        .setParameter("username", username)
        .getResultList()
        .stream()
        .findFirst()
        .map(UserDTO::of);
  }

  @Transactional(readOnly = true)
  public Map<Long, List<MemberDTO>> findMembersByGroupIds(List<Long> groupIds) {
    return em
        .createQuery(
            """
            SELECT new rest.felix.back.group.dto.MemberDTO(
                u.id,
                u.nickname,
                g.id,
                ug.groupRole
            )
            FROM User u
            JOIN UserGroup ug ON u.id = ug.user.id
            JOIN Group g ON g.id = ug.group.id
            WHERE g.id IN :groupIds
            """,
            MemberDTO.class)
        .setParameter("groupIds", groupIds)
        .getResultList()
        .stream()
        .collect(Collectors.groupingBy(MemberDTO::groupId));
  }

  @Transactional(readOnly = true)
  public List<MemberDTO> findMembersByGroupId(Long groupId) {
    return em.createQuery(
            """
            SELECT new rest.felix.back.group.dto.MemberDTO(
                u.id,
                u.nickname,
                g.id,
                ug.groupRole
            )
            FROM User u
            JOIN UserGroup ug ON u.id = ug.user.id
            JOIN Group g ON g.id = ug.group.id
            WHERE g.id = :groupId
            """,
            MemberDTO.class)
        .setParameter("groupId", groupId)
        .getResultList();
  }

  @Transactional
  public void save(User user) {
    em.persist(user);
  }

  @Transactional(readOnly = true)
  public Optional<UserDTO> findById(Long userId) {
    return em
        .createQuery("SELECT u FROM User u WHERE u.id = :userId", User.class)
        .setParameter("userId", userId)
        .getResultList()
        .stream()
        .findFirst()
        .map(UserDTO::of);
  }
}
