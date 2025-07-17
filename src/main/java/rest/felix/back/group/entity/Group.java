package rest.felix.back.group.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import rest.felix.back.todo.entity.Todo;

@Getter
@Setter
@ToString
@Entity
public class Group {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_id_generator")
  @SequenceGenerator(
      name = "group_id_generator",
      sequenceName = "group_id_sequence",
      allocationSize = 50)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 200)
  private String description = "";

  @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
  private List<UserGroup> userGroups = List.of();

  @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
  private List<Todo> todos = List.of();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp private ZonedDateTime updatedAt;
}
