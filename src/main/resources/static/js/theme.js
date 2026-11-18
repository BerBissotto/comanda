/* Aplica o tema salvo antes da primeira pintura (evita piscar). Claro é o padrão. */
(function () {
  'use strict';
  var raiz = document.documentElement;
  var tema = 'light';
  try {
    var salvo = localStorage.getItem('comanda.tema');
    if (salvo === 'dark' || salvo === 'light') tema = salvo;
  } catch (e) { /* armazenamento indisponível: fica no claro */ }
  raiz.setAttribute('data-theme', tema);
  raiz.classList.add('js');
})();
