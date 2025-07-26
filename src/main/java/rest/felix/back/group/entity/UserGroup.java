package rest.felix.back.group.entity;

import jakarta.persistence.*;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rest.felix.back.group.entity.enumerated.GroupRole;
import rest.felix.back.user.entity.User;

@Getter
@Setter
@ToString
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "groupId"}))
public class UserGroup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GroupRole groupRole;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;
}
