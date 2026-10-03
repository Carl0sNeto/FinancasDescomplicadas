let usuarios = [];
let usuarioDaSenha = null;

const formNovo = document.getElementById('form-novo');
const dialogoSenha = document.getElementById('dialogo-senha');
const formSenha = document.getElementById('form-senha');

if (obterSessao()?.papel !== 'ADMIN') {
  window.location.href = 'painel.html';
}

async function carregar() {
  try {
    usuarios = await api('/admin/usuarios');
    renderizar();
  } catch (erro) {
    mostrarErro(erro);
  }
}

function renderizar() {
  const emailLogado = obterSessao()?.email;
  document.getElementById('lista').innerHTML = usuarios.map((u) => {
    const ehVoce = u.email === emailLogado;
    return `
      <tr>
        <td>${esc(u.nome)}${ehVoce ? ' <span class="texto-suave">(você)</span>' : ''}</td>
        <td>${esc(u.email)}</td>
        <td><span class="etiqueta ${u.papel === 'ADMIN' ? 'receita' : ''}">${u.papel === 'ADMIN' ? 'Administrador' : 'Usuário'}</span></td>
        <td class="data opcional">${u.criadoEm ? new Date(u.criadoEm).toLocaleDateString('pt-BR') : ''}</td>
        <td>
          <div class="acoes">
            <button type="button" class="pequeno secundario" data-senha="${u.id}">Trocar senha</button>
            ${ehVoce ? '' : `<button type="button" class="pequeno perigo" data-excluir="${u.id}">Excluir</button>`}
          </div>
        </td>
      </tr>`;
  }).join('');
}

formNovo.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const corpo = {
    nome: formNovo.nome.value.trim(),
    email: formNovo.email.value.trim(),
    senha: formNovo.senha.value,
    papel: formNovo.papel.value,
  };
  if (!corpo.nome || !corpo.email || corpo.senha.length < 8) {
    toast('Preencha nome, email e uma senha com pelo menos 8 caracteres.', 'erro');
    return;
  }
  try {
    await comCarregamento(formNovo.querySelector('button'),
      () => api('/admin/usuarios', { metodo: 'POST', corpo }));
    formNovo.reset();
    toast(`Conta de ${corpo.nome} criada`);
    await carregar();
  } catch (erro) {
    mostrarErro(erro);
  }
});

formSenha.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const senha = formSenha.senha.value;
  if (senha.length < 8) {
    toast('A senha precisa ter pelo menos 8 caracteres.', 'erro');
    return;
  }
  try {
    await comCarregamento(formSenha.querySelector('[type=submit]'),
      () => api(`/admin/usuarios/${usuarioDaSenha.id}/senha`, { metodo: 'PUT', corpo: { senha } }));
    dialogoSenha.close();
    toast(`Senha de ${usuarioDaSenha.nome} alterada`);
  } catch (erro) {
    mostrarErro(erro);
  }
});

document.querySelector('[data-fechar]').addEventListener('click', () => dialogoSenha.close());

document.getElementById('lista').addEventListener('click', async (evento) => {
  const botao = evento.target.closest('button');
  if (!botao) return;
  const usuario = usuarios.find((u) => u.id === (botao.dataset.senha || botao.dataset.excluir));

  if (botao.dataset.senha) {
    usuarioDaSenha = usuario;
    document.getElementById('senha-usuario').textContent = `${usuario.nome} · ${usuario.email}`;
    formSenha.reset();
    dialogoSenha.showModal();
  } else if (botao.dataset.excluir) {
    const confirmado = confirm(`Excluir a conta de ${usuario.nome} (${usuario.email})?\n\n`
      + 'Todas as transações, categorias, metas e importações dessa pessoa serão apagadas. '
      + 'Isso não pode ser desfeito.');
    if (!confirmado) return;
    try {
      await api(`/admin/usuarios/${usuario.id}`, { metodo: 'DELETE' });
      toast('Conta excluída');
      await carregar();
    } catch (erro) {
      mostrarErro(erro);
    }
  }
});

carregar();
