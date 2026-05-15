package co.edu.udea.bancodigital.dtos.requests;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TransferenciaRequest {

    @NotNull(message = "La cuenta origen es obligatoria")
    private UUID cuentaOrigen;

    @NotNull(message = "La cuenta destino es obligatoria")
    private UUID cuentaDestino;

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal monto;

    @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
    private String descripcion;
}
