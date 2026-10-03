let categorias = [];
let transacoes = [];

const filtroMes = document.getElementById('filtro-mes');
const filtroCategoria = document.getElementById('filtro-categoria');
const filtroPendentes = document.getElementById('filtro-pendentes');

const dialogoTransacao = document.getElementById('dialogo-transacao');
const formTransacao = document.getElementById('form-transacao');
const dialogoCategorizar = document.getElementById('dialogo-categorizar');
const formCategorizar = document.getElementById('form-categorizar');

// ---------- Carregamento ----------

async function iniciar() {
  const parametros = new URLSearchParams(window.location.search);
  filtroMes.value = mesAtualISO();
  filtroPendentes.checked = parametros.get('pendentes') === 'true';

  try {
    categorias = await api('/categorias');
    filtroCategoria.innerHTML = '<option value="">Todas</option>' + opcoesCategorias(categorias);
  } catch (erro) {
    mostrarErro(erro);
  }
  await carregar();
}

async function carregar() {
  const pendentes = filtroPendentes.checked;
  filtroMes.disabled = pendentes;
  filtroCategoria.disabled = pendentes;

  const parametros = new URLSearchParams();
  if (pendentes) {
    parametros.set('pendentes', 'true');
  } else {
    const { inicio, fim } = periodoDoMes(filtroMes.value || mesAtualISO());
    parametros.set('inicio', inicio);
    parametros.set('fim', fim);
    if (filtroCategoria.value) parametros.set('categoriaId', filtroCategoria.value);
  }

  try {
    transacoes = await api(`/transacoes?${parametros}`);
    renderizar();
  } catch (erro) {
    mostrarErro(erro);
  }
}

function renderizar() {
  const lista = document.getElementById('lista');
  const rodape = document.getElementById('rodape-lista');

  if (transacoes.length === 0) {
    lista.innerHTML = `<tr><td colspan="5" class="vazio">${filtroPendentes.checked
      ? 'Tudo categorizado por aqui.'
      : 'Nenhuma transação neste período.'}</td></tr>`;
    rodape.textContent = '';
    return;
  }

  lista.innerHTML = transacoes.map((t) => `
    <tr>
      <td class="data">${dataBR(t.data)}</td>
      <td>${esc(t.descricao)}</td>
      <td>${t.pendente
        ? `<button type="button" class="pequeno secundario" data-categorizar="${t.id}">Categorizar</button>`
        : `<span class="etiqueta">${esc(t.categoriaNome)}</span>`}</td>
      <td class="valor ${t.tipo === 'RECEITA' ? 'receita' : 'despesa'}">${moeda(t.valor)}</td>
      <td>
        <div class="acoes">
          <button type="button" class="pequeno secundario" data-editar="${t.id}">Editar</button>
          <button type="button" class="pequeno perigo" data-excluir="${t.id}">Excluir</button>
        </div>
      </td>
    </tr>`).join('');

  const total = transacoes.reduce((soma, t) => soma + Number(t.valor), 0);
  rodape.textContent = `${transacoes.length} transação(ões) · saldo ${moeda(total)}`;
}

function opcoesCategorias(lista, tipo) {
  return lista
    .filter((c) => !tipo || c.tipo === tipo)
    .map((c) => `<option value="${c.id}">${esc(c.nome)}${tipo ? '' : c.tipo === 'RECEITA' ? ' (receita)' : ''}</option>`)
    .join('');
}

// ---------- Criar / editar ----------

let transacaoEmEdicao = null;

function abrirFormulario(transacao) {
  transacaoEmEdicao = transacao;
  document.getElementById('titulo-dialogo').textContent = transacao ? 'Editar transação' : 'Nova transação';

  formTransacao.tipo.value = transacao?.tipo ?? 'DESPESA';
  formTransacao.descricao.value = transacao?.descricao ?? '';
  formTransacao.valor.value = transacao ? Math.abs(Number(transacao.valor)).toFixed(2) : '';
  formTransacao.data.value = transacao?.data ?? hojeISO();
  atualizarCategoriasDoFormulario(transacao?.categoriaId);
  dialogoTransacao.showModal();
  formTransacao.descricao.focus();
}

