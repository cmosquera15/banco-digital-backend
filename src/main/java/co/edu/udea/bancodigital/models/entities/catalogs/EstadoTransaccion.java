package co.edu.udea.bancodigital.models.entities.catalogs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "estados_transaccion")
@Getter
@Setter
public class EstadoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_transaccion")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;
}
