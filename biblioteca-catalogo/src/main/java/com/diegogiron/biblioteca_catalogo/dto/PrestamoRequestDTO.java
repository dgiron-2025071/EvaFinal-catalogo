package com.diegogiron.biblioteca_catalogo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoRequestDTO {

    @NotNull(message = "el usuario id es obligatorio")
    private Long usuarioId;

    @NotNull(message = "el libro id es obligatorio")
    private Long libroId;
}
