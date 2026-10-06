package com.diegogiron.biblioteca_catalogo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "el email es obligatorio")
    @Email(message = "el email no es valido")
    private String email;

    @NotBlank(message = "la contrasena es obligatoria")
    private String password;
}
