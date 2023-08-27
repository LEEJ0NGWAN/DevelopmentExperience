[돌아가기](https://github.com/LEEJ0NGWAN/DevelopmentExperience)

# Dynamic Request Json Deserialize

가변적인 리퀘스트 데이터 json에 대해 deserialization 및 validation 수행하는 커스텀 Jackson Deserializer입니다.

예를 들어, User DTO 객체의 필드로 lastName가 있다고 가정합니다.  
해당 필드를 파싱하기 위해서는 요청 데이터 json에 이름이 같은 프로퍼티 `lastName` 혹은 `last_name`을 고정적으로 받아와야 합니다.  
이런점을 개선하여 `lastName` 외에도 `surname` 및 `family_name` 처럼 다른 프로퍼티도 가변적으로 처리하여 동일한 필드로 설정해주고 동일한 검증을 수행할 수 있도록 처리해주는 커스텀 Deserializer를 구현합니다.

![](./dynamic%20json%20property.png)

이러한 설정은 yml 파일(혹은 환경변수)을 통해 변경할 수 있으며 소스 수정없이 애플리케이션 재기동 만으로도 api 요구 데이터 항목명을 쉽게 변경하여 기능 확장성 개선에 도움을 줍니다.

### application.yml
어떤 json 프로퍼티가 어떤 필드로 맵핑될지 설정합니다
```yaml
user:
  dynamic-json-property:
    family_name: lastName
    surname: lastName
    given_name: firstName
    mobile_phone: phoneNumber
    cell_phone: phoneNumber

```

### User.java
실제 Deserialize 될 DTO 입니다
```java
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Getter @Setter @ToString @FieldNameConstants
@JsonDeserialize(using = UserDeserializer.class)
public class User {

    @NotBlank
    private String lastName;

    @NotBlank
    private String firstName;
}

```

### UserDeserializer
yml에 정의된 dynamic json 항목들이 dto의 어떤 필드로 세팅 될지 맵핑 시켜주는 디시리얼라이저 입니다.
```java
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Component
@ConfigurationProperties("user")
public class UserDeserializer extends StdDeserializer<User> {

    private Map<String,String> dynamicJsonProperty;

    public UserDeserializer() { super(User.class); }

    @Override
    public User deserialize(JsonParser parser, DeserializationContext context)
    throws IOException, JacksonException {

        final JsonNode jsonNode = parser.getCodec().readTree(parser);
        final Iterator<Entry<String, JsonNode>> fields = jsonNode.fields();

        final User user = new User();
        while (fields.hasNext()) {

            final Entry<String, JsonNode> field = fields.next();

            final String fieldName = field.getKey();
            final String fieldValue = field.getValue().asText(null);

            // 성능을 위해 리플렉션은 지양한다
            if (User.Fields.lastName.equals(dynamicJsonProperty.get(fieldName)))
            user.setLastName(fieldValue);

            if (User.Fields.firstName.equals(dynamicJsonProperty.get(fieldName)))
            user.setFirstName(fieldValue);
        }

        return user;
    }
}

```