package com.diegogiron.biblioteca_catalogo.controller;

import com.diegogiron.biblioteca_catalogo.dto.LibroRequestDTO;
import com.diegogiron.biblioteca_catalogo.dto.LibroResponseDTO;
import com.diegogiron.biblioteca_catalogo.service.LibroService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
public class LibroController {

    private static final List<String> CAMPOS_ORDEN = List.of("id", "isbn", "titulo", "autor", "categoria");

    private final LibroService libroService;

    @GetMapping
    public ResponseEntity<Page<LibroResponseDTO>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String categoria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titulo,asc") String sort) {
        return ResponseEntity.ok(libroService.listar(titulo, categoria, construirPageable(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LibroResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(libroService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<LibroResponseDTO> crear(@Valid @RequestBody LibroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LibroResponseDTO> actualizar(@PathVariable Long id,
                                                       @Valid @RequestBody LibroRequestDTO dto) {
        return ResponseEntity.ok(libroService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Formato sort: campo,asc|desc (campo invalido -> titulo asc)
    private Pageable construirPageable(int page, int size, String sort) {
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        boolean descendente = partes.length > 1 && partes[1].trim().equalsIgnoreCase("desc");
        if (!CAMPOS_ORDEN.contains(campo)) {
            campo = "titulo";
        }
        Sort.Direction direccion = descendente ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direccion, campo));
    }
}
