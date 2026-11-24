package com.github.berbissotto.comanda.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDataSourceTest {

    private DataSource pool;
    private Connection real;
    private Statement comando;
    private TenantDataSource fonte;

    @BeforeEach
    void preparar() throws SQLException {
        pool = mock(DataSource.class);
        real = mock(Connection.class);
        comando = mock(Statement.class);
        when(pool.getConnection()).thenReturn(real);
        when(real.createStatement()).thenReturn(comando);
        fonte = new TenantDataSource(pool);
    }

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void comEmpresaFixaPapelESchemaAntesDeEntregar() throws SQLException {
        TenantContext.definir("empresa_5");

        fonte.getConnection();

        verify(comando).execute("SET ROLE \"empresa_5\"; SET search_path TO \"empresa_5\"");
    }

    @Test
    void semEmpresaVoltaAoContextoCentral() throws SQLException {
        fonte.getConnection();

        verify(comando).execute("RESET ROLE; SET search_path TO public");
    }

    @Test
    void conexaoReusadaPorOutraEmpresaRecebeOEstadoNovoCompleto() throws SQLException {
        TenantContext.definir("empresa_1");
        fonte.getConnection();
        TenantContext.definir("empresa_2");
        fonte.getConnection();

        verify(comando).execute("SET ROLE \"empresa_1\"; SET search_path TO \"empresa_1\"");
        verify(comando).execute("SET ROLE \"empresa_2\"; SET search_path TO \"empresa_2\"");
    }

    @Test
    void seAPreparacaoFalharAConexaoEhDescartadaENaoUsada() throws SQLException {
        TenantContext.definir("empresa_5");
        when(comando.execute("SET ROLE \"empresa_5\"; SET search_path TO \"empresa_5\""))
                .thenThrow(new SQLException("role inexistente"));

        assertThrows(SQLException.class, () -> fonte.getConnection());

        verify(real).close();
    }

    @Test
    void aoFecharLimpaOEstadoEDevolveAoPool() throws SQLException {
        TenantContext.definir("empresa_5");
        Connection entregue = fonte.getConnection();
        when(real.isClosed()).thenReturn(false);

        entregue.close();

        var ordem = inOrder(comando, real);
        ordem.verify(comando).execute("SET ROLE \"empresa_5\"; SET search_path TO \"empresa_5\"");
        ordem.verify(comando).execute("RESET ROLE; SET search_path TO public");
        ordem.verify(real).close();
    }

    @Test
    void falhaNaLimpezaNaoImpedeDevolverAoPool() throws SQLException {
        TenantContext.definir("empresa_5");
        Connection entregue = fonte.getConnection();
        when(real.isClosed()).thenReturn(false);
        when(comando.execute("RESET ROLE; SET search_path TO public")).thenThrow(new SQLException("tx abortada"));

        entregue.close();

        verify(real).close();
    }

    @Test
    void nomeDeEmpresaInvalidoNuncaViraSql() {
        assertThrows(IllegalArgumentException.class, () -> TenantDataSource.sqlDaEmpresa("public"));
        assertThrows(IllegalArgumentException.class,
                () -> TenantDataSource.sqlDaEmpresa("empresa_1\"; RESET ROLE; --"));
    }

    @Test
    void outrosMetodosSaoRepassadosParaAConexaoReal() throws SQLException {
        Connection entregue = fonte.getConnection();

        entregue.setAutoCommit(false);

        verify(real).setAutoCommit(false);
        verify(real, never()).close();
    }
}
