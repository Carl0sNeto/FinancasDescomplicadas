// Endereço da API (backend Spring Boot).
// Rodando localmente usa a porta 8080; publicado, usa a URL do backend no Render.
// Depois do deploy, troque API_PRODUCAO pela URL que o Render mostrar para o serviço financas-api.
const API_PRODUCAO = 'https://financasdescomplicadas.onrender.com';

const API_URL = ['localhost', '127.0.0.1'].includes(window.location.hostname)
  ? 'http://localhost:8080'
  : API_PRODUCAO;
