package com.example.distribuidora.dtos;

import com.example.distribuidora.models.Customer;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
public class CustomerDto {

    //TODO ver si en error de validacion devuelve 200 o un bad request en controller

    @Length(min = 3, max = 150, message = "Business name must be between 3 and 150 characters")
    private String businessName;

    @Length(min = 3, max = 30, message = "Tax ID must be between 3 and 30 characters")
    private String taxId;

    @Length(min = 3, max = 150, message = "Name must be between 3 and 150 characters")
    private String name;

    @Length(min = 10, max = 10, message = "Phone must be 10 characters")
    private String phone;

    @Email(message = "Email should be valid")
    private String email;

    public CustomerDto(Customer customer) {
        this.businessName = customer.getBusinessName();
        this.taxId = customer.getTaxId();
        this.name = customer.getName();
        this.phone = customer.getPhone();
        this.email = customer.getEmail();
    }
}
