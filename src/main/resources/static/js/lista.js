/*
 * Lista de produtos: o card selecionado abre num diálogo por cima da lista
 * (sem deslocar os outros itens), com o fundo desfocado ao redor, e permite
 * escolher a quantidade e adicionar/remover da comanda.
 */
(function () {
  'use strict';

  var UI = window.UI || { reduzido: false };
  var grid = document.getElementById('produtos-grid');
  var barra = document.querySelector('[data-comanda-bar]');
  var aviso = document.getElementById('aviso-status');
  var raiz = document.documentElement;

  function anunciar(texto) {
    if (aviso) { aviso.textContent = ''; setTimeout(function () { aviso.textContent = texto; }, 30); }
  }

  /* ---- barra fixa com o resumo da comanda (sobe e desce) ---- */
  function atualizarBarra() {
    if (!barra) return;
    var t = window.Comanda.totais();
    var visivel = t.unidades > 0;
    barra.classList.toggle('is-visible', visivel);
    barra.inert = !visivel;
    document.body.classList.toggle('has-bar', visivel);
    barra.querySelector('[data-bar-itens]').textContent =
      t.unidades + (t.unidades === 1 ? ' item' : ' itens');
    barra.querySelector('[data-bar-total]').textContent = window.Comanda.moeda(t.valor);
  }

  if (!grid) {
    atualizarBarra();
    document.addEventListener('comanda:change', atualizarBarra);
    return;
  }

  var dlg = document.getElementById('produto-dialog');
  var painel = dlg.querySelector('.pdlg__panel');
  var D = {
    midia: dlg.querySelector('[data-media]'),
    img: dlg.querySelector('[data-foto]'),
    semFoto: dlg.querySelector('[data-sem-foto]'),
    nome: dlg.querySelector('[data-nome]'),
    descricao: dlg.querySelector('[data-descricao]'),
    preco: dlg.querySelector('[data-preco]'),
    campo: dlg.querySelector('[data-qtd]'),
    menos: dlg.querySelector('[data-acao="menos"]'),
    adicionar: dlg.querySelector('[data-acao="adicionar"]'),
    remover: dlg.querySelector('[data-acao="remover"]')
  };

  var cards = Array.prototype.slice.call(grid.querySelectorAll('.produto-card'));
  var atual = null;          // card exibido no diálogo
  var terminar = null;       // encerra o fechamento animado em andamento
  var timerOk = null;

  function dados(card) {
    return {
      id: card.dataset.id,
      nome: card.dataset.nome,
      preco: Number(card.dataset.preco),
      foto: card.dataset.foto || null
    };
  }

  function lerQtd() { return window.Comanda.limitar(D.campo.value); }

  /* ---- estado do selo no card da lista ---- */
  function atualizarCard(card) {
    var item = window.Comanda.item(card.dataset.id);
    var selo = card.querySelector('[data-selo]');
    var texto = item ? 'Na comanda · ' + item.quantidade : '';
    var mudou = selo.textContent !== texto;

    selo.hidden = !item;
    selo.textContent = texto;
    if (item && mudou && !UI.reduzido) {
      selo.classList.remove('is-bump');
      void selo.offsetWidth;
      selo.classList.add('is-bump');
    }
  }

  /* ---- estado dos botões do diálogo conforme o que já está na comanda ---- */
  function atualizarDialog() {
    if (!atual) return;
    var item = window.Comanda.item(atual.dataset.id);
    D.adicionar.querySelector('[data-rotulo]').textContent = item ? 'Atualizar quantidade' : 'Adicionar à comanda';
    D.remover.hidden = !item;
    if (item && document.activeElement !== D.campo) D.campo.value = item.quantidade;
    D.menos.disabled = lerQtd() <= 1;
  }

  function atualizarTudo() {
    cards.forEach(atualizarCard);
    atualizarDialog();
    atualizarBarra();
  }

  /* ---- preencher o diálogo com os dados do card ---- */
  function preencher(card) {
    var d = dados(card);
    D.nome.textContent = d.nome;
    D.descricao.textContent = card.dataset.descricao ? card.dataset.descricao : 'Sem descrição.';
    D.preco.textContent = window.Comanda.moeda(d.preco);

    D.midia.classList.remove('is-loaded');
    if (d.foto) {
      D.midia.classList.add('has-img');
      D.semFoto.hidden = true;
      D.img.hidden = false;
      D.img.src = d.foto;
      if (D.img.complete && D.img.naturalWidth > 0) D.midia.classList.add('is-loaded');
    } else {
      D.midia.classList.remove('has-img');
      D.img.hidden = true;
      D.img.removeAttribute('src');
      D.semFoto.hidden = false;
    }

    D.campo.value = 1;
    D.adicionar.classList.remove('is-ok');
    clearTimeout(timerOk);
    atualizarDialog();
  }

  D.img.addEventListener('load', function () { D.midia.classList.add('is-loaded'); });
  D.img.addEventListener('error', function () {
    D.midia.classList.remove('has-img');
    D.midia.classList.add('is-loaded');
    D.img.hidden = true;
    D.semFoto.hidden = false;
  });

  /* ---- abrir: cresce a partir do card clicado ---- */
  function abrir(card, porTeclado) {
    if (terminar) terminar();
    if (atual) atual.classList.remove('is-selected');

    atual = card;
    atual.classList.add('is-selected');
    preencher(card);

    var animar = !UI.reduzido && !porTeclado;    // teclado nunca anima
    dlg.classList.remove('is-visible', 'is-closing');
    dlg.style.removeProperty('--from');
    dlg.dataset.anim = animar ? 'on' : 'off';
    raiz.classList.add('dlg-open');
    dlg.showModal();

    if (animar) {
      // origem = centro do card clicado; só translate + scale uniforme (sem distorcer)
      var c = card.getBoundingClientRect();
      var p = dlg.getBoundingClientRect();
      var dx = (c.left + c.width / 2) - (p.left + p.width / 2);
      var dy = (c.top + c.height / 2) - (p.top + p.height / 2);
      var s = Math.max(.4, Math.min(1, c.width / p.width));
      dlg.style.setProperty('--from', 'translate(' + dx + 'px, ' + dy + 'px) scale(' + s + ')');
      void painel.offsetWidth;                   // fixa o estado inicial antes de transicionar
    }
    dlg.classList.add('is-visible');
  }

  /* ---- fechar: mais curto e sutil que a abertura ---- */
  function concluirFechamento() {
    terminar = null;
    dlg.classList.remove('is-visible', 'is-closing');
    if (dlg.open) dlg.close();
    raiz.classList.remove('dlg-open');
    if (atual) {
      atual.classList.remove('is-selected');
      var gatilho = atual.querySelector('.produto-card__toggle');
      atual = null;
      if (gatilho) gatilho.focus({ preventScroll: true });
    }
  }

  function fechar(imediato) {
    if (!atual || terminar) return;
    if (imediato || UI.reduzido) { concluirFechamento(); return; }

    var feito = false;
    var fim = function () {
      if (feito) return;
      feito = true;
      painel.removeEventListener('transitionend', aoTransicionar);
      concluirFechamento();
    };
    var aoTransicionar = function (e) {
      if (e.target === painel && e.propertyName === 'opacity') fim();
    };
    terminar = fim;
    painel.addEventListener('transitionend', aoTransicionar);
    setTimeout(fim, 300);                        // garantia se o transitionend não vier
    dlg.classList.remove('is-visible');
    dlg.classList.add('is-closing');
  }

  // Esc: ação de teclado, fecha na hora
  dlg.addEventListener('cancel', function (e) { e.preventDefault(); fechar(true); });
  // Clique no fundo desfocado (fora do painel)
  dlg.addEventListener('click', function (e) { if (e.target === dlg) fechar(false); });
  dlg.addEventListener('close', function () { raiz.classList.remove('dlg-open'); });

  /* ---- eventos da lista ---- */
  grid.addEventListener('click', function (e) {
    var toggle = e.target.closest('.produto-card__toggle');
    if (!toggle) return;
    abrir(toggle.closest('.produto-card'), e.detail === 0);
  });

  /* ---- eventos do diálogo ---- */
  dlg.addEventListener('click', function (e) {
    var alvo = e.target.closest('[data-acao]');
    if (!alvo || !atual) return;
    var acao = alvo.dataset.acao;

    if (acao === 'mais') { D.campo.value = window.Comanda.limitar(lerQtd() + 1); atualizarDialog(); }
    if (acao === 'menos') { D.campo.value = window.Comanda.limitar(lerQtd() - 1); atualizarDialog(); }
    if (acao === 'fechar') fechar(e.detail === 0);
    if (acao === 'adicionar') {
      window.Comanda.definir(dados(atual), lerQtd());
      anunciar(atual.dataset.nome + ' na comanda: ' + lerQtd());
      D.adicionar.classList.add('is-ok');
      clearTimeout(timerOk);
      timerOk = setTimeout(function () { D.adicionar.classList.remove('is-ok'); }, 1400);
    }
    if (acao === 'remover') {
      window.Comanda.remover(atual.dataset.id);
      D.campo.value = 1;
      anunciar(atual.dataset.nome + ' removido da comanda');
      atualizarDialog();
    }
  });

  dlg.addEventListener('input', function (e) {
    if (e.target.matches('[data-qtd]')) D.menos.disabled = window.Comanda.limitar(e.target.value) <= 1;
  });

  // Ao sair do campo, normaliza o valor digitado (vazio, 0, negativo...)
  dlg.addEventListener('change', function (e) {
    if (e.target.matches('[data-qtd]')) {
      e.target.value = window.Comanda.limitar(e.target.value);
      atualizarDialog();
    }
  });

  document.addEventListener('comanda:change', atualizarTudo);
  atualizarTudo();
})();
