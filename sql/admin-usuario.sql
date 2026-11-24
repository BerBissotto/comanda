-- =====================================================================
-- Comanda: administrador do sistema (ROLE = ADMIN)
-- Banco: PostgreSQL (o do projeto se chama "Comanda").
--
-- O sistema NÃO cria mais o primeiro usuário sozinho nem gera senha aleatória.
-- O perfil ADMIN só existe se for gravado no banco, e é este script que faz isso.
-- A tela de Usuários cadastra apenas usuários comuns (USER).
--
-- Como usar (qualquer uma das duas):
--   psql -U <usuario> -d Comanda -f sql/admin-usuario.sql
--   ou abra este arquivo no pgAdmin (Query Tool) conectado ao banco Comanda e execute tudo.
--
-- Pode ser executado mais de uma vez sem causar erro. Se o banco for novo, rode antes
-- sql/01-preparar-banco.sql e suba a aplicação uma vez (ela cria a tabela "usuario"); depois rode
-- este script.
--
-- Cada empresa tem o seu próprio espaço de dados (schema empresa_<id>). Para usuários criados
-- por aqui, a aplicação cria esse espaço automaticamente no primeiro login (ou ao subir).
--
-- Resultado: existe o usuário admin@comanda.local com ROLE ADMIN e ativo.
--   * Se o e-mail ainda não existia, é criado com a senha inicial entregue junto com este
--     arquivo (só o hash BCrypt fica aqui; a senha em texto não é guardada no repositório).
--     TROQUE a senha no primeiro acesso: Usuários > selecionar o próprio usuário > nova senha.
--   * Se o e-mail já existia (ex.: o admin criado pela versão antiga), ele é PROMOVIDO a ADMIN
--     e mantém a senha que já tinha.
-- =====================================================================

BEGIN;

-- 0) Colunas novas da tabela usuario (a aplicação também as cria ao subir; aqui é idempotente).
--    Quem já existia vira USER e ativo.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS role  varchar(20) NOT NULL DEFAULT 'USER';
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ativo boolean     NOT NULL DEFAULT true;

-- 1) Cria o administrador (não faz nada se o e-mail já existir)
INSERT INTO usuario (nome, email, senha, role, ativo)
SELECT 'Administrador', 'admin@comanda.local',
       '$2a$10$P7/rYS5fmn6AsxxFJZ7MzuImrREIXQttu52eEfi8c99i0KM4wQOcO',
       'ADMIN', true
WHERE NOT EXISTS (SELECT 1 FROM usuario WHERE lower(email) = lower('admin@comanda.local'));

-- 2) Garante o perfil ADMIN e a situação ativo (promove o usuário se ele já existia como USER)
UPDATE usuario
   SET role = 'ADMIN', ativo = true
 WHERE lower(email) = lower('admin@comanda.local');

COMMIT;

-- Conferência: deve listar admin@comanda.local com role = ADMIN e ativo = true
SELECT id, nome, email, role, ativo FROM usuario ORDER BY id;


-- =====================================================================
-- OPCIONAL A) Criar outro ADMIN escolhendo e-mail e senha. A senha é criptografada no próprio
-- banco com a extensão pgcrypto (o hash gerado é BCrypt "$2a$", aceito pelo Spring Security).
-- Troque os três valores entre aspas, remova os "-- " do início das linhas e execute.
-- Se o CREATE EXTENSION der erro de permissão, execute-o conectado como superusuário (postgres).
-- =====================================================================
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- INSERT INTO usuario (nome, email, senha, role, ativo)
-- VALUES ('NOME DA EMPRESA', 'email@empresa.com', crypt('SENHA-COM-8-OU-MAIS', gen_salt('bf', 10)), 'ADMIN', true);

-- =====================================================================
-- OPCIONAL B) Promover um usuário que já existe para ADMIN (ou voltar a USER)
-- =====================================================================
-- UPDATE usuario SET role = 'ADMIN' WHERE lower(email) = lower('email@empresa.com');
-- UPDATE usuario SET role = 'USER'  WHERE lower(email) = lower('email@empresa.com');
