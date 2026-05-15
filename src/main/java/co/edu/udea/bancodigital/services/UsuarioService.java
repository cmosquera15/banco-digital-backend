package co.edu.udea.bancodigital.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udea.bancodigital.dtos.requests.ActualizarDatosRequest;
import co.edu.udea.bancodigital.dtos.requests.RegistroRequest;
import co.edu.udea.bancodigital.dtos.responses.ActualizarDatosResponse;
import co.edu.udea.bancodigital.dtos.responses.ListarClientesAdminResponse;
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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ROL_CLIENTE = "CLIENTE";
    private static final String ESTADO_ACTIVA = "ACTIVA";
    private static final int TIPO_CUENTA_DEFAULT_ID = 1;

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TipoCuentaRepository tipoCuentaRepository;
    private final EstadoCuentaRepository estadoCuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        String correo = normalizeEmail(request.getCorreo());

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + correo);
        }

        Integer idTipoDoc = request.getIdTipoDoc();

        String numeroDocumento = request.getNumeroDocumento().trim();
        
        UsuarioId id = new UsuarioId(idTipoDoc, numeroDocumento);

        if (usuarioRepository.existsById(id)) {
            throw new DuplicateResourceException("Ya existe un usuario con ese documento");
        }

        TipoDocumento tipoDocumento = entityManager.find(TipoDocumento.class, idTipoDoc);
        if (tipoDocumento == null) {
            throw new IllegalArgumentException("No existe tipo_documento con id: " + idTipoDoc);
        }

        Rol rolCliente = findRolCliente();

        Usuario usuario = Usuario.builder()
                .id(id)
                .tipoDocumento(tipoDocumento)
                .nombre(request.getNombre())
                .primerApellido(request.getPrimerApellido())
            .segundoApellido(normalizeOptionalText(request.getSegundoApellido()))
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .correo(correo)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .rol(rolCliente)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        crearCuentaInicial(guardado);

        return RegistroResponse.builder()
            .idTipoDoc(tipoDocumento.getId())
            .tipoDocumento(tipoDocumento.getNombre())
                .numeroDocumento(maskNumeroDocumento(id.getNumeroDocumento()))
                .nombre(usuario.getNombre())
                .primerApellido(usuario.getPrimerApellido())
                .segundoApellido(usuario.getSegundoApellido())
                .correo(usuario.getCorreo())
            .idRol(rolCliente.getId())
            .rol(rolCliente.getNombre())
                .build();
    }

    public ActualizarDatosResponse actualizarMisDatos(ActualizarDatosRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }

        String correoActual = authentication.getName();
        Usuario usuario = usuarioRepository.findByCorreo(correoActual)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));

        String nuevoCorreo = request.getCorreo().trim().toLowerCase();
        if (!usuario.getCorreo().equalsIgnoreCase(nuevoCorreo) && usuarioRepository.existsByCorreo(nuevoCorreo)) {
            throw new DuplicateResourceException("Ya existe un usuario con el correo: " + nuevoCorreo);
        }

        usuario.setNombre(request.getNombre().trim());
        usuario.setPrimerApellido(request.getPrimerApellido().trim());
        usuario.setSegundoApellido(normalizeOptionalText(request.getSegundoApellido()));
        usuario.setDireccion(request.getDireccion().trim());
        usuario.setTelefono(request.getTelefono().trim());
        usuario.setCorreo(nuevoCorreo);

        Usuario actualizado = usuarioRepository.save(usuario);

        return ActualizarDatosResponse.builder()
                .idTipoDoc(actualizado.getTipoDocumento().getId())
                .tipoDocumento(actualizado.getTipoDocumento().getNombre())
                .numeroDocumento(maskNumeroDocumento(actualizado.getId().getNumeroDocumento()))
                .nombre(actualizado.getNombre())
                .primerApellido(actualizado.getPrimerApellido())
                .segundoApellido(actualizado.getSegundoApellido())
                .direccion(actualizado.getDireccion())
                .telefono(actualizado.getTelefono())
                .correo(actualizado.getCorreo())
                .idRol(actualizado.getRol().getId())
                .rol(actualizado.getRol().getNombre())
                .updatedAt(actualizado.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ListarClientesAdminResponse> listarClientes() {
    return usuarioRepository.findClientesConRol(ROL_CLIENTE).stream()
        .map(usuario -> ListarClientesAdminResponse.builder()
            .idTipoDocumento(usuario.getTipoDocumento().getId())
            .tipoDocumento(usuario.getTipoDocumento().getNombre())
            .numeroDocumento(usuario.getId().getNumeroDocumento())
            .nombre(usuario.getNombre())
            .primerApellido(usuario.getPrimerApellido())
            .segundoApellido(usuario.getSegundoApellido())
            .correo(usuario.getCorreo())
            .telefono(usuario.getTelefono())
            .direccion(usuario.getDireccion())
            .rol(usuario.getRol().getNombre())
            .createdAt(usuario.getCreatedAt())
            .build())
        .toList();
    }

    private String maskNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return "****";
        }

        int visible = Math.min(4, numeroDocumento.length());
        String tail = numeroDocumento.substring(numeroDocumento.length() - visible);
        return "****" + tail;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Rol findRolCliente() {
        return entityManager.createQuery(
                        "select r from Rol r where upper(r.nombre) = :nombre", Rol.class)
                .setParameter("nombre", ROL_CLIENTE)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new DataIntegrityViolationException(
                        "No existe el rol CLIENTE en la tabla roles"));
    }

    private void crearCuentaInicial(Usuario usuario) {
        TipoCuenta tipoCuenta = tipoCuentaRepository.findById(TIPO_CUENTA_DEFAULT_ID)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe tipo_cuenta con id: " + TIPO_CUENTA_DEFAULT_ID));
        EstadoCuenta estadoActiva = estadoCuentaRepository.findByNombreIgnoreCase(ESTADO_ACTIVA)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe el estado ACTIVA en la tabla estados_cuenta"));

        cuentaRepository.save(Cuenta.builder()
                .dueno(usuario)
                .tipoCuenta(tipoCuenta)
                .estadoCuenta(estadoActiva)
                .saldo(BigDecimal.ZERO)
                .build());
    }

    private String normalizeEmail(String correo) {
        return correo.trim().toLowerCase();
    }
}
