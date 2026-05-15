package co.edu.udea.bancodigital.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HistorialTransaccionResponse {

    private UUID idTransaccion;
    private LocalDateTime fechaHora;
    private String tipoTransaccion;
    private BigDecimal monto;
    private UUID cuentaOrigen;
    private UUID cuentaDestino;
    private String estado;
}
