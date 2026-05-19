package co.edu.udea.bancodigital.controllers;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.UUID;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udea.bancodigital.dtos.requests.CrearCuentaRequest;
import co.edu.udea.bancodigital.dtos.responses.ConsultarCuentasResponse;
import co.edu.udea.bancodigital.dtos.responses.ConsultarSaldoResponse;
import co.edu.udea.bancodigital.dtos.responses.CrearCuentaResponse;
import co.edu.udea.bancodigital.services.CuentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas Bancarias", description = "Endpoints para gestión de cuentas bancarias")
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    @Operation(summary = "Crear una nueva cuenta bancaria para el usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "201", description = "Cuenta creada exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de la cuenta inválidos")
    @ApiResponse(responseCode = "401", description = "Token JWT inválido o expirado")
    @ApiResponse(responseCode = "404", description = "Tipo de cuenta no encontrado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<CrearCuentaResponse> crearCuenta(@Valid @RequestBody CrearCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.crearCuenta(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar las cuentas del usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Lista de cuentas del usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Token JWT inválido o expirado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<CollectionModel<ConsultarCuentasResponse.DetalleCuenta>> consultarMisCuentas() {
        ConsultarCuentasResponse response = cuentaService.consultarMisCuentas();
        CollectionModel<ConsultarCuentasResponse.DetalleCuenta> model = CollectionModel.of(response.getCuentas(),
                linkTo(methodOn(CuentaController.class).consultarMisCuentas()).withSelfRel());
        return ResponseEntity.ok(model);
    }

    @GetMapping("/{idCuenta}/saldo")
    @Operation(summary = "Consultar el saldo disponible de una cuenta", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Saldo consultado exitosamente")
    @ApiResponse(responseCode = "401", description = "Token JWT inválido o expirado")
    @ApiResponse(responseCode = "403", description = "La cuenta pertenece a otro usuario")
    @ApiResponse(responseCode = "404", description = "La cuenta no existe")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<EntityModel<ConsultarSaldoResponse>> consultarSaldoCuenta(@PathVariable UUID idCuenta) {
        ConsultarSaldoResponse response = cuentaService.consultarSaldoCuenta(idCuenta);
        EntityModel<ConsultarSaldoResponse> model = EntityModel.of(response,
                linkTo(methodOn(CuentaController.class).consultarSaldoCuenta(idCuenta)).withSelfRel(),
                linkTo(methodOn(CuentaController.class).consultarMisCuentas()).withRel("mis-cuentas"));
        return ResponseEntity.ok(model);
    }
}
