package com.diegogiron.biblioteca_catalogo.service;

import com.diegogiron.biblioteca_catalogo.dto.PrestamoRequestDTO;
import com.diegogiron.biblioteca_catalogo.dto.PrestamoResponseDTO;
import com.diegogiron.biblioteca_catalogo.entity.Libro;
import com.diegogiron.biblioteca_catalogo.entity.Prestamo;
import com.diegogiron.biblioteca_catalogo.entity.Usuario;
import com.diegogiron.biblioteca_catalogo.enums.EstadoPrestamo;
import com.diegogiron.biblioteca_catalogo.enums.EstadoUsuario;
import com.diegogiron.biblioteca_catalogo.enums.RolUsuario;
import com.diegogiron.biblioteca_catalogo.exception.ReglaNegocioException;
import com.diegogiron.biblioteca_catalogo.exception.ResourceNotFoundException;
import com.diegogiron.biblioteca_catalogo.repository.LibroRepository;
import com.diegogiron.biblioteca_catalogo.repository.PrestamoRepository;
import com.diegogiron.biblioteca_catalogo.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private static final int LIMITE_PRESTAMOS_LECTOR = 3;
    private static final int DIAS_PRESTAMO = 14;

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public PrestamoResponseDTO registrar(PrestamoRequestDTO dto) {
        Usuario usuario = usuarioRepository.findById(dto.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con id: " + dto.getUsuarioId()));
        Libro libro = libroRepository.findById(dto.getLibroId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Libro no encontrado con id: " + dto.getLibroId()));

        // Mora detectada -> sanciona en transaccion propia y rechaza el prestamo
        usuarioService.sancionarSiTieneMora(usuario);
        if (usuario.getEstado() == EstadoUsuario.SANCIONADO) {
            throw new ReglaNegocioException(
                    "El usuario esta SANCIONADO por prestamos en mora; no se puede registrar un nuevo prestamo");
        }

        if (usuario.getRol() == RolUsuario.LECTOR) {
            long activos = prestamoRepository.countByUsuarioIdAndEstado(usuario.getId(), EstadoPrestamo.ACTIVO);
            if (activos >= LIMITE_PRESTAMOS_LECTOR) {
                throw new ReglaNegocioException(
                        "El usuario ya tiene el limite de " + LIMITE_PRESTAMOS_LECTOR + " prestamos activos");
            }
        }

        if (libro.getStockDisponible() <= 0) {
            throw new ReglaNegocioException(
                    "No hay ejemplares disponibles de '" + libro.getTitulo() + "'");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Prestamo prestamo = Prestamo.builder()
                .usuarioId(usuario.getId())
                .libroId(libro.getId())
                .fechaPrestamo(ahora)
                .fechaDevolucionEsperada(ahora.plusDays(DIAS_PRESTAMO))
                .estado(EstadoPrestamo.ACTIVO)
                .build();
        prestamoRepository.save(prestamo);

        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);

        return aResponse(prestamo, usuario, libro);
    }

    @Transactional
    public PrestamoResponseDTO devolver(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prestamo no encontrado con id: " + id));
        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new ReglaNegocioException("El prestamo ya fue devuelto");
        }

        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);

        Libro libro = libroRepository.findById(prestamo.getLibroId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Libro no encontrado con id: " + prestamo.getLibroId()));
        libro.setStockDisponible(Math.min(libro.getStockDisponible() + 1, libro.getStockTotal()));
        libroRepository.save(libro);

        return aResponse(prestamo, null, libro);
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponseDTO> misPrestamos() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        return prestamoRepository.findByUsuarioIdOrderByFechaPrestamoDesc(usuario.getId())
                .stream()
                .map(p -> aResponse(p, usuario, buscarLibro(p.getLibroId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponseDTO> atrasados() {
        List<Prestamo> enMora = prestamoRepository
                .findByEstadoNotAndFechaDevolucionEsperadaBefore(EstadoPrestamo.DEVUELTO, LocalDateTime.now());
        return enMora.stream()
                .map(p -> aResponse(p, buscarUsuario(p.getUsuarioId()), buscarLibro(p.getLibroId())))
                .toList();
    }

    private Libro buscarLibro(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private PrestamoResponseDTO aResponse(Prestamo prestamo, Usuario usuario, Libro libro) {
        return PrestamoResponseDTO.builder()
                .id(prestamo.getId())
                .usuarioId(prestamo.getUsuarioId())
                .usuarioNombre(usuario != null ? usuario.getNombre() : null)
                .usuarioEmail(usuario != null ? usuario.getEmail() : null)
                .libroId(prestamo.getLibroId())
                .libroTitulo(libro != null ? libro.getTitulo() : null)
                .libroIsbn(libro != null ? libro.getIsbn() : null)
                .fechaPrestamo(prestamo.getFechaPrestamo())
                .fechaDevolucionEsperada(prestamo.getFechaDevolucionEsperada())
                .fechaDevolucionReal(prestamo.getFechaDevolucionReal())
                .estado(prestamo.getEstado() != null ? prestamo.getEstado().name() : null)
                .build();
    }
}
