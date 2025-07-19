package rest.felix.back.common.util;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseCleaner {

  @Autowired private EntityManager em;

  @SuppressWarnings("unchecked")
  @Transactional
  public void execute() {

    List<String> tableNames =
        em.createNativeQuery(
                "SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'PUBLIC'",
                String.class)
            .getResultList();

    em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

    // 3. 모든 테이블 TRUNCATE
    for (String tableName : tableNames) {
      em.createNativeQuery("TRUNCATE TABLE \"" + tableName + "\"").executeUpdate();
    }

    // 4. 외래 키 제약 조건 활성화
    em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
  }

  @Transactional
  public void remove(Object entity) {
    em.remove(entity);
  }
}
