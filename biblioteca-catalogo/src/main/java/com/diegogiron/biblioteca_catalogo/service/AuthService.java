package com.diegogiron.biblioteca_catalogo.service;

import com.diegogiron.biblioteca_catalogo.dto.AuthResponseDTO;
import com.diegogiron.biblioteca_catalogo.dto.LoginRequestDTO;
import com.diegogiron.biblioteca_catalogo.dto.RegisterRequestDTO;
import com.diegogiron.biblioteca_catalogo.entity.Usuario;
import com.diegogiron.biblioteca_catalogo.enums.EstadoUsuario;
import com.diegogiron.biblioteca_catalogo.enums.RolUsuario;
import com.diegogiron.biblioteca_catalogo.exception.EmailYaRegistradoException;
import com.diegogiron.biblioteca_catalogo.repository.UsuarioRepository;
import com.diegogiron.biblioteca_catalogo.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // Registro publico: siempre entra con rol LECTOR y estado ACTIVO
    @Transactional
    public AuthResponseDTO registrar(RegisterRequestDTO dto) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailYaRegistradoException(dto.getEmail());
        }
        Usuario usuario = Usuario.builder()
                .nombre(dto.getNombre())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .estado(EstadoUsuario.ACTIVO)
                .rol(RolUsuario.LECTOR)
                .build();
        usuarioRepository.save(usuario);
        return construirRespuesta(usuario);
    }

    public AuthResponseDTO iniciarSesion(LoginRequestDTO dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + dto.getEmail()));
        return construirRespuesta(usuario);
    }

    private AuthResponseDTO construirRespuesta(Usuario usuario) {
        String token = jwtUtils.generarToken(usuario.getEmail(), usuario.getRol(), usuario.getEstado());
        return AuthResponseDTO.builder()
                .token(token)
                .tipo("Bearer")
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }
}
