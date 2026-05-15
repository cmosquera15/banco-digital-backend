package co.edu.udea.bancodigital.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferenciaResponse {

    private UUID idTransaccion;
    private BigDecimal monto;
    private LocalDateTime fechaHora;
    private String estado;
    private String mensaje;
}
