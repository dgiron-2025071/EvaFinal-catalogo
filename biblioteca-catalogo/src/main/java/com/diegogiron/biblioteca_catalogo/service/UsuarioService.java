package com.diegogiron.biblioteca_catalogo.service;

import com.diegogiron.biblioteca_catalogo.entity.Usuario;
import com.diegogiron.biblioteca_catalogo.enums.EstadoPrestamo;
import com.diegogiron.biblioteca_catalogo.enums.EstadoUsuario;
import com.diegogiron.biblioteca_catalogo.repository.PrestamoRepository;
import com.diegogiron.biblioteca_catalogo.repository.UsuarioRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;

    // Transaccion propia (REQUIRES_NEW): la sancion persiste aunque la operacion
    // que la detecta se revierta por una regla de negocio
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sancionarSiTieneMora(Usuario usuario) {
        boolean enMora = prestamoRepository.existsByUsuarioIdAndEstadoNotAndFechaDevolucionEsperadaBefore(
                usuario.getId(), EstadoPrestamo.DEVUELTO, LocalDateTime.now());
        if (enMora && usuario.getEstado() != EstadoUsuario.SANCIONADO) {
            usuario.setEstado(EstadoUsuario.SANCIONADO);
            usuarioRepository.save(usuario);
        }
        return enMora;
    }
}
