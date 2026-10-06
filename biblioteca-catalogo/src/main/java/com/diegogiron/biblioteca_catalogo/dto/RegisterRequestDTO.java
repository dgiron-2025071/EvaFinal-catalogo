package com.diegogiron.biblioteca_catalogo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    @NotBlank(message = "el nombre es obligatorio")
    @Size(max = 100, message = "el nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotBlank(message = "el email es obligatorio")
    @Email(message = "el email no es valido")
    @Size(max = 150, message = "el email no puede exceder 150 caracteres")
    private String email;

    @NotBlank(message = "la contrasena es obligatoria")
    @Size(min = 6, max = 60, message = "la contrasena debe tener entre 6 y 60 caracteres")
    private String password;
}
