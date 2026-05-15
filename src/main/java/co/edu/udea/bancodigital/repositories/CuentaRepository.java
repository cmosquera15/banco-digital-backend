package co.edu.udea.bancodigital.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.udea.bancodigital.models.entities.Cuenta;
import co.edu.udea.bancodigital.models.entities.Usuario;

/**
 * Repositorio para la entidad Cuenta.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {

	/**
	 * Obtiene todas las cuentas de un usuario específico.
	 *
	 * @param correo el usuario dueño de las cuentas
	 * @return lista de cuentas del usuario
	 */
	@Query("""
	        SELECT c FROM Cuenta c
	        JOIN FETCH c.tipoCuenta tc
	        JOIN FETCH c.estadoCuenta ec
	        JOIN c.dueno u
	        WHERE u.correo = :correo
	        """)
	List<Cuenta> findAllByDuenoCorreo(@Param("correo") String correo);

	/**
	 * Obtiene todas las cuentas con las relaciones necesarias para el listado administrativo.
	 *
	 * @return lista de cuentas con dueno, tipo de documento, tipo de cuenta y estado
	 */
	@Query("""
	        SELECT c FROM Cuenta c
	        JOIN FETCH c.dueno u
	        JOIN FETCH u.tipoDocumento td
	        JOIN FETCH c.tipoCuenta tc
	        JOIN FETCH c.estadoCuenta ec
	        """)
	List<Cuenta> findAllForAdmin();

	@Query("""
	        SELECT c FROM Cuenta c
	        JOIN FETCH c.dueno u
	        JOIN FETCH c.estadoCuenta ec
	        JOIN FETCH c.tipoCuenta tc
	        WHERE c.idCuenta = :idCuenta
	        """)
	Optional<Cuenta> findByIdCuentaConDueno(@Param("idCuenta") UUID idCuenta);

	/**
	 * Obtiene una cuenta por id con las relaciones necesarias para validaciones y respuestas.
	 *
	 * @param idCuenta el identificador de la cuenta
	 * @return la cuenta con dueno, estado y tipo si existe
	 */
	@Query("""
	        SELECT c FROM Cuenta c
	        JOIN FETCH c.dueno u
	        JOIN FETCH c.estadoCuenta ec
	        JOIN FETCH c.tipoCuenta tc
	        WHERE c.idCuenta = :idCuenta
	        """)
	Optional<Cuenta> findByIdWithDuenoEstadoAndTipo(@Param("idCuenta") UUID idCuenta);

	/**
	 * Obtiene una cuenta bloqueando la fila para operaciones que modifican saldo.
	 * Debe usarse dentro de una transaccion activa.
	 *
	 * @param idCuenta el identificador de la cuenta
	 * @return la cuenta bloqueada si existe
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
	        SELECT c FROM Cuenta c
	        WHERE c.idCuenta = :idCuenta
	        """)
	Optional<Cuenta> findByIdForUpdate(@Param("idCuenta") UUID idCuenta);

	/**
	 * Obtiene cuentas bloqueadas en orden estable para evitar deadlocks en transferencias.
	 * Debe usarse dentro de una transaccion activa.
	 *
	 * @param idsCuenta los identificadores de las cuentas a bloquear
	 * @return cuentas bloqueadas ordenadas por identificador
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
	        SELECT c FROM Cuenta c
	        WHERE c.idCuenta IN :idsCuenta
	        ORDER BY c.idCuenta
	        """)
	List<Cuenta> findAllByIdCuentaInForUpdate(@Param("idsCuenta") List<UUID> idsCuenta);

	/**
	 * Verifica si un usuario tiene al menos una cuenta.
	 *
	 * @param dueno el usuario a verificar
	 * @return true si tiene cuentas, false en caso contrario
	 */
	boolean existsByDueno(Usuario dueno);

	/**
	 * Verifica si una cuenta existe y se encuentra activa.
	 *
	 * @param idCuenta el identificador de la cuenta
	 * @return true si la cuenta existe y su estado es ACTIVA
	 */
	@Query("""
	        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
	        FROM Cuenta c
	        JOIN c.estadoCuenta ec
	        WHERE c.idCuenta = :idCuenta
	          AND UPPER(ec.nombre) = 'ACTIVA'
	        """)
	boolean existsByIdCuentaAndEstadoActiva(@Param("idCuenta") UUID idCuenta);
}
