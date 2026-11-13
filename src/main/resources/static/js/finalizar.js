/* Tela final: itens, total, dados do cliente (com autocomplete), salvar e imprimir */
(function () {
  'use strict';

  var C = window.Comanda;
  var form = document.getElementById('form-comanda');
  if (!form) return;

  var lista = document.getElementById('itens');
  var vazio = document.getElementById('itens-vazio');
  var blocoTotal = document.getElementById('bloco-total');
  var valorTotal = document.getElementById('valor-total');
  var qtdTotal = document.getElementById('qtd-total');
  var mensagem = document.getElementById('mensagem');
  var btnImprimir = document.getElementById('btn-imprimir');
  var btnPdf = document.getElementById('btn-pdf');
  var btnVoltar = document.getElementById('btn-voltar');
  var recibo = document.getElementById('recibo');

  var estado = {
    itens: C.itens(),     // cópia usada na tela; depois de finalizar fica congelada
    comanda: null,        // resposta do servidor depois de salvar
    salvando: false
  };

  var ICONE_FOTO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="9" cy="10" r="1.5"/><path d="m21 16-5-5-8 8"/></svg>';
  var ICONE_LIXO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3"/></svg>';

  function esc(texto) {
    // escapa também aspas: o resultado é usado dentro de atributos HTML
    return String(texto == null ? '' : texto)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function totalDe(itens) {
    return itens.reduce(function (s, i) { return s + i.preco * i.quantidade; }, 0);
  }

  /* ---------------- lista de itens ---------------- */
  function desenhar() {
    var itens = estado.itens;
    var travado = !!estado.comanda;

    vazio.hidden = itens.length > 0;
    blocoTotal.hidden = itens.length === 0;
    lista.innerHTML = '';

    itens.forEach(function (item) {
      var li = document.createElement('li');
      li.className = 'item';
      li.dataset.id = item.id;
      li.innerHTML =
        '<div class="item__foto">' +
          (item.foto ? '<img src="' + esc(item.foto) + '" alt="" loading="lazy">' : ICONE_FOTO) +
        '</div>' +
        '<div class="item__info">' +
          '<div class="item__nome">' + esc(item.nome) + '</div>' +
          '<div class="item__unit">' + C.moeda(item.preco) + ' cada</div>' +
        '</div>' +
        '<button type="button" class="btn-icon btn-icon--danger item__remover" data-acao="excluir" ' +
          'aria-label="Remover ' + esc(item.nome) + ' da comanda"' + (travado ? ' disabled' : '') + '>' + ICONE_LIXO + '</button>' +
        '<div class="stepper item__qtd" role="group" aria-label="Quantidade de ' + esc(item.nome) + '">' +
          '<button type="button" data-acao="menos" aria-label="Diminuir quantidade"' + (travado || item.quantidade <= 1 ? ' disabled' : '') + '>−</button>' +
          '<input type="number" inputmode="numeric" min="1" max="' + C.QTD_MAX + '" value="' + item.quantidade + '" data-qtd aria-label="Quantidade"' + (travado ? ' disabled' : '') + '>' +
          '<button type="button" data-acao="mais" aria-label="Aumentar quantidade"' + (travado ? ' disabled' : '') + '>+</button>' +
        '</div>' +
        '<div class="item__subtotal">' + C.moeda(item.preco * item.quantidade) + '</div>';
      lista.appendChild(li);
    });

    var unidades = itens.reduce(function (s, i) { return s + i.quantidade; }, 0);
    valorTotal.textContent = C.moeda(totalDe(itens));
    qtdTotal.textContent = unidades + (unidades === 1 ? ' item' : ' itens');

    btnImprimir.disabled = itens.length === 0;
    btnPdf.disabled = itens.length === 0;
  }

  function sincronizar() {
    if (estado.comanda) return;
    estado.itens = C.itens();
    desenhar();
  }

  lista.addEventListener('click', function (e) {
    if (estado.comanda) return;
    var botao = e.target.closest('[data-acao]');
    var linha = e.target.closest('.item');
    if (!botao || !linha) return;
    var id = linha.dataset.id;
    var item = C.item(id);
    if (!item) return;

    if (botao.dataset.acao === 'excluir') C.remover(id);
    if (botao.dataset.acao === 'mais') C.definir(item, item.quantidade + 1);
    if (botao.dataset.acao === 'menos') C.definir(item, item.quantidade - 1);
  });

  lista.addEventListener('change', function (e) {
    if (estado.comanda || !e.target.matches('[data-qtd]')) return;
    var id = e.target.closest('.item').dataset.id;
    var item = C.item(id);
    if (item) C.definir(item, e.target.value);
    else sincronizar();
  });

  document.addEventListener('comanda:change', sincronizar);

  /* ---------------- máscara do telefone ---------------- */
  var telefone = document.getElementById('telefone');
  function mascararTelefone(valor) {
    var n = valor.replace(/\D/g, '').slice(0, 11);
    if (n.length > 10) return '(' + n.slice(0, 2) + ') ' + n.slice(2, 7) + '-' + n.slice(7);
    if (n.length > 6) return '(' + n.slice(0, 2) + ') ' + n.slice(2, 6) + '-' + n.slice(6);
    if (n.length > 2) return '(' + n.slice(0, 2) + ') ' + n.slice(2);
    return n;
  }
  telefone.addEventListener('input', function () { telefone.value = mascararTelefone(telefone.value); });

  /* ---------------- autocomplete de cliente ---------------- */
  var campoCliente = document.getElementById('cliente');
  var caixa = document.getElementById('cliente-lista');
  var campoEndereco = document.getElementById('endereco');
  var sugestoes = [];
  var ativo = -1;
  var espera = null;
  var requisicao = 0;

  function fecharLista() {
    caixa.hidden = true;
    campoCliente.setAttribute('aria-expanded', 'false');
    campoCliente.removeAttribute('aria-activedescendant');
    ativo = -1;
  }

  function marcarAtivo(novo) {
    var opcoes = caixa.querySelectorAll('[role="option"]');
    opcoes.forEach(function (o, i) { o.setAttribute('aria-selected', i === novo ? 'true' : 'false'); });
    ativo = novo;
    if (novo >= 0 && opcoes[novo]) {
      campoCliente.setAttribute('aria-activedescendant', opcoes[novo].id);
      opcoes[novo].scrollIntoView({ block: 'nearest' });
    } else {
      campoCliente.removeAttribute('aria-activedescendant');
    }
  }

  function escolher(indice) {
    var c = sugestoes[indice];
    if (!c) return;
    campoCliente.value = c.nome;
    telefone.value = c.telefone ? mascararTelefone(c.telefone) : '';
    campoEndereco.value = c.endereco || '';
    fecharLista();
  }

  function mostrarSugestoes(lista) {
    sugestoes = lista;
    caixa.innerHTML = '';
    if (!lista.length) { fecharLista(); return; }
    lista.forEach(function (c, i) {
      var li = document.createElement('li');
      li.id = 'cliente-opcao-' + i;
      li.className = 'combo__opcao';
      li.setAttribute('role', 'option');
      li.setAttribute('aria-selected', 'false');
      li.innerHTML = '<span>' + esc(c.nome) + '</span><small>' + esc([c.telefone ? mascararTelefone(c.telefone) : '', c.endereco].filter(Boolean).join(' · ')) + '</small>';
      // mousedown (e não click) para agir antes do blur do campo
      li.addEventListener('mousedown', function (ev) { ev.preventDefault(); escolher(i); });
      caixa.appendChild(li);
    });
    caixa.hidden = false;
    campoCliente.setAttribute('aria-expanded', 'true');
    ativo = -1;
  }

  function buscar(q) {
    var minha = ++requisicao;
    fetch('/clientes/busca?q=' + encodeURIComponent(q), { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
      .then(function (r) { return r.ok ? r.json() : []; })
      .then(function (dados) { if (minha === requisicao) mostrarSugestoes(dados); })
      .catch(function () { /* sem rede: o campo continua funcionando como texto livre */ });
  }

  campoCliente.addEventListener('input', function () {
    clearTimeout(espera);
    var q = campoCliente.value.trim();
    if (q.length < 2) { requisicao++; fecharLista(); return; }
    espera = setTimeout(function () { buscar(q); }, 200);
  });

  campoCliente.addEventListener('keydown', function (e) {
    if (caixa.hidden) return;
    if (e.key === 'ArrowDown') { e.preventDefault(); marcarAtivo((ativo + 1) % sugestoes.length); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); marcarAtivo(ativo <= 0 ? sugestoes.length - 1 : ativo - 1); }
    else if (e.key === 'Enter' && ativo >= 0) { e.preventDefault(); escolher(ativo); }
    else if (e.key === 'Escape') { e.preventDefault(); e.stopPropagation(); fecharLista(); }
  });

  campoCliente.addEventListener('blur', fecharLista);

  /* ---------------- salvar ---------------- */
  function mostrarMensagem(tipo, html) {
    mensagem.className = 'alert alert--' + tipo;
    mensagem.innerHTML = html;
    mensagem.hidden = false;
    mensagem.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }

  function csrf() {
    var token = document.querySelector('meta[name="_csrf"]');
    var cab = document.querySelector('meta[name="_csrf_header"]');
    return token && cab && token.content && cab.content ? { nome: cab.content, valor: token.content } : null;
  }

  function dadosDoForm() {
    return {
      mesa: form.mesa.value.trim(),
      cliente: form.cliente.value.trim(),
      telefone: form.telefone.value.trim(),
      endereco: form.endereco.value.trim(),
      observacoes: form.observacoes.value.trim(),
      itens: estado.itens.map(function (i) { return { produtoId: Number(i.id), quantidade: i.quantidade }; })
    };
  }

  function travarTela() {
    Array.prototype.forEach.call(form.elements, function (el) { el.disabled = true; });
    btnPdf.disabled = false;
    btnImprimir.disabled = false;
    btnVoltar.textContent = 'Nova comanda';
    desenhar();
  }

  // Devolve uma Promise com a comanda salva (salva uma única vez)
  function garantirSalva() {
    if (estado.comanda) return Promise.resolve(estado.comanda);
    if (estado.salvando) return Promise.reject(new Error('Salvando…'));
    if (!estado.itens.length) return Promise.reject(new Error('Adicione ao menos um produto à comanda.'));
    if (!form.reportValidity()) return Promise.reject(new Error('Preencha os campos obrigatórios.'));

    estado.salvando = true;
    var cabecalhos = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    var t = csrf();
    if (t) cabecalhos[t.nome] = t.valor;

    return fetch('/comandas', {
      method: 'POST',
      headers: cabecalhos,
      credentials: 'same-origin',
      body: JSON.stringify(dadosDoForm())
    }).then(function (r) {
      return r.json().catch(function () { return {}; }).then(function (corpo) {
        if (!r.ok) throw new Error(corpo.erro || 'Não foi possível salvar a comanda.');
        return corpo;
      });
    }).then(function (comanda) {
      estado.comanda = comanda;
      estado.dados = dadosDoForm();
      mostrarMensagem('ok', '<strong>Comanda nº ' + numeroFormatado(comanda) + ' salva.</strong> Os dados do cliente ficam guardados para as próximas comandas.');
      C.limpar();          // esvazia o carrinho; a tela segue com a cópia em estado.itens
      travarTela();
      return comanda;
    }).finally(function () { estado.salvando = false; });
  }

  function numeroFormatado(comanda) {
    var partes = String(comanda.anoMes).split('-');
    return String(comanda.numero).padStart(4, '0') + '/' + partes[1] + '-' + partes[0];
  }

  /* ---------------- impressão ---------------- */
  function montarRecibo(comanda) {
    var d = estado.dados;
    var linhas = estado.itens.map(function (i) {
      return '<div class="linha"><span>' + i.quantidade + ' x ' + esc(i.nome) + '</span><span>' + C.moeda(i.preco * i.quantidade) + '</span></div>' +
             '<div class="linha"><span>&nbsp;&nbsp;' + C.moeda(i.preco) + ' cada</span><span></span></div>';
    }).join('');

    recibo.innerHTML =
      '<h2>COMANDA Nº ' + esc(numeroFormatado(comanda)) + '</h2>' +
      '<div class="centro">' + esc(comanda.dataHora) + '</div>' +
      (d.mesa ? '<div class="centro">Mesa: ' + esc(d.mesa) + '</div>' : '') +
      '<hr>' + linhas + '<hr>' +
      '<div class="linha total"><span>TOTAL</span><span>' + C.moeda(Number(comanda.total)) + '</span></div>' +
      '<hr>' +
      '<div>Cliente: ' + esc(d.cliente) + '</div>' +
      '<div>Telefone: ' + esc(d.telefone) + '</div>' +
      '<div>Endereço: ' + esc(d.endereco) + '</div>' +
      (d.observacoes ? '<hr><div>Obs.: ' + esc(d.observacoes) + '</div>' : '') +
      '<hr><div class="centro">Documento sem valor fiscal</div>';
  }

  function imprimir(modo) {
    garantirSalva().then(function (comanda) {
      montarRecibo(comanda);
      var estilo = document.createElement('style');
      estilo.id = 'estilo-pagina';
      estilo.textContent = modo === 'termica'
        ? '@page { size: 80mm auto; margin: 4mm; }'
        : '@page { size: A4; margin: 16mm; }';
      document.head.appendChild(estilo);
      document.body.dataset.print = modo;

      var limpar = function () {
        var e = document.getElementById('estilo-pagina');
        if (e) e.remove();
        delete document.body.dataset.print;
        window.removeEventListener('afterprint', limpar);
      };
      window.addEventListener('afterprint', limpar);
      window.print();
    }).catch(function (erro) {
      mostrarMensagem('erro', '<strong>Não foi possível finalizar.</strong> ' + esc(erro.message));
    });
  }

  btnImprimir.addEventListener('click', function () { imprimir('termica'); });
  btnPdf.addEventListener('click', function () { imprimir('pdf'); });

  // Depois de finalizada, "Nova comanda" leva à lista com o carrinho vazio
  btnVoltar.addEventListener('click', function () { window.location.href = btnVoltar.dataset.href; });

  desenhar();
})();
