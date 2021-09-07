<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Celeste Custom Entity and Trigger List</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="A big list containing all custom entities and triggers from mods published on GameBanana.">
    <meta property="og:title" content="Celeste Custom Entity and Trigger List">
    <meta property="og:description" content="A big list containing all custom entities and triggers from mods published on GameBanana.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link rel="stylesheet"
        href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
        integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
        crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common-v6.css">

    <style>
        .btn {
            margin: 3px 1px;
        }

        .alert p {
            margin-top: 0.5rem;
            margin-bottom: 0.5rem;
        }
    </style>
</head>

<body>
    <div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog" class="active">Custom Entity Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status">Update Checker status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/banana-mirror-browser">Banana Mirror Browser</a> <span class="sep">|</span>
            <a href="/celeste/font-generator">Font Generator</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/wipe-converter">Wipe Converter</a>
        </div>

        <a href="https://github.com/EverestAPI/Resources/wiki" class="btn btn-primary" style="margin-bottom: 20px">
            &lt; Back to Wiki
        </a>

        <% if((boolean) request.getAttribute("error")) { %>
            <div class="alert alert-danger">
                An error occurred while refreshing the entity and trigger list. Please try again in an hour.<br>
                If this keeps happening, get in touch with max480#4596 on <a href="https://discord.gg/6qjaePQ" target="_blank">Discord</a>.
            </div>
        <% } else { %>
            <h1>Celeste Custom Entity and Trigger List</h1>

            <p>
                Here is the list of entities and triggers you can get from helpers on GameBanana.
                You can get those by installing the helper they belong to, then restarting Ahorn.
                If something looks interesting to you, check the GameBanana page for the mod for more details!
            </p>

            <div class="alert alert-info">
                <p>
                    This list is mostly generated automatically from Ahorn plugin file names. It was last updated on <b><%= request.getAttribute("lastUpdated") %></b>.
                </p>
                <p>
                    If you are a mod maker and want to <b>rename</b> an entity appearing here, rename its Ahorn plugin file.
                    If you want to <b>add a button that links to your documentation</b>,
                    <a href="https://github.com/EverestAPI/Resources/wiki/Helper-Manuals" rel="noopener" target="_blank">update this GitHub wiki page</a>!
                </p>
            </div>

            <p style="margin-bottom: 0.5rem">
                <b><%= request.getAttribute("entityCount") %></b> entities, <b><%= request.getAttribute("triggerCount") %></b> triggers and
                <b><%= request.getAttribute("effectCount") %></b> effects are available through <b><%= request.getAttribute("modCount") %></b> mods:
            </p>

            <ul>
                <% for(CelesteModCatalogService.QueriedModInfo mod : (List<CelesteModCatalogService.QueriedModInfo>) request.getAttribute("mods")) { %>
                    <li>
                        <a href="#<%= CelesteModCatalogService.dasherize(mod.modName) %>"><%= escapeHtml4(mod.modName) %></a>
                        <% if(mod.categoryId == 5081 || mod.categoryId == 4634 || mod.categoryId == 4635) { %>
                            <span class="badge badge-success"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } else if(mod.categoryId == 6800) { %>
                            <span class="badge badge-danger"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } else { %>
                            <span class="badge badge-warning"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } %>
                    </li>
                <% } %>
            </ul>

            <br>

            <% for(CelesteModCatalogService.QueriedModInfo mod : (List<CelesteModCatalogService.QueriedModInfo>) request.getAttribute("mods")) { %>
                <hr>

                <h3 id="<%= CelesteModCatalogService.dasherize(mod.modName) %>">
                    <%= escapeHtml4(mod.modName) %>
                    <% if(mod.categoryId == 5081 || mod.categoryId == 4634 || mod.categoryId == 4635) { %>
                        <span class="badge badge-success"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } else if(mod.categoryId == 6800) { %>
                        <span class="badge badge-danger"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } else { %>
                        <span class="badge badge-warning"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } %>
                </h3>
                <p>
                    <a href="https://gamebanana.com/<%= mod.itemtype.toLowerCase() %>s/<%= mod.itemid %>" rel="noopener" target="_blank"
                        class="btn btn-outline-primary">GameBanana page</a>
                    <% for(Map.Entry<String, String> docLink : mod.documentationLinks.entrySet()) { %>
                        <a href="<%= escapeHtml4(docLink.getValue()) %>" rel="noopener" target="_blank"
                            class="btn btn-outline-secondary"><%= escapeHtml4(docLink.getKey()) %></a>
                    <% } %>
                </p>
                <% if(!mod.effectList.isEmpty()) { %>
                    <h4>Effects</h4>
                    <ul>
                        <% for(String effect : mod.effectList) { %>
                            <li><%= escapeHtml4(effect) %></li>
                        <% } %>
                    </ul>
                <% } %>
                <% if(!mod.entityList.isEmpty()) { %>
                    <h4>Entities</h4>
                    <ul>
                        <% for(String entity : mod.entityList) { %>
                            <li><%= escapeHtml4(entity) %></li>
                        <% } %>
                    </ul>
                <% } %>
                <% if(!mod.triggerList.isEmpty()) { %>
                    <h4>Triggers</h4>
                    <ul>
                        <% for(String trigger : mod.triggerList) { %>
                            <li><%= escapeHtml4(trigger) %></li>
                        <% } %>
                    </ul>
                <% } %>
            <% } %>
        <% } %>

        <!-- Developed by max480 - version 1.4 - last updated on Apr 3, 2021 -->
        <!-- What are you doing here? :thinkeline: -->
    </div>
</body>
</html>