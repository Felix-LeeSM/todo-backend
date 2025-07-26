package rest.felix.back.user.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rest.felix.back.group.entity.UserGroup;

@Getter
@Setter
@ToString
@Entity
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column(nullable = false, length = 50, unique = true)
  private String username;

  @Column(nullable = false, length = 200)
  private String hashedPassword;

  @Column(nullable = false, length = 50)
  private String nickname;

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
  private List<UserGroup> userGroups = List.of();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;
}
