package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordForm {

    @NotBlank
    private String token;

    @ValidPassword
    private String password;

    @NotBlank(message = "{validation.confirmPassword.required}")
    private String confirmPassword;
}
