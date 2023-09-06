<h1>Radio LNJ</h1>

<button id="play" class="btn btn-primary">Play</button>

<div>
    <img src="/img/winamp.png">
</div>

<span id="timer">-- --</span>

<p class="header">
    <b><%= request.getAttribute("elementCount") %></b> éléments dans la playlist,
    durée totale <b><%= request.getAttribute("totalDuration") %></b>
</p>

<div id="pre-title">Now playing:</div>
<div id="title">--</div>

<script src="/js/radio-lnj.js"></script>