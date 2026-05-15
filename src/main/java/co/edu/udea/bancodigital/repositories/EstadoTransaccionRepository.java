package co.edu.udea.bancodigital.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.udea.bancodigital.models.entities.catalogs.EstadoTransaccion;

@Repository
public interface EstadoTransaccionRepository extends JpaRepository<EstadoTransaccion, Integer> {

    @Query("""
            SELECT e
            FROM EstadoTransaccion e
            WHERE UPPER(e.nombre) = UPPER(:nombre)
        """)
    Optional<EstadoTransaccion> findByNombreIgnoreCase(String nombre);
}
