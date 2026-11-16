const JOURS = {MONDAY:'Lundi',TUESDAY:'Mardi',WEDNESDAY:'Mercredi',THURSDAY:'Jeudi',FRIDAY:'Vendredi',SATURDAY:'Samedi'};
const JOURS_EN = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'];
const JOURS_COURTS = ['Lun','Mar','Mer','Jeu','Ven','Sam'];
const DOW = {SUNDAY:0,MONDAY:1,TUESDAY:2,WEDNESDAY:3,THURSDAY:4,FRIDAY:5,SATURDAY:6};
const STATUTS = {EN_ATTENTE:'En attente',CONFIRMEE:'Confirmée',REFUSEE:'Refusée',ANNULEE:'Annulée'};
const TYPES = {COURS_RATTRAPAGE:'Rattrapage de cours',REUNION:'Réunion',FORMATION:'Formation',
  SOUTENANCE_THESE:'Soutenance de thèse',ACTIVITE_PARASCOLAIRE:'Activité parascolaire'};
const TYPES_SALLE = {AMPHI:'Amphithéâtre',SALLE_COURS:'Salle de cours',LABO:'Laboratoire',
  SOUTENANCE:'Salle de soutenance',REUNION:'Salle de réunion'};
const TITRES = {
  dashboard: ['Tableau de bord', "Vue d'ensemble de votre activité"],
  planning: ['Emploi du temps', 'Vos cours et réservations de la semaine'],
  plan: ['Carte et réservation', 'Trouvez une salle libre et envoyez votre demande'],
  reservations: ['Mes réservations', 'Suivi de vos demandes auprès de l’administration'],
  annonces: ['Annonces aux étudiants', 'Informez vos groupes en quelques secondes'],
  notifications: ['Notifications', 'Décisions et informations de l’administration'],
  profil: ['Mon profil', 'Vos informations personnelles et votre sécurité']
};
const H0 = 8, H1 = 20, HH = 56; // plage horaire affichée et hauteur d'une heure (px)

const state = { profil: null, seances: [], reservations: [], notifications: [], salles: [], groupes: [],
                annonces: [], suggest: [], prefill: null, fStatut: '', q: '', weekStart: null };
let currentView = 'dashboard';

/* ---------- Utilitaires ---------- */
const hm = t => t.slice(0, 5);
const mins = t => +t.slice(0, 2) * 60 + +t.slice(3, 5);
const maj = s => s.charAt(0).toUpperCase() + s.slice(1);
const fmtJour = d => new Date(d).toLocaleDateString('fr-FR', { weekday: 'short', day: '2-digit', month: 'short' });
const fmtHeure = d => new Date(d).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
const fmt = d => `${fmtJour(d)} · ${fmtHeure(d)}`;
const dateLongue = d => maj(d.toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }));
const sameDay = (a, b) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
const addDays = (d, n) => { const x = new Date(d); x.setDate(x.getDate() + n); return x; };
const mondayOf = d => addDays(new Date(d.getFullYear(), d.getMonth(), d.getDate()), -((d.getDay() + 6) % 7));
const initials = p => ((p.prenom || '?')[0] + (p.nom || '')[0]).toUpperCase();
const chip = s => `<span class="chip ${s}">${STATUTS[s]}</span>`;
const avatarHtml = (p, cls = '') =>
  `<div class="avatar ${cls}">${p.photo ? `<img src="${esc(p.photo)}" alt="">` : initials(p)}</div>`;

document.addEventListener('DOMContentLoaded', init);

