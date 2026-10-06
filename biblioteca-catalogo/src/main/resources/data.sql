-- Carga inicial: usuarios de prueba, catálogo y préstamos de ejemplo.
-- Credenciales (encriptadas con BCrypt): admin@biblioteca.com / Admin123*
--                                        bibliotecario@biblioteca.com / Biblio123*
--                                        lector@biblioteca.com / Lector123*
-- Los INSERT usan ON CONFLICT DO NOTHING para que el script pueda re-ejecutarse en cada arranque.

INSERT INTO usuario (id, nombre, email, password, estado, rol) VALUES
    (1, 'Admin Biblioteca', 'admin@biblioteca.com', '$2a$10$ewpk60X5tr/f9CUTXNUq7e15udYogbA6lojtl.sXBxa3Gj2UCbMFS', 'ACTIVO', 'ADMIN'),
    (2, 'Bibliotecario Principal', 'bibliotecario@biblioteca.com', '$2a$10$raNCti0s9g7lWGwlYQq0ve0uRgZ5XBOsdFL6Hq9V7.kwy364aAA5W', 'ACTIVO', 'BIBLIOTECARIO'),
    (3, 'Lector Ejemplo', 'lector@biblioteca.com', '$2a$10$RHjCgrHrqvOlcD7OlTk5s.BjqsJAnFfUWENPikAeYW8XapTQ4Bnze', 'ACTIVO', 'LECTOR')
ON CONFLICT DO NOTHING;

INSERT INTO libro (id, isbn, titulo, autor, categoria, stock_total, stock_disponible) VALUES
    (1, '978-84-376-0494-7', 'Cien años de soledad', 'Gabriel García Márquez', 'Literatura', 5, 5),
    (2, '978-0-13-235088-4', 'Clean Code', 'Robert C. Martin', 'Programación', 4, 3),
    (3, '978-0-13-468599-1', 'Effective Java', 'Joshua Bloch', 'Programación', 3, 3),
    (4, '978-84-206-3303-7', 'Cálculo diferencial', 'Luis Díaz Alva', 'Matemáticas', 6, 6),
    (5, '978-84-306-0576-7', 'Sapiens', 'Yuval Noah Harari', 'Historia', 2, 0),
    (6, '978-84-89662-82-7', 'Breve historia del tiempo', 'Stephen Hawking', 'Ciencias', 4, 4)
ON CONFLICT DO NOTHING;

-- Préstamo 1: activo y vigente (libro 2 -> stock_disponible ya descontado)
-- Préstamo 2: vencido hace 6 días (estado ATRASADO, libro 5 -> agotado)
-- Préstamo 3: ya devuelto (libro 1 -> stock repuesto)
INSERT INTO prestamo (id, usuario_id, libro_id, fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado) VALUES
    (1, 3, 2, NOW() - INTERVAL '2 days', NOW() + INTERVAL '12 days', NULL, 'ACTIVO'),
    (2, 3, 5, NOW() - INTERVAL '20 days', NOW() - INTERVAL '6 days', NULL, 'ATRASADO'),
    (3, 3, 1, NOW() - INTERVAL '30 days', NOW() - INTERVAL '16 days', NOW() - INTERVAL '18 days', 'DEVUELTO')
ON CONFLICT DO NOTHING;

-- Re-sincroniza las secuencias tras insertar IDs explícitos
SELECT setval(pg_get_serial_sequence('usuario', 'id'), COALESCE((SELECT MAX(id) FROM usuario), 1));
SELECT setval(pg_get_serial_sequence('libro', 'id'), COALESCE((SELECT MAX(id) FROM libro), 1));
SELECT setval(pg_get_serial_sequence('prestamo', 'id'), COALESCE((SELECT MAX(id) FROM prestamo), 1));
