package rest.felix.back.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import java.io.IOException;

public class NullableFieldDeserializer extends JsonDeserializer<NullableField<?>>
    implements ContextualDeserializer {

  private final JavaType valueType;

  // 기본 생성자 (초기 생성 시)
  public NullableFieldDeserializer() {
    this.valueType = null;
  }

  // 타입 정보를 가진 생성자 (컨텍스트 생성 시)
  public NullableFieldDeserializer(JavaType valueType) {
    this.valueType = valueType;
  }

  @Override
  public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
      throws JsonMappingException {
    JavaType wrapperType = ctxt.getContextualType();
    JavaType valueType = wrapperType.containedType(0);
    return new NullableFieldDeserializer(valueType); // 새로운 인스턴스 생성
  }

  @Override
  public NullableField<?> deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException {
    Object value = valueType != null ? ctxt.readValue(p, valueType) : p.readValueAs(Object.class);

    return new NullableField.Present<>(value);
  }

  @Override
  public NullableField<?> getNullValue(DeserializationContext ctxt) {
    return new NullableField.Present<>(null);
  }

  @Override
  public NullableField<?> getAbsentValue(DeserializationContext ctxt) {
    return new NullableField.Absent<>();
  }
}
