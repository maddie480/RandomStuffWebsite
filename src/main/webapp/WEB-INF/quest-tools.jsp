<%@ page import="java.util.List, com.max480.randomstuff.gae.quest.Tool, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static com.max480.randomstuff.gae.quest.ModListPage.normalize"%>

<%@page session="false"%>

<script src="https://code.jquery.com/jquery-3.7.1.min.js"
    integrity="sha256-/JqT3SQfawRcv/BIHPThkBvs0OEvtFFmqPF/lYI/Cxo=" crossorigin="anonymous"></script>

<h1>Liste des outils et logiciels de la communauté</h1>

Pour ajouter des outils ici, <a href="https://maddie480.ovh/quest/discord" target="_blank">rejoins le serveur QUEST Community</a> et demande à Maddie.

<% if((boolean) request.getAttribute("error")) { %>
    <div class="alert alert-danger">Une erreur est survenue lors du
        chargement de la page.
        Veuillez réessayer dans quelques minutes, ou contacter Maddie si l'erreur persiste.
    </div>
<% } else { %>
    <% for(Tool tool : (List<Tool>) request.getAttribute("tools")) { %>
        <div class="card my-3">
            <div class="card-body">
                <div class="row">
                    <% if(tool.imageUrl != null && !tool.imageUrl.isEmpty()) { %>
                        <div class="col-md-4">
                            <img class="tool-image" src="<%= escapeHtml4(tool.imageUrl) %>" alt="Image de l'outil" data-name="<%= escapeHtml4(tool.name) %>" data-bs-toggle="modal" data-bs-target="#zoom-image-popup">
                        </div>
                        <div class="col-md-8 mt-3 mt-md-0">
                    <% } else { %>
                        <div class="col-md-12">
                    <% } %>
                        <h5 class="card-title"><%= escapeHtml4(tool.name) %></h5>
                        <h6 class="card-subtitle mb-2 text-muted">par <%= escapeHtml4(tool.author) %> - v<%= escapeHtml4(tool.version) %></h6>
                        <%= tool.longDescription %>
                        <% if(tool.moreInfoUrl != null && !tool.moreInfoUrl.isEmpty() && !tool.moreInfoUrl.startsWith("http")) { %>
                            <hr><h5>Plus d'infos</h5>
                        <%= tool.moreInfoUrl %>
                        <% } %>

                        <div class="mt-3">
                            <a href="<%= escapeHtml4(tool.downloadUrl) %>" target="_blank" class="btn btn-success">Télécharger</a>
                            <% if(tool.moreInfoUrl != null && !tool.moreInfoUrl.isEmpty() && tool.moreInfoUrl.startsWith("http")) { %>
                                <a href="<%= escapeHtml4(tool.moreInfoUrl) %>" class="btn btn-link" target="_blank">Plus d'infos</a>
                            <% } %>
                        </div>
                    </div>
                </div>

                <!-- Permalink = https://maddie480.ovh/quest/tools/<%= normalize(tool.name) %> -->
            </div>
        </div>
    <% } %>
<% } %>

<div class="modal fade" id="zoom-image-popup" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg" role="document">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="tool-image-modal-title">Modal title</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <img id="tool-image-modal">
            </div>
        </div>
    </div>
</div>

<script src="js/tools.js"></script>
