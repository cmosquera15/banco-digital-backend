package co.edu.udea.bancodigital.dtos.requests;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FiltroHistorialRequest {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