function atualizarCategoriasDoFormulario(selecionada) {
  const tipo = formTransacao.tipo.value;
  formTransacao.categoriaId.innerHTML = '<option value="">Sem categoria</option>' + opcoesCategorias(categorias, tipo);
  formTransacao.categoriaId.value = selecionada && categorias.some((c) => c.id === selecionada && c.tipo === tipo)
    ? selecionada
    : '';
}

formTransacao.tipo.addEventListener('change', () => atualizarCategoriasDoFormulario(formTransacao.categoriaId.value));

formTransacao.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const valor = Number(formTransacao.valor.value);
  if (!formTransacao.descricao.value.trim() || !(valor > 0) || !formTransacao.data.value) {
    toast('Preencha descrição, um valor maior que zero e a data.', 'erro');
    return;
  }

  // O sinal do valor define o tipo: despesa é negativa
  const corpo = {
    descricao: formTransacao.descricao.value.trim(),
    valor: formTransacao.tipo.value === 'DESPESA' ? -valor : valor,
    data: formTransacao.data.value,
    categoriaId: formTransacao.categoriaId.value || null,
  };

  try {
    await comCarregamento(formTransacao.querySelector('[type=submit]'), () => transacaoEmEdicao
      ? api(`/transacoes/${transacaoEmEdicao.id}`, { metodo: 'PUT', corpo })
      : api('/transacoes', { metodo: 'POST', corpo }));
    dialogoTransacao.close();
    toast(transacaoEmEdicao ? 'Transação atualizada' : 'Transação criada');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

// ---------- Categorizar ----------

let transacaoACategorizar = null;

function abrirCategorizacao(transacao) {
  transacaoACategorizar = transacao;
  const opcoes = opcoesCategorias(categorias, transacao.tipo);
  if (!opcoes) {
    toast(`Crie antes uma categoria de ${transacao.tipo === 'RECEITA' ? 'receita' : 'despesa'}.`, 'erro');
    return;
  }
  document.getElementById('c-descricao').textContent = `${transacao.descricao} · ${moeda(transacao.valor)}`;
  formCategorizar.categoriaId.innerHTML = opcoes;
  formCategorizar.lembrar.checked = true;
  formCategorizar.palavraChave.value = transacao.descricao;
  document.getElementById('c-campo-palavra').hidden = false;
  dialogoCategorizar.showModal();
}

formCategorizar.lembrar.addEventListener('change', () => {
  document.getElementById('c-campo-palavra').hidden = !formCategorizar.lembrar.checked;
});

formCategorizar.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const corpo = {
    categoriaId: formCategorizar.categoriaId.value,
    lembrar: formCategorizar.lembrar.checked,
    palavraChave: formCategorizar.palavraChave.value.trim() || null,
  };
  try {
    await comCarregamento(formCategorizar.querySelector('[type=submit]'),
      () => api(`/transacoes/${transacaoACategorizar.id}/categoria`, { metodo: 'PATCH', corpo }));
    dialogoCategorizar.close();
    toast(corpo.lembrar
      ? 'Categorizada. A regra foi salva e aplicada às outras pendentes parecidas'
      : 'Transação categorizada');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

// ---------- Eventos ----------

document.getElementById('botao-nova').addEventListener('click', () => abrirFormulario(null));
filtroMes.addEventListener('change', carregar);
filtroCategoria.addEventListener('change', carregar);
filtroPendentes.addEventListener('change', carregar);

document.querySelectorAll('[data-fechar]').forEach((botao) =>
  botao.addEventListener('click', () => botao.closest('dialog').close()));

document.getElementById('lista').addEventListener('click', async (evento) => {
  const botao = evento.target.closest('button');
  if (!botao) return;
  const id = botao.dataset.editar || botao.dataset.excluir || botao.dataset.categorizar;
  const transacao = transacoes.find((t) => t.id === id);

  if (botao.dataset.editar) {
    abrirFormulario(transacao);
  } else if (botao.dataset.categorizar) {
    abrirCategorizacao(transacao);
  } else if (botao.dataset.excluir && confirm(`Excluir "${transacao.descricao}"?`)) {
    try {
      await api(`/transacoes/${id}`, { metodo: 'DELETE' });
      toast('Transação excluída');
      await carregar();
    } catch (erro) {
      mostrarErro(erro);
    }
  }
});

iniciar();