async function init() {
  try { state.profil = await api('/api/profil'); } catch (e) { return; }
  if (state.profil.role === 'ADMIN') { location.href = '/admin.html'; return; }
  if (state.profil.role === 'ETUDIANT') { location.href = '/etudiant.html'; return; }
  majIdentite();
  state.weekStart = mondayOf(new Date());

  document.querySelectorAll('[data-ico]').forEach(b => b.insertAdjacentHTML('afterbegin', ico(b.dataset.ico, 19)));
  $('#bell').insertAdjacentHTML('afterbegin', ico('bell', 20));
  document.querySelectorAll('.nav [data-view]').forEach(b => b.onclick = () => show(b.dataset.view));
  $('#bell').onclick = () => show('notifications');
  $('#logout').onclick = async () => {
    await fetch('/api/auth/logout', { method: 'POST' });
    location.href = '/login.html';
  };
  $('#dialog').onclick = e => { if (e.target.id === 'dialog') closeDialog(); };
  document.addEventListener('keydown', e => { if (e.key === 'Escape') { closeDialog(); closeModal(); } });

  await Promise.all([loadSeances(), loadReservations(), loadNotifications(), loadGroupes(), loadAnnonces()]);
  initPlan();
  show('dashboard');
  setInterval(() => loadNotifications().catch(() => {}), 10000);
}

function majIdentite() {
  const p = state.profil;
  $('#userName').textContent = `${p.prenom} ${p.nom}`;
  $('#userRole').textContent = p.grade || 'Enseignant';
  $('#avatar').innerHTML = p.photo ? `<img src="${esc(p.photo)}" alt="">` : initials(p);
}

function show(v) {
  currentView = v;
  document.querySelectorAll('.view').forEach(e => e.classList.toggle('hidden', e.id !== 'view-' + v));
  document.querySelectorAll('.nav [data-view]').forEach(b => b.classList.toggle('active', b.dataset.view === v));
  $('#pageTitle').textContent = TITRES[v][0];
  $('#pageSub').textContent = TITRES[v][1];
  ({ dashboard: renderDashboard, planning: renderPlanning, plan: loadPlan, reservations: renderReservations,
     annonces: renderAnnonces, notifications: renderNotifications, profil: renderProfil })[v]();
  window.scrollTo(0, 0);
}

async function loadSeances() { state.seances = await api('/api/emploi-du-temps'); }
async function loadReservations() { state.reservations = await api('/api/reservations'); }
async function loadGroupes() { state.groupes = await api('/api/groupes'); }
async function loadAnnonces() { state.annonces = await api('/api/annonces'); }

let derniereNonLues = null;

/** Rafraîchit la vue courante sans remonter en haut de page (la carte n'est pas redessinée). */
function rafraichirVue() {
  const rendu = { dashboard: renderDashboard, planning: renderPlanning, reservations: renderReservations, annonces: renderAnnonces };
  if (rendu[currentView]) rendu[currentView]();
}

async function loadNotifications() {
  state.notifications = await api('/api/notifications');
  const n = state.notifications.filter(x => !x.lue).length;
  ['#badge', '#bellDot'].forEach(s => { $(s).textContent = n; $(s).classList.toggle('hidden', !n); });
  // Une nouvelle notification est arrivée (décision de l'administration, par exemple) : on met tout à jour
  if (derniereNonLues !== null && n > derniereNonLues) {
    toast('Nouvelle notification reçue');
    await loadReservations();
    rafraichirVue();
  }
  derniereNonLues = n;
  if (currentView === 'notifications') renderNotifications();
}
async function refreshData() { await Promise.all([loadReservations(), loadNotifications()]); }

/* ---------- Boîtes de dialogue ---------- */
function openDialog(html) { $('#dialogBody').innerHTML = html; $('#dialog').classList.remove('hidden'); }
function closeDialog() { $('#dialog').classList.add('hidden'); }

/* ---------- Tableau de bord ---------- */
function nextDate(s) {
  const now = new Date(), d = new Date(now);
  const [h, m] = s.heureDebut.split(':');
  d.setHours(+h, +m, 0, 0);
  d.setDate(now.getDate() + (DOW[s.jour] - now.getDay() + 7) % 7);
  if (d < now) d.setDate(d.getDate() + 7);
  return d;
}

