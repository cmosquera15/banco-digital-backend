package co.edu.udea.bancodigital.config.security;

import java.util.Collections;
import java.util.Locale;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import co.edu.udea.bancodigital.models.entities.Usuario;

/**
 * Mapeador que convierte la entidad Usuario a UserDetails de Spring Security.
 */
public class UserDetailsMapper {

	/**
	 * Mapea un Usuario a un objeto UserDetails.
	 *
	 * @param usuario la entidad Usuario
	 * @return un UserDetails con la información del usuario
	 */
	private UserDetailsMapper() {
		// Constructor privado para evitar instanciación al ser una utility class
	}

	public static UserDetails mapToUserDetails(Usuario usuario) {
		String rolNombre = usuario.getRol().getNombre().toUpperCase(Locale.ROOT);

		return User.builder()
				.username(usuario.getCorreo())
				.password(usuario.getContrasena())
				.authorities(Collections
						.singletonList(new SimpleGrantedAuthority("ROLE_" + rolNombre)))
				.accountExpired(false)
				.accountLocked(false)
				.credentialsExpired(false)
				.disabled(false)
				.build();
	}
}
