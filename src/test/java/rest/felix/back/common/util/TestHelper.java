package rest.felix.back.common.util;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestHelper {
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private GenericDeleter deleter;

  public void cleanUp() {
    databaseCleaner.execute();
  }

  public void delete(List<Object> entities) {
    for (Object entity : entities) {
      deleter.deleteImmediately(entity);
    }
  }

  public void delete(Object entity) {
    deleter.deleteImmediately(entity);
  }
}
