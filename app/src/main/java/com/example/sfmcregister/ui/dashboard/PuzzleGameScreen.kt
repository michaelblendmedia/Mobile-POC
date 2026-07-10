package com.example.sfmcregister.ui.dashboard

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PuzzleGameScreen(
    onBackClick: () -> Unit = {}
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun closeGame() {
                        post { onBackClick() }
                    }
                }, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(
                            "document.getElementById('closeBtn').addEventListener('click', function() { Android.closeGame(); });",
                            null
                        )
                    }
                }

                loadDataWithBaseURL(null, GAME_HTML, "text/html", "UTF-8", null)
            }
        }
    )
}

private val GAME_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>OCBC Poinseru — Shape Fill</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Press+Start+2P&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
<style>
  :root{
    --ocbc-red:      #D91E36;
    --ocbc-red-dark: #7A0E1C;
    --ocbc-gold:     #F5B942;
    --cream:         #FFF6E9;
    --ink:           #2B1B12;
    --board-void:    #F1E4C9;
    --danger:        #B4212E;
  }
  *{box-sizing:border-box;}
  html,body{margin:0;padding:0;background:transparent;}
  body{
    font-family:'DM Mono', ui-monospace, monospace;
    color:var(--ink);
    display:flex;
    align-items:center;
    justify-content:center;
    min-height:100vh;
    padding:16px;
  }

  .modal{
    width:100%;
    max-width:360px;
    background:var(--cream);
    border:4px solid var(--ink);
    box-shadow:6px 6px 0 var(--ocbc-red-dark);
    padding:14px 14px 18px;
    position:relative;
  }

  /* ---- header ---- */
  .titlebar{
    display:flex;
    justify-content:space-between;
    align-items:flex-start;
    margin-bottom:10px;
  }
  .brand{
    font-family:'Press Start 2P', monospace;
    font-size:11px;
    line-height:1.5;
    color:var(--ocbc-red);
    letter-spacing:0.5px;
  }
  .brand small{display:block;font-size:7px;color:var(--ink);opacity:0.6;margin-top:3px;letter-spacing:0.3px;}
  .headerbtns{display:flex;gap:6px;align-items:flex-start;}
  .pillbtn{
    font-family:'DM Mono',monospace;
    font-size:10px;
    font-weight:500;
    background:var(--cream);
    border:2px solid var(--ink);
    padding:4px 7px;
    cursor:pointer;
    color:var(--ink);
  }
  .pillbtn:hover{background:var(--ocbc-gold);}
  .pillbtn:focus-visible, .piece:focus-visible{outline:3px solid var(--ocbc-red);outline-offset:2px;}
  .closebtn{
    font-family:'Press Start 2P',monospace;
    font-size:10px;
    border:2px solid var(--ink);
    background:var(--ink);
    color:var(--cream);
    width:26px;height:26px;
    cursor:pointer;
    line-height:1;
  }

  .statusbar{
    display:flex;
    justify-content:space-between;
    font-size:10px;
    background:var(--ink);
    color:var(--cream);
    padding:6px 8px;
    margin-bottom:10px;
  }
  .statusbar .val{color:var(--ocbc-gold);}

  .sr-only{
    position:absolute;width:1px;height:1px;padding:0;margin:-1px;
    overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0;
  }

  /* ---- board ---- */
  .board-wrap{
    display:flex;
    justify-content:center;
    margin-bottom:14px;
    position:relative;
  }
  .board{
    position:relative;
    background:var(--board-void);
    border:3px solid var(--ink);
    background-image:
      linear-gradient(to right, rgba(43,27,18,0.08) 1px, transparent 1px),
      linear-gradient(to bottom, rgba(43,27,18,0.08) 1px, transparent 1px);
  }
  .cell{
    position:absolute;
    box-sizing:border-box;
  }
  .cell.target{ background:rgba(217,30,54,0.16); }
  .cell.filled{
    background:var(--ocbc-red);
    border:1px solid var(--ocbc-red-dark);
    animation:pop 160ms ease-out;
  }
  .cell.preview-ok{ background:rgba(120,180,90,0.55); outline:1px dashed #4C8C4A; outline-offset:-1px; }
  .cell.preview-bad{ background:rgba(180,33,46,0.35); outline:1px dashed var(--danger); outline-offset:-1px; }
  @keyframes pop{
    0%{ transform:scale(0.4); opacity:0.4; }
    70%{ transform:scale(1.08); opacity:1; }
    100%{ transform:scale(1); }
  }
  .board.flash-invalid{ animation:flashred 260ms ease-out; }
  @keyframes flashred{
    0%{ box-shadow:0 0 0 0 rgba(180,33,46,0.0) inset; }
    30%{ box-shadow:0 0 0 6px rgba(180,33,46,0.35) inset; }
    100%{ box-shadow:0 0 0 0 rgba(180,33,46,0.0) inset; }
  }

  /* ---- tray ---- */
  .tray{
    display:flex;
    justify-content:space-between;
    gap:8px;
    margin-bottom:6px;
  }
  .slot{
    flex:1;
    background:#F7EEDC;
    border:2px dashed rgba(43,27,18,0.35);
    min-height:138px;
    display:flex;
    align-items:center;
    justify-content:center;
    position:relative;
  }
  .piece-hit{
    min-width:44px;
    min-height:44px;
    display:flex;
    align-items:center;
    justify-content:center;
    cursor:grab;
    touch-action:none;
  }
  .piece{ position:relative; }
  .piece .sq{
    position:absolute;
    background:var(--ocbc-gold);
    border:1px solid var(--ocbc-red-dark);
  }
  .piece.dragging{ cursor:grabbing; }
  .ghost{
    position:fixed;
    top:0; left:0;
    pointer-events:none;
    z-index:999;
    opacity:0.92;
    transition:transform 60ms linear;
  }
  .ghost.snapping{ transform:scale(1.05); }
  .ghost .sq{
    position:absolute;
    background:var(--ocbc-gold);
    border:1px solid var(--ocbc-red-dark);
  }
  .ghost.bounce{
    transition:top 220ms cubic-bezier(.34,1.56,.64,1), left 220ms cubic-bezier(.34,1.56,.64,1), opacity 220ms ease-in;
  }
  .ghost.shake{ animation:shake 220ms ease-in-out; }
  @keyframes shake{
    0%,100%{ margin-left:0; }
    25%{ margin-left:-5px; }
    75%{ margin-left:5px; }
  }

  .footer-msg{
    text-align:center;
    font-size:10px;
    opacity:0.75;
    min-height:14px;
  }

  /* ---- win overlay ---- */
  .overlay{
    position:absolute; inset:0;
    background:rgba(43,27,18,0.92);
    display:none;
    align-items:center;
    justify-content:center;
    flex-direction:column;
    text-align:center;
    padding:24px;
  }
  .overlay.show{ display:flex; }
  .overlay h2{
    font-family:'Press Start 2P',monospace;
    color:var(--ocbc-gold);
    font-size:14px;
    line-height:1.6;
    margin:0 0 14px;
  }
  .overlay p{ color:var(--cream); font-size:11px; line-height:1.7; margin:4px 0; }
  .overlay .reward{ color:var(--ocbc-gold); font-weight:500; }
  .overlay .playagain{
    margin-top:16px;
    font-family:'Press Start 2P',monospace;
    font-size:10px;
    background:var(--ocbc-red);
    color:var(--cream);
    border:2px solid var(--cream);
    padding:10px 14px;
    cursor:pointer;
  }
  .overlay .playagain:hover{ background:var(--ocbc-gold); color:var(--ink); }
</style>
</head>
<body>

<div class="modal" id="modal">
  <p class="sr-only" id="srSummary">
    OCBC Poinseru shape fill puzzle. Drag tile pieces onto the faint hexagon outline to fill it completely and earn Poinseru rewards.
  </p>

  <div class="titlebar">
    <div class="brand" id="titleText">OCBC<br><small id="subTitleText">POINSERU · FILL THE MARK</small></div>
    <div class="headerbtns">
      <button class="pillbtn" id="langBtn" type="button" aria-label="Toggle language">EN / ID</button>
      <button class="closebtn" id="closeBtn" type="button" aria-label="Close">×</button>
    </div>
  </div>

  <div class="statusbar">
    <span id="movesLabel">MOVES: <span class="val" id="movesVal">0</span></span>
    <span id="poinLabel">POINSERU: <span class="val" id="poinVal">0</span></span>
  </div>

  <div class="board-wrap">
    <div class="board" id="board"></div>
    <div class="overlay" id="overlay">
      <h2 id="winTitle">SHAPE COMPLETE!</h2>
      <p id="winBody">You've filled the mark and earned a reward.</p>
      <p class="reward" id="winReward">+50 Poinseru</p>
      <p class="reward" id="winSpin">🎡 Spin &amp; Win token unlocked!</p>
      <button class="playagain" id="playAgainBtn" type="button">PLAY AGAIN</button>
    </div>
  </div>

  <div class="tray" id="tray"></div>
  <div class="footer-msg" id="footerMsg">Drag a piece onto the outline to begin.</div>
</div>

<script>
(function(){
  "use strict";

  /* ============ localization ============ */
  var STRINGS = {
    en:{
      title:"OCBC", subtitle:"POINSERU · FILL THE MARK",
      moves:"MOVES:", poinseru:"POINSERU:",
      hint:"Drag a piece onto the outline to begin.",
      hintMid:"Keep going — the shape is taking form.",
      hintInvalid:"That piece doesn't fit there.",
      winTitle:"SHAPE COMPLETE!",
      winBody:"You've filled the mark and earned a reward.",
      winReward:"+" + 50 + " Poinseru",
      winSpin:"🎡 Spin & Win token unlocked!",
      playAgain:"PLAY AGAIN",
      lang:"EN / ID"
    },
    id:{
      title:"OCBC", subtitle:"POINSERU · ISI LOGO",
      moves:"LANGKAH:", poinseru:"POINSERU:",
      hint:"Seret satu bagian ke garis luar untuk mulai.",
      hintMid:"Terus lanjutkan — bentuknya mulai terlihat.",
      hintInvalid:"Bagian ini tidak pas di sana.",
      winTitle:"BENTUK SELESAI!",
      winBody:"Kamu berhasil mengisi logo dan mendapat hadiah.",
      winReward:"+" + 50 + " Poinseru",
      winSpin:"🎡 Token Spin & Win terbuka!",
      playAgain:"MAIN LAGI",
      lang:"EN / ID"
    }
  };
  var locale = "en";

  function applyStrings(){
    var s = STRINGS[locale];
    document.getElementById("titleText").firstChild.textContent = s.title;
    document.getElementById("subTitleText").textContent = s.subtitle;
    document.getElementById("movesLabel").firstChild.textContent = s.moves + " ";
    document.getElementById("poinLabel").firstChild.textContent = s.poinseru + " ";
    document.getElementById("winTitle").textContent = s.winTitle;
    document.getElementById("winBody").textContent = s.winBody;
    document.getElementById("winReward").textContent = s.winReward;
    document.getElementById("winSpin").textContent = s.winSpin;
    document.getElementById("playAgainBtn").textContent = s.playAgain;
    setFooter(state.won ? "" : (state.moves === 0 ? s.hint : s.hintMid));
  }

  /* ============ grid + silhouette ============ */
  var COLS = 10, ROWS = 10, CELL = 32;

  // Pre-computed hexagon-ring "mark" silhouette (grid-decomposed ahead of time).
  // Lower resolution + bigger CELL size = larger, easier-to-hit tiles while still
  // fitting a phone-width modal (10 * 32px = 320px board).
  var TARGET_CELLS = [[1,3],[1,4],[1,5],[1,6],[2,1],[2,2],[2,3],[2,4],[2,5],[2,6],[2,7],[2,8],
    [3,1],[3,2],[3,7],[3,8],[4,1],[4,2],[4,7],[4,8],[5,1],[5,2],[5,7],[5,8],
    [6,1],[6,2],[6,7],[6,8],[7,1],[7,2],[7,3],[7,4],[7,5],[7,6],[7,7],[7,8],
    [8,3],[8,4],[8,5],[8,6]];

  var TARGET_SET = {};
  TARGET_CELLS.forEach(function(rc){ TARGET_SET[rc[0]+","+rc[1]] = true; });

  /* ============ shape library (fixed orientations, no live rotation) ============ */
  var SHAPE_VARIANTS = {
    mono:    [[[0,0]]],
    domino:  [[[0,0],[0,1]], [[0,0],[1,0]]],
    tri_I:   [[[0,0],[0,1],[0,2]], [[0,0],[1,0],[2,0]]],
    tri_L:   [[[0,0],[1,0],[1,1]], [[0,0],[0,1],[1,0]], [[0,0],[0,1],[1,1]], [[0,1],[1,0],[1,1]]],
    tetra_I: [[[0,0],[0,1],[0,2],[0,3]], [[0,0],[1,0],[2,0],[3,0]]],
    tetra_O: [[[0,0],[0,1],[1,0],[1,1]]],
    tetra_T: [[[0,0],[0,1],[0,2],[1,1]], [[0,1],[1,0],[1,1],[2,1]], [[0,1],[1,0],[1,1],[1,2]], [[0,0],[1,0],[1,1],[2,0]]],
    tetra_S: [[[0,1],[0,2],[1,0],[1,1]], [[0,0],[1,0],[1,1],[2,1]]],
    tetra_L: [[[0,0],[1,0],[2,0],[2,1]], [[0,0],[0,1],[0,2],[1,0]], [[0,0],[0,1],[1,1],[2,1]], [[0,2],[1,0],[1,1],[1,2]]],
    tetra_J: [[[0,1],[1,1],[2,0],[2,1]], [[0,0],[1,0],[1,1],[1,2]], [[0,0],[0,1],[1,0],[2,0]], [[0,0],[0,1],[0,2],[1,2]]]
  };

  var SHAPE_POOL = []; // flattened {cells:[[r,c]...], size}
  Object.keys(SHAPE_VARIANTS).forEach(function(fam){
    SHAPE_VARIANTS[fam].forEach(function(cells){
      SHAPE_POOL.push({ cells: cells, size: cells.length, family: fam });
    });
  });
  var MONO_SHAPE = SHAPE_POOL[0]; // single-cell fallback, guarantees winnability

  /* ============ game state ============ */
  var state = {
    filled: {},      // "r,c" -> true
    filledCount: 0,
    moves: 0,
    poinseru: 0,
    won: false,
    locked: false,   // input lock during animations
    tray: [null, null]
  };
  var TOTAL_TARGET = TARGET_CELLS.length;

  /* ============ placement checks ============ */
  function canPlaceAt(cells, originR, originC){
    for (var i=0;i<cells.length;i++){
      var r = originR + cells[i][0];
      var c = originC + cells[i][1];
      var key = r+","+c;
      if (!TARGET_SET[key]) return false;
      if (state.filled[key]) return false;
    }
    return true;
  }

  function fitsSomewhere(cells){
    for (var r=0;r<ROWS;r++){
      for (var c=0;c<COLS;c++){
        if (canPlaceAt(cells, r, c)) return true;
      }
    }
    return false;
  }

  function findBestAnchor(cells, hoverR, hoverC, grabOffset){
    // authoritative + preview share this cheap check; anchor derived from where the
    // grabbed cell of the piece currently hovers.
    var originR = hoverR - grabOffset[0];
    var originC = hoverC - grabOffset[1];
    return { r: originR, c: originC, ok: canPlaceAt(cells, originR, originC) };
  }

  /* ============ adaptive piece generation ============ */
  function fillRatio(){ return state.filledCount / TOTAL_TARGET; }

  function weightedPick(list){
    var total = 0;
    for (var i=0;i<list.length;i++) total += list[i].weight;
    var roll = Math.random() * total;
    for (var j=0;j<list.length;j++){
      roll -= list[j].weight;
      if (roll <= 0) return list[j].shape;
    }
    return list[list.length-1].shape;
  }

  function generatePiece(){
    var ratio = fillRatio();
    var remaining = TOTAL_TARGET - state.filledCount;
    if (remaining <= 0) return MONO_SHAPE;

    // Early phase: fully random from the whole shape set.
    if (ratio < 0.35){
      var idx = Math.floor(Math.random() * SHAPE_POOL.length);
      var candidate = SHAPE_POOL[idx];
      // still must never hand out a piece that fits nowhere (hard requirement)
      if (fitsSomewhere(candidate.cells)) return candidate;
      // fall through to guided logic below if the random pick doesn't fit
    }

    // Guided phase: bias toward shapes that fit at least one remaining gap.
    var fitting = [];
    for (var k=0;k<SHAPE_POOL.length;k++){
      var s = SHAPE_POOL[k];
      if (fitsSomewhere(s.cells)){
        var biggerBias = Math.pow(s.size, 1 + ratio * 1.5);
        fitting.push({ shape:s, weight: biggerBias });
      }
    }
    // Monomino fallback is always eligible on its own and always fits any single
    // empty cell — this is what guarantees the last gap(s) can always be completed.
    if (fitting.length === 0){
      return MONO_SHAPE;
    }
    // keep a small guaranteed sliver of weight on mono so tight endgames stay easy
    var monoIncluded = fitting.some(function(f){ return f.shape.family === "mono"; });
    if (!monoIncluded) fitting.push({ shape: MONO_SHAPE, weight: 1 });
    return weightedPick(fitting);
  }

  /* ============ rendering ============ */
  var boardEl = document.getElementById("board");
  var trayEl = document.getElementById("tray");
  boardEl.style.width = (COLS*CELL) + "px";
  boardEl.style.height = (ROWS*CELL) + "px";

  var cellEls = {}; // "r,c" -> element
  function buildBoardDom(){
    var frag = document.createDocumentFragment();
    TARGET_CELLS.forEach(function(rc){
      var el = document.createElement("div");
      el.className = "cell target";
      el.style.left = (rc[1]*CELL) + "px";
      el.style.top = (rc[0]*CELL) + "px";
      el.style.width = CELL + "px";
      el.style.height = CELL + "px";
      boardEl.appendChild(el);
      cellEls[rc[0]+","+rc[1]] = el;
    });
  }
  buildBoardDom();

  function renderFilledCell(r,c){
    var el = cellEls[r+","+c];
    if (el){ el.classList.add("filled"); el.classList.remove("target"); }
  }

  function clearPreview(){
    Object.keys(cellEls).forEach(function(key){
      cellEls[key].classList.remove("preview-ok","preview-bad");
    });
  }

  function showPreview(cells, originR, originC, ok){
    for (var i=0;i<cells.length;i++){
      var r = originR + cells[i][0], c = originC + cells[i][1];
      var el = cellEls[r+","+c];
      if (el) el.classList.add(ok ? "preview-ok" : "preview-bad");
    }
  }

  function pieceDims(cells){
    var maxR=0, maxC=0;
    cells.forEach(function(rc){ maxR=Math.max(maxR,rc[0]); maxC=Math.max(maxC,rc[1]); });
    return { w:(maxC+1)*CELL, h:(maxR+1)*CELL };
  }

  function buildPieceEl(cells, cellSize){
    var dims = pieceDims(cells);
    var wrap = document.createElement("div");
    wrap.className = "piece";
    wrap.style.width = (dims.w * cellSize / CELL) + "px";
    wrap.style.height = (dims.h * cellSize / CELL) + "px";
    cells.forEach(function(rc){
      var sq = document.createElement("div");
      sq.className = "sq";
      sq.style.left = (rc[1]*cellSize) + "px";
      sq.style.top = (rc[0]*cellSize) + "px";
      sq.style.width = cellSize + "px";
      sq.style.height = cellSize + "px";
      wrap.appendChild(sq);
    });
    return wrap;
  }

  function renderTraySlot(index){
    var slotEl = trayEl.children[index];
    slotEl.innerHTML = "";
    var shape = state.tray[index];
    if (!shape) return;
    var hit = document.createElement("div");
    hit.className = "piece-hit";
    hit.tabIndex = 0;
    hit.setAttribute("role","button");
    hit.setAttribute("aria-label","Draggable puzzle piece");
    var pieceEl = buildPieceEl(shape.cells, CELL);
    hit.appendChild(pieceEl);
    slotEl.appendChild(hit);
    attachDrag(hit, index);
  }

  function buildTrayDom(){
    trayEl.innerHTML = "";
    for (var i=0;i<state.tray.length;i++){
      var slot = document.createElement("div");
      slot.className = "slot";
      trayEl.appendChild(slot);
    }
  }
  buildTrayDom();

  function refillSlot(index){
    state.tray[index] = generatePiece();
    renderTraySlot(index);
  }

  function setFooter(msg){
    document.getElementById("footerMsg").textContent = msg;
  }

  /* ============ drag interaction ============ */
  var DRAG = null; // active drag session

  // Normalizes touch and mouse events to a single {x,y} point. Using explicit
  // touch events (rather than relying on Pointer Events + touch-action alone)
  // is the most reliably cross-browser way to get real drag tracking on phones —
  // some mobile browsers/WebViews don't consistently honor touch-action:none,
  // and end up treating the drag as a page-scroll instead of feeding move events.
  function getPoint(ev){
    if (ev.touches && ev.touches.length > 0) return { x: ev.touches[0].clientX, y: ev.touches[0].clientY };
    if (ev.changedTouches && ev.changedTouches.length > 0) return { x: ev.changedTouches[0].clientX, y: ev.changedTouches[0].clientY };
    return { x: ev.clientX, y: ev.clientY };
  }

  function attachDrag(hitEl, slotIndex){
    hitEl.addEventListener("touchstart", function(ev){ onDragStart(ev, hitEl, slotIndex); }, { passive: false });
    hitEl.addEventListener("mousedown", function(ev){ onDragStart(ev, hitEl, slotIndex); });
  }

  function onDragStart(ev, hitEl, slotIndex){
    if (state.locked || state.won) return;
    var shape = state.tray[slotIndex];
    if (!shape) return;
    ev.preventDefault(); // blocks the page from scrolling once a drag actually starts

    var pt = getPoint(ev);
    var rect = hitEl.getBoundingClientRect();
    var boardRect = boardEl.getBoundingClientRect();

    // which cell of the piece did the user grab (grid units)
    var localX = pt.x - rect.left;
    var localY = pt.y - rect.top;
    var grabCol = Math.min(Math.max(Math.floor(localX / CELL), 0), 99);
    var grabRow = Math.min(Math.max(Math.floor(localY / CELL), 0), 99);

    var ghost = buildPieceEl(shape.cells, CELL);
    ghost.classList.add("ghost");
    document.body.appendChild(ghost);
    ghost.style.left = (pt.x - grabCol*CELL - CELL/2) + "px";
    ghost.style.top  = (pt.y - grabRow*CELL - CELL/2) + "px";

    hitEl.style.visibility = "hidden";

    DRAG = {
      slotIndex: slotIndex,
      shape: shape,
      grab: [grabRow, grabCol],
      ghost: ghost,
      hitEl: hitEl,
      lastAnchor: null,
      lastOk: null,
      boardRect: boardRect
    };

    document.addEventListener("touchmove", onDragMove, { passive: false });
    document.addEventListener("touchend", onDragEnd, { passive: false });
    document.addEventListener("touchcancel", onDragEnd, { passive: false });
    document.addEventListener("mousemove", onDragMove);
    document.addEventListener("mouseup", onDragEnd);
  }

  function onDragMove(ev){
    if (!DRAG) return;
    ev.preventDefault();
    var pt = getPoint(ev);
    var d = DRAG;
    var ghost = d.ghost;
    ghost.style.left = (pt.x - d.grab[1]*CELL - CELL/2) + "px";
    ghost.style.top  = (pt.y - d.grab[0]*CELL - CELL/2) + "px";

    var boardRect = boardEl.getBoundingClientRect();
    var relX = pt.x - boardRect.left;
    var relY = pt.y - boardRect.top;
    var hoverC = Math.floor(relX / CELL);
    var hoverR = Math.floor(relY / CELL);

    var HYST = 10; // px hysteresis margin — easier to stay snapped than to leave
    var candidate = findBestAnchor(d.shape.cells, hoverR, hoverC, d.grab);

    var useAnchor = candidate;
    if (d.lastAnchor && d.lastOk){
      // check if pointer is still within the padded zone of the previous valid anchor
      var prevPxLeft = boardRect.left + d.lastAnchor.c*CELL - HYST;
      var prevPxTop  = boardRect.top  + d.lastAnchor.r*CELL - HYST;
      var dims = pieceDims(d.shape.cells);
      var prevPxRight = prevPxLeft + dims.w + HYST*2;
      var prevPxBottom = prevPxTop + dims.h + HYST*2;
      var stillInside = pt.x >= prevPxLeft && pt.x <= prevPxRight &&
                         pt.y >= prevPxTop && pt.y <= prevPxBottom;
      if (stillInside && candidate.r === d.lastAnchor.r && candidate.c === d.lastAnchor.c){
        useAnchor = candidate;
      } else if (stillInside && !candidate.ok){
        // keep previous good snap rather than flicker to invalid right at the edge
        useAnchor = { r:d.lastAnchor.r, c:d.lastAnchor.c, ok:true };
      }
    }

    clearPreview();
    var onBoard = hoverR >= -2 && hoverR <= ROWS+2 && hoverC >= -2 && hoverC <= COLS+2;
    if (onBoard){
      showPreview(d.shape.cells, useAnchor.r, useAnchor.c, useAnchor.ok);
      ghost.classList.toggle("snapping", useAnchor.ok);
    } else {
      ghost.classList.remove("snapping");
    }
    d.lastAnchor = { r: useAnchor.r, c: useAnchor.c };
    d.lastOk = useAnchor.ok;
  }

  function onDragEnd(ev){
    if (!DRAG) return;
    ev.preventDefault();
    var d = DRAG;
    document.removeEventListener("touchmove", onDragMove);
    document.removeEventListener("touchend", onDragEnd);
    document.removeEventListener("touchcancel", onDragEnd);
    document.removeEventListener("mousemove", onDragMove);
    document.removeEventListener("mouseup", onDragEnd);
    clearPreview();

    // Authoritative full grid fit-check happens here, once, on release.
    var finalOk = d.lastAnchor ? canPlaceAt(d.shape.cells, d.lastAnchor.r, d.lastAnchor.c) : false;

    if (finalOk){
      lockInPiece(d);
    } else {
      bounceBack(d);
    }
    DRAG = null;
  }

  function lockInPiece(d){
    state.locked = true;
    var anchor = d.lastAnchor;
    // atomic state update: grid, tray, win-check all happen together
    d.shape.cells.forEach(function(rc){
      var r = anchor.r+rc[0], c = anchor.c+rc[1];
      state.filled[r+","+c] = true;
    });
    state.filledCount += d.shape.cells.length;
    state.moves += 1;
    document.getElementById("movesVal").textContent = state.moves;

    d.shape.cells.forEach(function(rc){ renderFilledCell(anchor.r+rc[0], anchor.c+rc[1]); });

    d.ghost.remove();
    d.hitEl.style.visibility = "";
    refillSlot(d.slotIndex);

    setFooter(STRINGS[locale].hintMid);

    var didWin = (state.filledCount === TOTAL_TARGET);
    state.locked = false;
    if (didWin) triggerWin();
  }

  function bounceBack(d){
    state.locked = true;
    var origin = d.hitEl.getBoundingClientRect();
    d.ghost.classList.add("bounce");
    d.ghost.classList.add("shake");
    boardEl.classList.add("flash-invalid");
    requestAnimationFrame(function(){
      d.ghost.style.left = origin.left + "px";
      d.ghost.style.top = origin.top + "px";
    });
    setFooter(STRINGS[locale].hintInvalid);
    setTimeout(function(){
      d.ghost.remove();
      d.hitEl.style.visibility = "";
      boardEl.classList.remove("flash-invalid");
      state.locked = false;
      setFooter(state.moves === 0 ? STRINGS[locale].hint : STRINGS[locale].hintMid);
    }, 240);
  }

  /* ============ win + reward ============ */
  var REWARD_POINSERU = 50; // flat award — always granted, never withheld or scaled

  function triggerWin(){
    state.won = true;
    state.poinseru += REWARD_POINSERU;
    document.getElementById("poinVal").textContent = state.poinseru;
    document.getElementById("overlay").classList.add("show");
  }

  document.getElementById("playAgainBtn").addEventListener("click", function(){
    resetGame();
  });

  function resetGame(){
    state.filled = {};
    state.filledCount = 0;
    state.moves = 0;
    state.won = false;
    state.locked = false;
    document.getElementById("movesVal").textContent = 0;
    document.getElementById("overlay").classList.remove("show");
    Object.keys(cellEls).forEach(function(key){
      cellEls[key].className = "cell target";
    });
    for (var i=0;i<state.tray.length;i++) refillSlot(i);
    setFooter(STRINGS[locale].hint);
  }

  /* ============ language + close ============ */
  document.getElementById("langBtn").addEventListener("click", function(){
    locale = (locale === "en") ? "id" : "en";
    applyStrings();
  });

  // CLOSE BUTTON IS HANDLED BY ANDROID JAVASCRIPT INTERFACE

  function destroy(){
    // clean up listeners so nothing lingers once the popup closes
    document.removeEventListener("touchmove", onDragMove);
    document.removeEventListener("touchend", onDragEnd);
    document.removeEventListener("touchcancel", onDragEnd);
    document.removeEventListener("mousemove", onDragMove);
    document.removeEventListener("mouseup", onDragEnd);
  }

  /* ============ init ============ */
  for (var i=0;i<state.tray.length;i++) refillSlot(i);
  applyStrings();
})();
</script>
</body>
</html>
"""
