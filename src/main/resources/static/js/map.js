/* ===== Carte du campus ENSA Khouribga (Leaflet) : toutes les salles sont dessinées ===== */
const CAMPUS_BOUNDS = [[32.8962133, -6.9144184], [32.8981563, -6.91293]];

// Bâtiments non réservables (contexte visuel, utile surtout en mode « Plan simplifié »)
const CONTEXTE = [
  [[32.8980142,-6.9139654],[32.8979667,-6.9136451],[32.8976938,-6.9136988],[32.8977414,-6.914019]],
  [[32.8980848,-6.9143657],[32.8980431,-6.9140844],[32.8977048,-6.914151],[32.8977465,-6.9144323]],
  [[32.897893,-6.9129469],[32.8978493,-6.9126526],[32.8975729,-6.912707],[32.8976165,-6.9130013]],
  [[32.8974767,-6.9143295],[32.8974369,-6.9140611],[32.8969749,-6.9141521],[32.8970147,-6.9144204]],
  [[32.8973294,-6.9133874],[32.8972883,-6.9131104],[32.8970337,-6.9131606],[32.8970748,-6.9134375]],
  [[32.8970625,-6.9129767],[32.8970381,-6.9128122],[32.8968381,-6.9128516],[32.8968625,-6.9130161]],
  [[32.8968175,-6.9130917],[32.8967822,-6.9128537],[32.8965458,-6.9129002],[32.8965811,-6.9131383]]
];

const ETAT_LABEL = { LIBRE: 'Libre', OCCUPEE: 'Occupée', EN_ATTENTE: 'Demandes en attente' };
const HINTS = {
  COURS_RATTRAPAGE: 'Priorité élevée (3/5) : un rattrapage passe avant une activité de club.',
  REUNION: 'Priorité normale (1/5).',
  FORMATION: 'Priorité moyenne (2/5).',
  SOUTENANCE_THESE: 'Priorité maximale (4/5) : soutenance de thèse.'
};

let map = null, baseSat, baseOsm, baseBlank;
const layers = {};
let batiments = [];
let selEtage = 0;       // étage affiché sur toute la carte
let focusBat = null;    // bâtiment ciblé (zoom + liste), null = tout le campus
let curSalle = null;    // salle en cours de réservation
let tileErrors = 0;

/* ---------- Initialisation ---------- */
function initPlan() {
  const demain = new Date(); demain.setDate(demain.getDate() + 1);
  if (demain.getDay() === 0) demain.setDate(demain.getDate() + 1);   // pas de réservation le dimanche
  $('#pDate').value = isoDate(demain);
  $('#pDate').min = isoDate(new Date());
  $('#pDebut').value = '08:30';
  $('#pFin').value = '10:30';
  ['pDate', 'pDebut', 'pFin'].forEach(id => $('#' + id).onchange = loadPlan);
  ['pType', 'pCap'].forEach(id => $('#' + id).oninput = onFiltre);

  $('#floors').onclick = ev => {
    const b = ev.target.closest('button');
    if (!b || !map) return;
    if (b.dataset.act === 'campus') { focusBat = null; map.flyToBounds(CAMPUS_BOUNDS, { duration: 0.8 }); }
    else if (b.dataset.act === 'all') focusBat = null;
    else if (b.dataset.etage !== undefined) selEtage = +b.dataset.etage;
    renderBar(); renderCarte();
  };

  $('#resaForm').onsubmit = submitResa;
  $('#mCancel').onclick = closeModal;
  $('#mClose').onclick = closeModal;
  $('#modal').onclick = e => { if (e.target.id === 'modal') closeModal(); };
  $('#mType').onchange = majHint;
  $('#mUrgent').onchange = majHint;
  $('#mNiveau').onchange = majEffectif;
  $('#mNb').oninput = majAlertes;
  $('#mJustif').oninput = majCompteur;
}

