
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

@Documented @Constraint(validatedBy =  DateValidator.class) @Target({ ElementType.METHOD, ElementType.FIELD }) @Retention(RetentionPolicy.RUNTIME) public @interface Date {

    String message() default "must be a date - yyyyMMdd";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String min() default "";
    String max() default "";
}
class DateValidator implements ConstraintValidator<Date, String> {

    private String GREATER = "must be greater than or equal to ";
    private String LESS = "must be less than or equal to ";

    private String min = null; private LocalDate minDate = null;
    private String max = null; private LocalDate maxDate = null;

    @Override
    public void initialize(Date constraintAnnotation) {

        if (!constraintAnnotation.min().isEmpty()) {

            GREATER += (min = constraintAnnotation.min());
            minDate = LocalDate.parse(min, BASIC_ISO_DATE);
        }

        if (!constraintAnnotation.max().isEmpty()) {

            LESS += (max = constraintAnnotation.max());
            maxDate = LocalDate.parse(max, BASIC_ISO_DATE);
        }
    }

    @Override
    public boolean isValid(String field, ConstraintValidatorContext context) {

        boolean result = false;
        try {

            if (field==null||field.trim().isEmpty()) {

                context.disableDefaultConstraintViolation();
                context
                .buildConstraintViolationWithTemplate("must not be blank")
                .addConstraintViolation();
            }

            final LocalDate date = LocalDate.parse(field, BASIC_ISO_DATE); // yyyyMMdd

            final boolean isAfter = min==null||date.isAfter(minDate)||date.isEqual(minDate);
            final boolean isBefore = max==null||date.isBefore(maxDate)||date.isEqual(maxDate);

            if (!isAfter) {

                context.disableDefaultConstraintViolation();
                context
                .buildConstraintViolationWithTemplate(GREATER)
                .addConstraintViolation();
            }

            if (!isBefore) {

                context.disableDefaultConstraintViolation();
                context
                .buildConstraintViolationWithTemplate(LESS)
                .addConstraintViolation();
            }

            result = isAfter&&isBefore;

        } catch (Exception e) {}

        return result;
    }
}
