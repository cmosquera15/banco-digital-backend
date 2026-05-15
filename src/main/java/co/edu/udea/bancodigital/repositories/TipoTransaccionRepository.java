package co.edu.udea.bancodigital.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.udea.bancodigital.models.entities.catalogs.TipoTransaccion;

@Repository
public interface TipoTransaccionRepository extends JpaRepository<TipoTransaccion, Integer> {

    @Query("""
            SELECT t
            FROM TipoTransaccion t
            WHERE UPPER(t.nombre) = UPPER(:nombre)
        """)
    Optional<TipoTransaccion> findByNombreIgnoreCase(String nombre);
}
