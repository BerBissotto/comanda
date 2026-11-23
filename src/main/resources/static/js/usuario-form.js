/*
 * Cadastro de usuário: botão para mostrar/ocultar a senha digitada.
 * O botão nasce escondido e só aparece com JavaScript, então sem script o campo segue funcionando.
 */
(function () {
  'use strict';

  var campo = document.getElementById('senha');
  var botao = document.querySelector('[data-senha-ver]');
  if (!campo || !botao) return;

  botao.hidden = false;

  function definir(mostrar) {
    campo.type = mostrar ? 'text' : 'password';
    botao.setAttribute('aria-pressed', mostrar ? 'true' : 'false');
    botao.setAttribute('aria-label', mostrar ? 'Ocultar senha' : 'Mostrar senha');
  }

  botao.addEventListener('click', function () {
    definir(campo.type === 'password');
    campo.focus();
  });

  // Nunca envia nem guarda no histórico com a senha à vista
  if (campo.form) {
    campo.form.addEventListener('submit', function () { definir(false); });
  }
  window.addEventListener('pageshow', function () { definir(false); });
})();
