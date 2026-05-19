package co.edu.udea.bancodigital.controllers;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udea.bancodigital.dtos.responses.DetalleTransaccionResponse;
import co.edu.udea.bancodigital.dtos.responses.HistorialTransaccionesResponse;
import co.edu.udea.bancodigital.services.TransaccionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Tag(name = "Historial de Transacciones", description = "Endpoints para consultar historial y detalles de transacciones")
public class TransaccionController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TransaccionService transaccionService;

    @GetMapping("/me")
    @Operation(summary = "Consultar historial de transacciones de una cuenta propia", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Historial consultado exitosamente")
    @ApiResponse(responseCode = "400", description = "Parametros de consulta invalidos")
    @ApiResponse(responseCode = "401", description = "Token JWT invalido o expirado")
    @ApiResponse(responseCode = "403", description = "La cuenta pertenece a otro usuario")
    @ApiResponse(responseCode = "404", description = "La cuenta no existe")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<HistorialTransaccionesResponse> consultarHistorial(
            @RequestParam UUID cuentaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, normalizarSize(size));
        return ResponseEntity.ok(transaccionService.consultarHistorial(cuentaId, fechaInicio, fechaFin, pageable));
    }

    @GetMapping("/{idTransaccion}")
    @Operation(summary = "Consultar detalle de una transaccion propia", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Detalle consultado exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT invalido o expirado")
    @ApiResponse(responseCode = "403", description = "La transaccion pertenece a otra cuenta")
    @ApiResponse(responseCode = "404", description = "La transaccion no existe")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<DetalleTransaccionResponse> consultarDetalle(@PathVariable UUID idTransaccion) {
        return ResponseEntity.ok(transaccionService.consultarDetalle(idTransaccion));
    }

    private int normalizarSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("El tamano de pagina debe ser mayor a cero");
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
