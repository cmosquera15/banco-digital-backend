package co.edu.udea.bancodigital.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.udea.bancodigital.config.security.JwtService;
import co.edu.udea.bancodigital.dtos.requests.LoginRequest;
import co.edu.udea.bancodigital.dtos.responses.LoginResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("CP-LOG-01: login exitoso")
    void loginConCredencialesCorrectas_deberiaRetornarToken() {
        LoginRequest request = loginRequest("juan@example.com", "Abc123#@");
        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("CLIENTE");
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .correo("juan@example.com")
                .rol(rol)
                .intentosFallidos(2)
                .bloqueadoHasta(LocalDateTime.now().minusMinutes(1))
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("Juan", response.getNombre());
        assertEquals("juan@example.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRol());
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        verify(usuarioRepository).save(usuario);
        System.out.println("=== CP-LOG-01 RESULTADO OBTENIDO ===");
        System.out.println("Token: " + response.getToken());
    }

    @Test
    @DisplayName("CP-LOG-02: clave incorrecta rechaza login")
    void loginConClaveIncorrecta_deberiaPropagarBadCredentialsException() {
        LoginRequest request = loginRequest("juan@example.com", "incorrecta");
        Usuario usuario = Usuario.builder()
                .correo("juan@example.com")
                .intentosFallidos(0)
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals("Credenciales incorrectas", exception.getMessage());
        assertEquals(1, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        verify(usuarioRepository).save(usuario);

        System.out.println("=== CP-LOG-02 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
        System.out.println("Contador de intentos: " + usuario.getIntentosFallidos());
    }

    @Test
    @DisplayName("CP-LOG-03: 2 intentos fallidos")
    void segundoIntentoFallido_deberiaIncrementarContadorADos() {
        LoginRequest request = loginRequest("juan@example.com", "incorrecta");
        Usuario usuario = Usuario.builder()
                .correo("juan@example.com")
                .intentosFallidos(1)
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authService.login(request));
        assertEquals(2, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        verify(usuarioRepository).save(usuario);
        System.out.println("=== CP-LOG-03 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
        System.out.println("Contador de intentos: " + usuario.getIntentosFallidos());  
    }

    @Test
    @DisplayName("CP-LOG-04: tercer intento fallido bloquea cuenta por 5 minutos")
    void tercerIntentoFallido_deberiaBloquearCuenta() {
        LoginRequest request = loginRequest("juan@example.com", "incorrecta");
        Usuario usuario = Usuario.builder()
                .correo("juan@example.com")
                .intentosFallidos(2)
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        LockedException exception = assertThrows(LockedException.class, () -> authService.login(request));
        assertEquals(3, usuario.getIntentosFallidos());
        verify(usuarioRepository).save(usuario);
        System.out.println("=== CP-LOG-04 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
        System.out.println("Contador de intentos: " + usuario.getIntentosFallidos());
        System.out.println("Cuenta bloqueada hasta: " + usuario.getBloqueadoHasta());
    }

    @Test
    @DisplayName("CP-LOG-05: login durante bloqueo rechaza acceso")
    void loginDuranteBloqueo_deberiaLanzarLockedException() {
        LoginRequest request = loginRequest("juan@example.com", "Abc123#@");
        Usuario usuario = Usuario.builder()
                .correo("juan@example.com")
                .intentosFallidos(3)
                .bloqueadoHasta(LocalDateTime.now().plusMinutes(3))
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));

        LockedException exception = assertThrows(LockedException.class, () -> authService.login(request));
        System.out.println("=== CP-LOG-05 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
        System.out.println("Cuenta bloqueada hasta: " + usuario.getBloqueadoHasta());
    }

    @Test
    @DisplayName("CP-LOG-06: login después del bloqueo")
    void loginDespuesDelBloqueo_deberiaPermitirAccesoYResetearContador() {
        LoginRequest request = loginRequest("juan@example.com", "Abc123#@");
        Rol rol = new Rol();
        rol.setId(2);
        rol.setNombre("CLIENTE");
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .correo("juan@example.com")
                .rol(rol)
                .intentosFallidos(3)
                .bloqueadoHasta(LocalDateTime.now().minusMinutes(1))
                .build();

        when(usuarioRepository.findByCorreo("juan@example.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(usuario)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals("Juan", response.getNombre());
        assertEquals("juan@example.com", response.getCorreo());
        assertEquals("CLIENTE", response.getRol());
        assertEquals(0, usuario.getIntentosFallidos());
        assertNull(usuario.getBloqueadoHasta());
        verify(usuarioRepository).save(usuario);

        System.out.println("=== CP-LOG-06 RESULTADO OBTENIDO ===");
        System.out.println("Token: " + response.getToken());
        System.out.println("Intentos fallidos reseteados: " + usuario.getIntentosFallidos());
    }

    @Test
    @DisplayName("CP-LOG-07: usuario no existe rechaza login")
    void loginConUsuarioInexistente_deberiaLanzarEntityNotFoundException() {
        LoginRequest request = loginRequest("noexiste@example.com", "Abc123#@");

        when(usuarioRepository.findByCorreo("noexiste@example.com")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> authService.login(request));
        System.out.println("=== CP-LOG-07 RESULTADO OBTENIDO ===");
        System.out.println("Error: " + exception.getMessage());
    }

    private static LoginRequest loginRequest(String correo, String contrasena) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "correo", correo);
        ReflectionTestUtils.setField(request, "contrasena", contrasena);
        return request;
    }
}
