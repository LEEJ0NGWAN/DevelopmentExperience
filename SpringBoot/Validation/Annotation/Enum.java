
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy =  EnumValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Enum {

    String message() default "must be one of the values {values}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] values() default {};
}
class EnumValidator implements ConstraintValidator<Enum, String> {

    private Set<String> set = new HashSet<>();

    @Override
    public void initialize(Enum constraintAnnotation) {

        for (final String value: constraintAnnotation.values()) set.add(value);
    }

    @Override
    public boolean isValid(String field, ConstraintValidatorContext context) {

        if (field==null||field.trim().isEmpty()) {

            context.disableDefaultConstraintViolation();
            context
            .buildConstraintViolationWithTemplate("must not be blank")
            .addConstraintViolation();
        }

        return set.contains(field);
    }
}
