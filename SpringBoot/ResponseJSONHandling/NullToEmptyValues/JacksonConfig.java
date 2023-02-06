import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ReflectionUtils;

import static java.util.Collections.singletonList;

// LEE JONG WAN - 2023.02.06
// Jackson Object Mapper Customizer
@Configuration
public class JacksonConfig {

    private static final String EMPTY_STRING = "";
    private static final String JSON_EMPTY_STRING = "\"\"";
    private static final String JSON_EMPTY_OBJECT = "{}";
    private static final String JSON_EMPTY_ARRAY  = "[]";

    /**
    * cache empty value types of fields of dto classes.
    * 
    * e.g.,
    * class1 {
    * 
    *  private String field1;
    *  private Map<String, List<String>> field2;
    * 
    * }
    * 
    * then, cache is
    * {
    *  class1: {
    *      field1: [ "" ] -> field1's empty value type is ""
    *      field2: [ "{}", "[]", "" ]
    *      -> field2's depth 0 empty value is {}, depth 1 is [], depth 2 is ""
    *  }
    * }
    */
    private static final Map<String, Map<String, List<String>>> cache = new HashMap<>();

    // O(n)
    // but loop size is small -> average 0 ms operation
    @Bean @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {

        final ObjectMapper objectMapper = builder.build();

        objectMapper
        .getSerializerProvider()
        .setNullValueSerializer(new JsonSerializer<Object>() {

            @Override
            public void serialize(Object o, JsonGenerator g, SerializerProvider p)
            throws IOException {

                JsonStreamContext currentContext = g.getOutputContext();
                JsonStreamContext dtoContext = null;

                int depth = -1;

                if (currentContext==null) { g.writeString(EMPTY_STRING); return; }

                // find the dto context
                // and calculate a depth from dto to origin context
                while (currentContext.getParent() != null) {

                    if (currentContext.getParent().inRoot())
                    dtoContext = currentContext;

                    currentContext = currentContext.getParent();
                    depth++;
                }

                if (dtoContext==null||
                    dtoContext.getCurrentName()==null||
                    dtoContext.getCurrentValue()==null) {

                    g.writeString(EMPTY_STRING); return;
                }

                final Class<?> dtoClass = dtoContext.getCurrentValue().getClass();

                final String className = dtoClass.getSimpleName();
                final String fieldName = dtoContext.getCurrentName();

                if (!cache.containsKey(className)||
                    !cache.get(className).containsKey(fieldName))
                cacheEmptyTypes(dtoClass, fieldName);

                if (0<=depth && depth<cache.get(className).get(fieldName).size())
                g.writeRawValue(cache.get(className).get(fieldName).get(depth));

                else g.writeString(EMPTY_STRING);
            }
        });

        return objectMapper;
    }

    private void cacheEmptyTypes(final Class<?> dtoClass, final String fieldName) {

        final Field field = ReflectionUtils.findField(dtoClass, fieldName);
        final Type genericType = field.getGenericType();

        // if the field is collection type
        // then genericTypeString should contains '<' and '>'.
        // else genericTypeString doesn't have '<' or '>'.
        final String genericTypeString = genericType.getTypeName();

        List<String> emptyTypes;

        // if genericTypeString is not collection type
        if (genericTypeString.charAt(genericTypeString.length()-1) != '>')
        emptyTypes = singletonList(JSON_EMPTY_STRING);

        else {

            emptyTypes = new ArrayList<>();

            loop:
            for (int p=0, i=0, l=genericTypeString.length(); i<l; i++) {

                final char c = genericTypeString.charAt(i);

                switch (c) {

                    case '<': case '>':

                        final String type = genericTypeString.substring(p,i);

                        if (type.contains("Map")) emptyTypes.add(JSON_EMPTY_OBJECT);
                        else if (type.contains("List")) emptyTypes.add(JSON_EMPTY_ARRAY);
                        else emptyTypes.add(JSON_EMPTY_STRING);

                        if (c == '>') break loop;

                    case '.': p=i+1;
                }
            }
        }

        final String className = dtoClass.getSimpleName();

        if (!cache.containsKey(className))
        cache.put(className, new HashMap<>());

        cache.get(className).put(fieldName, emptyTypes);
    }
}