function createMap() {
  if (typeof L === 'undefined') { toast('Leaflet non chargé : connexion internet requise', 'ko'); return false; }
  map = L.map('map', { minZoom: 15, maxZoom: 21, zoomSnap: 0.25 });

  baseSat = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    { maxNativeZoom: 18, maxZoom: 21, attribution: 'Imagerie © Esri, Maxar, Earthstar Geographics' });
  baseOsm = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    { maxNativeZoom: 19, maxZoom: 21, attribution: '© contributeurs OpenStreetMap' });
  baseBlank = L.layerGroup();
  baseSat.addTo(map);
  L.control.layers({ 'Satellite': baseSat, 'Plan OpenStreetMap': baseOsm, 'Plan simplifié': baseBlank },
                   null, { position: 'topright' }).addTo(map);
  map.on('baselayerchange', e => $('#map').classList.toggle('blank', e.layer === baseBlank));
  map.on('movestart', hideTip);
  map.on('zoomend', majZoom);

  // Si les tuiles satellite ne se chargent pas (réseau), bascule automatiquement sur le plan simplifié
  baseSat.on('tileerror', () => {
    if (++tileErrors === 6 && map.hasLayer(baseSat)) {
      map.removeLayer(baseSat);
      baseBlank.addTo(map);
      $('#map').classList.add('blank');
      toast('Fond satellite indisponible : plan simplifié affiché', 'ko');
    }
  });

  layers.contexte = L.layerGroup().addTo(map);
  layers.salles = L.layerGroup().addTo(map);
  layers.batiments = L.layerGroup().addTo(map);
  CONTEXTE.forEach(p => L.polygon(p, { className: 'ctx', interactive: false, weight: 1,
    color: '#64748b', fillColor: '#94a3b8', fillOpacity: 0.3 }).addTo(layers.contexte));

  map.fitBounds(CAMPUS_BOUNDS);
  majZoom();
  return true;
}

/** Selon le zoom : repères visibles de loin, noms des salles visibles de près. */
function majZoom() {
  const z = map.getZoom(), el = $('#map');
  el.classList.toggle('z-mid', z >= 19.5);
  el.classList.toggle('z-near', z >= 20);
}

/* ---------- Chargement des données ---------- */
async function loadPlan() {
  if (!map) { if (!createMap()) return; }
  else map.invalidateSize();

  const d = $('#pDate').value, a = $('#pDebut').value, b = $('#pFin').value;
  if (!d || !a || !b) return;
  if (b <= a) return toast("L'heure de fin doit être après le début", 'ko');
  try {
    if (!batiments.length) batiments = await api('/api/batiments');
    state.salles = await api(`/api/salles/disponibilite?debut=${d}T${a}&fin=${d}T${b}`);
  } catch (e) {
    $('#planStats').innerHTML = `${ico('alert', 16)} ${esc(e.message)}`;
    return toast(e.message, 'ko');
  }
  if (focusBat) focusBat = batiments.find(x => x.id === focusBat.id) || null;
  choisirEtageAuto();
  majStats();
  renderBar();
  renderCarte();
}

function onFiltre() {
  choisirEtageAuto();
  majStats();
  renderBar();
  renderCarte();
}

/** Si l'étage affiché ne contient aucune salle du type choisi, bascule sur celui qui en contient. */
function choisirEtageAuto() {
  const n = e => state.salles.filter(s => s.etage === e && matches(s)).length;
  if (n(selEtage) === 0) {
    const autre = selEtage === 0 ? 1 : 0;
    if (n(autre) > 0) selEtage = autre;
  }
}

function majStats() {
  const libres = state.salles.filter(s => s.etat !== 'OCCUPEE' && matches(s)).length;
  const total = state.salles.filter(matches).length;
  $('#planStats').innerHTML = `${ico('check', 16)} <b>${libres}</b> salle(s) réservable(s) sur ${total}`;
}

/* ---------- Géométrie ---------- */
// P = [coin haut-gauche, haut-droit, bas-droit, bas-gauche] ; u : 0→1 gauche→droite ; v : 0→1 haut→bas
function pt(P, u, v) {
  const top = [P[0][0] + (P[1][0] - P[0][0]) * u, P[0][1] + (P[1][1] - P[0][1]) * u];
  const bot = [P[3][0] + (P[2][0] - P[3][0]) * u, P[3][1] + (P[2][1] - P[3][1]) * u];
  return [top[0] + (bot[0] - top[0]) * v, top[1] + (bot[1] - top[1]) * v];
}

function cercle(b, opts) {
  return L.circle(pt(b.polygone, 0.5, 0.5),
    { radius: map.distance(b.polygone[0], b.polygone[1]) / 2, ...opts });
}

function contour(b, opts) {
  return b.forme === 'CERCLE' ? cercle(b, opts) : L.polygon(b.polygone, opts);
}

