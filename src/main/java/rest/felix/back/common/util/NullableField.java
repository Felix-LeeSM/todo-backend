package rest.felix.back.common.util;

import java.util.function.Consumer;

public sealed interface NullableField<T> {
  record Present<T>(T value) implements NullableField<T> {}

  record Absent<T>() implements NullableField<T> {}

  default void ifPresent(Consumer<T> consumer) {
    if (this instanceof NullableField.Present<T>) {
      consumer.accept(this.getValue());
    }
  }

  default boolean isPresent() {
    return this instanceof Present;
  }

  default boolean isAbsent() {
    return this instanceof Absent;
  }

  default T getValue() {
    return this instanceof Present<T>(T value) ? value : null;
  }
}
