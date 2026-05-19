package co.edu.udea.bancodigital.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.udea.bancodigital.dtos.requests.ActualizarDatosRequest;
import co.edu.udea.bancodigital.dtos.requests.RegistroRequest;
import co.edu.udea.bancodigital.dtos.responses.ActualizarDatosResponse;
import co.edu.udea.bancodigital.dtos.responses.RegistroResponse;
import co.edu.udea.bancodigital.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para gestión de perfil de usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/registro")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de registro inválidos o incompletos")
    @ApiResponse(responseCode = "409", description = "El correo o documento ya está registrado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @PutMapping("/me")
    @Operation(summary = "Actualizar datos personales del cliente autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Datos actualizados exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de actualización inválidos")
    @ApiResponse(responseCode = "401", description = "Token JWT inválido o expirado")
    @ApiResponse(responseCode = "404", description = "Usuario autenticado no encontrado")
    @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    public ResponseEntity<ActualizarDatosResponse> actualizarMisDatos(@Valid @RequestBody ActualizarDatosRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarMisDatos(request));
    }
}
