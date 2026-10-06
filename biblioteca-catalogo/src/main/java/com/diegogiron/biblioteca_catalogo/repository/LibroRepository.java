package com.diegogiron.biblioteca_catalogo.repository;

import com.diegogiron.biblioteca_catalogo.entity.Libro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Long id);

    Page<Libro> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    Page<Libro> findByCategoriaIgnoreCase(String categoria, Pageable pageable);

    Page<Libro> findByTituloContainingIgnoreCaseAndCategoriaIgnoreCase(String titulo, String categoria, Pageable pageable);
}