function kpi(icon, color, label, value, sub) {
  return `<div class="kpi"><div class="kpi-ico ${color}">${ico(icon, 22)}</div>
    <div><span>${label}</span><b>${value}</b><small>${sub}</small></div></div>`;
}

function renderDashboard() {
  const now = new Date();
  const cours = state.seances.map(s => ({ d: nextDate(s), kind: 'cours', titre: `${s.matiere} · ${s.groupe}`, lieu: s.salle }));
  const resas = state.reservations.filter(r => r.statut === 'CONFIRMEE' && new Date(r.dateFin) > now)
    .map(r => ({ d: new Date(r.dateDebut), kind: 'resa', titre: r.objet, lieu: r.salle }));
  const events = [...cours, ...resas].sort((a, b) => a.d - b.d).slice(0, 6);
  const next = [...cours].sort((a, b) => a.d - b.d)[0];
  const attente = state.reservations.filter(r => r.statut === 'EN_ATTENTE');
  const urgentes = attente.filter(r => r.urgent).length;
  const nonLues = state.notifications.filter(n => !n.lue).length;
  const recent = state.reservations.slice(0, 4);

  $('#view-dashboard').innerHTML = `
    <section class="hero">
      <div><span class="hand">Bonjour,</span><h2>Pr. ${esc(state.profil.prenom)} ${esc(state.profil.nom)}</h2><p>${dateLongue(now)}</p>
        <div class="hero-actions">
          <button class="btn" onclick="show('plan')">${ico('plus', 16)} Nouvelle réservation</button>
          <button class="btn ghost" onclick="show('annonces')">${ico('send', 16)} Annonce aux étudiants</button>
        </div></div>
      ${next ? `<div class="hero-note"><small>Prochain cours</small><b>${esc(next.titre)}</b>
        <span>${fmtJour(next.d)} · ${fmtHeure(next.d)}</span><span>Salle ${esc(next.lieu)}</span></div>` : ''}
    </section>
    <div class="kpis">
      ${kpi('clock', 'blue', 'Prochain cours', next ? fmtHeure(next.d) : '—', next ? `${fmtJour(next.d)} · ${esc(next.lieu)}` : 'Aucun cours')}
      ${kpi('list', 'amber', 'Demandes en attente', attente.length, urgentes ? `dont ${urgentes} urgente(s)` : 'En cours de validation')}
      ${kpi('check', 'green', 'Réservations confirmées', resas.length, 'À venir')}
      ${kpi('bell', 'violet', 'Notifications', nonLues, 'Non lues')}
    </div>
    <div class="cols">
      <div class="card"><h3>Prochains événements</h3>
        ${events.map(e => `<div class="row-ev">
          <div class="date-chip"><b>${e.d.getDate()}</b><span>${e.d.toLocaleDateString('fr-FR', { month: 'short' })}</span></div>
          <div><b>${esc(e.titre)}</b><small>${fmtHeure(e.d)} · ${esc(e.lieu)}</small></div>
          <span class="tag ${e.kind}">${e.kind === 'cours' ? 'Cours' : 'Réservation'}</span></div>`).join('')
          || '<p class="muted">Rien de prévu.</p>'}
      </div>
      <div class="card"><h3>Mes dernières demandes</h3>
        ${recent.map(r => `<div class="row-ev click" onclick="openResa(${r.id})">
          <div class="date-chip"><b>${new Date(r.dateDebut).getDate()}</b><span>${new Date(r.dateDebut).toLocaleDateString('fr-FR', { month: 'short' })}</span></div>
          <div><b>${esc(r.salle)} ${r.urgent ? '<span class="chip urgent">Urgent</span>' : ''}</b><small>${esc(r.objet)}</small></div>${chip(r.statut)}</div>`).join('')
          || '<p class="muted">Aucune demande pour le moment.</p>'}
        <p style="margin:14px 0 0"><button class="btn ghost sm" onclick="show('reservations')">Voir toutes mes réservations</button></p>
      </div>
    </div>`;
}

