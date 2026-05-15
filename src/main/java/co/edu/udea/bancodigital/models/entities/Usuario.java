package co.edu.udea.bancodigital.models.entities;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

import co.edu.udea.bancodigital.models.entities.base.AuditableEntity;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoDocumento;
import co.edu.udea.bancodigital.models.pks.UsuarioId;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad Usuario del banco digital.
 * Contiene información de los usuarios del sistema.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario extends AuditableEntity {

	@EmbeddedId
	private UsuarioId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("idTipoDoc")
	@JoinColumn(name = "id_tipo_doc", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_tipo_doc"))
	private TipoDocumento tipoDocumento;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_rol", nullable = false, foreignKey = @ForeignKey(name = "fk_usuario_rol"))
	private Rol rol;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "primer_apellido", nullable = false, length = 100)
	private String primerApellido;

	@Column(name = "segundo_apellido", nullable = true, length = 100)
	private String segundoApellido;

	@Column(name = "direccion", nullable = false, length = 200)
	private String direccion;

	@Column(name = "telefono", nullable = false, length = 20)
	private String telefono;

	@Column(name = "correo", nullable = false, unique = true, length = 100)
	private String correo;

	@Column(name = "contrasena", nullable = false, length = 255)
	private String contrasena;

	@Column(name = "intentos_fallidos", nullable = false)
	@Builder.Default
	private Integer intentosFallidos = 0;

	@Column(name = "bloqueado_hasta")
	private LocalDateTime bloqueadoHasta;

	@OneToMany(mappedBy = "dueno")
	@Builder.Default
	private List<Cuenta> cuentas = new ArrayList<>();
}
