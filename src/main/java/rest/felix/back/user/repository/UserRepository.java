package rest.felix.back.user.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import rest.felix.back.user.dto.SignupDTO;
import rest.felix.back.user.entity.User;
import rest.felix.back.user.exception.UsernameTakenException;

@Repository
@AllArgsConstructor
public class UserRepository {

  private final EntityManager em;

  public User createUser(SignupDTO signupDTO) throws UsernameTakenException {
    try {
      User user = new User();

      user.setHashedPassword(signupDTO.getHashedPassword());
      user.setNickname(signupDTO.getNickname());
      user.setUsername(signupDTO.getUsername());

      em.persist(user);

      return user;
    } catch (DataIntegrityViolationException e) {

      throw new UsernameTakenException();
    }
  }

  public Optional<User> getByUsername(String username) {
    try {
      TypedQuery<User> query =
          em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
      query.setParameter("username", username);

      User user = query.getSingleResult();
      return Optional.of(user);

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  public void save(User user) {
    em.persist(user);
  }

  public Optional<User> getById(Long userId) {
    try {
      TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class);
      query.setParameter("id", userId);

      User user = query.getSingleResult();
      return Optional.of(user);

    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