/* ---------- Emploi du temps (semaine) ---------- */
function semaine(n) { state.weekStart = n === 0 ? mondayOf(new Date()) : addDays(state.weekStart, 7 * n); renderPlanning(); }

function evBlock(startMin, endMin, cls, html, click) {
  const top = (startMin - H0 * 60) / 60 * HH;
  const h = Math.max((endMin - startMin) / 60 * HH - 2, 24);
  return `<div class="ev ${cls}" style="top:${top}px;height:${h}px" onclick="${click}">${html}</div>`;
}

function colonne(i) {
  const day = addDays(state.weekStart, i);
  let h = state.seances.filter(s => s.jour === JOURS_EN[i]).map(s =>
    evBlock(mins(s.heureDebut), mins(s.heureFin), 'cours',
      `<b>${hm(s.heureDebut)} – ${hm(s.heureFin)}</b><span>${esc(s.matiere)}</span><small>${esc(s.groupe)} · ${esc(s.salle)}</small>`,
      `openSeance(${s.id})`)).join('');
  h += state.reservations.filter(r => ['CONFIRMEE', 'EN_ATTENTE'].includes(r.statut) && sameDay(new Date(r.dateDebut), day))
    .map(r => {
      const a = new Date(r.dateDebut), b = new Date(r.dateFin);
      return evBlock(a.getHours() * 60 + a.getMinutes(), b.getHours() * 60 + b.getMinutes(), r.statut,
        `<b>${fmtHeure(a)} – ${fmtHeure(b)}</b><span>${esc(r.salle)}</span><small>${esc(r.objet)}</small>`, `openResa(${r.id})`);
    }).join('');
  const now = new Date(), nm = now.getHours() * 60 + now.getMinutes();
  if (sameDay(day, now) && nm >= H0 * 60 && nm <= H1 * 60)
    h += `<div class="now" style="top:${(nm - H0 * 60) / 60 * HH}px"></div>`;
  return h;
}

function renderPlanning() {
  const ws = state.weekStart, we = addDays(ws, 5);
  const titre = `${ws.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })} – ${we.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}`;
  $('#view-planning').innerHTML = `
    <div class="cal-nav">
      <div><button class="icon-btn" onclick="semaine(-1)" title="Semaine précédente">${ico('left')}</button>
        <button class="icon-btn" onclick="semaine(1)" title="Semaine suivante">${ico('right')}</button>
        <button class="btn ghost sm" onclick="semaine(0)">Aujourd'hui</button></div>
      <h3>Semaine du ${titre}</h3>
      <div class="cal-legend"><span><i class="lg cours"></i>Cours</span><span><i class="lg CONFIRMEE"></i>Réservation confirmée</span><span><i class="lg EN_ATTENTE"></i>En attente</span></div>
    </div>
    <div class="cal">
      <div class="cal-head"><div></div>${JOURS_COURTS.map((j, i) => {
        const d = addDays(ws, i);
        return `<div class="cal-h ${sameDay(d, new Date()) ? 'today' : ''}"><span>${j}</span><b>${d.getDate()}</b></div>`;
      }).join('')}</div>
      <div class="cal-body" style="--hh:${HH}px">
        <div class="cal-hours">${Array.from({ length: H1 - H0 }, (_, i) => `<div class="cal-hr"><span>${pad(H0 + i)}:00</span></div>`).join('')}</div>
        ${JOURS_COURTS.map((_, i) => `<div class="cal-col" style="height:${(H1 - H0) * HH}px">${colonne(i)}</div>`).join('')}
      </div>
    </div>`;
}