function salleShape(b, s, opts) {
  if (b.forme === 'CERCLE') return cercle(b, opts);
  const g = 0.012; // petit espace entre deux salles
  const u0 = s.colonne / b.gridCols + g, u1 = (s.colonne + s.largeur) / b.gridCols - g;
  const v0 = s.ligne / b.gridRows + g, v1 = (s.ligne + s.hauteur) / b.gridRows - g;
  return L.polygon([pt(b.polygone, u0, v0), pt(b.polygone, u1, v0),
                    pt(b.polygone, u1, v1), pt(b.polygone, u0, v1)], opts);
}

function matches(s) {
  const type = $('#pType').value, cap = +$('#pCap').value || 0;
  return (!type || s.type === type) && s.capacite >= cap;
}
const filtreActif = () => !!$('#pType').value || (+$('#pCap').value || 0) > 0;

/* ---------- Affichage ---------- */
function pinIcon(couleur) {
  return L.divIcon({ className: 'pin-wrap', iconSize: [26, 26], iconAnchor: [13, 31],
    html: `<div class="pin" style="background:${couleur}"></div>` });
}

function renderBar() {
  const etages = [...new Set(state.salles.map(s => s.etage))].sort();
  const libres = e => state.salles.filter(s => s.etage === e && matches(s) && s.etat !== 'OCCUPEE').length;
  let h = etages.map(e => `<button data-etage="${e}" class="${e === selEtage ? 'active' : ''}">
      ${e === 0 ? 'Rez-de-chaussée' : 'Étage ' + e} <em>${libres(e)}</em></button>`).join('');
  h += '<button data-act="campus">Recentrer la carte</button>';
  if (focusBat) h += `<button data-act="all" class="active">${esc(focusBat.nom)} ✕</button>`;
  $('#floors').innerHTML = h;
}

function renderCarte() {
  if (!map) return;
  layers.salles.clearLayers();
  layers.batiments.clearLayers();
  hideTip();
  const actif = filtreActif();

  batiments.forEach(b => {
    // Bâtiment sans cet étage (ex. amphithéâtres) : simple emprise grisée
    if (b.nbEtages <= selEtage) {
      contour(b, { className: 'bld-nofloor', interactive: false }).addTo(layers.salles);
    }
    state.salles.filter(s => s.batimentId === b.id && s.etage === selEtage).forEach(s => {
      const dim = !matches(s);
      const shape = salleShape(b, s, { className: `room ${s.etat} ${dim ? 'dim' : ''} ${actif && !dim ? 'match' : ''}` });
      shape.on('mousemove', e => showSalleTip(e.originalEvent, s))
           .on('mouseout', hideTip)
           .on('click', () => clickSalle(s));
      if (!dim) shape.bindTooltip(`<b>${esc(s.nom)}</b><br>${s.capacite} pl.`,
        { permanent: true, direction: 'center', className: 'room-label' });
      shape.addTo(layers.salles);
    });

    // Emprise du bâtiment (clic sur le trait = zoom) et repère façon Google Earth
    contour(b, { className: 'bld-outline' }).on('click', () => focusBuilding(b)).addTo(layers.batiments);
    L.marker([b.latitude, b.longitude], { icon: pinIcon(b.couleur), riseOnHover: true })
      .on('click', () => focusBuilding(b))
      .bindTooltip(b.nom, { permanent: true, direction: 'bottom', offset: [0, 6], className: 'bld-label' })
      .addTo(layers.batiments);
  });
  renderListe();
}

function focusBuilding(b) {
  focusBat = b;
  map.flyToBounds(L.latLngBounds(b.polygone).pad(0.35), { maxZoom: 21, duration: 0.8 });
  renderBar();
  renderListe();
}

function voirSalle(id) {
  const s = state.salles.find(x => x.id === id);
  const b = batiments.find(x => x.id === s.batimentId);
  selEtage = s.etage;
  focusBat = b;
  renderBar();
  renderCarte();
  map.flyToBounds(L.latLngBounds(b.polygone).pad(0.35), { maxZoom: 21, duration: 0.8 });
  window.scrollTo({ top: $('#map').getBoundingClientRect().top + window.scrollY - 90, behavior: 'smooth' });
}

