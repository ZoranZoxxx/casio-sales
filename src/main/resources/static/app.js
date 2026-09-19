const $ = id => document.getElementById(id);
const fmt = n => Number(n || 0).toFixed(2);
let active = false;
const api = async (url, options = {}) => {
  const response = await fetch(url, {headers: {'Content-Type': 'application/json'}, ...options});
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'Greska pri radu sa bazom.');
  return data;
};
const notice = message => $('notice').textContent = message || '';
function renderCart(cart) {
  $('lines').innerHTML = cart.items.map(item => `<tr><td>${safe(item.article.code)}</td><td>${safe(item.article.name)}</td><td>${item.quantity}</td><td>${fmt(item.sellingPrice)}</td></tr>`).join('');
  $('batch-total').textContent = fmt(cart.total);
}
function safe(value) { const e = document.createElement('span'); e.textContent = value; return e.innerHTML; }
function setWorkControls(enabled) {
  ['code', 'name', 'search-display', 'results', 'quantity', 'price', 'add', 'remove', 'execute'].forEach(id => $(id).disabled = !enabled);
  $('entry').classList.toggle('disabled', !enabled);
}
function renderDay(day) {
  $('date').textContent = new Date(day.date + 'T00:00:00').toLocaleDateString('sr-RS');
  $('daily-total').textContent = fmt(day.dailyTotal);
  if (day.closed) { active = false; setWorkControls(false); $('start').disabled = true; $('close-day').disabled = true; notice('Ovaj dan je ZAKLJUCEN. Novi unos nije dozvoljen.'); }
}
async function selectArticle(article) { $('code').value = article.code; $('name').value = article.name; $('search-display').value = article.name; $('results').innerHTML = ''; $('quantity').focus(); }
$('start').onclick = async () => { try { renderCart(await api('/api/cart/start', {method:'POST'})); active = true; setWorkControls(true); $('code').focus(); notice(''); } catch(e) { notice(e.message); } };
$('code').addEventListener('change', async () => { if (!$('code').value.trim()) return; try { await selectArticle(await api('/api/articles/code/' + encodeURIComponent($('code').value.trim()))); notice(''); } catch(e) { notice(e.message); } });
$('name').addEventListener('input', async () => { const query = $('name').value.trim(); $('search-display').value = query; if (query.length < 3) { $('results').innerHTML = ''; return; } try { const articles = await api('/api/articles?query=' + encodeURIComponent(query)); $('results').innerHTML = articles.map(a => `<option value="${safe(a.code)}">${safe(a.name)} (${safe(a.code)})</option>`).join(''); } catch(e) { notice(e.message); } });
$('results').onchange = async () => { if ($('results').value) try { await selectArticle(await api('/api/articles/code/' + encodeURIComponent($('results').value))); } catch(e) { notice(e.message); } };
$('add').onclick = async () => { try { const cart = await api('/api/cart/items', {method:'POST', body:JSON.stringify({articleCode:$('code').value, quantity:$('quantity').value, sellingPrice:$('price').value})}); renderCart(cart); $('code').value = $('name').value = $('search-display').value = $('quantity').value = $('price').value = ''; $('code').focus(); notice(''); } catch(e) { notice(e.message); } };
$('remove').onclick = async () => { try { renderCart(await api('/api/cart/remove-last', {method:'POST'})); } catch(e) { notice(e.message); } };
$('execute').onclick = async () => { try { const day = await api('/api/cart/execute', {method:'POST'}); renderCart({items:[], total:0}); renderDay(day); notice('Racun je izvrsen.'); } catch(e) { notice(e.message); } };
$('close-day').onclick = () => { $('deposit').value = ''; $('deposit-dialog').showModal(); };
$('confirm-close').onclick = async event => { event.preventDefault(); try { const day = await api('/api/day/close', {method:'POST', body:JSON.stringify({deposit:$('deposit').value || 0})}); $('deposit-dialog').close(); renderDay(day); notice('DAN JE ZAKLJUCEN. Stanje u kasi: ' + fmt(day.cashInRegister) + ' EUR'); } catch(e) { notice(e.message); } };
api('/api/day').then(renderDay).catch(e => notice(e.message));
