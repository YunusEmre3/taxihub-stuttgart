package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    private String firstName;

    private String lastName;

    @ValidPassword
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;
}
