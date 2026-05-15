package co.edu.udea.bancodigital.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.udea.bancodigital.dtos.requests.TransferenciaRequest;
import co.edu.udea.bancodigital.dtos.responses.TransferenciaResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Transaccion;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoTransaccion;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoTransaccion;
import co.edu.udea.bancodigital.models.pks.UsuarioId;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.EstadoTransaccionRepository;
import co.edu.udea.bancodigital.repositories.TipoTransaccionRepository;
import co.edu.udea.bancodigital.repositories.TransaccionRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.MailService;
import co.edu.udea.bancodigital.services.TransferenciaService;

@ExtendWith(MockitoExtension.class)
class TransferenciaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoTransaccionRepository tipoTransaccionRepository;

    @Mock
    private EstadoTransaccionRepository estadoTransaccionRepository;

    @Mock
    private MailService mailService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferenciaService transferenciaService;

    private Usuario usuario;
    private Usuario usuarioDestino;
    private TipoCuenta tipoCuenta;
    private EstadoCuenta estadoActiva;

    @BeforeEach
    void setUpSecurityContext() {
        usuario = Usuario.builder()
                .id(new UsuarioId(1, "123"))
                .correo("juan@example.com")
                .build();
        usuarioDestino = Usuario.builder()
                .id(new UsuarioId(1, "456"))
                .correo("maria@example.com")
                .build();

        tipoCuenta = new TipoCuenta();
        tipoCuenta.setId(1);
        tipoCuenta.setNombre("Ahorros");

        estadoActiva = new EstadoCuenta();
        estadoActiva.setId(1);
        estadoActiva.setNombre("ACTIVA");

        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        org.mockito.Mockito.lenient().when(authentication.isAuthenticated()).thenReturn(true);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("juan@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("US7: Transferencia exitosa")
    void transferir_deberiaRegistrarTransaccionYActualizarSaldos() {
        UUID cuentaOrigenId = UUID.randomUUID();
        UUID cuentaDestinoId = UUID.randomUUID();
        TransferenciaRequest request = crearRequest(cuentaOrigenId, cuentaDestinoId, new BigDecimal("1000.00"));

        Cuenta cuentaOrigen = crearCuenta(cuentaOrigenId, usuario, new BigDecimal("5000.00"));
        Cuenta cuentaDestino = crearCuenta(cuentaDestinoId, usuarioDestino, new BigDecimal("2000.00"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findAllByIdCuentaInForUpdate(List.of(cuentaOrigenId, cuentaDestinoId)))
                .thenReturn(List.of(cuentaOrigen, cuentaDestino));
        when(tipoTransaccionRepository.findByNombreIgnoreCase("TRANSFERENCIA"))
                .thenReturn(Optional.of(tipoTransaccion()));
        when(estadoTransaccionRepository.findByNombreIgnoreCase("COMPLETADA"))
                .thenReturn(Optional.of(estadoTransaccion()));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion transaccion = invocation.getArgument(0);
            transaccion.setIdTransaccion(UUID.randomUUID());
            return transaccion;
        });

        TransferenciaResponse response = transferenciaService.transferir(request);

        assertNotNull(response.getIdTransaccion());
        assertEquals(new BigDecimal("4000.00"), cuentaOrigen.getSaldo());
        assertEquals(new BigDecimal("3000.00"), cuentaDestino.getSaldo());
        verify(mailService, never()).sendAccountAlert(any(String.class), any(String.class));
    }

    @Test
    @DisplayName("US7: Saldo insuficiente")
    void transferir_deberiaRechazarCuandoSaldoInsuficiente() {
        UUID cuentaOrigenId = UUID.randomUUID();
        UUID cuentaDestinoId = UUID.randomUUID();
        TransferenciaRequest request = crearRequest(cuentaOrigenId, cuentaDestinoId, new BigDecimal("1000.00"));

        Cuenta cuentaOrigen = crearCuenta(cuentaOrigenId, usuario, new BigDecimal("500.00"));
        Cuenta cuentaDestino = crearCuenta(cuentaDestinoId, usuarioDestino, new BigDecimal("2000.00"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findAllByIdCuentaInForUpdate(List.of(cuentaOrigenId, cuentaDestinoId)))
                .thenReturn(List.of(cuentaOrigen, cuentaDestino));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(request));
        assertEquals("Saldo insuficiente para realizar la transferencia", exception.getMessage());
    }

    @Test
    @DisplayName("US7: Cuenta destino inexistente")
    void transferir_deberiaRechazarCuandoCuentaDestinoNoExiste() {
        UUID cuentaOrigenId = UUID.randomUUID();
        UUID cuentaDestinoId = UUID.randomUUID();
        TransferenciaRequest request = crearRequest(cuentaOrigenId, cuentaDestinoId, new BigDecimal("1000.00"));

        Cuenta cuentaOrigen = crearCuenta(cuentaOrigenId, usuario, new BigDecimal("5000.00"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findAllByIdCuentaInForUpdate(List.of(cuentaOrigenId, cuentaDestinoId)))
                .thenReturn(List.of(cuentaOrigen));

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> transferenciaService.transferir(request));
        assertEquals("Cuenta con id " + cuentaDestinoId + " no existe", exception.getMessage());
    }

    @Test
    @DisplayName("US7: Monto invalido")
    void transferir_deberiaRechazarMontoInvalido() {
        UUID cuentaOrigenId = UUID.randomUUID();
        UUID cuentaDestinoId = UUID.randomUUID();
        TransferenciaRequest request = crearRequest(cuentaOrigenId, cuentaDestinoId, new BigDecimal("0"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> transferenciaService.transferir(request));
        assertEquals("El monto debe ser mayor a cero", exception.getMessage());
    }

    @Test
    @DisplayName("US7: Transferencia exitosa con monto alto")
    void transferir_deberiaEnviarAlertaCuandoMontoAlto() {
        UUID cuentaOrigenId = UUID.randomUUID();
        UUID cuentaDestinoId = UUID.randomUUID();
        TransferenciaRequest request = crearRequest(cuentaOrigenId, cuentaDestinoId, new BigDecimal("7000000"));

        Cuenta cuentaOrigen = crearCuenta(cuentaOrigenId, usuario, new BigDecimal("9000000"));
        Cuenta cuentaDestino = crearCuenta(cuentaDestinoId, usuarioDestino, new BigDecimal("2000.00"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findAllByIdCuentaInForUpdate(List.of(cuentaOrigenId, cuentaDestinoId)))
                .thenReturn(List.of(cuentaOrigen, cuentaDestino));
        when(tipoTransaccionRepository.findByNombreIgnoreCase("TRANSFERENCIA"))
                .thenReturn(Optional.of(tipoTransaccion()));
        when(estadoTransaccionRepository.findByNombreIgnoreCase("COMPLETADA"))
                .thenReturn(Optional.of(estadoTransaccion()));
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocation -> {
            Transaccion transaccion = invocation.getArgument(0);
            transaccion.setIdTransaccion(UUID.randomUUID());
            transaccion.setFechaHora(LocalDateTime.now());
            return transaccion;
        });

        transferenciaService.transferir(request);

        ArgumentCaptor<String> mensajeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendAccountAlert(any(String.class), mensajeCaptor.capture());
        String mensaje = mensajeCaptor.getValue();
        String cuentaId = cuentaOrigenId.toString();
        String last4 = cuentaId.substring(cuentaId.length() - 4);
        assertEquals(true, mensaje.contains("****" + last4));
    }

    @Test
    @DisplayName("US7: Usuario sin autenticacion")
    void transferir_deberiaRechazarCuandoUsuarioNoAutenticado() {
        SecurityContextHolder.clearContext();
        TransferenciaRequest request = crearRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000"));

        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> transferenciaService.transferir(request));
    }

    private TransferenciaRequest crearRequest(UUID cuentaOrigen, UUID cuentaDestino, BigDecimal monto) {
        TransferenciaRequest request = new TransferenciaRequest();
        ReflectionTestUtils.setField(request, "cuentaOrigen", cuentaOrigen);
        ReflectionTestUtils.setField(request, "cuentaDestino", cuentaDestino);
        ReflectionTestUtils.setField(request, "monto", monto);
        ReflectionTestUtils.setField(request, "descripcion", "Pago servicio");
        return request;
    }

    private Cuenta crearCuenta(UUID id, Usuario dueno, BigDecimal saldo) {
        return Cuenta.builder()
                .idCuenta(id)
                .dueno(dueno)
                .tipoCuenta(tipoCuenta)
                .estadoCuenta(estadoActiva)
                .saldo(saldo)
                .build();
    }

    private TipoTransaccion tipoTransaccion() {
        TipoTransaccion tipo = new TipoTransaccion();
        tipo.setId(1);
        tipo.setNombre("TRANSFERENCIA");
        return tipo;
    }

    private EstadoTransaccion estadoTransaccion() {
        EstadoTransaccion estado = new EstadoTransaccion();
        estado.setId(1);
        estado.setNombre("COMPLETADA");
        return estado;
    }
}
