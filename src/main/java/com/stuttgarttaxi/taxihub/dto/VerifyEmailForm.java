package com.stuttgarttaxi.taxihub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailForm {

    @NotBlank
    private String email;

    @NotBlank(message = "{validation.verificationCode.required}")
    @Pattern(regexp = "\\d{6}", message = "{validation.verificationCode.invalid}")
    private String code;
}
