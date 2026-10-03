const form = document.getElementById('form-importar');
const campoArquivo = document.getElementById('arquivo');
const zona = document.getElementById('zona-upload');
const resultado = document.getElementById('resultado');

async function carregarHistorico() {
  const alvo = document.getElementById('historico');
  try {
    const importacoes = await api('/importacoes');
    if (importacoes.length === 0) {
      alvo.innerHTML = '<tr><td colspan="5" class="vazio">Nenhum extrato importado ainda.</td></tr>';
      return;
    }
    alvo.innerHTML = importacoes.map((i) => `
      <tr>
        <td>${esc(i.nomeArquivo)}</td>
        <td class="opcional"><span class="etiqueta">${i.formato}</span></td>
        <td class="data">${new Date(i.importadoEm).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}</td>
        <td class="valor">${i.totalTransacoes}</td>
        <td><div class="acoes">
          <button type="button" class="pequeno perigo" data-desfazer="${i.id}" data-nome="${esc(i.nomeArquivo)}">Desfazer</button>
        </div></td>
      </tr>`).join('');
  } catch (erro) {
    mostrarErro(erro);
  }
}

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const arquivo = campoArquivo.files[0];
  if (!arquivo) {
    toast('Escolha um arquivo .ofx ou .csv.', 'erro');
    return;
  }

  const dados = new FormData();
  dados.append('arquivo', arquivo);

  try {
    const r = await comCarregamento(form.querySelector('[type=submit]'),
      () => api('/importacoes', { metodo: 'POST', corpo: dados }));
    resultado.className = 'aviso sucesso';
    resultado.innerHTML = `
      <strong>${r.importacao.totalTransacoes} transações importadas</strong> de ${esc(r.importacao.nomeArquivo)}:
      ${r.categorizadas} categorizadas automaticamente
      ${r.pendentes > 0
        ? ` e ${r.pendentes} para revisar. <a href="transacoes.html?pendentes=true">Revisar agora</a>`
        : '.'}`;
    resultado.hidden = false;
    form.reset();
    await carregarHistorico();
  } catch (erro) {
    resultado.className = 'aviso erro';
    resultado.textContent = erro.message;
    resultado.hidden = false;
  }
});

document.getElementById('historico').addEventListener('click', async (evento) => {
  const botao = evento.target.closest('[data-desfazer]');
  if (!botao) return;
  const confirmado = confirm(`Desfazer a importação de "${botao.dataset.nome}"?\n\n`
    + 'Todas as transações criadas por ela serão apagadas.');
  if (!confirmado) return;

  try {
    await api(`/importacoes/${botao.dataset.desfazer}`, { metodo: 'DELETE' });
    toast('Importação desfeita');
    await carregarHistorico();
  } catch (erro) {
    mostrarErro(erro);
  }
});

// Arrastar e soltar
['dragenter', 'dragover'].forEach((tipo) => zona.addEventListener(tipo, (evento) => {
  evento.preventDefault();
  zona.classList.add('arrastando');
}));
['dragleave', 'drop'].forEach((tipo) => zona.addEventListener(tipo, () => zona.classList.remove('arrastando')));
zona.addEventListener('drop', (evento) => {
  evento.preventDefault();
  if (evento.dataTransfer.files.length) {
    campoArquivo.files = evento.dataTransfer.files;
  }
});

carregarHistorico();
