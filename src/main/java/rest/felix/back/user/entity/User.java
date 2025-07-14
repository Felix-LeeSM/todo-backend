package rest.felix.back.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rest.felix.back.group.entity.UserGroup;
import rest.felix.back.todo.entity.Todo;

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

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserGroup> userGroups = new ArrayList<>();

  @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Todo> todos = new ArrayList<>();

  @CreationTimestamp private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;

  public void addUserGroup(UserGroup userGroup) {
    this.userGroups.add(userGroup);
    userGroup.setUser(this);
  }
}
