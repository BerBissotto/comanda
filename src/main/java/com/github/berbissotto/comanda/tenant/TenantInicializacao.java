package com.github.berbissotto.comanda.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * No início da aplicação: garante a tabela central de usuários e leva todas as empresas à
 * estrutura mais recente. Para desligar a verificação das empresas em instalações muito
 * grandes (e fazê-la só ao subir uma versão nova): TENANT_MIGRAR_NO_INICIO=false.
 */
@Component
public class TenantInicializacao implements CommandLineRunner {

    private final TenantProvisioner provisionador;
    private final boolean migrarEmpresas;

    public TenantInicializacao(TenantProvisioner provisionador,
                               @Value("${app.tenant.migrar-no-inicio:true}") boolean migrarEmpresas) {
        this.provisionador = provisionador;
        this.migrarEmpresas = migrarEmpresas;
    }

    @Override
    public void run(String... args) {
        provisionador.migrarCentral();
        if (migrarEmpresas) {
            provisionador.provisionarTodas();
        }
    }
}
