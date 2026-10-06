package com.diegogiron.biblioteca_catalogo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LibroRequestDTO {

    @NotBlank(message = "el ISBN es obligatorio")
    @Size(max = 20, message = "el ISBN no puede exceder 20 caracteres")
    private String isbn;

    @NotBlank(message = "el titulo es obligatorio")
    @Size(max = 200, message = "el titulo no puede exceder 200 caracteres")
    private String titulo;

    @Size(max = 150, message = "el autor no puede exceder 150 caracteres")
    private String autor;

    @Size(max = 100, message = "la categoria no puede exceder 100 caracteres")
    private String categoria;

    @NotNull(message = "el stock total es obligatorio")
    @Min(value = 0, message = "el stock total no puede ser negativo")
    private Integer stockTotal;

    // Solo se usa al actualizar stock; al crear se deriva de stockTotal
    @Min(value = 0, message = "el stock disponible no puede ser negativo")
    private Integer stockDisponible;
}
