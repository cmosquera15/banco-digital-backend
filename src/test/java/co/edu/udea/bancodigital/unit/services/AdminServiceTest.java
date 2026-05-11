package co.edu.udea.bancodigital.unit.services;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.udea.bancodigital.dtos.responses.ListarClientesAdminResponse;
import co.edu.udea.bancodigital.models.entities.Usuario;
import co.edu.udea.bancodigital.models.entities.catalogs.Rol;
import co.edu.udea.bancodigital.models.entities.catalogs.TipoDocumento;
import co.edu.udea.bancodigital.models.pks.UsuarioId;
import co.edu.udea.bancodigital.repositories.CuentaRepository;
import co.edu.udea.bancodigital.repositories.EstadoCuentaRepository;
import co.edu.udea.bancodigital.repositories.TipoCuentaRepository;
import co.edu.udea.bancodigital.repositories.UsuarioRepository;
import co.edu.udea.bancodigital.services.UsuarioService;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private TipoCuentaRepository tipoCuentaRepository;

    @Mock
    private EstadoCuentaRepository estadoCuentaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("CP-ADM-01: Consulta exitosa")
    void listarClientes_deberiaMapearUsuariosCliente() {
        TipoDocumento tipoDocumento = mock(TipoDocumento.class);
        Rol rol = mock(Rol.class);
        Usuario usuario = mock(Usuario.class);
        UsuarioId usuarioId = new UsuarioId(1, "123456");
        LocalDateTime createdAt = LocalDateTime.now();

        when(tipoDocumento.getId()).thenReturn(1);
        when(tipoDocumento.getNombre()).thenReturn("CC");
        when(rol.getNombre()).thenReturn("CLIENTE");
        when(usuario.getTipoDocumento()).thenReturn(tipoDocumento);
        when(usuario.getRol()).thenReturn(rol);
        when(usuario.getId()).thenReturn(usuarioId);
        when(usuario.getNombre()).thenReturn("Juan");
        when(usuario.getPrimerApellido()).thenReturn("Perez");
        when(usuario.getSegundoApellido()).thenReturn("Gomez");
        when(usuario.getCorreo()).thenReturn("juan@example.com");
        when(usuario.getTelefono()).thenReturn("3000000000");
        when(usuario.getDireccion()).thenReturn("Calle 123");
        when(usuario.getCreatedAt()).thenReturn(createdAt);

        when(usuarioRepository.findClientesConRol("CLIENTE"))
            .thenReturn(List.of(usuario));

        List<ListarClientesAdminResponse> response = usuarioService.listarClientes();

        assertNotNull(response);
        assertEquals(1, response.size());

        ListarClientesAdminResponse item = response.get(0);
        assertEquals(1, item.getIdTipoDocumento());
        assertEquals("CC", item.getTipoDocumento());
        assertEquals("123456", item.getNumeroDocumento());
        assertEquals("Juan", item.getNombre());
        assertEquals("Perez", item.getPrimerApellido());
        assertEquals("Gomez", item.getSegundoApellido());
        assertEquals("juan@example.com", item.getCorreo());
        assertEquals("3000000000", item.getTelefono());
        assertEquals("Calle 123", item.getDireccion());
        assertEquals("CLIENTE", item.getRol());
        assertEquals(createdAt, item.getCreatedAt());
        System.out.println("=== CP-ADM-01 RESULTADO OBTENIDO ===");
        System.out.println("Clientes encontrados: " + response.size());
        System.out.println("Primer cliente: " + response.get(0).getNombre() + " " + response.get(0).getPrimerApellido());
    }

    @Test
    @DisplayName("CP-ADM-02: Lista vacía")
    void listarClientes_deberiaRetornarListaVacia() {
        when(usuarioRepository.findClientesConRol("CLIENTE"))
            .thenReturn(List.of());

        List<ListarClientesAdminResponse> response = usuarioService.listarClientes();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        System.out.println("=== CP-ADM-02 RESULTADO OBTENIDO ===");
        System.out.println("Clientes encontrados: " + response.size());
        System.out.println("Lista vacía: " + response.isEmpty());
    }
}
