# Banco de dados: uma empresa = um espaço de dados próprio

Um único banco PostgreSQL (`Comanda`):

| Onde | O que fica |
|---|---|
| schema `public` | só o login: tabela `usuario` |
| schema `empresa_<id>` (um por usuário) | `produto`, `cliente`, `comanda`, `item_comanda` daquela empresa |

Cada empresa também tem um papel (role) `empresa_<id>`, dono do schema. A cada requisição a
aplicação executa `SET ROLE empresa_<id>` na conexão; o PostgreSQL passa a recusar qualquer acesso
a outra empresa e à tabela `usuario`. Se o redirecionamento falhar, o resultado é erro de permissão,
nunca dado de outra empresa.

## Ordem de execução

1. **Uma vez**, como superusuário: `psql -U postgres -d Comanda -f sql/01-preparar-banco.sql`
   (se o usuário da aplicação não for `admin`: `-v app_user=nome_do_usuario`).
2. Subir a aplicação. Ela cria a tabela `usuario` (se faltar) e o espaço de cada usuário.
3. Se já existiam produtos/clientes/comandas da versão antiga: parar a aplicação e rodar
   `psql -U <usuario_da_aplicacao> -d Comanda -f sql/02-migrar-dados-atuais.sql`
   (copia tudo para a empresa do `admin@comanda.local`; outro e-mail: `-v admin_email=...`).
4. `sql/admin-usuario.sql` cria/promove o administrador.

## Mudar a estrutura das tabelas das empresas

Crie `src/main/resources/db/tenant/V2__descricao.sql` (depois V3, ...). Ao subir, a aplicação aplica
em todas as empresas o que ainda não foi aplicado (controle na tabela `schema_versao` de cada
empresa). Nunca edite um arquivo V já publicado.
