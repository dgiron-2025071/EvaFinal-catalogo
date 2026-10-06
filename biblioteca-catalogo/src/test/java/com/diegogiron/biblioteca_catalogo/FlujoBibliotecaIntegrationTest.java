package com.diegogiron.biblioteca_catalogo;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// Pruebas de integracion de punta a punta contra PostgreSQL local.
// Cada test crea datos propios con sufijo unico para poder re-ejecutarse.
@SpringBootTest
@AutoConfigureMockMvc
class FlujoBibliotecaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    private String emailLector() {
        return "lector." + sufijo + "@correo.com";
    }

    private String login(String email, String password) throws Exception {
        String cuerpo = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();
        return leer(resultado, "token").asText();
    }

    private String registrarLector() throws Exception {
        String cuerpo = "{\"nombre\":\"Lector Prueba\",\"email\":\"" + emailLector()
                + "\",\"password\":\"Lector123*\"}";
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "token").asText();
    }

    private JsonNode leer(MvcResult resultado, String campo) throws Exception {
        String json = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(json).get(campo);
    }

    private long crearLibro(String tokenAdmin, String titulo, int stockTotal) throws Exception {
        String cuerpo = "{\"isbn\":\"978" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
                + "\",\"titulo\":\"" + titulo + "\",\"autor\":\"Autor Prueba\",\"categoria\":\"Pruebas\","
                + "\"stockTotal\":" + stockTotal + "}";
        MvcResult resultado = mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andReturn();
        return leer(resultado, "id").asLong();
    }

    private int stockDisponible(String tokenAdmin, long libroId) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/libros/" + libroId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        return leer(resultado, "stockDisponible").asInt();
    }

    private void registrarPrestamo(String tokenAutorizado, long usuarioId, long libroId, int esperado)
            throws Exception {
        String cuerpo = "{\"usuarioId\":" + usuarioId + ",\"libroId\":" + libroId + "}";
        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenAutorizado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().is(esperado));
    }

    private long idUsuario(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    @Test
    void autenticacionYAccesoConRoles() throws Exception {
        String tokenAdmin = login("admin@biblioteca.com", "Admin123*");
        String tokenLector = registrarLector();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Duplicado\",\"email\":\"" + emailLector()
                                + "\",\"password\":\"Lector123*\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@biblioteca.com\",\"password\":\"PasswordMala123*\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales invalidas"));

        mockMvc.perform(get("/api/v1/libros")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/libros").header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-x\",\"titulo\":\"No debe crearse\",\"stockTotal\":1}"))
                .andExpect(status().isForbidden());

        long libroId = crearLibro(tokenAdmin, "Libro Autenticacion " + sufijo, 3);
        mockMvc.perform(get("/api/v1/libros/" + libroId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockDisponible").value(3));

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-dup-" + sufijo
                                + "\",\"titulo\":\"Otro\",\"stockTotal\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-dup-" + sufijo
                                + "\",\"titulo\":\"Duplicado\",\"stockTotal\":1}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/libros")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .param("titulo", "Libro Autenticacion " + sufijo)
                        .param("categoria", "Pruebas")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "titulo,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/v1/libros/" + libroId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"978-editado-" + sufijo
                                + "\",\"titulo\":\"Titulo editado\",\"autor\":\"Autor\","
                                + "\"categoria\":\"Pruebas\",\"stockTotal\":4,\"stockDisponible\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Titulo editado"));

        long libroSinPrestamos = crearLibro(tokenAdmin, "Para eliminar " + sufijo, 1);
        mockMvc.perform(delete("/api/v1/libros/" + libroSinPrestamos)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/libros/" + libroSinPrestamos)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void flujoCompletoDePrestamoYDevolucion() throws Exception {
        String tokenAdmin = login("admin@biblioteca.com", "Admin123*");
        String tokenLector = registrarLector();
        long libroId = crearLibro(tokenAdmin, "Circulacion " + sufijo, 5);
        long usuarioId = idUsuario(emailLector());

        registrarPrestamo(tokenAdmin, usuarioId, libroId, 201);
        org.assertj.core.api.Assertions.assertThat(stockDisponible(tokenAdmin, libroId)).isEqualTo(4);

        mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        MvcResult listado = mockMvc.perform(get("/api/v1/prestamos/mis-prestamos")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isOk())
                .andReturn();
        long prestamoId = objectMapper
                .readTree(listado.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get(0).get("id").asLong();

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DEVUELTO"));
        org.assertj.core.api.Assertions.assertThat(stockDisponible(tokenAdmin, libroId)).isEqualTo(5);

        mockMvc.perform(patch("/api/v1/prestamos/" + prestamoId + "/devolucion")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/libros/" + libroId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede eliminar el libro porque tiene prestamos asociados"));
    }

    @Test
    void reglasDeNegocioDeStockLimiteYMora() throws Exception {
        String tokenAdmin = login("admin@biblioteca.com", "Admin123*");

        long libroStock1 = crearLibro(tokenAdmin, "Stock justo " + sufijo, 1);
        String tokenUno = registrarLector();
        String emailDos = "lector.dos." + sufijo + "@correo.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Segundo\",\"email\":\"" + emailDos
                                + "\",\"password\":\"Lector123*\"}"))
                .andExpect(status().isCreated());
        registrarPrestamo(tokenAdmin, idUsuario(emailLector()), libroStock1, 201);
        registrarPrestamo(tokenAdmin, idUsuario(emailDos), libroStock1, 400);
        org.assertj.core.api.Assertions.assertThat(tokenUno).isNotBlank();

        long libroStock4 = crearLibro(tokenAdmin, "Limite tres " + sufijo, 4);
        String emailTres = "lector.tres." + sufijo + "@correo.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tercero\",\"email\":\"" + emailTres
                                + "\",\"password\":\"Lector123*\"}"))
                .andExpect(status().isCreated());
        long usuarioTres = idUsuario(emailTres);
        registrarPrestamo(tokenAdmin, usuarioTres, libroStock4, 201);
        registrarPrestamo(tokenAdmin, usuarioTres, libroStock4, 201);
        registrarPrestamo(tokenAdmin, usuarioTres, libroStock4, 201);
        registrarPrestamo(tokenAdmin, usuarioTres, libroStock4, 400);

        long libroMora = crearLibro(tokenAdmin, "Mora " + sufijo, 2);
        String emailMora = "lector.mora." + sufijo + "@correo.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"En Mora\",\"email\":\"" + emailMora
                                + "\",\"password\":\"Lector123*\"}"))
                .andExpect(status().isCreated());
        long usuarioMora = idUsuario(emailMora);
        jdbcTemplate.update(
                "INSERT INTO prestamo (usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada, estado) "
                        + "VALUES (?, ?, ?, ?, 'ACTIVO')",
                usuarioMora, libroMora, LocalDateTime.now().minusDays(20), LocalDateTime.now().minusDays(6));

        registrarPrestamo(tokenAdmin, usuarioMora, libroMora, 400);
        String estado = jdbcTemplate.queryForObject(
                "SELECT estado FROM usuario WHERE id = ?", String.class, usuarioMora);
        org.assertj.core.api.Assertions.assertThat(estado).isEqualTo("SANCIONADO");

        registrarPrestamo(tokenAdmin, 999999L, libroMora, 404);
        registrarPrestamo(tokenAdmin, usuarioMora, 999999L, 404);
    }

    @Test
    void rolesDePrestamos() throws Exception {
        String tokenAdmin = login("admin@biblioteca.com", "Admin123*");
        String tokenBibliotecario = login("bibliotecario@biblioteca.com", "Biblio123*");
        String tokenLector = registrarLector();
        long libroId = crearLibro(tokenAdmin, "Roles " + sufijo, 5);

        registrarPrestamo(tokenBibliotecario, idUsuario(emailLector()), libroId, 201);

        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenBibliotecario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/v1/prestamos/atrasados")
                        .header("Authorization", "Bearer " + tokenLector))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/prestamos")
                        .header("Authorization", "Bearer " + tokenLector)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":" + libroId + "}"))
                .andExpect(status().isForbidden());
    }
}
