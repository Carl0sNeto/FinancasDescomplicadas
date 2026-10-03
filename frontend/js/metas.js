let metas = [];
let metaEmEdicao = null;

const lista = document.getElementById('lista');
const dialogo = document.getElementById('dialogo-meta');
const form = document.getElementById('form-meta');

async function carregar() {
  try {
    metas = await api('/metas');
    renderizar();
  } catch (erro) {
    mostrarErro(erro);
  }
}

function renderizar() {
  if (metas.length === 0) {
    lista.innerHTML = `
      <div class="cartao vazio">
        Nenhuma meta ainda. Que tal começar por uma reserva de emergência?
      </div>`;
    return;
  }

  lista.innerHTML = metas.map((m) => `
    <article class="cartao">
      <div class="meta-cabecalho">
        <h2 style="margin: 0">${esc(m.titulo)}</h2>
        ${m.concluida ? '<span class="etiqueta receita">Concluída</span>' : ''}
      </div>
      <div class="meta-valores">${moeda(m.valorAtual)} <small>de ${moeda(m.valorAlvo)}</small></div>
      <div class="barra ${m.concluida ? 'concluida' : ''}"
           role="progressbar" aria-valuenow="${m.percentual}" aria-valuemin="0" aria-valuemax="100">
        <div style="width: ${m.percentual}%"></div>
      </div>
      <div class="meta-rodape">
        <span>${Number(m.percentual).toLocaleString('pt-BR')}% · faltam ${moeda(m.valorRestante)}</span>
        <span>${m.dataLimite ? `até ${dataBR(m.dataLimite)}` : ''}</span>
      </div>
      <form class="meta-movimento" data-meta="${m.id}" novalidate>
        <label class="sr-only" for="valor-${m.id}">Valor</label>
        <input id="valor-${m.id}" name="valor" type="number" min="0.01" step="0.01" inputmode="decimal" placeholder="R$">
        <button type="submit" name="acao" value="guardar" class="pequeno">Guardar</button>
        <button type="submit" name="acao" value="retirar" class="pequeno secundario">Retirar</button>
      </form>
      <div class="acoes" style="margin-top: 12px">
        <button type="button" class="link" data-editar="${m.id}">Editar</button>
        <button type="button" class="link" data-excluir="${m.id}">Excluir</button>
      </div>
    </article>`).join('');
}

function abrirFormulario(meta) {
  metaEmEdicao = meta;
  document.getElementById('titulo-dialogo').textContent = meta ? 'Editar meta' : 'Nova meta';
  form.titulo.value = meta?.titulo ?? '';
  form.valorAlvo.value = meta?.valorAlvo ?? '';
  form.valorAtual.value = meta?.valorAtual ?? '';
  form.dataLimite.value = meta?.dataLimite ?? '';
  dialogo.showModal();
  form.titulo.focus();
}

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const corpo = {
    titulo: form.titulo.value.trim(),
    valorAlvo: Number(form.valorAlvo.value),
    valorAtual: form.valorAtual.value ? Number(form.valorAtual.value) : 0,
    dataLimite: form.dataLimite.value || null,
  };
  if (!corpo.titulo || !(corpo.valorAlvo > 0)) {
    toast('Informe o objetivo e um valor maior que zero.', 'erro');
    return;
  }
  try {
    await comCarregamento(form.querySelector('[type=submit]'), () => metaEmEdicao
      ? api(`/metas/${metaEmEdicao.id}`, { metodo: 'PUT', corpo })
      : api('/metas', { metodo: 'POST', corpo }));
    dialogo.close();
    toast(metaEmEdicao ? 'Meta atualizada' : 'Meta criada');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

// Guardar / retirar dinheiro de uma meta
lista.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const formMovimento = evento.target;
  const valor = Number(formMovimento.valor.value);
  if (!(valor > 0)) {
    toast('Informe um valor maior que zero.', 'erro');
    return;
  }
  const retirar = evento.submitter?.value === 'retirar';
  try {
    await api(`/metas/${formMovimento.dataset.meta}/movimentos`, {
      metodo: 'POST',
      corpo: { valor: retirar ? -valor : valor },
    });
    toast(retirar ? 'Valor retirado da meta' : 'Valor guardado na meta');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

lista.addEventListener('click', async (evento) => {
  const botao = evento.target.closest('button[type=button]');
  if (!botao) return;
  const meta = metas.find((m) => m.id === (botao.dataset.editar || botao.dataset.excluir));

  if (botao.dataset.editar) {
    abrirFormulario(meta);
  } else if (botao.dataset.excluir && confirm(`Excluir a meta "${meta.titulo}"?`)) {
    try {
      await api(`/metas/${meta.id}`, { metodo: 'DELETE' });
      toast('Meta excluída');
      await carregar();
    } catch (erro) {
      mostrarErro(erro);
    }
  }
});

document.getElementById('botao-nova').addEventListener('click', () => abrirFormulario(null));
document.querySelector('[data-fechar]').addEventListener('click', () => dialogo.close());

carregar();
