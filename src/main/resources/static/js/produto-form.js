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
  var urlAtual = null;

  function limpar() {
    if (urlAtual) { URL.revokeObjectURL(urlAtual); urlAtual = null; }
    entrada.value = '';
    preview.classList.remove('has-image');
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

    if (urlAtual) URL.revokeObjectURL(urlAtual);
    urlAtual = URL.createObjectURL(arquivo);

    var img = document.createElement('img');
    img.src = urlAtual;
    img.alt = 'Pré-visualização da imagem escolhida';
    preview.textContent = '';
    preview.appendChild(img);
    preview.classList.add('has-image');

    nome.textContent = arquivo.name;
    remover.hidden = false;
  });

  remover.addEventListener('click', function () { limpar(); avisar(''); });
})();
