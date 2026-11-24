-- =====================================================================
-- Comanda: preparo do banco para "um espaço de dados por empresa"
-- Banco: PostgreSQL 13 ou superior (testado no 16).
--
-- COMO FUNCIONA (resumo)
--   * O schema "public" guarda só o que é comum: a tabela "usuario" (login).
--   * Cada usuário cadastrado ganha, automaticamente, no mesmo banco:
--       - um schema próprio  empresa_<id>   (produtos, clientes, comandas)
--       - um papel (role) próprio  empresa_<id>, sem login, dono desse schema.
--   * Ao atender uma requisição da empresa X, a aplicação executa SET ROLE empresa_X
--     naquela conexão. A partir daí o PRÓPRIO PostgreSQL impede ler ou gravar dados de
--     outra empresa ou a tabela "usuario": se o redirecionamento falhar, dá erro de
--     permissão em vez de mostrar dado alheio.
--
-- QUEM EXECUTA ESTE SCRIPT: um superusuário (ex.: postgres), UMA vez, conectado ao banco
-- da aplicação:
--   psql -U postgres -d Comanda -f sql/01-preparar-banco.sql
-- (ou no pgAdmin, Query Tool, conectado como postgres ao banco Comanda; nesse caso troque
--  a linha "\set app_user" por um valor fixo ou rode via psql, pois "\set" é comando do psql)
--
-- Pode ser executado mais de uma vez.
-- =====================================================================

\set ON_ERROR_STOP on

-- >>> USUÁRIO QUE A APLICAÇÃO USA PARA CONECTAR (o mesmo de DB_USERNAME). Ajuste se precisar,
--     ou informe na linha de comando:  psql ... -v app_user=meu_usuario -f sql/01-preparar-banco.sql
\if :{?app_user}
\else
  \set app_user admin
\endif

SELECT current_database() AS banco \gset

BEGIN;

-- 1) Permissões que a aplicação precisa para criar a empresa de cada novo usuário.
--    CREATEROLE : criar o papel empresa_<id>. No PostgreSQL 16 só dá poder sobre os papéis
--                 que ele mesmo criou; não permite mexer em superusuários nem em outros papéis.
--    NOINHERIT  : a aplicação NÃO herda os direitos das empresas. Só enxerga os dados de uma
--                 empresa quando executa SET ROLE empresa_<id> de propósito. Fora disso
--                 (tela de login, tela de Usuários) ela só enxerga o schema public.
ALTER ROLE :"app_user" CREATEROLE NOINHERIT;
GRANT CONNECT, CREATE ON DATABASE :"banco" TO :"app_user";

-- 2) O schema public deixa de ser visível para papéis comuns. Assim nenhuma empresa consegue
--    sequer listar o que existe lá (nem a tabela usuario).
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO :"app_user";

COMMIT;

-- Conferência. Esperado: app_user com superusuario = f (se vier t, veja o aviso abaixo).
SELECT rolname AS app_user, rolsuper AS superusuario, rolcreaterole AS pode_criar_papeis,
       rolinherit AS herda_direitos
  FROM pg_roles WHERE rolname = :'app_user';

-- AVISO: se "superusuario" for t, o isolamento por papel continua funcionando (SET ROLE
-- retira os poderes de superusuário da sessão), mas um bug que executasse RESET ROLE
-- voltaria a ter acesso total. Em produção, use para a aplicação um usuário sem SUPERUSER:
--   CREATE ROLE comanda_app LOGIN PASSWORD '...';  e então \set app_user comanda_app e rode de novo.
--   (o novo usuário precisa ser dono, ou ter acesso, às tabelas do schema public.)
