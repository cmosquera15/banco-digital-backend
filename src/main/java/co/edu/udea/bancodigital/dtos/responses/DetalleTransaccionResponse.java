package co.edu.udea.bancodigital.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetalleTransaccionResponse {

    private UUID idTransaccion;
    private LocalDateTime fechaHora;
    private String tipoTransaccion;
    private BigDecimal monto;
    private UUID cuentaOrigen;
    private UUID cuentaDestino;
    private String estado;
    private String descripcion;
    private String tipoCuentaOrigen;
    private String tipoCuentaDestino;
    private String estadoCuentaOrigen;
    private String estadoCuentaDestino;
}
