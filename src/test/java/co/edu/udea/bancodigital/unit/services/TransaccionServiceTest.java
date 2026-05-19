package co.edu.udea.bancodigital.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import co.edu.udea.bancodigital.dtos.responses.DetalleTransaccionResponse;
import co.edu.udea.bancodigital.dtos.responses.HistorialTransaccionResponse;
import co.edu.udea.bancodigital.dtos.responses.HistorialTransaccionesResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Transaccion;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoTransaccion;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoDocumento;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoTransaccion;
import co.edu.udea.bancodigital.models.pks.UsuarioId;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.TransaccionRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.TransaccionService;

@ExtendWith(MockitoExtension.class)
class TransaccionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TransaccionService transaccionService;

    private static final UUID CUENTA_ID = UUID.randomUUID();
    private static final UUID OTRA_CUENTA_ID = UUID.randomUUID();
    private static final UUID TRANSACCION_ID = UUID.randomUUID();
    private static final String CORREO_USUARIO = "juan@example.com";
    private static final String CORREO_OTRO_USUARIO = "otro@example.com";

    // ==================== SETUP TESTS ====================

    @Test
    @DisplayName("HU11-001: Consultar historial completo de transacciones")
    void consultarHistorialCompleto_deberiaRetornarTodasLasTransacciones() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);
        Transaccion transaccion = crearTransaccion(TRANSACCION_ID, cuenta, cuenta);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaccion> page = new PageImpl<>(List.of(transaccion), pageable, 1);
        when(transaccionRepository.findHistorialByCuenta(CUENTA_ID, pageable)).thenReturn(page);

        HistorialTransaccionesResponse response = transaccionService.consultarHistorial(CUENTA_ID, null, null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertEquals(1, response.getTransacciones().size());
        assertTrue(response.isTieneDatos());
        System.out.println("=== HU11-001 RESULTADO OBTENIDO ===");
        System.out.println("Total de transacciones: " + response.getTotal());
        System.out.println("Transacciones en página: " + response.getTransacciones().size());
    }

    @Test
    @DisplayName("HU11-002: Consultar historial sin transacciones")
    void consultarHistorialSinTransacciones_deberiaRetornarMensaje() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaccion> page = new PageImpl<>(List.of(), pageable, 0);
        when(transaccionRepository.findHistorialByCuenta(CUENTA_ID, pageable)).thenReturn(page);

        HistorialTransaccionesResponse response = transaccionService.consultarHistorial(CUENTA_ID, null, null, pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertTrue(response.getTransacciones().isEmpty());
        assertFalse(response.isTieneDatos());
        assertEquals("No existen transacciones para esta cuenta", response.getMensaje());
        System.out.println("=== HU11-002 RESULTADO OBTENIDO ===");
        System.out.println("Mensaje: " + response.getMensaje());
        System.out.println("Tiene datos: " + response.isTieneDatos());
    }

    @Test
    @DisplayName("HU13-001: Filtrar historial por rango de fechas válido")
    void consultarHistorialConFiltroFechas_deberiaRetornarTransaccionesEnRango() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);
        Transaccion transaccion = crearTransaccion(TRANSACCION_ID, cuenta, cuenta);

        LocalDate fechaInicio = LocalDate.of(2024, 1, 1);
        LocalDate fechaFin = LocalDate.of(2024, 12, 31);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaccion> page = new PageImpl<>(List.of(transaccion), pageable, 1);
        when(transaccionRepository.findHistorialByCuentaAndFechaHoraBetween(
                eq(CUENTA_ID),
                any(),
                any(),
                eq(pageable)))
                .thenReturn(page);

        HistorialTransaccionesResponse response = transaccionService.consultarHistorial(CUENTA_ID, fechaInicio, fechaFin, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotal());
        assertTrue(response.isTieneDatos());
        System.out.println("=== HU13-001 RESULTADO OBTENIDO ===");
        System.out.println("Transacciones en rango: " + response.getTotal());
        System.out.println("Desde: " + fechaInicio + " hasta: " + fechaFin);
    }

    @Test
    @DisplayName("HU13-002: Rango de fechas sin transacciones")
    void consultarHistorialConFiltroFechaSinResultados_deberiaRetornarMensaje() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);

        LocalDate fechaInicio = LocalDate.of(2020, 1, 1);
        LocalDate fechaFin = LocalDate.of(2020, 12, 31);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);
        Page<Transaccion> page = new PageImpl<>(List.of(), pageable, 0);
        when(transaccionRepository.findHistorialByCuentaAndFechaHoraBetween(
                eq(CUENTA_ID),
                any(),
                any(),
                eq(pageable)))
                .thenReturn(page);

        HistorialTransaccionesResponse response = transaccionService.consultarHistorial(CUENTA_ID, fechaInicio, fechaFin, pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotal());
        assertFalse(response.isTieneDatos());
        assertEquals("No existen transacciones para esta cuenta", response.getMensaje());
        System.out.println("=== HU13-002 RESULTADO OBTENIDO ===");
        System.out.println("Rango sin datos: " + fechaInicio + " a " + fechaFin);
        System.out.println("Mensaje: " + response.getMensaje());
    }

    @Test
    @DisplayName("HU13-003: Validación - fecha inicio posterior a fecha fin")
    void consultarHistorialConFechaInicioPosterior_deberiaLanzarExcepcion() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);

        LocalDate fechaInicio = LocalDate.of(2024, 12, 31);
        LocalDate fechaFin = LocalDate.of(2024, 1, 1);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transaccionService.consultarHistorial(CUENTA_ID, fechaInicio, fechaFin, pageable));

        assertEquals("La fecha inicio no puede ser posterior a la fecha fin", exception.getMessage());
        System.out.println("=== HU13-003 RESULTADO OBTENIDO ===");
        System.out.println("Error validado: " + exception.getMessage());
    }

    @Test
    @DisplayName("HU13-004: Validación - solo fecha inicio sin fecha fin")
    void consultarHistorialConSoloFechaInicio_deberiaLanzarExcepcion() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Cuenta cuenta = crearCuenta(CUENTA_ID, usuario);

        LocalDate fechaInicio = LocalDate.of(2024, 1, 1);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuenta));

        Pageable pageable = PageRequest.of(0, 20);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transaccionService.consultarHistorial(CUENTA_ID, fechaInicio, null, pageable));

        assertEquals("Debe enviar fechaInicio y fechaFin para filtrar por rango", exception.getMessage());
        System.out.println("=== HU13-004 RESULTADO OBTENIDO ===");
        System.out.println("Error validado: " + exception.getMessage());
    }

    @Test
    @DisplayName("HU11-003: Usuario accede a cuenta ajena")
    void consultarHistorialDeOtraCuenta_deberiaLanzarAccessDeniedException() {
        Usuario usuarioAutenticado = crearUsuarioConId(CORREO_USUARIO, new UsuarioId(1, "1234567890"));
        Usuario usuarioPropietario = crearUsuarioConId(CORREO_OTRO_USUARIO, new UsuarioId(2, "0987654321"));
        Cuenta cuentaAjena = crearCuenta(CUENTA_ID, usuarioPropietario);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuarioAutenticado));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.of(cuentaAjena));

        Pageable pageable = PageRequest.of(0, 20);

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transaccionService.consultarHistorial(CUENTA_ID, null, null, pageable));

        assertEquals("No tiene permisos para consultar esta cuenta", exception.getMessage());
        System.out.println("=== HU11-003 RESULTADO OBTENIDO ===");
        System.out.println("Error de acceso: " + exception.getMessage());
    }

    @Test
    @DisplayName("HU11-004: Cuenta inexistente")
    void consultarHistorialDeCuentaInexistente_deberiaLanzarEntityNotFoundException() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(CUENTA_ID)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> transaccionService.consultarHistorial(CUENTA_ID, null, null, pageable));

        assertTrue(exception.getMessage().contains("no existe"));
        System.out.println("=== HU11-004 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
    }

    // ==================== DETALLE DE TRANSACCION ====================

    @Test
    @DisplayName("HU12-001: Consultar detalle de transacción propia (como origen)")
    void consultarDetalleTransaccionPropia_deberiaRetornarDetalleCompleto() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Usuario usuarioDestino = crearUsuario("destino@example.com");
        Cuenta cuentaOrigen = crearCuenta(CUENTA_ID, usuario);
        Cuenta cuentaDestino = crearCuenta(OTRA_CUENTA_ID, usuarioDestino);
        Transaccion transaccion = crearTransaccion(TRANSACCION_ID, cuentaOrigen, cuentaDestino);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.findDetalleById(TRANSACCION_ID)).thenReturn(Optional.of(transaccion));

        DetalleTransaccionResponse response = transaccionService.consultarDetalle(TRANSACCION_ID);

        assertNotNull(response);
        assertEquals(TRANSACCION_ID, response.getIdTransaccion());
        assertEquals(CUENTA_ID, response.getCuentaOrigen());
        assertEquals(OTRA_CUENTA_ID, response.getCuentaDestino());
        assertNotNull(response.getMonto());
        assertNotNull(response.getFechaHora());
        System.out.println("=== HU12-001 RESULTADO OBTENIDO ===");
        System.out.println("ID Transacción: " + response.getIdTransaccion());
        System.out.println("Monto: " + response.getMonto());
        System.out.println("Tipo: " + response.getTipoTransaccion());
    }

    @Test
    @DisplayName("HU12-002: Consultar detalle de transacción propia (como destino)")
    void consultarDetalleTransaccionComoDestino_deberiaRetornarDetalleCompleto() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);
        Usuario usuarioOrigen = crearUsuario("origen@example.com");
        Cuenta cuentaOrigen = crearCuenta(CUENTA_ID, usuarioOrigen);
        Cuenta cuentaDestino = crearCuenta(OTRA_CUENTA_ID, usuario);
        Transaccion transaccion = crearTransaccion(TRANSACCION_ID, cuentaOrigen, cuentaDestino);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.findDetalleById(TRANSACCION_ID)).thenReturn(Optional.of(transaccion));

        DetalleTransaccionResponse response = transaccionService.consultarDetalle(TRANSACCION_ID);

        assertNotNull(response);
        assertEquals(TRANSACCION_ID, response.getIdTransaccion());
        System.out.println("=== HU12-002 RESULTADO OBTENIDO ===");
        System.out.println("Usuario accede como destino: " + CORREO_USUARIO);
        System.out.println("Detalle consultado exitosamente");
    }

    @Test
    @DisplayName("HU12-003: Usuario intenta acceder a transacción ajena")
    void consultarDetalleTransaccionAjena_deberiaLanzarAccessDeniedException() {
        Usuario usuarioAutenticado = crearUsuarioConId(CORREO_USUARIO, new UsuarioId(1, "1234567890"));
        Usuario usuarioOrigen = crearUsuarioConId("origen@example.com", new UsuarioId(2, "0987654321"));
        Usuario usuarioDestino = crearUsuarioConId("destino@example.com", new UsuarioId(3, "5555555555"));
        Cuenta cuentaOrigen = crearCuenta(CUENTA_ID, usuarioOrigen);
        Cuenta cuentaDestino = crearCuenta(OTRA_CUENTA_ID, usuarioDestino);
        Transaccion transaccion = crearTransaccion(TRANSACCION_ID, cuentaOrigen, cuentaDestino);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuarioAutenticado));
        when(transaccionRepository.findDetalleById(TRANSACCION_ID)).thenReturn(Optional.of(transaccion));

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transaccionService.consultarDetalle(TRANSACCION_ID));

        assertEquals("No tiene permisos para consultar esta transaccion", exception.getMessage());
        System.out.println("=== HU12-003 RESULTADO OBTENIDO ===");
        System.out.println("Error de acceso: " + exception.getMessage());
    }

    @Test
    @DisplayName("HU12-004: Transacción inexistente")
    void consultarDetalleTransaccionInexistente_deberiaLanzarEntityNotFoundException() {
        Usuario usuario = crearUsuario(CORREO_USUARIO);

        configureSecurityContext(CORREO_USUARIO);
        when(usuarioRepository.findByCorreo(CORREO_USUARIO)).thenReturn(Optional.of(usuario));
        when(transaccionRepository.findDetalleById(TRANSACCION_ID)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> transaccionService.consultarDetalle(TRANSACCION_ID));

        assertTrue(exception.getMessage().contains("no existe"));
        System.out.println("=== HU12-004 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private void configureSecurityContext(String correo) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                correo, null, List.of());
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private Usuario crearUsuario(String correo) {
        UsuarioId id = new UsuarioId(1, "1234567890");
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(1);
        tipoDocumento.setNombre("CC");

        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("CLIENTE");

        return Usuario.builder()
                .id(id)
                .tipoDocumento(tipoDocumento)
                .rol(rol)
                .nombre("Juan")
                .primerApellido("Perez")
                .correo(correo)
                .build();
    }

    private Usuario crearUsuarioConId(String correo, UsuarioId id) {
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(1);
        tipoDocumento.setNombre("CC");

        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("CLIENTE");

        return Usuario.builder()
                .id(id)
                .tipoDocumento(tipoDocumento)
                .rol(rol)
                .nombre("Usuario")
                .primerApellido("Prueba")
                .correo(correo)
                .build();
    }

    private Cuenta crearCuenta(UUID idCuenta, Usuario usuario) {
        TipoCuenta tipoCuenta = new TipoCuenta();
        tipoCuenta.setId(1);
        tipoCuenta.setNombre("AHORROS");

        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setId(1);
        estadoCuenta.setNombre("ACTIVA");

        return Cuenta.builder()
                .idCuenta(idCuenta)
                .dueno(usuario)
                .tipoCuenta(tipoCuenta)
                .estadoCuenta(estadoCuenta)
                .saldo(new BigDecimal("1000.00"))
                .build();
    }

    private Transaccion crearTransaccion(UUID idTransaccion, Cuenta origen, Cuenta destino) {
        TipoTransaccion tipoTransaccion = new TipoTransaccion();
        tipoTransaccion.setId(1);
        tipoTransaccion.setNombre("TRANSFERENCIA");

        EstadoTransaccion estadoTransaccion = new EstadoTransaccion();
        estadoTransaccion.setId(1);
        estadoTransaccion.setNombre("EXITOSA");

        return Transaccion.builder()
                .idTransaccion(idTransaccion)
                .cuentaOrigen(origen)
                .cuentaDestino(destino)
                .tipo(tipoTransaccion)
                .estado(estadoTransaccion)
                .monto(new BigDecimal("500.00"))
                .fechaHora(LocalDateTime.now())
                .descripcion("Transferencia de prueba")
                .build();
    }
}
