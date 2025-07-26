package rest.felix.back.group.entity;

import jakarta.persistence.*;
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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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