function openSeance(id) {
  const s = state.seances.find(x => x.id === id);
  openDialog(`<div class="dlg-head"><h3>${esc(s.matiere)}</h3><button class="icon-btn" onclick="closeDialog()">${ico('x')}</button></div>
    <dl class="dl"><dt>Groupe</dt><dd>${esc(s.groupe)}</dd><dt>Jour</dt><dd>${JOURS[s.jour]}</dd>
      <dt>Horaire</dt><dd>${hm(s.heureDebut)} – ${hm(s.heureFin)}</dd><dt>Salle</dt><dd>${esc(s.salle)}</dd></dl>
    <div class="actions"><button class="btn ghost" onclick="closeDialog()">Fermer</button>
      <button class="btn" onclick="rattrapage(${s.id})">${ico('repeat', 16)} Planifier un rattrapage</button></div>`);
}

function rattrapage(id) {
  const s = state.seances.find(x => x.id === id);
  closeDialog();
  $('#pDebut').value = hm(s.heureDebut);
  $('#pFin').value = hm(s.heureFin);
  state.prefill = { type: 'COURS_RATTRAPAGE', matiere: s.matiere, niveau: s.groupe };
  toast('Choisissez la date, puis une salle libre sur la carte');
  show('plan');
}

/* ---------- Mes réservations ---------- */
function renderReservations() {
  const count = s => state.reservations.filter(r => !s || r.statut === s).length;
  $('#view-reservations').innerHTML = `
    <div class="toolbar-line">
      <div class="search">${ico('search', 16)}<input id="qResa" placeholder="Rechercher une salle, une matière…" value="${esc(state.q)}" oninput="state.q=this.value;renderResaTable()"></div>
      <div class="pills">${['', ...Object.keys(STATUTS)].map(s =>
        `<button class="pill ${state.fStatut === s ? 'active' : ''}" onclick="state.fStatut='${s}';renderReservations()">${s ? STATUTS[s] : 'Toutes'} <em>${count(s)}</em></button>`).join('')}</div>
      <button class="btn" onclick="show('plan')">${ico('plus', 16)} Nouvelle</button>
    </div>
    <div id="resaTable"></div>`;
  renderResaTable();
}

function renderResaTable() {
  const q = state.q.trim().toLowerCase();
  const l = state.reservations.filter(r => (!state.fStatut || r.statut === state.fStatut)
    && (!q || (r.salle + ' ' + r.objet + ' ' + r.niveau).toLowerCase().includes(q)));
  $('#resaTable').innerHTML = `<table><thead><tr><th>Créneau</th><th>Salle</th><th>Matière · niveau</th><th>Type</th><th>Statut</th><th></th></tr></thead><tbody>
    ${l.map(r => `<tr class="click" onclick="openResa(${r.id})">
      <td><b>${fmtJour(r.dateDebut)}</b><br><small class="muted">${fmtHeure(r.dateDebut)} → ${fmtHeure(r.dateFin)}</small></td>
      <td>${esc(r.salle)}</td>
      <td>${esc(r.matiere)} · <b>${esc(r.niveau)}</b> ${r.urgent ? '<span class="chip urgent">Urgent</span>' : ''}</td>
      <td>${TYPES[r.type]}</td>
      <td>${chip(r.statut)}${r.motifRefus ? `<br><small class="ko-text">${esc(r.motifRefus)}</small>` : ''}</td>
      <td class="right">${['EN_ATTENTE', 'CONFIRMEE'].includes(r.statut)
        ? `<button class="btn danger sm" onclick="event.stopPropagation();annulerResa(${r.id})">Annuler</button>` : ''}</td>
    </tr>`).join('') || '<tr><td colspan="6" class="muted center">Aucune réservation ne correspond.</td></tr>'}</tbody></table>`;
}

