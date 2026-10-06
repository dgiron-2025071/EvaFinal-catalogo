package com.diegogiron.biblioteca_catalogo.service;

import com.diegogiron.biblioteca_catalogo.dto.LibroRequestDTO;
import com.diegogiron.biblioteca_catalogo.dto.LibroResponseDTO;
import com.diegogiron.biblioteca_catalogo.entity.Libro;
import com.diegogiron.biblioteca_catalogo.exception.ReglaNegocioException;
import com.diegogiron.biblioteca_catalogo.exception.ResourceNotFoundException;
import com.diegogiron.biblioteca_catalogo.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    @Transactional(readOnly = true)
    public Page<LibroResponseDTO> listar(String titulo, String categoria, Pageable pageable) {
        boolean hayTitulo = titulo != null && !titulo.isBlank();
        boolean hayCategoria = categoria != null && !categoria.isBlank();
        Page<Libro> pagina;

        if (hayTitulo && hayCategoria) {
            pagina = libroRepository.findByTituloContainingIgnoreCaseAndCategoriaIgnoreCase(
                    titulo.trim(), categoria.trim(), pageable);
        } else if (hayTitulo) {
            pagina = libroRepository.findByTituloContainingIgnoreCase(titulo.trim(), pageable);
        } else if (hayCategoria) {
            pagina = libroRepository.findByCategoriaIgnoreCase(categoria.trim(), pageable);
        } else {
            pagina = libroRepository.findAll(pageable);
        }
        return pagina.map(this::aResponse);
    }

    @Transactional(readOnly = true)
    public LibroResponseDTO obtener(Long id) {
        return aResponse(buscarOError(id));
    }

    @Transactional
    public LibroResponseDTO crear(LibroRequestDTO dto) {
        if (libroRepository.existsByIsbn(dto.getIsbn())) {
            throw new ReglaNegocioException("El ISBN ya existe: " + dto.getIsbn());
        }
        int stockTotal = dto.getStockTotal();
        int stockDisponible = dto.getStockDisponible() != null ? dto.getStockDisponible() : stockTotal;
        validarStock(stockTotal, stockDisponible);

        Libro libro = Libro.builder()
                .isbn(dto.getIsbn())
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .categoria(dto.getCategoria())
                .stockTotal(stockTotal)
                .stockDisponible(stockDisponible)
                .build();
        return aResponse(libroRepository.save(libro));
    }

    @Transactional
    public LibroResponseDTO actualizar(Long id, LibroRequestDTO dto) {
        Libro libro = buscarOError(id);
        if (!libro.getIsbn().equals(dto.getIsbn()) && libroRepository.existsByIsbnAndIdNot(dto.getIsbn(), id)) {
            throw new ReglaNegocioException("El ISBN ya existe: " + dto.getIsbn());
        }
        int stockTotal = dto.getStockTotal();
        int stockDisponible = dto.getStockDisponible() != null ? dto.getStockDisponible() : libro.getStockDisponible();
        validarStock(stockTotal, stockDisponible);

        libro.setIsbn(dto.getIsbn());
        libro.setTitulo(dto.getTitulo());
        libro.setAutor(dto.getAutor());
        libro.setCategoria(dto.getCategoria());
        libro.setStockTotal(stockTotal);
        libro.setStockDisponible(stockDisponible);
        return aResponse(libroRepository.save(libro));
    }

    @Transactional
    public void eliminar(Long id) {
        Libro libro = buscarOError(id);
        try {
            libroRepository.delete(libro);
            libroRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el libro porque tiene prestamos asociados");
        }
    }

    private Libro buscarOError(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
    }

    private void validarStock(int stockTotal, int stockDisponible) {
        if (stockDisponible > stockTotal) {
            throw new ReglaNegocioException(
                    "El stock disponible (" + stockDisponible + ") no puede superar el stock total (" + stockTotal + ")");
        }
    }

    private LibroResponseDTO aResponse(Libro libro) {
        return LibroResponseDTO.builder()
                .id(libro.getId())
                .isbn(libro.getIsbn())
                .titulo(libro.getTitulo())
                .autor(libro.getAutor())
                .categoria(libro.getCategoria())
                .stockTotal(libro.getStockTotal())
                .stockDisponible(libro.getStockDisponible())
                .build();
    }
}
