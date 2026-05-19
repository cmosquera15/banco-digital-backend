package co.edu.udea.bancodigital.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.udea.bancodigital.dtos.requests.ActualizarDatosRequest;
import co.edu.udea.bancodigital.dtos.requests.RegistroRequest;
import co.edu.udea.bancodigital.dtos.responses.ActualizarDatosResponse;
import co.edu.udea.bancodigital.dtos.responses.RegistroResponse;
import co.edu.udea.bancodigital.exception.DuplicateResourceException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoDocumento;
import co.edu.udea.bancodigital.models.pks.UsuarioId;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.EstadoCuentaRepository;
import co.edu.udea.bancodigital.repositories.TipoCuentaRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.UsuarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TipoCuentaRepository tipoCuentaRepository;

    @Mock
    private EstadoCuentaRepository estadoCuentaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Rol> rolQuery;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CP-REG-01: registro exitoso crea usuario cliente")
    void registrar_deberiaCrearUsuarioCliente() {
        RegistroRequest request = registroRequest("juan@example.com", "1234567890", "Abc123#@");
        TipoDocumento tipoDocumento = tipoDocumento();
        Rol rolCliente = rolCliente();
        TipoCuenta tipoCuenta = tipoCuenta();
        EstadoCuenta estadoActiva = estadoCuentaActiva();

        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioRepository.existsById(new UsuarioId(1, "1234567890"))).thenReturn(false);
        when(entityManager.find(TipoDocumento.class, 1)).thenReturn(tipoDocumento);
        when(entityManager.createQuery(any(String.class), eq(Rol.class))).thenReturn(rolQuery);
        when(rolQuery.setParameter("nombre", "CLIENTE")).thenReturn(rolQuery);
        when(rolQuery.getResultStream()).thenReturn(Stream.of(rolCliente));
        when(passwordEncoder.encode("Abc123#@")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tipoCuentaRepository.findById(1)).thenReturn(Optional.of(tipoCuenta));
        when(estadoCuentaRepository.findByNombreIgnoreCase("ACTIVA")).thenReturn(Optional.of(estadoActiva));

        RegistroResponse response = usuarioService.registrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertEquals(new UsuarioId(1, "1234567890"), guardado.getId());
        assertEquals("juan@example.com", guardado.getCorreo());
        assertEquals("hash", guardado.getContrasena());
        assertEquals("CLIENTE", guardado.getRol().getNombre());
        assertEquals("****7890", response.getNumeroDocumento());
        assertEquals("CLIENTE", response.getRol());

        ArgumentCaptor<Cuenta> cuentaCaptor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository, times(1)).save(cuentaCaptor.capture());
        Cuenta cuentaInicial = cuentaCaptor.getValue();
        assertEquals(guardado, cuentaInicial.getDueno());
        assertEquals(tipoCuenta, cuentaInicial.getTipoCuenta());
        assertEquals(estadoActiva, cuentaInicial.getEstadoCuenta());
        assertEquals(BigDecimal.ZERO, cuentaInicial.getSaldo());

        System.out.println("=== CP-REG-01 RESULTADO OBTENIDO ===");
        System.out.println("Usuario creado: " + guardado.getCorreo());
        System.out.println("Cuenta con saldo: $" + cuentaInicial.getSaldo());
    }

    @Test
    @DisplayName("CP-REG-03: documento duplicado rechaza registro")
    void registrarConDocumentoDuplicado_deberiaLanzarDuplicateResourceException() {
        RegistroRequest request = registroRequest("nuevo@example.com", "1234567890", "Abc123#@");

        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.existsById(new UsuarioId(1, "1234567890"))).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> usuarioService.registrar(request));
        System.out.println("=== CP-REG-03 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + assertThrows(DuplicateResourceException.class, () -> usuarioService.registrar(request)).getMessage());
    }

    @Test
    @DisplayName("CP-REG-04: email duplicado rechaza registro")
    void registrarConEmailDuplicado_deberiaLanzarDuplicateResourceException() {
        RegistroRequest request = registroRequest("existente@example.com", "1234567890", "Abc123#@");

        when(usuarioRepository.existsByCorreo("existente@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> usuarioService.registrar(request));
        System.out.println("=== CP-REG-04 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + assertThrows(DuplicateResourceException.class, () -> usuarioService.registrar(request)).getMessage());
    }

    @Test
    @DisplayName("CP-UPD-01: actualizacion exitosa persiste datos")
    void actualizarMisDatos_deberiaActualizarUsuarioAutenticado() {
        TipoDocumento tipoDocumento = tipoDocumento();
        Rol rolCliente = rolCliente();
        Usuario usuario = Usuario.builder()
                .id(new UsuarioId(1, "1234567890"))
                .tipoDocumento(tipoDocumento)
                .rol(rolCliente)
                .nombre("Juan")
                .primerApellido("Perez")
                .segundoApellido("Gomez")
                .direccion("Calle 123")
                .telefono("3001234567")
                .correo("juan@example.com")
                .contrasena("hash")
                .build();
        ActualizarDatosRequest request = actualizarDatosRequest("Nuevo Nombre", "nuevo@example.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        ActualizarDatosResponse response = usuarioService.actualizarMisDatos(request);

        assertEquals("Nuevo Nombre", response.getNombre());
        assertEquals("nuevo@example.com", response.getCorreo());
        verify(usuarioRepository).save(usuario);

        System.out.println("=== CP-UPD-01 RESULTADO OBTENIDO ===");
        System.out.println("Nombre actualizado: " + response.getNombre());
        System.out.println("Correo actualizado: " + response.getCorreo());
    }

    @Test
    @DisplayName("CP-UPD-05: email duplicado en edicion rechaza actualizacion")
    void actualizarMisDatosConEmailDuplicado_deberiaLanzarDuplicateResourceException() {
        Usuario usuario = Usuario.builder()
                .correo("juan@example.com")
                .build();
        ActualizarDatosRequest request = actualizarDatosRequest("Juan", "otro@example.com");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByCorreo("otro@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> usuarioService.actualizarMisDatos(request));
        System.out.println("=== CP-UPD-05 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + assertThrows(DuplicateResourceException.class, () -> usuarioService.actualizarMisDatos(request)).getMessage());
    }

    @Test
    @DisplayName("CP-UPD-04: actualizacion sin autenticacion rechaza solicitud")
    void actualizarMisDatosSinAutenticacion_deberiaLanzarIllegalArgumentException() {
        SecurityContextHolder.clearContext();
        ActualizarDatosRequest request = actualizarDatosRequest("Juan", "juan@example.com");

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarMisDatos(request));
        System.out.println("=== CP-UPD-04 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarMisDatos(request)).getMessage());
    }

    private static RegistroRequest registroRequest(String correo, String numeroDocumento, String contrasena) {
        RegistroRequest request = new RegistroRequest();
        ReflectionTestUtils.setField(request, "idTipoDoc", 1);
        ReflectionTestUtils.setField(request, "numeroDocumento", numeroDocumento);
        ReflectionTestUtils.setField(request, "nombre", "Juan");
        ReflectionTestUtils.setField(request, "primerApellido", "Perez");
        ReflectionTestUtils.setField(request, "segundoApellido", "Gomez");
        ReflectionTestUtils.setField(request, "direccion", "Calle 123");
        ReflectionTestUtils.setField(request, "telefono", "3001234567");
        ReflectionTestUtils.setField(request, "correo", correo);
        ReflectionTestUtils.setField(request, "contrasena", contrasena);
        return request;
    }

    private static ActualizarDatosRequest actualizarDatosRequest(String nombre, String correo) {
        ActualizarDatosRequest request = new ActualizarDatosRequest();
        ReflectionTestUtils.setField(request, "nombre", nombre);
        ReflectionTestUtils.setField(request, "primerApellido", "Perez");
        ReflectionTestUtils.setField(request, "segundoApellido", "Gomez");
        ReflectionTestUtils.setField(request, "direccion", "Calle 123");
        ReflectionTestUtils.setField(request, "telefono", "3001234567");
        ReflectionTestUtils.setField(request, "correo", correo);
        return request;
    }

    private static TipoDocumento tipoDocumento() {
        TipoDocumento tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(1);
        tipoDocumento.setNombre("CC");
        return tipoDocumento;
    }

    private static Rol rolCliente() {
        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("CLIENTE");
        return rol;
    }

    private static TipoCuenta tipoCuenta() {
        TipoCuenta tipoCuenta = new TipoCuenta();
        tipoCuenta.setId(1);
        tipoCuenta.setNombre("AHORROS");
        return tipoCuenta;
    }

    private static EstadoCuenta estadoCuentaActiva() {
        EstadoCuenta estadoCuenta = new EstadoCuenta();
        estadoCuenta.setId(1);
        estadoCuenta.setNombre("ACTIVA");
        return estadoCuenta;
    }
}
