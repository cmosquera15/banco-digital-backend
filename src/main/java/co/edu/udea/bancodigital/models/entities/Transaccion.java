package co.edu.udea.bancodigital.models.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import co.edu.udea.bancodigital.models.entities.base.AuditableEntity;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoTransaccion;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoTransaccion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Transaccion del banco digital.
 * Representa las transacciones bancarias entre cuentas.
 */
@Entity
@Table(name = "transacciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id_transaccion")
	private UUID idTransaccion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cuenta_origen", nullable = false, foreignKey = @ForeignKey(name = "fk_transaccion_cuenta_origen"))
	private Cuenta cuentaOrigen;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_cuenta_destino", nullable = false, foreignKey = @ForeignKey(name = "fk_transaccion_cuenta_destino"))
	private Cuenta cuentaDestino;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_tipo_transaccion", nullable = false, foreignKey = @ForeignKey(name = "fk_transaccion_tipo"))
	private TipoTransaccion tipo;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal monto;

	@Column(name = "fecha_hora", nullable = false)
	private LocalDateTime fechaHora;

	@Column(length = 255)
	private String descripcion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_estado_transaccion", nullable = false, foreignKey = @ForeignKey(name = "fk_transaccion_estado"))
	private EstadoTransaccion estado;
}