function stepper(r) {
  const s = r.statut;
  const fin = { EN_ATTENTE: ['Décision', ''], CONFIRMEE: ['Confirmée', 'ok'], REFUSEE: ['Refusée', 'ko'], ANNULEE: ['Annulée', 'cancel'] }[s];
  return `<div class="steps">
    <div class="step done"><i></i><span>Demande envoyée</span><small>${fmt(r.dateDemande)}</small></div>
    <div class="step ${s === 'EN_ATTENTE' ? 'active' : 'done'}"><i></i><span>Examen par l'administration</span></div>
    <div class="step ${fin[1]}"><i></i><span>${fin[0]}</span></div></div>`;
}

function openResa(id) {
  const r = state.reservations.find(x => x.id === id);
  const actif = ['EN_ATTENTE', 'CONFIRMEE'].includes(r.statut);
  openDialog(`<div class="dlg-head"><h3>${esc(r.objet)}</h3><button class="icon-btn" onclick="closeDialog()">${ico('x')}</button></div>
    <p>${chip(r.statut)} ${r.urgent ? '<span class="chip urgent">Urgent</span>' : ''}</p>
    ${stepper(r)}
    <dl class="dl"><dt>Salle</dt><dd>${esc(r.salle)}</dd><dt>Créneau</dt><dd>${fmt(r.dateDebut)} → ${fmtHeure(r.dateFin)}</dd>
      <dt>Type</dt><dd>${TYPES[r.type]}</dd><dt>Matière</dt><dd>${esc(r.matiere)}</dd><dt>Niveau</dt><dd>${esc(r.niveau)}</dd>
      <dt>Participants</dt><dd>${r.nbParticipants}</dd><dt>Priorité</dt><dd>${r.priorite} / 5</dd>
      <dt>Étudiants informés</dt><dd>${r.notifierEtudiants ? 'Oui, après confirmation' : 'Non'}</dd>
      ${r.motifRefus ? `<dt>Motif du refus</dt><dd class="ko-text">${esc(r.motifRefus)}</dd>` : ''}
      ${r.concurrentes ? `<dt>Concurrence</dt><dd>${r.concurrentes} autre(s) demande(s) sur ce créneau</dd>` : ''}</dl>
    <div class="callout"><b>Justification transmise</b><p>${esc(r.justification)}</p></div>
    <div class="actions"><button class="btn ghost" onclick="closeDialog()">Fermer</button>
      ${r.statut === 'CONFIRMEE' ? `<button class="btn ghost" onclick="telechargerICS(${r.id})">${ico('download', 16)} Ajouter au calendrier</button>` : ''}
      ${actif ? `<button class="btn danger" onclick="annulerResa(${r.id})">Annuler la réservation</button>` : ''}</div>`);
}

async function annulerResa(id) {
  if (!confirm('Annuler cette réservation ?')) return;
  try {
    await api(`/api/reservations/${id}/annuler`, { method: 'POST' });
    closeDialog();
    toast('Réservation annulée');
    await refreshData();
    show(currentView);
  } catch (e) { toast(e.message, 'ko'); }
}

function telechargerICS(id) {
  const r = state.reservations.find(x => x.id === id);
  const f = d => d.replace(/[-:]/g, '').slice(0, 15);
  const lignes = ['BEGIN:VCALENDAR', 'VERSION:2.0', 'PRODID:-//SmartCampus ENSA Khouribga//FR', 'BEGIN:VEVENT',
    `UID:reservation-${r.id}@smartcampus`, `DTSTAMP:${f(new Date().toISOString())}Z`,
    `DTSTART:${f(r.dateDebut)}`, `DTEND:${f(r.dateFin)}`,
    `SUMMARY:${r.objet.replace(/[,;]/g, ' ')}`, `LOCATION:${r.salle} - ENSA Khouribga`, 'END:VEVENT', 'END:VCALENDAR'];
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([lignes.join('\r\n')], { type: 'text/calendar' }));
  a.download = `reservation-${r.id}.ics`;
  a.click();
  URL.revokeObjectURL(a.href);
}