function renderListe() {
  const ordre = { LIBRE: 0, EN_ATTENTE: 1, OCCUPEE: 2 };
  const l = state.salles.filter(s => matches(s) && (!focusBat || s.batimentId === focusBat.id))
    .sort((a, b) => ordre[a.etat] - ordre[b.etat] || a.batimentNom.localeCompare(b.batimentNom) || a.nom.localeCompare(b.nom));
  const reservables = l.filter(s => s.etat !== 'OCCUPEE').length;
  $('#roomList').innerHTML = `
    <div class="list-head"><h3>${l.length} salle(s) correspondent à vos critères${focusBat ? ' — ' + esc(focusBat.nom) : ''}</h3>
      <span class="muted">${reservables} réservable(s) sur ce créneau</span></div>
    <div class="scroll-y"><table><thead><tr><th>Salle</th><th>Type</th><th>Places</th><th>Équipements</th><th>Disponibilité</th><th></th></tr></thead><tbody>
    ${l.map(s => `<tr>
      <td><b>${esc(s.nom)}</b><br><small class="muted">${esc(s.batimentNom)} · ${s.etage === 0 ? 'RDC' : 'étage ' + s.etage}</small></td>
      <td>${TYPES_SALLE[s.type]}</td><td>${s.capacite}</td><td>${esc(s.equipements)}</td>
      <td><span class="chip ${s.etat}">${ETAT_LABEL[s.etat]}</span><br><small class="muted">${esc(s.detailEtat)}</small></td>
      <td class="right"><button class="btn ghost sm" onclick="voirSalle(${s.id})">${ico('map', 14)} Voir</button>
        ${s.etat !== 'OCCUPEE' ? `<button class="btn sm" onclick="reserverSalle(${s.id})">Réserver</button>` : ''}</td>
    </tr>`).join('') || '<tr><td colspan="6" class="muted center">Aucune salle ne correspond à ces critères.</td></tr>'}
    </tbody></table></div>`;
}

/* ---------- Infobulle ---------- */
function placeTip(e) {
  const tip = $('#tip');
  tip.style.left = Math.min(e.clientX + 16, innerWidth - 300) + 'px';
  tip.style.top = (e.clientY + 16) + 'px';
  tip.classList.remove('hidden');
}

function showSalleTip(e, s) {
  const etat = { LIBRE: '✔ Libre', OCCUPEE: '✖ Occupée', EN_ATTENTE: '⏳ Demandes en attente' }[s.etat];
  $('#tip').innerHTML = `<b>${esc(s.nom)}</b>
    <div>${TYPES_SALLE[s.type]} · ${esc(s.batimentNom)}, ${s.etage === 0 ? 'RDC' : 'étage ' + s.etage}</div>
    <div>${s.capacite} places</div><div>${esc(s.equipements)}</div>
    <div class="etat">${etat} — ${esc(s.detailEtat)}</div>
    ${s.etat !== 'OCCUPEE' ? '<small>Cliquez pour réserver</small>' : ''}`;
  placeTip(e);
}

function hideTip() { $('#tip').classList.add('hidden'); }

/* ---------- Formulaire de réservation ---------- */
function clickSalle(s) {
  if (s.etat === 'OCCUPEE') return toast(`${s.nom} est occupée : ${s.detailEtat}`, 'ko');
  openModal(s);
}
function reserverSalle(id) { clickSalle(state.salles.find(s => s.id === id)); }

function setSalleModal(s) {
  curSalle = s;
  $('#mSalle').textContent = s.nom;
  $('#mSalle').dataset.id = s.id;
  $('#mInfo').innerHTML = `<div class="rc-title">${ico('map', 16)} ${esc(s.batimentNom)} · ${s.etage === 0 ? 'Rez-de-chaussée' : 'Étage ' + s.etage}</div>
    <div class="rc-meta"><span>${TYPES_SALLE[s.type]}</span><span>${ico('users', 14)} ${s.capacite} places</span></div>
    <small>${esc(s.equipements)}</small>`;
  majAlertes();
}

function majAlertes() {
  if (!curSalle) return;
  const msgs = [];
  if (curSalle.etat === 'EN_ATTENTE') msgs.push(`${curSalle.detailEtat} — l'administration arbitrera selon la priorité.`);
  const nb = +$('#mNb').value || 0;
  if (nb > curSalle.capacite) msgs.push(`Capacité dépassée : ${curSalle.nom} compte ${curSalle.capacite} places pour ${nb} participants.`);
  const w = $('#mWarn');
  w.innerHTML = msgs.map(esc).join('<br>');
  w.classList.toggle('hidden', !msgs.length);
}

