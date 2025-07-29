package rest.felix.back.common.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

@Component
public class GenericDeleter {
  @Autowired private EntityManager em;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteImmediately(Object entity) {
    if (entity == null) {
      throw new IllegalArgumentException("삭제할 엔티티는 null일 수 없습니다.");
    }

    try {
      String tableName = getTableName(entity.getClass());

      IdField idField = getIdField(entity);
      String idColumnName = idField.columnName();
      Object idValue = idField.value();

      String sql =
          String.format(
              """
                                    DELETE FROM "%s" WHERE "%s" = :idValue
                                    """,
              tableName, idColumnName);
      em.createNativeQuery(sql).setParameter("idValue", idValue).executeUpdate();

    } catch (Exception e) {
      throw new RuntimeException("동적 삭제 처리 중 오류 발생", e);
    }
  }

  private String getTableName(Class<?> entityClass) {
    Table tableAnnotation = entityClass.getAnnotation(Table.class);
    if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
      return tableAnnotation.name();
    }

    String regex = "(?<!^)(?=[A-Z])";
    return entityClass.getSimpleName().replaceAll(regex, "_").toLowerCase();
  }

  private IdField getIdField(Object entity) throws IllegalAccessException {
    Class<?> entityClass = entity.getClass();
    // JPA 메타모델을 통해 PK 필드 이름을 찾음
    String idFieldName = em.getMetamodel().entity(entityClass).getId(Object.class).getName();

    Field field = ReflectionUtils.findField(entityClass, idFieldName);
    if (field == null) {
      throw new IllegalArgumentException("@Id 필드를 찾을 수 없습니다: " + entityClass.getName());
    }
    ReflectionUtils.makeAccessible(field);

    // 실제 DB 컬럼 이름은 @Column 어노테이션을 따름 (여기서는 간단히 필드명으로 가정)
    String columnName = field.getName();
    Object value = field.get(entity);

    return new IdField(columnName, value);
  }

  // PK의 컬럼명과 값을 담을 간단한 레코드
  private record IdField(String columnName, Object value) {}
}
