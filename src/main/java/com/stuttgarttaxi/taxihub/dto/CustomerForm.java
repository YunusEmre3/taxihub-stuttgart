package com.stuttgarttaxi.taxihub.dto;

import com.stuttgarttaxi.taxihub.entity.AccountType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Backs both "Add New Customer" and the edit form. On edit, email is
 * rendered read-only (it's the key that ties a customer back to their
 * booking history) but still round-trips through the form.
 */
@Getter
@Setter
public class CustomerForm {

    @NotBlank(message = "{validation.firstName.required}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    private String lastName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    private String phoneNumber;

    @NotNull(message = "{validation.accountType.required}")
    private AccountType accountType = AccountType.PERSONAL;
}
