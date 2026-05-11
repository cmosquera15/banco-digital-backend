package co.edu.udea.bancodigital.unit.services;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import co.edu.udea.bancodigital.dtos.requests.CrearCuentaRequest;
import co.edu.udea.bancodigital.dtos.responses.ConsultarCuentasResponse;
import co.edu.udea.bancodigital.dtos.responses.ConsultarSaldoResponse;
import co.edu.udea.bancodigital.dtos.responses.CrearCuentaResponse;
import co.edu.udea.bancodigital.dtos.responses.ListarCuentasAdminResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoDocumento;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoCuenta;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.EstadoCuentaRepository;
import co.edu.udea.bancodigital.repositories.TipoCuentaRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.CuentaService;
import co.edu.udea.bancodigital.models.pks.UsuarioId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuentaServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoCuentaRepository tipoCuentaRepository;

    @Mock
    private EstadoCuentaRepository estadoCuentaRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private TipoCuenta tipoCuenta;

    @Mock  
    private EstadoCuenta estadoCuenta;

    @InjectMocks
    private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void setUpSecurityContext() {
        usuario = Usuario.builder().correo("juan@example.com").build();
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        lenient().when(authentication.getName()).thenReturn("juan@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CP-ACC-01: Consulta exitosa de cuenta")
    void consultarMisCuentas_deberiaRetornarCuentasCuandoUsuarioTieneCuentas() {
        TipoCuenta tipoCuentaAhorros = new TipoCuenta();
        tipoCuentaAhorros.setId(1);
        tipoCuentaAhorros.setNombre("Ahorros");
        
        EstadoCuenta estadoActiva = new EstadoCuenta();
        estadoActiva.setId(1);
        estadoActiva.setNombre("ACTIVA");
        
        Cuenta cuenta1 = crearCuentaValida(UUID.randomUUID(), usuario, tipoCuentaAhorros, estadoActiva, new BigDecimal("1000.00"));
        Cuenta cuenta2 = crearCuentaValida(UUID.randomUUID(), usuario, tipoCuentaAhorros, estadoActiva, new BigDecimal("500.00"));
        List<Cuenta> cuentas = List.of(cuenta1, cuenta2);

        when(cuentaRepository.findAllByDuenoCorreo("juan@example.com")).thenReturn(cuentas);

        ConsultarCuentasResponse response = cuentaService.consultarMisCuentas();

        assertEquals(2, response.getTotalCuentas());
        assertEquals(2, response.getCuentas().size());
        
        ConsultarCuentasResponse.DetalleCuenta detalle1 = response.getCuentas().get(0);
        assertEquals(cuenta1.getIdCuenta(), detalle1.getIdCuenta());
        assertEquals(cuenta1.getSaldo(), detalle1.getSaldo());
        assertEquals("Ahorros", detalle1.getTipoCuenta());
        assertEquals("ACTIVA", detalle1.getEstadoCuenta());
        
        ConsultarCuentasResponse.DetalleCuenta detalle2 = response.getCuentas().get(1);
        assertEquals(cuenta2.getIdCuenta(), detalle2.getIdCuenta());
        assertEquals(cuenta2.getSaldo(), detalle2.getSaldo());
        
        System.out.println("=== CP-ACC-01 RESULTADO OBTENIDO ===");
        System.out.println("Total de cuentas: " + response.getTotalCuentas());
        System.out.println("Cuenta 1 - ID: " + detalle1.getIdCuenta() + ", Saldo: " + detalle1.getSaldo());
        System.out.println("Cuenta 2 - ID: " + detalle2.getIdCuenta() + ", Saldo: " + detalle2.getSaldo());
    }

    @Test
    @DisplayName("CP-ACC-03: Usuario sin cuenta bancaria asignada")
    void consultarMisCuentas_deberiaLanzarEntityNotFoundCuandoNoTieneCuentas() {
        when(cuentaRepository.findAllByDuenoCorreo("juan@example.com")).thenReturn(List.of());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> cuentaService.consultarMisCuentas());

        assertEquals("Cuenta no disponible", exception.getMessage());
        System.out.println("=== CP-ACC-03 RESULTADO OBTENIDO ===");
        System.out.println("Mensaje de error: " + exception.getMessage());
    }

    @Test
    @DisplayName("CP-ADMC-01: Consulta exitosa")
    void listarCuentasAdmin_deberiaRetornarTodasLasCuentasActivasConUsuarioYSaldo() {
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(1);
        tipoDocumento.setNombre("CC");

        Rol rolAdmin = new Rol();
        rolAdmin.setId(1);
        rolAdmin.setNombre("ADMIN");

        UsuarioId usuarioId = new UsuarioId(1, "123456789");
        Usuario adminUsuario = Usuario.builder()
                .id(usuarioId)
                .tipoDocumento(tipoDocumento)
                .rol(rolAdmin)
                .nombre("Ana")
                .primerApellido("Perez")
                .segundoApellido("Lopez")
                .correo("admin@example.com")
                .build();

        TipoCuenta tipoCuentaAhorros = new TipoCuenta();
        tipoCuentaAhorros.setId(1);
        tipoCuentaAhorros.setNombre("Ahorros");

        EstadoCuenta estadoActiva = new EstadoCuenta();
        estadoActiva.setId(1);
        estadoActiva.setNombre("ACTIVA");

        Cuenta cuenta1 = crearCuentaValida(UUID.randomUUID(), adminUsuario, tipoCuentaAhorros, estadoActiva, new BigDecimal("1500.00"));
        Cuenta cuenta2 = crearCuentaValida(UUID.randomUUID(), adminUsuario, tipoCuentaAhorros, estadoActiva, new BigDecimal("2500.00"));

        when(cuentaRepository.findAll()).thenReturn(List.of(cuenta1, cuenta2));

        List<ListarCuentasAdminResponse> response = cuentaService.listarCuentasAdmin();

        assertEquals(2, response.size());

        ListarCuentasAdminResponse detalle1 = response.get(0);
        assertEquals(cuenta1.getIdCuenta(), detalle1.getIdCuenta());
        assertEquals("CC", detalle1.getTipoDocumentoDueno());
        assertEquals("123456789", detalle1.getNumeroDocumentoDueno());
        assertEquals("Ana Perez Lopez", detalle1.getNombreCompletoDueno());
        assertEquals(new BigDecimal("1500.00"), detalle1.getSaldo());

        ListarCuentasAdminResponse detalle2 = response.get(1);
        assertEquals(cuenta2.getIdCuenta(), detalle2.getIdCuenta());
        assertEquals(new BigDecimal("2500.00"), detalle2.getSaldo());

        System.out.println("=== CP-ADMC-01 RESULTADO OBTENIDO ===");
        System.out.println("Cuenta 1 - ID: " + detalle1.getIdCuenta() + ", Usuario: " + detalle1.getNombreCompletoDueno() + ", Saldo: " + detalle1.getSaldo());
        System.out.println("Cuenta 2 - ID: " + detalle2.getIdCuenta() + ", Usuario: " + detalle2.getNombreCompletoDueno() + ", Saldo: " + detalle2.getSaldo());
    }

    @Test
    @DisplayName("CP-ADMC-02 Consulta sin cuentas registradas")
    void listarCuentasAdmin_deberiaRetornarListaVaciaCuandoNoHayCuentasRegistradas() {
        when(cuentaRepository.findAll()).thenReturn(List.of());

        List<ListarCuentasAdminResponse> response = cuentaService.listarCuentasAdmin();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        System.out.println("=== CP-ADMC-02 RESULTADO OBTENIDO ===");
        System.out.println("Total de cuentas: " + response.size());
    }

    @Test
    @DisplayName("CP-ACC-02: Consulta de saldo sin sesión")
    void consultarSaldoCuenta_deberiaLanzarNullPointerCuandoNoHaySesionActiva() {
        UUID idCuenta = UUID.randomUUID();
        SecurityContextHolder.clearContext();

        assertThrows(NullPointerException.class, () -> cuentaService.consultarSaldoCuenta(idCuenta));
        System.out.println("=== CP-ACC-02 RESULTADO OBTENIDO ==="); 
        System.out.println("Error: " + assertThrows(NullPointerException.class, () -> cuentaService.consultarSaldoCuenta(idCuenta)).getMessage());
    }

    @Test
    @DisplayName("CP-CSD-01: Consulta exitosa")
    void consultarSaldoCuenta_deberiaRetornarSaldoCuandoCuentaPerteneceAlUsuario() {
        when(estadoCuenta.getNombre()).thenReturn("ACTIVA");
        Cuenta cuenta = crearCuentaValida(UUID.randomUUID(), usuario, null, estadoCuenta, new BigDecimal("1234.56"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(cuenta.getIdCuenta())).thenReturn(Optional.of(cuenta));

        ConsultarSaldoResponse response = cuentaService.consultarSaldoCuenta(cuenta.getIdCuenta());

        assertEquals(cuenta.getIdCuenta(), response.getIdCuenta());
        assertEquals(cuenta.getSaldo(), response.getSaldo());
        assertNotNull(response.getConsultedAt());
        System.out.println("=== CP-CSD-01 RESULTADO OBTENIDO ===");
        System.out.println("Saldo consultado: " + response.getSaldo()); 
    }

    @Test
    @DisplayName("CP-CSD-02: Cuenta sin movimientos")
    void consultarSaldoCuenta_deberiaRetornarSaldoCeroCuandoCuentaSinMovimientos() {
        when(estadoCuenta.getNombre()).thenReturn("ACTIVA");
        Cuenta cuenta = crearCuentaValida(UUID.randomUUID(), usuario, null, estadoCuenta, new BigDecimal("0.00"));

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(cuenta.getIdCuenta())).thenReturn(Optional.of(cuenta));

        ConsultarSaldoResponse response = cuentaService.consultarSaldoCuenta(cuenta.getIdCuenta());

        assertEquals(cuenta.getIdCuenta(), response.getIdCuenta());
        assertEquals(cuenta.getSaldo(), response.getSaldo());
        assertNotNull(response.getConsultedAt());
        System.out.println("=== CP-CSD-02 RESULTADO OBTENIDO ===");
        System.out.println("Saldo consultado: " + response.getSaldo());
    }

    @Test
    @DisplayName("CP-CSD-03: Usuario sin autenticación")
    void consultarSaldoCuenta_deberiaLanzarEntityNotFoundExceptionCuandoNoEncuentraDueno() {
        UUID idCuenta = UUID.randomUUID();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(cuentaRepository.findByIdCuentaConDueno(idCuenta)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> cuentaService.consultarSaldoCuenta(idCuenta));
        System.out.println("=== CP-CSD-03 RESULTADO OBTENIDO ==="); 
        System.out.println("Error: " + assertThrows(EntityNotFoundException.class, () -> cuentaService.consultarSaldoCuenta(idCuenta)).getMessage());   
    }

    @Test
    @DisplayName("Crear cuenta satisfactoriamente")
    void crearCuenta_deberiaCrearCuentaConDatosValidos() throws Exception {
        CrearCuentaRequest request = crearRequestConTipoCuenta(1);
        TipoCuenta tipoCuenta1 = new TipoCuenta();
        tipoCuenta1.setId(1);
        tipoCuenta1.setNombre("Ahorros");
        EstadoCuenta estado = new EstadoCuenta();
        estado.setId(1);
        estado.setNombre("ACTIVA");
        Cuenta guardada = crearCuentaValida(UUID.randomUUID(), usuario, tipoCuenta1, estado, BigDecimal.ZERO);
        guardada.setCreatedAt(LocalDateTime.now());

        when(authentication.getName()).thenReturn("juan@example.com");
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(tipoCuentaRepository.findById(1)).thenReturn(Optional.of(tipoCuenta1));
        when(estadoCuentaRepository.findByNombreIgnoreCase("ACTIVA")).thenReturn(Optional.of(estado));
        when(cuentaRepository.save(org.mockito.Mockito.any(Cuenta.class))).thenReturn(guardada);

        CrearCuentaResponse response = cuentaService.crearCuenta(request);

        assertEquals(guardada.getIdCuenta(), response.getIdCuenta());
        assertEquals(tipoCuenta1.getNombre(), response.getTipoCuenta());
        assertEquals(estado.getNombre(), response.getEstadoCuenta());
        assertEquals(BigDecimal.ZERO, response.getSaldo());
        assertNotNull(response.getCreatedAt());
    }

    private static Cuenta crearCuentaValida(UUID idCuenta, Usuario dueno, TipoCuenta tipoCuenta, EstadoCuenta estadoCuenta, BigDecimal saldo) {
        return Cuenta.builder()
                .idCuenta(idCuenta)
                .dueno(dueno)
                .tipoCuenta(tipoCuenta)
                .estadoCuenta(estadoCuenta)
                .saldo(saldo)
                .build();
    }

    private static CrearCuentaRequest crearRequestConTipoCuenta(int idTipoCuenta) throws Exception {
        CrearCuentaRequest request = new CrearCuentaRequest();
        java.lang.reflect.Field field = CrearCuentaRequest.class.getDeclaredField("idTipoCuenta");
        field.setAccessible(true);
        field.set(request, idTipoCuenta);
        return request;
    }
}
