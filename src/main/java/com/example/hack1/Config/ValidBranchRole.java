package com.example.hack1.Config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE}) // Se aplica a nivel de CLASE
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BranchRoleValidator.class) // La clase que contiene la lógica
public @interface ValidBranchRole {
    String message() default "La sucursal (branch) es obligatoria si el rol es BRANCH, y debe ser nula si el rol es CENTRAL";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}