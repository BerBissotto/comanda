-- =====================================================================
-- Comanda: leva os produtos, clientes e comandas que já existem (schema public, versão
-- antiga do sistema) para a empresa do administrador.
--
-- QUANDO USAR: uma vez, só se você já tinha dados cadastrados antes desta versão.
-- Instalação nova não precisa deste script.
--
-- ANTES: 1) rode sql/01-preparar-banco.sql;
--        2) suba a aplicação uma vez (ela cria o schema empresa_<id> de cada usuário);
--        3) pare a aplicação para ninguém gravar durante a cópia.
-- COMO:  psql -U <usuario_da_aplicacao> -d Comanda -f sql/02-migrar-dados-atuais.sql
--        (outro e-mail de destino:  -v admin_email=outro@empresa.com)
--
-- O que faz, tudo numa única transação (se algo falhar, nada é alterado):
--   copia cliente, produto, comanda e item_comanda mantendo os mesmos ids; ajusta as
--   sequências; confere as quantidades; e renomeia as tabelas antigas para legado_*,
--   para nunca mais serem lidas por engano. Depois de conferir, apague-as se quiser:
--     DROP TABLE legado_item_comanda, legado_comanda, legado_produto, legado_cliente;
-- =====================================================================

\set ON_ERROR_STOP on

\if :{?admin_email}
\else
  \set admin_email 'admin@comanda.local'
\endif

-- Empresa de destino = a do usuário informado
SELECT COALESCE((SELECT 'empresa_' || id FROM public.usuario
                  WHERE lower(email) = lower(:'admin_email')), '') AS esquema \gset
SELECT :'esquema' <> '' AS usuario_ok \gset
\if :usuario_ok
\else
  \echo 'ERRO: usuário' :admin_email 'não encontrado em public.usuario. Nada foi alterado.'
  \quit
\endif

-- O schema da empresa precisa existir (a aplicação o cria ao subir)
-- (consulta o catálogo: a aplicação, fora do SET ROLE, não tem acesso ao schema da empresa)
SELECT EXISTS (SELECT 1 FROM pg_tables
                WHERE schemaname = :'esquema'
                  AND tablename IN ('cliente', 'produto', 'comanda', 'item_comanda')
               HAVING count(*) = 4) AS destino_ok \gset
\if :destino_ok
\else
  \echo 'ERRO: schema' :esquema 'ainda não existe. Suba a aplicação uma vez e rode de novo.'
  \quit
\endif

-- Precisa existir algo a migrar (depois da migração as tabelas viram legado_*)
SELECT to_regclass('public.produto') IS NOT NULL
   AND to_regclass('public.cliente') IS NOT NULL
   AND to_regclass('public.comanda') IS NOT NULL
   AND to_regclass('public.item_comanda') IS NOT NULL AS ha_legado \gset
\if :ha_legado
\else
  \echo 'Nada a migrar: as tabelas antigas (public.produto, ...) não existem.'
  \quit
\endif

\echo 'Migrando dados antigos para o schema' :esquema 'da empresa' :admin_email

BEGIN;

-- A empresa só precisa LER o legado durante a cópia; as permissões são retiradas no fim.
GRANT USAGE ON SCHEMA public TO :"esquema";
GRANT SELECT ON public.cliente, public.produto, public.comanda, public.item_comanda TO :"esquema";

SET LOCAL ROLE :"esquema";
SET LOCAL search_path TO :"esquema";

-- Não sobrescreve nada: o destino precisa estar vazio
SELECT (SELECT count(*) FROM cliente) + (SELECT count(*) FROM produto)
     + (SELECT count(*) FROM comanda) + (SELECT count(*) FROM item_comanda) = 0 AS destino_vazio \gset
\if :destino_vazio
\else
  \echo 'ERRO: a empresa já tem dados. Nada foi alterado.'
  ROLLBACK;
  \quit
\endif

INSERT INTO cliente (id, nome, telefone, endereco) OVERRIDING SYSTEM VALUE
SELECT id, nome, telefone, endereco FROM public.cliente;

INSERT INTO produto (id, nome, descricao, categoria, quantidade, preco, foto, foto_tipo) OVERRIDING SYSTEM VALUE
SELECT id, nome, descricao, categoria, quantidade, preco, foto, foto_tipo FROM public.produto;

INSERT INTO comanda (id, ano_mes, numero_mes, data_hora, mesa, cliente_id, cliente_nome,
                     telefone, endereco, observacoes, total) OVERRIDING SYSTEM VALUE
SELECT id, ano_mes, numero_mes, data_hora, mesa, cliente_id, cliente_nome,
       telefone, endereco, observacoes, total FROM public.comanda;

INSERT INTO item_comanda (id, comanda_id, produto_id, nome_produto, preco_unitario, quantidade) OVERRIDING SYSTEM VALUE
SELECT id, comanda_id, produto_id, nome_produto, preco_unitario, quantidade FROM public.item_comanda;

-- Novas linhas continuam a numeração depois do maior id copiado
SELECT setval(pg_get_serial_sequence('cliente', 'id'),      COALESCE(max(id), 0) + 1, false) FROM cliente;
SELECT setval(pg_get_serial_sequence('produto', 'id'),      COALESCE(max(id), 0) + 1, false) FROM produto;
SELECT setval(pg_get_serial_sequence('comanda', 'id'),      COALESCE(max(id), 0) + 1, false) FROM comanda;
SELECT setval(pg_get_serial_sequence('item_comanda', 'id'), COALESCE(max(id), 0) + 1, false) FROM item_comanda;

-- Conferência de quantidades (cópia incompleta = desfaz tudo)
SELECT (SELECT count(*) FROM cliente)      = (SELECT count(*) FROM public.cliente)
   AND (SELECT count(*) FROM produto)      = (SELECT count(*) FROM public.produto)
   AND (SELECT count(*) FROM comanda)      = (SELECT count(*) FROM public.comanda)
   AND (SELECT count(*) FROM item_comanda) = (SELECT count(*) FROM public.item_comanda) AS conferiu \gset
\if :conferiu
\else
  \echo 'ERRO: as quantidades não batem. Nada foi alterado.'
  ROLLBACK;
  \quit
\endif

RESET ROLE;

REVOKE SELECT ON public.cliente, public.produto, public.comanda, public.item_comanda FROM :"esquema";
REVOKE USAGE ON SCHEMA public FROM :"esquema";

ALTER TABLE public.item_comanda RENAME TO legado_item_comanda;
ALTER TABLE public.comanda      RENAME TO legado_comanda;
ALTER TABLE public.produto      RENAME TO legado_produto;
ALTER TABLE public.cliente      RENAME TO legado_cliente;

COMMIT;

\echo 'Concluído. Dados antigos copiados; tabelas antigas renomeadas para legado_*.'
