let categorias = [];
let categoriaEmEdicao = null;

const formNova = document.getElementById('form-nova');
const dialogoEditar = document.getElementById('dialogo-editar');
const formEditar = document.getElementById('form-editar');

async function carregar() {
  try {
    const [listaCategorias, regras] = await Promise.all([api('/categorias'), api('/regras')]);
    categorias = listaCategorias;
    renderizarCategorias('DESPESA', document.getElementById('lista-despesas'));
    renderizarCategorias('RECEITA', document.getElementById('lista-receitas'));
    renderizarRegras(regras);
  } catch (erro) {
    mostrarErro(erro);
  }
}

function renderizarCategorias(tipo, alvo) {
  const lista = categorias.filter((c) => c.tipo === tipo);
  if (lista.length === 0) {
    alvo.innerHTML = '<tr><td class="vazio">Nenhuma categoria.</td></tr>';
    return;
  }
  alvo.innerHTML = lista.map((c) => `
    <tr>
      <td>${esc(c.nome)}</td>
      <td>
        <div class="acoes">
          <button type="button" class="pequeno secundario" data-editar="${c.id}">Editar</button>
          <button type="button" class="pequeno perigo" data-excluir="${c.id}">Excluir</button>
        </div>
      </td>
    </tr>`).join('');
}

function renderizarRegras(regras) {
  const alvo = document.getElementById('lista-regras');
  if (regras.length === 0) {
    alvo.innerHTML = '<tr><td colspan="3" class="vazio">Nenhuma regra ainda.</td></tr>';
    return;
  }
  alvo.innerHTML = regras.map((r) => `
    <tr>
      <td><code>${esc(r.palavraChave)}</code></td>
      <td><span class="etiqueta">${esc(r.categoriaNome)}</span></td>
      <td><div class="acoes"><button type="button" class="pequeno perigo" data-excluir-regra="${r.id}">Excluir</button></div></td>
    </tr>`).join('');
}

formNova.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const nome = formNova.nome.value.trim();
  if (!nome) {
    toast('Dê um nome para a categoria.', 'erro');
    return;
  }
  try {
    await comCarregamento(formNova.querySelector('button'),
      () => api('/categorias', { metodo: 'POST', corpo: { nome, tipo: formNova.tipo.value } }));
    formNova.nome.value = '';
    toast('Categoria criada');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

formEditar.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const corpo = { nome: formEditar.nome.value.trim(), tipo: formEditar.tipo.value };
  try {
    await comCarregamento(formEditar.querySelector('[type=submit]'),
      () => api(`/categorias/${categoriaEmEdicao.id}`, { metodo: 'PUT', corpo }));
    dialogoEditar.close();
    toast('Categoria atualizada');
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

document.querySelector('[data-fechar]').addEventListener('click', () => dialogoEditar.close());

document.querySelector('main').addEventListener('click', async (evento) => {
  const botao = evento.target.closest('button');
  if (!botao) return;

  if (botao.dataset.editar) {
    categoriaEmEdicao = categorias.find((c) => c.id === botao.dataset.editar);
    formEditar.nome.value = categoriaEmEdicao.nome;
    formEditar.tipo.value = categoriaEmEdicao.tipo;
    dialogoEditar.showModal();
  } else if (botao.dataset.excluir) {
    const categoria = categorias.find((c) => c.id === botao.dataset.excluir);
    const confirmado = confirm(`Excluir "${categoria.nome}"?\n\nAs transações dessa categoria não são apagadas: `
      + 'elas voltam a ficar sem categoria.');
    if (confirmado) await excluir(`/categorias/${categoria.id}`, 'Categoria excluída');
  } else if (botao.dataset.excluirRegra) {
    if (confirm('Excluir esta regra?')) await excluir(`/regras/${botao.dataset.excluirRegra}`, 'Regra excluída');
  }
});

async function excluir(caminho, mensagem) {
  try {
    await api(caminho, { metodo: 'DELETE' });
    toast(mensagem);
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
}

carregar();
