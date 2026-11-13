/*
 * Carrinho da comanda, guardado no navegador (localStorage) para sobreviver à
 * paginação da lista e a um F5. Os preços aqui servem só para exibição: ao
 * finalizar, o servidor recalcula tudo a partir do banco.
 */
(function () {
  'use strict';

  var CHAVE = 'comanda.itens.v1';
  var QTD_MAX = 999;

  function ler() {
    try {
      var bruto = localStorage.getItem(CHAVE);
      var lista = bruto ? JSON.parse(bruto) : [];
      return Array.isArray(lista) ? lista : [];
    } catch (e) {
      return [];
    }
  }

  function gravar(itens) {
    try {
      localStorage.setItem(CHAVE, JSON.stringify(itens));
    } catch (e) { /* armazenamento indisponível: segue só em memória */ }
    document.dispatchEvent(new CustomEvent('comanda:change'));
  }

  function limitar(qtd) {
    var n = Math.floor(Number(qtd));
    if (!isFinite(n) || n < 1) return 1;
    return Math.min(n, QTD_MAX);
  }

  var moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

  window.Comanda = {
    itens: ler,

    item: function (id) {
      return ler().find(function (i) { return String(i.id) === String(id); }) || null;
    },

    // Adiciona o produto ou troca a quantidade dele
    definir: function (produto, quantidade) {
      var itens = ler();
      var existente = itens.find(function (i) { return String(i.id) === String(produto.id); });
      if (existente) {
        existente.quantidade = limitar(quantidade);
      } else {
        itens.push({
          id: produto.id,
          nome: produto.nome,
          preco: Number(produto.preco) || 0,
          foto: produto.foto || null,
          quantidade: limitar(quantidade)
        });
      }
      gravar(itens);
    },

    remover: function (id) {
      gravar(ler().filter(function (i) { return String(i.id) !== String(id); }));
    },

    limpar: function () { gravar([]); },

    totais: function () {
      var itens = ler();
      return {
        linhas: itens.length,
        unidades: itens.reduce(function (s, i) { return s + i.quantidade; }, 0),
        valor: itens.reduce(function (s, i) { return s + i.preco * i.quantidade; }, 0)
      };
    },

    moeda: function (valor) { return moeda.format(valor); },
    limitar: limitar,
    QTD_MAX: QTD_MAX
  };

  // Contador do menu, em todas as telas
  function atualizarMenu() {
    var t = window.Comanda.totais();
    document.querySelectorAll('[data-comanda-count]').forEach(function (el) {
      el.textContent = t.unidades;
    });
  }
  document.addEventListener('comanda:change', atualizarMenu);
  window.addEventListener('storage', function (e) { if (e.key === CHAVE) { atualizarMenu(); document.dispatchEvent(new CustomEvent('comanda:change')); } });
  document.addEventListener('DOMContentLoaded', atualizarMenu);

  // Ao sair, a comanda em andamento não fica para o próximo usuário
  document.addEventListener('submit', function (e) {
    if (e.target.matches && e.target.matches('form[data-logout]')) {
      try { localStorage.removeItem(CHAVE); } catch (err) { /* ignora */ }
    }
  });
})();
