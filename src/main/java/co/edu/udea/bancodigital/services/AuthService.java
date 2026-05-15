package co.edu.udea.bancodigital.services;

import co.edu.udea.bancodigital.config.security.JwtService;
import co.edu.udea.bancodigital.dtos.requests.LoginRequest;
import co.edu.udea.bancodigital.dtos.responses.LoginResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS_FALLIDOS = 3;
    private static final int MINUTOS_BLOQUEO = 5;

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByCorreo(request.getCorreo())
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado"));

        LocalDateTime ahora = LocalDateTime.now();
        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(ahora)) {
            throw new LockedException("Por favor intente nuevamente más tarde");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getCorreo(), request.getContrasena()));
        } catch (BadCredentialsException ex) {
            int intentos = usuario.getIntentosFallidos() == null ? 0 : usuario.getIntentosFallidos();
            usuario.setIntentosFallidos(intentos + 1);

            if (usuario.getIntentosFallidos() >= MAX_INTENTOS_FALLIDOS) {
                usuario.setBloqueadoHasta(ahora.plusMinutes(MINUTOS_BLOQUEO));
                usuarioRepository.save(usuario);
                throw new LockedException(
                        "Credenciales incorrectas, por favor intente nuevamente en 5 minutos");
            }

            usuarioRepository.save(usuario);
            throw ex;
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .nombre(usuario.getNombre())
                .correo(usuario.getCorreo())
                .idRol(usuario.getRol().getId())
                .rol(usuario.getRol().getNombre())
                .build();
    }
}
