package com.diegogiron.biblioteca_catalogo.controller;

import com.diegogiron.biblioteca_catalogo.dto.PrestamoRequestDTO;
import com.diegogiron.biblioteca_catalogo.dto.PrestamoResponseDTO;
import com.diegogiron.biblioteca_catalogo.service.PrestamoService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoResponseDTO> registrar(
            @Valid @RequestBody PrestamoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.registrar(dto));
    }

    @PatchMapping("/{id}/devolucion")
    public ResponseEntity<PrestamoResponseDTO> devolver(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.devolver(id));
    }

    @GetMapping("/mis-prestamos")
    public ResponseEntity<List<PrestamoResponseDTO>> misPrestamos() {
        return ResponseEntity.ok(prestamoService.misPrestamos());
    }

    @GetMapping("/atrasados")
    public ResponseEntity<List<PrestamoResponseDTO>> atrasados() {
        return ResponseEntity.ok(prestamoService.atrasados());
    }
}
