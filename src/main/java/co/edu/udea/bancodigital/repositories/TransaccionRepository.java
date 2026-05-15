package co.edu.udea.bancodigital.repositories;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.udea.bancodigital.models.entities.Transaccion;

/**
 * Repositorio para la entidad Transaccion.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {

	/**
	 * Obtiene el historial de una cuenta, ya sea como origen o destino.
	 *
	 * @param idCuenta identificador de la cuenta
	 * @param pageable configuracion de paginacion
	 * @return pagina de transacciones relacionadas con la cuenta
	 */
	@Query(value = """
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        WHERE co.idCuenta = :idCuenta
	           OR cd.idCuenta = :idCuenta
	        ORDER BY t.fechaHora DESC
	        """,
	        countQuery = """
	        SELECT COUNT(t)
	        FROM Transaccion t
	        WHERE t.cuentaOrigen.idCuenta = :idCuenta
	           OR t.cuentaDestino.idCuenta = :idCuenta
	        """)
	Page<Transaccion> findHistorialByCuenta(@Param("idCuenta") UUID idCuenta, Pageable pageable);

	/**
	 * Obtiene el historial de una cuenta en un rango de fechas.
	 *
	 * @param idCuenta identificador de la cuenta
	 * @param inicio fecha y hora inicial
	 * @param fin fecha y hora final
	 * @param pageable configuracion de paginacion
	 * @return pagina de transacciones filtradas por fecha
	 */
	@Query(value = """
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        WHERE (co.idCuenta = :idCuenta OR cd.idCuenta = :idCuenta)
	          AND t.fechaHora BETWEEN :inicio AND :fin
	        ORDER BY t.fechaHora DESC
	        """,
	        countQuery = """
	        SELECT COUNT(t)
	        FROM Transaccion t
	        WHERE (t.cuentaOrigen.idCuenta = :idCuenta OR t.cuentaDestino.idCuenta = :idCuenta)
	          AND t.fechaHora BETWEEN :inicio AND :fin
	        """)
	Page<Transaccion> findHistorialByCuentaAndFechaHoraBetween(
			@Param("idCuenta") UUID idCuenta,
			@Param("inicio") LocalDateTime inicio,
			@Param("fin") LocalDateTime fin,
			Pageable pageable);

	/**
	 * Obtiene el detalle de una transaccion con sus relaciones principales.
	 *
	 * @param idTransaccion identificador de la transaccion
	 * @return transaccion con sus relaciones si existe
	 */
	@Query("""
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH co.dueno cod
	        JOIN FETCH co.tipoCuenta cotc
	        JOIN FETCH co.estadoCuenta coec
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH cd.dueno cdd
	        JOIN FETCH cd.tipoCuenta cdtc
	        JOIN FETCH cd.estadoCuenta cdec
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        WHERE t.idTransaccion = :idTransaccion
	        """)
	Optional<Transaccion> findDetalleById(@Param("idTransaccion") UUID idTransaccion);

	/**
	 * Obtiene transacciones para auditoria administrativa.
	 *
	 * @param pageable configuracion de paginacion
	 * @return pagina de transacciones ordenadas por fecha descendente
	 */
	@Query(value = """
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        ORDER BY t.fechaHora DESC
	        """,
	        countQuery = """
	        SELECT COUNT(t)
	        FROM Transaccion t
	        """)
	Page<Transaccion> findAllForAuditoria(Pageable pageable);

	/**
	 * Obtiene transacciones con monto superior a un umbral.
	 *
	 * @param montoMinimo monto minimo a consultar
	 * @param pageable configuracion de paginacion
	 * @return pagina de transacciones cuyo monto supera el umbral
	 */
	@Query(value = """
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        WHERE t.monto > :montoMinimo
	        ORDER BY t.fechaHora DESC
	        """,
	        countQuery = """
	        SELECT COUNT(t)
	        FROM Transaccion t
	        WHERE t.monto > :montoMinimo
	        """)
	Page<Transaccion> findByMontoGreaterThan(
			@Param("montoMinimo") BigDecimal montoMinimo,
			Pageable pageable);

	/**
	 * Obtiene transacciones de alto valor definidas como montos superiores a 6.000.000.
	 *
	 * @param pageable configuracion de paginacion
	 * @return pagina de transacciones de alto valor
	 */
	@Query(value = """
	        SELECT t
	        FROM Transaccion t
	        JOIN FETCH t.cuentaOrigen co
	        JOIN FETCH t.cuentaDestino cd
	        JOIN FETCH t.tipo tt
	        JOIN FETCH t.estado et
	        WHERE t.monto > 6000000
	        ORDER BY t.fechaHora DESC
	        """,
	        countQuery = """
	        SELECT COUNT(t)
	        FROM Transaccion t
	        WHERE t.monto > 6000000
	        """)
	Page<Transaccion> findTransaccionesAltoValor(Pageable pageable);
}
