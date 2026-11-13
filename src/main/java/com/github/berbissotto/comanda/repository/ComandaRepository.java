package com.github.berbissotto.comanda.repository;

import com.github.berbissotto.comanda.model.Comanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    @Query("SELECT COALESCE(MAX(c.numeroMes), 0) FROM Comanda c WHERE c.anoMes = :anoMes")
    int maiorNumeroDoMes(@Param("anoMes") String anoMes);
}