/* ---------- Annonces aux étudiants ---------- */
function renderAnnonces() {
  const gs = state.groupes.filter(g => g.inscrits > 0);
  $('#view-annonces').innerHTML = `
    <div class="cols">
      <form class="card" onsubmit="return envoyerAnnonce(event)">
        <h3>Nouvelle annonce</h3>
        <p class="muted">Le message arrive dans l'espace de tous les étudiants du groupe choisi.</p>
        <label>Groupe destinataire
          <select id="anGroupe" required>${gs.map(g =>
            `<option value="${esc(g.code)}">${esc(g.code)} — ${esc(g.libelle)} (${g.inscrits} inscrits)</option>`).join('')}</select></label>
        <label>Message
          <textarea id="anMsg" rows="5" minlength="5" maxlength="1000" required
            placeholder="Ex. : Le cours de vendredi est déplacé à lundi 10h30, salle D-02."></textarea></label>
        <button class="btn">${ico('send', 16)} Envoyer l'annonce</button>
        <p class="hint" style="margin-top:12px">Astuce : cochez « Informer mes étudiants » dans une demande de rattrapage pour les prévenir automatiquement dès la confirmation.</p>
      </form>
      <div class="card"><h3>Annonces envoyées</h3>
        ${state.annonces.map(a => `<div class="annonce">
          <div class="annonce-head"><span class="chip CONFIRMEE">${esc(a.groupe)}</span><small>${fmt(a.dateEnvoi)} · ${a.nbDestinataires} destinataire(s)</small></div>
          <p>${esc(a.message)}</p></div>`).join('') || '<p class="muted">Aucune annonce envoyée pour le moment.</p>'}
      </div>
    </div>`;
}

async function envoyerAnnonce(e) {
  e.preventDefault();
  try {
    await api('/api/annonces', { method: 'POST',
      body: JSON.stringify({ groupe: $('#anGroupe').value, message: $('#anMsg').value }) });
    await loadAnnonces();
    renderAnnonces();
    toast('Annonce envoyée aux étudiants');
  } catch (err) { toast(err.message, 'ko'); }
  return false;
}

/* ---------- Notifications ---------- */
function renderNotifications() {
  $('#view-notifications').innerHTML = `
    <div class="toolbar-line"><span class="muted">${state.notifications.filter(n => !n.lue).length} non lue(s)</span>
      <button class="btn ghost" onclick="marquerLues()">${ico('check', 16)} Tout marquer comme lu</button></div>
    <div class="card flush">
      ${state.notifications.map(n => {
        const ok = n.message.startsWith('✔'), ko = n.message.startsWith('✖');
        return `<div class="notif ${n.lue ? '' : 'new'}"><div class="n-ico ${ok ? 'green' : ko ? 'red' : 'blue'}">${ico(ok ? 'check' : ko ? 'alert' : 'bell', 18)}</div>
          <div><p>${esc(n.message.replace(/^[✔✖]\s*/, ''))}</p><small class="muted">${fmt(n.dateEnvoi)}</small></div></div>`;
      }).join('') || '<p class="muted" style="padding:18px">Aucune notification.</p>'}
    </div>`;
}
async function marquerLues() { await api('/api/notifications/lues', { method: 'POST' }); await loadNotifications(); }

