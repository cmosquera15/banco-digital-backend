package co.edu.udea.bancodigital.models.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import co.edu.udea.bancodigital.models.entities.base.AuditableEntity;
import co.edu.udea.bancodigital.models.entities.catalogs.EstadoCuenta;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoCuenta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Cuenta del banco digital.
 * Representa las cuentas bancarias de los usuarios.
 */
@Entity
@Table(name = "cuentas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuenta extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id_cuenta")
	private UUID idCuenta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumns(value = {
			@JoinColumn(name = "tipo_doc_dueno", referencedColumnName = "id_tipo_doc", nullable = false),
			@JoinColumn(name = "num_doc_dueno", referencedColumnName = "numero_documento", nullable = false)
	}, foreignKey = @ForeignKey(name = "fk_cuenta_usuario"))
	private Usuario dueno;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_tipo_cuenta", nullable = false, foreignKey = @ForeignKey(name = "fk_cuenta_tipo"))
	private TipoCuenta tipoCuenta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_estado_cuenta", nullable = false, foreignKey = @ForeignKey(name = "fk_cuenta_estado"))
	private EstadoCuenta estadoCuenta;

	@Column(nullable = false, precision = 18, scale = 2)
	@Builder.Default
	private BigDecimal saldo = BigDecimal.ZERO;

	@OneToMany(mappedBy = "cuentaOrigen")
	@Builder.Default
	private List<Transaccion> transaccionesOrigen = new ArrayList<>();

	@OneToMany(mappedBy = "cuentaDestino")
	@Builder.Default
	private List<Transaccion> transaccionesDestino = new ArrayList<>();
}
