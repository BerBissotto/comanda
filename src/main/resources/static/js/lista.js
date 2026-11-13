/* Lista de produtos: card que expande, quantidade e adicionar/remover da comanda */
(function () {
  'use strict';

  var grid = document.getElementById('produtos-grid');
  var barra = document.querySelector('[data-comanda-bar]');
  var aviso = document.getElementById('aviso-status');
  if (!grid) {
    atualizarBarra();
    document.addEventListener('comanda:change', atualizarBarra);
    return;
  }

  var reduzir = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var cards = Array.prototype.slice.call(grid.querySelectorAll('.produto-card'));
  var aberto = null;

  // Nome único por card para o navegador animar a posição/tamanho de cada um
  cards.forEach(function (card) {
    card.style.viewTransitionName = 'card-' + card.dataset.id;
  });

  function dados(card) {
    return {
      id: card.dataset.id,
      nome: card.dataset.nome,
      preco: Number(card.dataset.preco),
      foto: card.dataset.foto || null
    };
  }

  function campoQtd(card) { return card.querySelector('[data-qtd]'); }

  function lerQtd(card) { return window.Comanda.limitar(campoQtd(card).value); }

  function anunciar(texto) {
    if (aviso) { aviso.textContent = ''; setTimeout(function () { aviso.textContent = texto; }, 30); }
  }

  /* ---- estado visual de um card conforme o que já está na comanda ---- */
  function atualizarCard(card) {
    var item = window.Comanda.item(card.dataset.id);
    var selo = card.querySelector('[data-selo]');
    var btnAdd = card.querySelector('[data-acao="adicionar"]');
    var btnRem = card.querySelector('[data-acao="remover"]');

    selo.hidden = !item;
    if (item) selo.textContent = 'Na comanda · ' + item.quantidade;

    btnAdd.textContent = item ? 'Atualizar quantidade' : 'Adicionar à comanda';
    btnRem.hidden = !item;

    var campo = campoQtd(card);
    if (item && document.activeElement !== campo) campo.value = item.quantidade;
    card.querySelector('[data-acao="menos"]').disabled = lerQtd(card) <= 1;
  }

  function atualizarBarra() {
    if (!barra) return;
    var t = window.Comanda.totais();
    barra.hidden = t.unidades === 0;
    barra.querySelector('[data-bar-itens]').textContent =
      t.unidades + (t.unidades === 1 ? ' item' : ' itens');
    barra.querySelector('[data-bar-total]').textContent = window.Comanda.moeda(t.valor);
  }

  function atualizarTudo() {
    cards.forEach(atualizarCard);
    atualizarBarra();
  }

  /* ---- abrir / fechar com animação ---- */
  function aplicar(card, abrir) {
    if (aberto && aberto !== card) fecharAgora(aberto);
    if (abrir) {
      card.classList.add('is-open');
      card.querySelector('.produto-card__toggle').setAttribute('aria-expanded', 'true');
      card.querySelector('.produto-card__detalhe').hidden = false;
      aberto = card;
    } else {
      fecharAgora(card);
      aberto = null;
    }
  }

  function fecharAgora(card) {
    card.classList.remove('is-open');
    card.querySelector('.produto-card__toggle').setAttribute('aria-expanded', 'false');
    card.querySelector('.produto-card__detalhe').hidden = true;
  }

  function alternar(card) {
    var abrir = aberto !== card;
    var depois = function () {
      if (abrir) card.scrollIntoView({ block: 'nearest', behavior: reduzir ? 'auto' : 'smooth' });
    };

    if (document.startViewTransition && !reduzir) {
      try {
        document.startViewTransition(function () { aplicar(card, abrir); }).finished.then(depois, depois);
        return;
      } catch (e) { /* cai para a troca simples */ }
    }
    aplicar(card, abrir);
    depois();
  }

  /* ---- eventos (delegação) ---- */
  grid.addEventListener('click', function (e) {
    var card = e.target.closest('.produto-card');
    if (!card) return;

    var alvo = e.target.closest('[data-acao]');
    if (!alvo) {
      if (e.target.closest('.produto-card__toggle')) alternar(card);
      return;
    }

    var acao = alvo.dataset.acao;
    var campo = campoQtd(card);

    if (acao === 'mais') { campo.value = window.Comanda.limitar(lerQtd(card) + 1); atualizarCard(card); }
    if (acao === 'menos') { campo.value = window.Comanda.limitar(lerQtd(card) - 1); atualizarCard(card); }
    if (acao === 'fechar') { alternar(card); card.querySelector('.produto-card__toggle').focus(); }
    if (acao === 'adicionar') {
      window.Comanda.definir(dados(card), lerQtd(card));
      anunciar(card.dataset.nome + ' na comanda: ' + lerQtd(card));
    }
    if (acao === 'remover') {
      window.Comanda.remover(card.dataset.id);
      campo.value = 1;
      anunciar(card.dataset.nome + ' removido da comanda');
    }
  });

  grid.addEventListener('input', function (e) {
    if (e.target.matches('[data-qtd]')) {
      var card = e.target.closest('.produto-card');
      card.querySelector('[data-acao="menos"]').disabled = window.Comanda.limitar(e.target.value) <= 1;
    }
  });

  // Ao sair do campo, normaliza o valor digitado (vazio, 0, negativo...)
  grid.addEventListener('change', function (e) {
    if (e.target.matches('[data-qtd]')) {
      e.target.value = window.Comanda.limitar(e.target.value);
      atualizarCard(e.target.closest('.produto-card'));
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && aberto) {
      var card = aberto;
      alternar(card);
      card.querySelector('.produto-card__toggle').focus();
    }
  });

  document.addEventListener('comanda:change', atualizarTudo);
  atualizarTudo();
})();