/* ---------- Profil ---------- */
function renderProfil() {
  const p = state.profil;
  const matieres = [...new Map(state.seances.map(s => [s.matiere + s.groupe, s])).values()];
  $('#view-profil').innerHTML = `
    <div class="cols">
      <div>
        <form class="card" onsubmit="return saveProfil(event)">
          <div class="profil-head">
            ${avatarHtml(p, 'xl')}
            <div>
              <h3 style="margin:0">Pr. ${esc(p.prenom)} ${esc(p.nom)}</h3>
              <p class="muted" style="margin:2px 0 10px">${esc(p.email)}</p>
              <input type="file" id="photoFile" accept="image/*" hidden onchange="changerPhoto(this)">
              <button type="button" class="btn ghost sm" onclick="$('#photoFile').click()">${ico('camera', 15)} Changer la photo</button>
              ${p.photo ? `<button type="button" class="btn ghost sm" onclick="supprimerPhoto()">${ico('trash', 15)}</button>` : ''}
            </div>
          </div>
          <div class="grid2">
            <label>Département<input id="pfDep" value="${esc(p.departement)}"></label>
            <label>Grade<input id="pfGrade" value="${esc(p.grade)}"></label>
            <label>Bureau<input id="pfBureau" value="${esc(p.bureau)}" placeholder="Bureau 12 — Département A"></label>
            <label>Téléphone<input id="pfTel" value="${esc(p.telephone)}" placeholder="06 00 00 00 00"></label>
          </div>
          <label>Présentation<textarea id="pfBio" rows="3" maxlength="600" placeholder="Quelques mots sur vos enseignements et vos centres d'intérêt">${esc(p.bio)}</textarea></label>
          <label class="check"><input type="checkbox" id="pfNotif" ${p.notifAuto ? 'checked' : ''}>
            <span>Informer automatiquement mes étudiants quand un rattrapage est confirmé<small>Cette option est pré-cochée dans chaque demande de réservation.</small></span></label>
          <button class="btn">Enregistrer</button>
        </form>
      </div>
      <div>
        <div class="card"><h3>Mes enseignements</h3>
          ${matieres.map(s => `<div class="row-ev"><div class="date-chip"><b>${esc(s.groupe)}</b></div>
            <div><b>${esc(s.matiere)}</b><small>${JOURS[s.jour]} · ${hm(s.heureDebut)}–${hm(s.heureFin)} · ${esc(s.salle)}</small></div></div>`).join('')
            || '<p class="muted">Aucun enseignement.</p>'}
        </div>
        <form class="card" style="margin-top:20px" onsubmit="return savePwd(event)">
          <h3>Sécurité</h3><p class="muted">Choisissez un mot de passe d'au moins 6 caractères.</p>
          <label>Ancien mot de passe<input type="password" id="pwOld" required></label>
          <label>Nouveau mot de passe<input type="password" id="pwNew" required minlength="6"></label>
          <button class="btn">Modifier le mot de passe</button>
        </form>
      </div>
    </div>`;
}

async function saveProfil(e) {
  e.preventDefault();
  try {
    state.profil = await api('/api/profil', { method: 'PUT', body: JSON.stringify({
      departement: $('#pfDep').value, grade: $('#pfGrade').value, bureau: $('#pfBureau').value,
      telephone: $('#pfTel').value, bio: $('#pfBio').value, notifAuto: $('#pfNotif').checked }) });
    majIdentite();
    toast('Profil mis à jour');
  } catch (err) { toast(err.message, 'ko'); }
  return false;
}

async function changerPhoto(input) {
  const f = input.files[0];
  if (!f) return;
  try {
    const data = await compresserImage(f, 256);
    state.profil = await api('/api/profil/photo', { method: 'PUT', body: JSON.stringify({ photo: data }) });
    majIdentite();
    renderProfil();
    toast('Photo de profil mise à jour');
  } catch (err) { toast(err.message, 'ko'); }
}

async function supprimerPhoto() {
  try {
    state.profil = await api('/api/profil/photo', { method: 'DELETE' });
    majIdentite();
    renderProfil();
    toast('Photo supprimée');
  } catch (err) { toast(err.message, 'ko'); }
}

async function savePwd(e) {
  e.preventDefault();
  try {
    await api('/api/profil/mot-de-passe', { method: 'PUT',
      body: JSON.stringify({ ancien: $('#pwOld').value, nouveau: $('#pwNew').value }) });
    toast('Mot de passe modifié');
    e.target.reset();
  } catch (err) { toast(err.message, 'ko'); }
  return false;
}