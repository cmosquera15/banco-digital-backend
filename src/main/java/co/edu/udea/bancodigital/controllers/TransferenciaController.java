package co.edu.udea.bancodigital.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udea.bancodigital.dtos.requests.TransferenciaRequest;
import co.edu.udea.bancodigital.dtos.responses.TransferenciaResponse;
import co.edu.udea.bancodigital.services.TransferenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transferencias")
@RequiredArgsConstructor
public class TransferenciaController {

    private final TransferenciaService transferenciaService;

    @PostMapping
    @Operation(summary = "Realizar una transferencia bancaria entre cuentas", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Transferencia realizada exitosamente")
    @ApiResponse(responseCode = "400", description = "Solicitud invalida, saldo insuficiente o cuenta inactiva")
    @ApiResponse(responseCode = "401", description = "Token JWT invalido o expirado")
    @ApiResponse(responseCode = "403", description = "La cuenta origen pertenece a otro usuario")
    @ApiResponse(responseCode = "404", description = "Cuenta o catalogo requerido no encontrado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<TransferenciaResponse> transferir(@Valid @RequestBody TransferenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferenciaService.transferir(request));
    }
}
