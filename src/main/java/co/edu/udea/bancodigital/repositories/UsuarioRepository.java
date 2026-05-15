package co.edu.udea.bancodigital.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.pks.UsuarioId;

/**
 * Repositorio para la entidad Usuario.
 * Proporciona operaciones CRUD y consultas personalizadas.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UsuarioId> {

	/**
	 * Busca un usuario por su correo electrónico.
	 *
	 * @param correo el correo a buscar
	 * @return Optional con el usuario si existe
	 */
	@EntityGraph(attributePaths = {"rol", "tipoDocumento"})
	@Query("""
		SELECT u
		FROM Usuario u
		WHERE u.correo = :correo""")
	Optional<Usuario> findByCorreo(String correo);

	/**
	 * Verifica si existe un usuario con el correo especificado.
	 *
	 * @param correo el correo a verificar
	 * @return true si existe, false en caso contrario
	 */
	@Query("""
	        SELECT
				CASE WHEN COUNT(u) > 0
					THEN true ELSE false END
			FROM Usuario u
			WHERE u.correo = :correo
	        """)
	boolean existsByCorreo(String correo);

	/**
	 * Obtiene todos los usuarios con un rol específico.
	 *
	 * @param rol el rol a filtrar
	 * @return lista de usuarios con el rol especificado
	 */
	@Query("""
	        SELECT u
			FROM Usuario u
			WHERE u.rol = :rol
	        """)
	List<Usuario> findAllByRol(Rol rol);

	/**
	 * Obtiene todos los usuarios cuyo rol coincide con el nombre especificado,
	 * sin distinguir mayusculas o minusculas.
	 *
	 * @param nombreRol el nombre del rol a filtrar
	 * @return lista de usuarios con el nombre de rol especificado
	 */
	@Query("""
		SELECT u FROM Usuario u
		JOIN FETCH u.rol r
		JOIN FETCH u.tipoDocumento td
		WHERE UPPER(r.nombre) = UPPER(:nombreRol)
		""")
	List<Usuario> findClientesConRol(@Param("nombreRol") String nombreRol);
}
