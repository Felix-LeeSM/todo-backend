package rest.felix.back.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.ZonedDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rest.felix.back.user.entity.User;

@ToString
@Getter
@Setter
@Entity
public class GroupInvitation {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_invitation_id_generator")
  @SequenceGenerator(
      name = "group_invitation_id_generator",
      sequenceName = "group_invitation_id_sequence",
      allocationSize = 50)
  @Setter(AccessLevel.NONE)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "issuer_id", nullable = false)
  private User issuer;

  @Column(nullable = false, unique = true)
  private String token;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;
}
