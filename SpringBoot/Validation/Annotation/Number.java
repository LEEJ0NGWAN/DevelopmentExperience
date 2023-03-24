import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy =  NumberValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Number {

    String message() default "must be a number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int minSize() default 1;
    int maxSize() default 50;

    String minValue() default "";
    String maxValue() default "";
}
class NumberValidator implements ConstraintValidator<Number, String> {

    private String SIZE = "size must be ";
    private String GREATER = "must be greater than or equal to ";
    private String LESS = "must be less than or equal to ";

    private int minSize = -1;
    private int maxSize = -1;

    private String minValue;
    private String maxValue;

    @Override
    public void initialize(Number constraintAnnotation) {

        minSize = constraintAnnotation.minSize();
        maxSize = constraintAnnotation.maxSize();

        if (!constraintAnnotation.minValue().isEmpty()) {

            GREATER += (minValue = new Integer(constraintAnnotation.minValue()).toString());
            minSize = minValue.length();
        }
        if (!constraintAnnotation.maxValue().isEmpty()) {

            LESS += (maxValue = new Integer(constraintAnnotation.maxValue()).toString());
            maxSize = maxValue.length();
        }

        SIZE += minSize==maxSize? minSize: "between " + minSize + " and " + maxSize;
    }

    @Override
    public boolean isValid(String field, ConstraintValidatorContext context) {

        if (field==null||field.trim().isEmpty()) {

            context.disableDefaultConstraintViolation();
            context
            .buildConstraintViolationWithTemplate("must not be blank")
            .addConstraintViolation();

            return false;
        }

        final int length = field.length();

        if (length<minSize || maxSize<length) {

            context.disableDefaultConstraintViolation();
            context
            .buildConstraintViolationWithTemplate(SIZE)
            .addConstraintViolation();

            return false;
        }

        boolean minCheck = minSize==length&&minValue!=null;
        boolean maxCheck = maxSize==length&&maxValue!=null;
        boolean minValid = true;
        boolean maxValid = true;

        for (int i=0; i<length; i++) {

            final char c = field.charAt(i);

            if (!Character.isDigit(c)) return false;

            if (minCheck) {

                final char minValueChar = minValue.charAt(i);

                minValid = minValueChar<=c;
                minCheck = minValueChar==c;
            }

            if (maxCheck) {

                final char maxValueChar = maxValue.charAt(i);

                maxValid = maxValueChar>=c;
                maxCheck = maxValueChar==c;
            }
        }

        if (!minValid) {

            context.disableDefaultConstraintViolation();
            context
            .buildConstraintViolationWithTemplate(GREATER)
            .addConstraintViolation();
        }

        if (!maxValid) {

            context.disableDefaultConstraintViolation();
            context
            .buildConstraintViolationWithTemplate(LESS)
            .addConstraintViolation();
        }

        return minValid&&maxValid;
    }
}
