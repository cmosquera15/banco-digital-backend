package co.edu.udea.bancodigital.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.udea.bancodigital.dtos.responses.DetalleTransaccionResponse;
import co.edu.udea.bancodigital.dtos.responses.HistorialTransaccionResponse;
import co.edu.udea.bancodigital.dtos.responses.HistorialTransaccionesResponse;
import co.edu.udea.bancodigital.exception.EntityNotFoundException;
import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Transaccion;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.TransaccionRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public HistorialTransaccionesResponse consultarHistorial(
            UUID idCuenta,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Pageable pageable) {

        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        Cuenta cuenta = cuentaRepository.findByIdCuentaConDueno(idCuenta)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta con id " + idCuenta + " no existe"));

        validarPropietarioCuenta(cuenta, usuarioAutenticado);

        Page<HistorialTransaccionResponse> pagina;

        if (fechaInicio != null && fechaFin != null) {
            if (fechaInicio.isAfter(fechaFin)) {
                throw new IllegalArgumentException("La fecha inicio no puede ser posterior a la fecha fin");
            }
            pagina = transaccionRepository.findHistorialByCuentaAndFechaHoraBetween(
                    idCuenta,
                    fechaInicio.atStartOfDay(),
                    fechaFin.atTime(LocalTime.MAX),
                    pageable)
                    .map(this::toHistorialResponse);
        } else if (fechaInicio != null || fechaFin != null) {
            throw new IllegalArgumentException("Debe enviar fechaInicio y fechaFin para filtrar por rango");
        } else {
            pagina = transaccionRepository.findHistorialByCuenta(idCuenta, pageable)
                    .map(this::toHistorialResponse);
        }

        return construirRespuestaHistorial(pagina);
    }

    @Transactional(readOnly = true)
    public DetalleTransaccionResponse consultarDetalle(UUID idTransaccion) {
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();
        Transaccion transaccion = transaccionRepository.findDetalleById(idTransaccion)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Transaccion con id " + idTransaccion + " no existe"));

        boolean perteneceAlUsuario = transaccion.getCuentaOrigen().getDueno().getId().equals(usuarioAutenticado.getId())
                || transaccion.getCuentaDestino().getDueno().getId().equals(usuarioAutenticado.getId());

        if (!perteneceAlUsuario) {
            throw new AccessDeniedException("No tiene permisos para consultar esta transaccion");
        }

        return toDetalleResponse(transaccion);
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String correo = authentication.getName();

        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new EntityNotFoundException("Usuario autenticado no encontrado"));
    }

    private void validarPropietarioCuenta(Cuenta cuenta, Usuario usuarioAutenticado) {
        if (!cuenta.getDueno().getId().equals(usuarioAutenticado.getId())) {
            throw new AccessDeniedException("No tiene permisos para consultar esta cuenta");
        }
    }

    private HistorialTransaccionResponse toHistorialResponse(Transaccion transaccion) {
        return HistorialTransaccionResponse.builder()
                .idTransaccion(transaccion.getIdTransaccion())
                .fechaHora(transaccion.getFechaHora())
                .tipoTransaccion(transaccion.getTipo().getNombre())
                .monto(transaccion.getMonto())
                .cuentaOrigen(transaccion.getCuentaOrigen().getIdCuenta())
                .cuentaDestino(transaccion.getCuentaDestino().getIdCuenta())
                .estado(transaccion.getEstado().getNombre())
                .build();
    }

    private HistorialTransaccionesResponse construirRespuestaHistorial(Page<HistorialTransaccionResponse> pagina) {
        String mensaje;
        boolean tieneDatos;

        if (pagina.isEmpty()) {
            mensaje = "No existen transacciones para esta cuenta";
            tieneDatos = false;
        } else {
            mensaje = null;
            tieneDatos = true;
        }

        return HistorialTransaccionesResponse.builder()
                .transacciones(pagina.getContent())
                .total(pagina.getTotalElements())
                .pagina(pagina.getNumber())
                .tamanoPagina(pagina.getSize())
                .totalPaginas(pagina.getTotalPages())
                .mensaje(mensaje)
                .tieneDatos(tieneDatos)
                .build();
    }

    private DetalleTransaccionResponse toDetalleResponse(Transaccion transaccion) {
        return DetalleTransaccionResponse.builder()
                .idTransaccion(transaccion.getIdTransaccion())
                .fechaHora(transaccion.getFechaHora())
                .tipoTransaccion(transaccion.getTipo().getNombre())
                .monto(transaccion.getMonto())
                .cuentaOrigen(transaccion.getCuentaOrigen().getIdCuenta())
                .cuentaDestino(transaccion.getCuentaDestino().getIdCuenta())
                .estado(transaccion.getEstado().getNombre())
                .descripcion(transaccion.getDescripcion())
                .tipoCuentaOrigen(transaccion.getCuentaOrigen().getTipoCuenta().getNombre())
                .tipoCuentaDestino(transaccion.getCuentaDestino().getTipoCuenta().getNombre())
                .estadoCuentaOrigen(transaccion.getCuentaOrigen().getEstadoCuenta().getNombre())
                .estadoCuentaDestino(transaccion.getCuentaDestino().getEstadoCuenta().getNombre())
                .build();
    }
}
