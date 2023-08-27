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
