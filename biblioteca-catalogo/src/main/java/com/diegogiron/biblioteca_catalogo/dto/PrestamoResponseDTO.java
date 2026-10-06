package com.diegogiron.biblioteca_catalogo.dto;

import java.time.LocalDateTime;
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
public class PrestamoResponseDTO {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long libroId;
    private String libroTitulo;
    private String libroIsbn;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionEsperada;
    private LocalDateTime fechaDevolucionReal;
    private String estado;
}
