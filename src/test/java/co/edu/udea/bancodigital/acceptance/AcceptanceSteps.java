package co.edu.udea.bancodigital.acceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.udea.bancodigital.config.security.JwtService;
import co.edu.udea.bancodigital.dtos.requests.ActualizarDatosRequest;
import co.edu.udea.bancodigital.dtos.requests.LoginRequest;
import co.edu.udea.bancodigital.dtos.requests.RegistroRequest;
import co.edu.udea.bancodigital.dtos.responses.ActualizarDatosResponse;
import co.edu.udea.bancodigital.dtos.responses.ConsultarCuentasResponse;
import co.edu.udea.bancodigital.dtos.responses.RegistroResponse;
import co.edu.udea.bancodigital.exception.DuplicateResourceException;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
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
import co.edu.udea.bancodigital.services.AuthService;
import co.edu.udea.bancodigital.services.CuentaService;
import co.edu.udea.bancodigital.services.UsuarioService;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class AcceptanceSteps {

    private UsuarioRepository usuarioRepository;
    private CuentaRepository cuentaRepository;
    private TipoCuentaRepository tipoCuentaRepository;
    private EstadoCuentaRepository estadoCuentaRepository;
    private PasswordEncoder passwordEncoder;
    private EntityManager entityManager;
    private TypedQuery<Rol> rolQuery;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private UsuarioService usuarioService;
    private AuthService authService;
    private CuentaService cuentaService;
    private RegistroRequest registroRequest;
    private ActualizarDatosRequest actualizarRequest;
    private LoginRequest loginRequest;
    private Object resultado;
    private Exception excepcion;
    private Usuario usuario;
    private TipoDocumento tipoDocumento;
    private Rol rolCliente;
    private TipoCuenta tipoCuenta;
    private EstadoCuenta estadoCuenta;
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Before
    public void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        cuentaRepository = mock(CuentaRepository.class);
        tipoCuentaRepository = mock(TipoCuentaRepository.class);
        estadoCuentaRepository = mock(EstadoCuentaRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        entityManager = mock(EntityManager.class);
        rolQuery = mock(TypedQuery.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        usuarioService = new UsuarioService(usuarioRepository, cuentaRepository, tipoCuentaRepository,
                estadoCuentaRepository, passwordEncoder, entityManager);
        authService = new AuthService(authenticationManager, usuarioRepository, jwtService);
        cuentaService = new CuentaService(cuentaRepository, usuarioRepository, tipoCuentaRepository,
                estadoCuentaRepository);
        tipoDocumento = tipoDocumento();
        rolCliente = rolCliente();
        tipoCuenta = tipoCuenta();
        estadoCuenta = estadoCuentaActiva();
        resultado = null;
        excepcion = null;
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Given("que el usuario no esta registrado en el sistema")
    public void usuarioNoRegistrado() {
        registroRequest = registroRequest("juan@example.com", "1234567890", "Abc123#@");
        when(usuarioRepository.existsByCorreo("juan@example.com")).thenReturn(false);
        when(usuarioRepository.existsById(new UsuarioId(1, "1234567890"))).thenReturn(false);
        stubCatalogosRegistro();
    }

    @And("ingresa sus datos personales completos y validos")
    public void datosValidos() {
        when(passwordEncoder.encode("Abc123#@")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @And("omite el correo obligatorio")
    public void omiteCorreo() {
        registroRequest = registroRequest("juan@example.com", "1234567890", "Abc123#@");
        ReflectionTestUtils.setField(registroRequest, "correo", "");
    }

    @Given("que ya existe un cliente registrado con el mismo numero de documento")
    public void documentoDuplicado() {
        registroRequest = registroRequest("nuevo@example.com", "1234567890", "Abc123#@");
        when(usuarioRepository.existsByCorreo("nuevo@example.com")).thenReturn(false);
        when(usuarioRepository.existsById(new UsuarioId(1, "1234567890"))).thenReturn(true);
    }

    @Given("que ya existe un cliente registrado con el mismo correo electronico")
    public void correoDuplicado() {
        registroRequest = registroRequest("existente@example.com", "1234567890", "Abc123#@");
        when(usuarioRepository.existsByCorreo("existente@example.com")).thenReturn(true);
    }

    @And("el email no tiene formato valido")
    public void emailInvalido() {
        registroRequest = registroRequest("juan.gmail.com", "1234567890", "Abc123#@");
    }

    @And("ingresa la contrasena {string}")
    public void ingresaContrasena(String contrasena) {
        registroRequest = registroRequest("juan@example.com", "1234567890", contrasena);
    }

    @When("envia la solicitud de registro")
    public void enviaRegistro() {
        if (!validator.validate(registroRequest).isEmpty()) {
            excepcion = new IllegalArgumentException("Error de validacion");
            return;
        }
        try {
            resultado = usuarioService.registrar(registroRequest);
        } catch (Exception ex) {
            excepcion = ex;
        }
    }

    @Then("puede ver un mensaje de registro exitoso")
    public void registroExitoso() {
        assertNotNull(resultado);
        assertEquals("juan@example.com", ((RegistroResponse) resultado).getCorreo());
        Mockito.verify(cuentaRepository).save(any(Cuenta.class));
    }

    @Then("puede ver un mensaje indicando que faltan datos obligatorios")
    public void faltanDatos() {
        assertTrue(excepcion instanceof IllegalArgumentException);
    }

    @Then("puede ver un mensaje indicando que el documento ya esta registrado")
    public void documentoYaRegistrado() {
        assertTrue(excepcion instanceof DuplicateResourceException);
    }

    @Then("puede ver un mensaje indicando que el correo ya esta registrado")
    public void correoYaRegistrado() {
        assertTrue(excepcion instanceof DuplicateResourceException);
    }

    @Then("puede ver un mensaje indicando que el correo no tiene un formato valido")
    public void correoFormatoInvalido() {
        assertTrue(excepcion instanceof IllegalArgumentException);
    }

    @Then("puede ver un mensaje indicando que la contrasena es invalida")
    public void contrasenaInvalida() {
        assertTrue(excepcion instanceof IllegalArgumentException);
    }

    @Given("que el cliente esta registrado en el sistema")
    public void clienteRegistrado() {
        usuario = Usuario.builder()
                .correo("juan@example.com")
                .nombre("Juan")
                .rol(rolCliente)
                .intentosFallidos(0)
                .build();
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        loginRequest = loginRequest("juan@example.com", "Abc123#@");
    }

    @And("tiene credenciales validas")
    public void credencialesValidas() {
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");
    }

    @Given("que el cliente tiene {int} intento fallido previo")
    public void intentosPrevios(int intentos) {
        usuario = Usuario.builder()
                .correo("juan@example.com")
                .rol(rolCliente)
                .intentosFallidos(intentos)
                .build();
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        loginRequest = loginRequest("juan@example.com", "incorrecta");
    }

    @Given("que el cliente tiene {int} intentos fallidos consecutivos previos")
    public void intentosPreviosPlural(int intentos) {
        intentosPrevios(intentos);
    }

    @Given("que el cliente tuvo 3 intentos fallidos consecutivos")
    public void clienteBloqueado() {
        usuario = Usuario.builder()
                .correo("juan@example.com")
                .rol(rolCliente)
                .intentosFallidos(3)
                .bloqueadoHasta(LocalDateTime.now().minusMinutes(1))
                .build();
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        loginRequest = loginRequest("juan@example.com", "Abc123#@");
    }

    @And("han pasado 5 minutos desde el bloqueo")
    public void pasaronCincoMinutos() {
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");
    }

    @Given("que el email ingresado no esta registrado")
    public void emailNoRegistrado() {
        loginRequest = loginRequest("noexiste@example.com", "Abc123#@");
        when(usuarioRepository.findByCorreo("noexiste@example.com")).thenReturn(Optional.empty());
    }

    @When("ingresa su email y contrasena correctos")
    public void loginCorrecto() {
        ejecutarLogin();
    }

    @When("ingresa credenciales incorrectas")
    public void credencialesIncorrectas() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));
        ejecutarLogin();
    }

    @When("ingresa credenciales incorrectas por tercera vez")
    public void tercerIntentoIncorrecto() {
        credencialesIncorrectas();
    }

    @When("intenta iniciar sesion")
    public void intentaIniciarSesion() {
        ejecutarLogin();
    }

    @Then("el sistema autentica al cliente")
    public void sistemaAutentica() {
        assertNotNull(resultado);
    }

    @And("permite acceso a los servicios")
    public void permiteAcceso() {
        assertEquals("jwt-token", ((co.edu.udea.bancodigital.dtos.responses.LoginResponse) resultado).getToken());
    }

    @Then("puede ver un mensaje {string}")
    public void puedeVerMensaje(String mensaje) {
        assertNotNull(excepcion);
    }

    @Then("el sistema bloquea el acceso del cliente")
    public void sistemaBloquea() {
        assertTrue(excepcion instanceof LockedException);
        assertNotNull(usuario.getBloqueadoHasta());
    }

    @Then("puede acceder nuevamente a su cuenta")
    public void accedeNuevamente() {
        assertNotNull(resultado);
        assertEquals(0, usuario.getIntentosFallidos());
    }

    @Then("puede ver un mensaje Usuario no encontrado")
    public void usuarioNoEncontrado() {
        assertTrue(excepcion instanceof EntityNotFoundException);
    }

    @Given("que el administrador esta autenticado en el sistema")
    public void adminAutenticado() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin@example.com");
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        
        Rol rolAdmin = new Rol();
        rolAdmin.setId(1);
        rolAdmin.setNombre("ADMIN");
        
        usuario = Usuario.builder()
                .id(new UsuarioId(1, "1111111111"))
                .tipoDocumento(tipoDocumento)
                .rol(rolAdmin)
                .nombre("Admin")
                .primerApellido("Sistema")
                .direccion("Calle Admin")
                .telefono("3009999999")
                .correo("admin@example.com")
                .build();
        when(usuarioRepository.findByCorreo("admin@example.com")).thenReturn(Optional.of(usuario));
    }

    @And("existen clientes registrados")
    public void existenClientes() {
        Usuario cliente = mock(Usuario.class);
        when(cliente.getTipoDocumento()).thenReturn(tipoDocumento);
        when(cliente.getRol()).thenReturn(rolCliente);
        when(cliente.getId()).thenReturn(new UsuarioId(1, "1234567890"));
        when(cliente.getNombre()).thenReturn("Juan");
        when(cliente.getPrimerApellido()).thenReturn("Perez");
        when(cliente.getCorreo()).thenReturn("juan@example.com");
        when(cliente.getTelefono()).thenReturn("3001234567");
        when(cliente.getDireccion()).thenReturn("Calle 123");
        when(cliente.getCreatedAt()).thenReturn(LocalDateTime.now());
        when(usuarioRepository.findClientesConRol("CLIENTE")).thenReturn(List.of(cliente));
    }

    @And("no existen clientes registrados")
    public void noExistenClientes() {
        when(usuarioRepository.findClientesConRol("CLIENTE")).thenReturn(List.of());
    }

    @When("solicita la lista de clientes")
    public void solicitaListaClientes() {
        resultado = usuarioService.listarClientes();
    }

    @Then("puede ver la lista de clientes con su informacion basica")
    public void veListaClientes() {
        assertEquals(1, ((List<?>) resultado).size());
    }

    @Then("puede ver un mensaje indicando que no hay clientes registrados")
    public void noHayClientes() {
        assertTrue(((List<?>) resultado).isEmpty());
    }

    @Given("que el cliente esta autenticado en el sistema")
    public void clienteAutenticado() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("juan@example.com");
        SecurityContextImpl context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        usuario = Usuario.builder()
                .id(new UsuarioId(1, "1234567890"))
                .tipoDocumento(tipoDocumento)
                .rol(rolCliente)
                .nombre("Juan")
                .primerApellido("Perez")
                .direccion("Calle 123")
                .telefono("3001234567")
                .correo("juan@example.com")
                .build();
        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
    }

    @When("modifica su nombre o correo con valores validos y guarda los cambios")
    public void actualizaDatosValidos() {
        actualizarRequest = actualizarRequest("Juan Actualizado", "juan.actualizado@example.com");
        when(usuarioRepository.existsByCorreo("juan.actualizado@example.com")).thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        resultado = usuarioService.actualizarMisDatos(actualizarRequest);
    }

    @Then("puede ver un mensaje confirmando la actualizacion")
    public void confirmaActualizacion() {
        assertNotNull(resultado);
    }

    @And("puede ver sus datos actualizados")
    public void datosActualizados() {
        assertEquals("Juan Actualizado", ((ActualizarDatosResponse) resultado).getNombre());
    }

    @And("tiene cuenta registrada")
    public void tieneCuentaRegistrada() {
        Cuenta cuenta = Cuenta.builder()
                .idCuenta(java.util.UUID.randomUUID())
                .dueno(usuario)
                .tipoCuenta(tipoCuenta)
                .estadoCuenta(estadoCuenta)
                .saldo(BigDecimal.ZERO)
                .build();
        when(tipoCuenta.getId()).thenReturn(1);
        when(tipoCuenta.getNombre()).thenReturn("Cuenta de Ahorros Digital");
        when(estadoCuenta.getId()).thenReturn(1);
        when(estadoCuenta.getNombre()).thenReturn("ACTIVA");
        when(cuentaRepository.findAllByDuenoCorreo("juan@example.com")).thenReturn(List.of(cuenta));
    }

    @When("solicita consultar su cuenta")
    public void consultaCuenta() {
        resultado = cuentaService.consultarMisCuentas();
    }

    @Then("puede ver el numero de cuenta saldo y estado")
    public void veCuentaSaldoEstado() {
        ConsultarCuentasResponse response = (ConsultarCuentasResponse) resultado;
        assertEquals(1, response.getTotalCuentas());
        assertEquals(BigDecimal.ZERO, response.getCuentas().get(0).getSaldo());
    }

    @And("no tiene cuenta registrada")
    public void sinCuentaRegistrada() {
        when(cuentaRepository.findAllByDuenoCorreo("juan@example.com")).thenReturn(List.of());
    }

    @Then("puede ver un mensaje Cuenta no disponible")
    public void cuentaNoDisponible() {
        assertTrue(excepcion instanceof EntityNotFoundException);
        assertEquals("Cuenta no disponible", excepcion.getMessage());
    }

    @When("solicita consultar su cuenta sin registros")
    public void consultaCuentaSinRegistros() {
        try {
            resultado = cuentaService.consultarMisCuentas();
        } catch (Exception ex) {
            excepcion = ex;
        }
    }

    private void ejecutarLogin() {
        try {
            resultado = authService.login(loginRequest);
        } catch (Exception ex) {
            excepcion = ex;
        }
    }

    private void stubCatalogosRegistro() {
        when(entityManager.find(TipoDocumento.class, 1)).thenReturn(tipoDocumento);
        when(entityManager.createQuery(any(String.class), eq(Rol.class))).thenReturn(rolQuery);
        when(rolQuery.setParameter("nombre", "CLIENTE")).thenReturn(rolQuery);
        when(rolQuery.getResultStream()).thenReturn(Stream.of(rolCliente));
        when(tipoCuentaRepository.findById(1)).thenReturn(Optional.of(tipoCuenta));
        when(estadoCuentaRepository.findByNombreIgnoreCase("ACTIVA")).thenReturn(Optional.of(estadoCuenta));
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

    private static LoginRequest loginRequest(String correo, String contrasena) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "correo", correo);
        ReflectionTestUtils.setField(request, "contrasena", contrasena);
        return request;
    }

    private static ActualizarDatosRequest actualizarRequest(String nombre, String correo) {
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
        return mock(TipoCuenta.class);
    }

    private static EstadoCuenta estadoCuentaActiva() {
        return mock(EstadoCuenta.class);
    }
}
