package com.diegogiron.biblioteca_catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibroResponseDTO {

    private Long id;
    private String isbn;
    private String titulo;
    private String autor;
    private String categoria;
    private Integer stockTotal;
    private Integer stockDisponible;
}
