package com.diegogiron.biblioteca_catalogo.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.diegogiron.biblioteca_catalogo.enums.EstadoUsuario;
import com.diegogiron.biblioteca_catalogo.enums.RolUsuario;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Genera, valida y verifica los JWT firmados con HMAC-SHA256
@Component
public class JwtUtils {

    private final SecretKey claveSecreta;
    private final long expirationMs;

    public JwtUtils(@Value("${app.jwt.secret}") String secretoBase64,
                    @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.claveSecreta = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.expirationMs = expirationMs;
    }

    public String generarToken(String email, RolUsuario rol, EstadoUsuario estado) {
        Date ahora = new Date();
        Date vencimiento = new Date(ahora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol.name())
                .claim("estado", estado.name())
                .issuedAt(ahora)
                .expiration(vencimiento)
                .signWith(claveSecreta)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            Jwts.parser().verifyWith(claveSecreta).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String obtenerEmailDelToken(String token) {
        return Jwts.parser()
                .verifyWith(claveSecreta)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
