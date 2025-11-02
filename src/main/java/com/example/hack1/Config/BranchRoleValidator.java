package com.example.hack1.Config;

import com.example.hack1.DTO.Request.RegisterUserDTO;
import com.example.hack1.User.domain.Rol;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BranchRoleValidator implements ConstraintValidator<ValidBranchRole, RegisterUserDTO> {
    @Override
    public boolean isValid(RegisterUserDTO dto, ConstraintValidatorContext context) {
        if (dto.getRole() == null) {
            return true;
        }

        if (dto.getRole() == Rol.BRANCH) {
            return dto.getBranch() != null && !dto.getBranch().isBlank();
        }

        if (dto.getRole() == Rol.CENTRAL) {
            // Si es CENTRAL, 'branch' DEBE ser null
            return dto.getBranch() == null;
        }

        return false;
    }
}
