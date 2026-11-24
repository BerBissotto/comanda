package com.github.berbissotto.comanda.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantContextTest {

    @AfterEach
    void limpar() {
        TenantContext.limpar();
    }

    @Test
    void schemaVemDoIdDoUsuario() {
        assertEquals("empresa_7", TenantContext.schemaDe(7L));
    }

    @Test
    void idInvalidoEhRecusado() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.schemaDe(null));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.schemaDe(0L));
        assertThrows(IllegalArgumentException.class, () -> TenantContext.schemaDe(-3L));
    }

    @Test
    void soAceitaOFormatoExatoDeEmpresa() {
        assertTrue(TenantContext.valido("empresa_1"));
        assertTrue(TenantContext.valido("empresa_123456"));
        assertFalse(TenantContext.valido(null));
        assertFalse(TenantContext.valido(""));
        assertFalse(TenantContext.valido("empresa_0"));
        assertFalse(TenantContext.valido("empresa_01"));
        assertFalse(TenantContext.valido("public"));
        assertFalse(TenantContext.valido("empresa_1; DROP TABLE usuario"));
        assertFalse(TenantContext.valido("empresa_1\"; SET ROLE postgres; --"));
        assertFalse(TenantContext.valido("EMPRESA_1"));
        assertFalse(TenantContext.valido("empresa_1\n"));
    }

    @Test
    void definirRecusaNomeInvalidoEMantemContextoCentral() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.definir("public"));
        assertNull(TenantContext.atual());
    }

    @Test
    void definirELimpar() {
        TenantContext.definir("empresa_9");
        assertEquals("empresa_9", TenantContext.atual());
        TenantContext.limpar();
        assertNull(TenantContext.atual());
    }

    @Test
    void cadaThreadTemASuaEmpresa() throws Exception {
        TenantContext.definir("empresa_1");
        String[] naOutraThread = new String[1];
        Thread outra = new Thread(() -> naOutraThread[0] = TenantContext.atual());
        outra.start();
        outra.join();
        assertNull(naOutraThread[0]);
        assertEquals("empresa_1", TenantContext.atual());
    }
}
