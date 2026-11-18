/* Cadastro de produto: escolher imagem e ver o preview antes de salvar */
(function () {
  'use strict';

  var entrada = document.getElementById('foto');
  var preview = document.getElementById('preview');
  var nome = document.getElementById('foto-nome');
  var remover = document.getElementById('foto-remover');
  var erro = document.getElementById('foto-erro');
  if (!entrada) return;

  var LIMITE = 10 * 1024 * 1024; // mesmo limite do servidor (10MB)
  var textoVazio = preview.textContent.trim();
  var leitor = null;

  function limpar() {
    if (leitor) { leitor.onload = leitor.onerror = null; leitor.abort(); leitor = null; }
    entrada.value = '';
    preview.classList.remove('has-image', 'is-loading', 'is-ready');
    preview.textContent = textoVazio;
    nome.textContent = 'Nenhuma imagem escolhida';
    remover.hidden = true;
  }

  function avisar(texto) {
    erro.textContent = texto;
    erro.hidden = !texto;
  }

  entrada.addEventListener('change', function () {
    avisar('');
    var arquivo = entrada.files && entrada.files[0];
    if (!arquivo) { limpar(); return; }

    if (!arquivo.type || arquivo.type.indexOf('image/') !== 0) {
      limpar(); avisar('O arquivo precisa ser uma imagem (PNG, JPG, WEBP...).'); return;
    }
    if (arquivo.size > LIMITE) {
      limpar(); avisar('A imagem tem mais de 10 MB. Escolha uma menor.'); return;
    }

    // A CSP da página só aceita imagens do próprio site ou data:, então o preview vem de data URL
    if (leitor) { leitor.onload = leitor.onerror = null; leitor.abort(); }
    // esqueleto já durante a leitura do arquivo
    preview.textContent = '';
    preview.classList.remove('is-ready');
    preview.classList.add('has-image', 'is-loading');
    leitor = new FileReader();
    leitor.onerror = function () { limpar(); avisar('Não foi possível ler essa imagem.'); };
    leitor.onload = function () { mostrarPreview(arquivo, leitor.result); };
    leitor.readAsDataURL(arquivo);
  });

  function mostrarPreview(arquivo, url) {
    // esqueleto até a imagem ficar pronta; depois aparece com fade
    var img = document.createElement('img');
    img.alt = 'Pré-visualização da imagem escolhida';
    preview.textContent = '';
    preview.classList.remove('is-ready');
    preview.classList.add('has-image', 'is-loading');
    img.addEventListener('load', function () {
      preview.classList.remove('is-loading');
      preview.classList.add('is-ready');
    });
    img.addEventListener('error', function () { limpar(); avisar('Não foi possível ler essa imagem.'); });
    preview.appendChild(img);
    img.src = url;

    nome.textContent = arquivo.name;
    remover.hidden = false;
  }

  remover.addEventListener('click', function () { limpar(); avisar(''); });
})();