function majEffectif() {
  const g = state.groupes.find(x => x.code === $('#mNiveau').value);
  if (g) $('#mNb').value = g.effectif;
  majAlertes();
}

function majHint() {
  const urgent = $('#mUrgent').checked;
  $('#mHint').textContent = HINTS[$('#mType').value] + (urgent ? ' Urgence : +1, pensez à expliquer le motif dans la justification.' : '');
}

function majCompteur() {
  const n = $('#mJustif').value.length;
  const c = $('#mCount');
  c.textContent = `${n} / 1000 caractères (minimum 20)`;
  c.classList.toggle('ko-text', n < 20);
}

function openModal(s) {
  hideTip();
  const pre = state.prefill;
  state.prefill = null;
  $('#mProf').value = `Pr. ${state.profil.prenom} ${state.profil.nom}`;
  $('#mNiveau').innerHTML = state.groupes.map(g => `<option value="${esc(g.code)}">${esc(g.code)} — ${esc(g.libelle)}</option>`).join('');
  $('#matieres').innerHTML = [...new Set(state.seances.map(x => x.matiere))].map(m => `<option value="${esc(m)}">`).join('');
  $('#mType').value = pre ? pre.type : 'COURS_RATTRAPAGE';
  $('#mMatiere').value = pre ? pre.matiere : '';
  const niveauDefaut = pre ? pre.niveau : (state.seances[0] ? state.seances[0].groupe : 'MGSI3');
  if (state.groupes.some(g => g.code === niveauDefaut)) $('#mNiveau').value = niveauDefaut;
  $('#mDate').value = $('#pDate').value;
  $('#mDebut').value = $('#pDebut').value;
  $('#mFin').value = $('#pFin').value;
  $('#mJustif').value = '';
  $('#mUrgent').checked = false;
  $('#mNotif').checked = state.profil.notifAuto;
  $('#mSugg').classList.add('hidden');
  setSalleModal(s);
  majEffectif();
  majHint();
  majCompteur();
  $('#modal').classList.remove('hidden');
}
function closeModal() { $('#modal').classList.add('hidden'); }

/** Propose d'autres salles libres sur le créneau saisi (capacité suffisante, la plus proche du besoin d'abord). */
async function suggerer() {
  const d = $('#mDate').value, a = $('#mDebut').value, b = $('#mFin').value, nb = +$('#mNb').value || 0;
  const box = $('#mSugg');
  try {
    const l = await api(`/api/salles/disponibilite?debut=${d}T${a}&fin=${d}T${b}`);
    const cur = +$('#mSalle').dataset.id;
    state.suggest = l;
    const libres = l.filter(s => s.etat === 'LIBRE' && s.id !== cur && s.capacite >= nb)
      .sort((x, y) => (x.capacite - nb) - (y.capacite - nb)).slice(0, 5);
    box.innerHTML = libres.length
      ? `<b>Salles libres sur ce créneau</b><div>${libres.map(s =>
          `<button type="button" class="pill" onclick="choisirSalle(${s.id})">${esc(s.nom)} · ${s.capacite} pl.</button>`).join('')}</div>`
      : '<b>Aucune autre salle libre sur ce créneau.</b> Essayez un autre horaire.';
    box.classList.remove('hidden');
  } catch (e) { /* suggestion facultative */ }
}
function choisirSalle(id) {
  setSalleModal(state.suggest.find(x => x.id === id));
  $('#mSugg').classList.add('hidden');
}

async function submitResa(e) {
  e.preventDefault();
  const d = $('#mDate').value;
  const body = {
    salleId: +$('#mSalle').dataset.id,
    dateDebut: `${d}T${$('#mDebut').value}`,
    dateFin: `${d}T${$('#mFin').value}`,
    matiere: $('#mMatiere').value.trim(),
    niveau: $('#mNiveau').value,
    nbParticipants: +$('#mNb').value,
    type: $('#mType').value,
    justification: $('#mJustif').value.trim(),
    urgent: $('#mUrgent').checked,
    notifierEtudiants: $('#mNotif').checked
  };
  try {
    const r = await api('/api/reservations', { method: 'POST', body: JSON.stringify(body) });
    closeModal();
    toast("Demande envoyée à l'administration");
    await refreshData();
    show('reservations');
    openResa(r.id);
  } catch (err) {
    toast(err.message, 'ko');
    if (err.status === 409 || /capacit/i.test(err.message)) suggerer();
  }
}