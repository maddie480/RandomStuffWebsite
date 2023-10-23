<%@ page import="java.util.List, com.max480.randomstuff.gae.quest.Background, com.max480.randomstuff.gae.quest.GameBackground, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<script src="https://code.jquery.com/jquery-3.7.1.min.js"
    integrity="sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=" crossorigin="anonymous"></script>

<h1>Liste des arrière-plans de profil</h1>

<% if((boolean) request.getAttribute("error")) { %>
    <div class="alert alert-danger">Une erreur est survenue lors du
        chargement de la page.
        Veuillez réessayer dans quelques minutes, ou contacter Maddie si l'erreur persiste.
    </div>
<% } else if((boolean) request.getAttribute("tokenExpired")) { %>
    <div class="alert alert-danger">
        L'URL a expiré. Relance la commande <code>!backgrounds</code> pour obtenir un nouveau lien.
    </div>
<% } else { %>
    <div class="alert alert-primary" role="alert">
        <b><%= escapeHtml4((String) request.getAttribute("pseudo")) %></b>, tu as <b><%= escapeHtml4((String) request.getAttribute("credit")) %></b>.
    </div>

    <!-- ==================== Arrière-plans custom ==================== -->

    <h2>Arrière-plans de la communauté</h2>

    <p>
        Pour choisir ou acheter un arrière-plan, utilise la commande <code>!choose_bg [nom de l'arrière-plan*]</code>.
        Tu peux cliquer sur "Copier la commande" pour copier cette commande dans le presse-papiers.
    </p>

    <div class="row">
        <% for(Background bg : (List<Background>) request.getAttribute("backgrounds")) { %>
            <div class="col-lg-4 col-md-6 px-3 py-3">
                <div class="card">
                    <img src="/backgrounds/<%= escapeHtml4(bg.fileName) %>" class="card-img-top" alt="Arrière-plan">
                    <div class="card-body">
                        <h5 class="card-title"><%= escapeHtml4(bg.nameUrlDecoded) %></h5>
                        <h6 class="card-subtitle mb-2 text-muted">
                            par <%= escapeHtml4(bg.author) %>
                        </h6>

                        <% if(bg.bought) { %>
                            <p class="card-text text-success">
                                <b>Acheté</b>
                            </p>
                        <% } else { %>
                            <p class="card-text">
                                Prix : <b><%= escapeHtml4(bg.price) %></b>
                            </p>
                        <% } %>

                        <a href="#" class="btn btn-primary copy" data-name="<%= escapeHtml4(bg.nameUrlDecoded) %>">
                            Copier la commande
                        </a>
                    </div>
                </div>
            </div>

        <% } %>
    </div>

    <!-- ==================== Arrière-plans de jeu ==================== -->
    <h2>Arrière-plans de jeu</h2>

    <p>
        Pour choisir ou acheter un arrière-plan de jeu, utilise la commande <code>!choose_game_bg [nom du jeu*]</code>.
        Pour revenir à l'arrière-plan par défaut, utilise <code>!reset_bg</code>.
        Tu peux cliquer sur "Copier la commande" en-dessous d'un arrière-plan pour copier la commande correspondante dans le presse-papiers.
    </p>

    <div class="alert alert-info" role="alert">
        La liste affichée ici ne contient que les arrière-plans que tu as achetés et celui que le bot t'a donné par défaut.
        Il y en a d'autres, essaie d'utiliser la commande <code>!choose_game_bg [nom du jeu*]</code> pour en choisir un.<br/>
        <b>Note :</b> La plupart de ces arrière-plans sont fournis par Discord, tu trouveras donc presque uniquement des jeux PC.
    </div>

    <div class="row">
        <% for(GameBackground bg : (List<GameBackground>) request.getAttribute("gameBackgrounds")) { %>
            <div class="col-lg-4 col-md-6 px-3 py-3">
                <div class="card">
                    <img src="<%= escapeHtml4(bg.url) %>" class="card-img-top" alt="Arrière-plan">
                    <div class="card-body">
                        <h5 class="card-title"><%= escapeHtml4(bg.name) %></h5>

                        <% if(bg.defaultBg) { %>
                            <p class="card-text text-secondary">
                                Arrière-plan par défaut
                            </p>

                            <a href="#" class="btn btn-primary copy-default">
                                Copier la commande
                            </a>
                        <% } else { %>
                            <p class="card-text text-success">
                                <b>Acheté</b>
                            </p>

                            <a href="#" class="btn btn-primary copy-game" data-name="<%= escapeHtml4(bg.name) %>">
                                Copier la commande
                            </a>
                        <% } %>
                    </div>
                </div>
            </div>
        <% } %>
    </div>

    <!-- ==================== Toast "commande copiée avec succès" ==================== -->

    <div class="toast" style="position: fixed; bottom: 10px; right: 10px;" id="success">
        <div class="toast-body">
            Commande copiée dans le presse-papiers
        </div>
    </div>

    <script src="js/backgrounds.js"></script>
<% } %>