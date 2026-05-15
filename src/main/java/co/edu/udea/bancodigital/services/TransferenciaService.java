package co.edu.udea.bancodigital.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udea.bancodigital.dtos.requests.TransferenciaRequest;
import co.edu.udea.bancodigital.dtos.responses.TransferenciaResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Transaccion;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoTransaccion;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoTransaccion;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.EstadoTransaccionRepository;
import co.edu.udea.bancodigital.repositories.TipoTransaccionRepository;
import co.edu.udea.bancodigital.repositories.TransaccionRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferenciaService {

    private static final String ESTADO_CUENTA_ACTIVA = "ACTIVA";
    private static final String ESTADO_TRANSACCION_COMPLETADA = "COMPLETADA";
    private static final String TIPO_TRANSACCION_TRANSFERENCIA = "TRANSFERENCIA";
    private static final BigDecimal MONTO_ALERTA = new BigDecimal("6000000");

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final EstadoTransaccionRepository estadoTransaccionRepository;
    private final MailService mailService;

    @Transactional
    public TransferenciaResponse transferir(TransferenciaRequest request) {
        validarSolicitud(request);

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        List<Cuenta> cuentasBloqueadas = cuentaRepository.findAllByIdCuentaInForUpdate(
                List.of(request.getCuentaOrigen(), request.getCuentaDestino()));

        Map<UUID, Cuenta> cuentasPorId = cuentasBloqueadas.stream()
                .collect(Collectors.toMap(Cuenta::getIdCuenta, Function.identity()));

        Cuenta cuentaOrigen = obtenerCuentaBloqueada(cuentasPorId, request.getCuentaOrigen());
        Cuenta cuentaDestino = obtenerCuentaBloqueada(cuentasPorId, request.getCuentaDestino());

        validarPropietarioCuentaOrigen(cuentaOrigen, usuarioAutenticado);
        validarCuentaActiva(cuentaOrigen, "origen");
        validarCuentaActiva(cuentaDestino, "destino");
        validarSaldoDisponible(cuentaOrigen, request.getMonto());

        cuentaOrigen.setSaldo(cuentaOrigen.getSaldo().subtract(request.getMonto()));
        cuentaDestino.setSaldo(cuentaDestino.getSaldo().add(request.getMonto()));

        TipoTransaccion tipoTransferencia = tipoTransaccionRepository
                .findByNombreIgnoreCase(TIPO_TRANSACCION_TRANSFERENCIA)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe el tipo de transaccion TRANSFERENCIA"));

        EstadoTransaccion estadoCompletada = estadoTransaccionRepository
                .findByNombreIgnoreCase(ESTADO_TRANSACCION_COMPLETADA)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe el estado de transaccion COMPLETADA"));

        Transaccion transaccion = Transaccion.builder()
                .cuentaOrigen(cuentaOrigen)
                .cuentaDestino(cuentaDestino)
                .tipo(tipoTransferencia)
                .estado(estadoCompletada)
                .monto(request.getMonto())
                .fechaHora(LocalDateTime.now())
                .descripcion(request.getDescripcion())
                .build();

        Transaccion guardada = transaccionRepository.save(transaccion);

        // Send alert email if transaction is over 6,000,000
        if (request.getMonto().compareTo(MONTO_ALERTA) > 0) {
            enviarAlertaTransferenciaAlta(cuentaOrigen, request.getMonto(), guardada.getFechaHora());
        }

        return TransferenciaResponse.builder()
                .idTransaccion(guardada.getIdTransaccion())
                .monto(guardada.getMonto())
                .fechaHora(guardada.getFechaHora())
                .estado(guardada.getEstado().getNombre())
                .mensaje("Transferencia realizada exitosamente")
                .build();
    }

    private void validarSolicitud(TransferenciaRequest request) {
        if (request.getCuentaOrigen() == null || request.getCuentaDestino() == null || request.getMonto() == null) {
            throw new IllegalArgumentException("Cuenta origen, cuenta destino y monto son obligatorios");
        }
        if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (request.getCuentaOrigen().equals(request.getCuentaDestino())) {
            throw new IllegalArgumentException("La cuenta origen y destino deben ser diferentes");
        }
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Usuario no autenticado");
        }
        String correo = authentication.getName();

        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
    }

    private Cuenta obtenerCuentaBloqueada(Map<UUID, Cuenta> cuentasPorId, UUID idCuenta) {
        Cuenta cuenta = cuentasPorId.get(idCuenta);
        if (cuenta == null) {
            throw new EntityNotFoundException("Cuenta con id " + idCuenta + " no existe");
        }
        return cuenta;
    }

    private void validarPropietarioCuentaOrigen(Cuenta cuentaOrigen, Usuario usuarioAutenticado) {
        if (!cuentaOrigen.getDueno().getId().equals(usuarioAutenticado.getId())) {
            throw new AccessDeniedException("No tiene permisos para transferir desde esta cuenta");
        }
    }

    private void validarCuentaActiva(Cuenta cuenta, String rolCuenta) {
        if (!ESTADO_CUENTA_ACTIVA.equalsIgnoreCase(cuenta.getEstadoCuenta().getNombre())) {
            throw new IllegalArgumentException("La cuenta " + rolCuenta + " no se encuentra activa");
        }
    }

    private void validarSaldoDisponible(Cuenta cuentaOrigen, BigDecimal monto) {
        if (cuentaOrigen.getSaldo().compareTo(monto) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente para realizar la transferencia");
        }
    }

    private void enviarAlertaTransferenciaAlta(Cuenta cuentaOrigen, BigDecimal monto, LocalDateTime fecha) {
        try {
            String cuentaMascara = maskLast4(cuentaOrigen.getIdCuenta().toString());
            String mensaje = String.format(
                    "Se ha registrado una transferencia de alto monto: $%s desde tu cuenta %s el %s. " +
                    "Si no realizaste esta transacción, contacta a soporte inmediatamente.",
                    monto, cuentaMascara, fecha);
            mailService.sendAccountAlert(cuentaOrigen.getDueno().getCorreo(), mensaje);
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Error sending high transfer alert email: " + e.getMessage());
        }
    }

    private String maskLast4(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        int visible = Math.min(4, value.length());
        return "****" + value.substring(value.length() - visible);
    }
}
