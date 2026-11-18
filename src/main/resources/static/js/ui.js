/*
 * Camada de interface compartilhada por todas as telas:
 * tema claro/escuro, barra de progresso, botões "a caminho", imagens com
 * esqueleto (lazy) e revelação dos itens ao entrarem na tela.
 * Movimento sempre respeita prefers-reduced-motion.
 */
(function () {
  'use strict';

  var raiz = document.documentElement;
  var mq = window.matchMedia('(prefers-reduced-motion: reduce)');

  var UI = window.UI = {
    get reduzido() { return mq.matches; }
  };

  /* ---------------- tema ---------------- */
  function marcarTema() {
    var escuro = raiz.getAttribute('data-theme') === 'dark';
    document.querySelectorAll('[data-theme-toggle]').forEach(function (b) {
      b.setAttribute('aria-pressed', escuro ? 'true' : 'false');
      b.setAttribute('aria-label', escuro ? 'Tema escuro ativo. Mudar para claro' : 'Tema claro ativo. Mudar para escuro');
    });
  }

  document.addEventListener('click', function (e) {
    var botao = e.target.closest('[data-theme-toggle]');
    if (!botao) return;
    var novo = raiz.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
    if (!UI.reduzido) {
      raiz.classList.add('theme-anim');
      setTimeout(function () { raiz.classList.remove('theme-anim'); }, 300);
    }
    raiz.setAttribute('data-theme', novo);
    try { localStorage.setItem('comanda.tema', novo); } catch (err) { /* ignora */ }
    marcarTema();
  });
  marcarTema();

  /* ---------------- barra de progresso ---------------- */
  var barra = null;
  var miolo = null;
  var valor = 0;
  var trilha = null;
  var fim = null;

  function criarBarra() {
    if (barra) return;
    barra = document.createElement('div');
    barra.className = 'progress';
    barra.setAttribute('role', 'progressbar');
    barra.setAttribute('aria-hidden', 'true');
    miolo = document.createElement('span');
    miolo.className = 'progress__bar';
    barra.appendChild(miolo);
    document.body.appendChild(barra);
  }

  function definir(v) {
    valor = Math.max(0, Math.min(1, v));
    miolo.style.transform = 'scaleX(' + valor + ')';
  }

  UI.progresso = {
    // Começa indeterminado: sobe rápido e vai desacelerando até ~90% enquanto espera
    iniciar: function () {
      criarBarra();
      clearInterval(trilha);
      clearTimeout(fim);
      barra.classList.remove('is-done');
      barra.classList.add('is-active');
      definir(.08);
      trilha = setInterval(function () { definir(valor + (.9 - valor) * .12); }, 400);
    },
    // Valor real (0 a 1), para quando houver progresso de verdade
    definir: function (v) {
      criarBarra();
      clearInterval(trilha);
      barra.classList.add('is-active');
      definir(v);
    },
    concluir: function () {
      if (!barra) return;
      clearInterval(trilha);
      barra.classList.add('is-done');
      definir(1);
      fim = setTimeout(function () {
        barra.classList.remove('is-active');
        fim = setTimeout(function () { barra.classList.remove('is-done'); definir(0); }, 220);
      }, 200);
    },
    reiniciar: function () {
      if (!barra) return;
      clearInterval(trilha);
      clearTimeout(fim);
      barra.classList.remove('is-active', 'is-done');
      definir(0);
    }
  };

  /* ---------------- botão a caminho ---------------- */
  UI.ocupado = function (el, sim) {
    if (!el) return;
    if (sim) el.setAttribute('aria-busy', 'true');
    else el.removeAttribute('aria-busy');
  };

  /* ---------------- navegação: progresso + saída suave ---------------- */
  function linkInterno(a) {
    if (!a || !a.href || a.target || a.hasAttribute('download') || a.hasAttribute('data-sem-progresso')) return false;
    var url;
    try { url = new URL(a.href, location.href); } catch (e) { return false; }
    if (url.origin !== location.origin) return false;
    if (url.pathname === location.pathname && url.search === location.search && url.hash) return false;
    return true;
  }

  document.addEventListener('click', function (e) {
    if (e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
    var a = e.target.closest('a[href]');
    if (!linkInterno(a)) return;
    UI.progresso.iniciar();
    raiz.classList.add('is-leaving');
  });

  document.addEventListener('submit', function (e) {
    var f = e.target;
    if (e.defaultPrevented || !f || f.tagName !== 'FORM') return;
    if (f.dataset.enviando) { e.preventDefault(); return; }   // evita envio duplo
    f.dataset.enviando = '1';
    var botao = e.submitter || f.querySelector('[type="submit"]');
    UI.ocupado(botao, true);
    UI.progresso.iniciar();
    raiz.classList.add('is-leaving');
  });

  // Voltar pelo histórico (cache): zera tudo o que a navegação deixou ligado
  window.addEventListener('pageshow', function (e) {
    if (!e.persisted) return;
    raiz.classList.remove('is-leaving');
    UI.progresso.reiniciar();
    document.querySelectorAll('form[data-enviando]').forEach(function (f) { delete f.dataset.enviando; });
    document.querySelectorAll('[aria-busy="true"]').forEach(function (el) { el.removeAttribute('aria-busy'); });
  });

  /* ---------------- imagens: esqueleto até carregar ---------------- */
  function imagemPronta(caixa) { caixa.classList.add('is-loaded'); }

  function imagemFalhou(caixa, img) {
    caixa.classList.remove('has-img');
    caixa.classList.add('is-loaded');
    img.hidden = true;
    var vazio = caixa.querySelector('[data-sem-foto]');
    if (vazio) vazio.hidden = false;
  }

  UI.midia = function (raizBusca) {
    (raizBusca || document).querySelectorAll('.media.has-img').forEach(function (caixa) {
      if (caixa.dataset.midia) return;
      var img = caixa.querySelector('img');
      if (!img) return;
      caixa.dataset.midia = '1';
      img.addEventListener('load', function () { imagemPronta(caixa); });
      img.addEventListener('error', function () { imagemFalhou(caixa, img); });
      if (img.complete) {
        if (img.naturalWidth > 0) imagemPronta(caixa);
        else if (img.getAttribute('src')) imagemFalhou(caixa, img);
      }
    });
  };

  /* ---------------- revelar ao entrar na tela (lazy) ---------------- */
  var observador = null;

  function terminarReveal(el) {
    el.removeAttribute('data-reveal');
    el.classList.remove('is-in');
    el.style.removeProperty('--d');
  }

  function revelar(el, atraso) {
    el.style.setProperty('--d', atraso + 'ms');
    el.classList.add('is-in');
    var limpar = function (ev) {
      if (ev && ev.target !== el) return;
      el.removeEventListener('transitionend', limpar);
      terminarReveal(el);
    };
    el.addEventListener('transitionend', limpar);
    setTimeout(limpar, atraso + 600);   // garantia se o transitionend não vier
  }

  UI.revelar = function (raizBusca) {
    var itens = Array.prototype.slice.call((raizBusca || document).querySelectorAll('[data-reveal]'));
    if (!itens.length) return;
    if (UI.reduzido || !('IntersectionObserver' in window)) {
      itens.forEach(terminarReveal);
      return;
    }
    if (!observador) {
      observador = new IntersectionObserver(function (entradas) {
        var visiveis = entradas.filter(function (x) { return x.isIntersecting; });
        visiveis.forEach(function (x, i) {
          observador.unobserve(x.target);
          revelar(x.target, Math.min(i, 8) * 40);   // escalonado, no máximo ~320ms
        });
      }, { rootMargin: '0px 0px -32px 0px', threshold: .05 });
    }
    itens.forEach(function (el) { observador.observe(el); });
  };

  UI.midia();
  UI.revelar();
})();
