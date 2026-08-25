package com.stuttgarttaxi.taxihub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Şifre en az 8 karakter, 1 büyük harf, 1 rakam ve 1 özel karakter içermelidir";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
