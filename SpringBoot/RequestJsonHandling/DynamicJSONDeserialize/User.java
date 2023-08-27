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
