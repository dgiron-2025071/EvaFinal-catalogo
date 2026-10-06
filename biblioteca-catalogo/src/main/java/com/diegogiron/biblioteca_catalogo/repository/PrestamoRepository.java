package com.diegogiron.biblioteca_catalogo.repository;

import com.diegogiron.biblioteca_catalogo.entity.Prestamo;
import com.diegogiron.biblioteca_catalogo.enums.EstadoPrestamo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioIdOrderByFechaPrestamoDesc(Long usuarioId);

    long countByUsuarioIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    boolean existsByUsuarioIdAndEstadoNotAndFechaDevolucionEsperadaBefore(
            Long usuarioId, EstadoPrestamo estado, LocalDateTime fecha);

    List<Prestamo> findByEstadoNotAndFechaDevolucionEsperadaBefore(EstadoPrestamo estado, LocalDateTime fecha);
}
