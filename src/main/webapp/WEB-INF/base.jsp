<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="static org.apache.commons.text.StringEscapeUtils.escapeHtml4, java.time.ZonedDateTime, java.util.Arrays"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title><%= escapeHtml4((String) request.getAttribute("pageTitle")) %></title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="maddie480">
    <meta name="description" content="<%= escapeHtml4((String) request.getAttribute("pageDescription")) %>">
    <meta property="og:site_name" content="Maddie's Random Stuff">
    <meta property="og:title" content="<%= escapeHtml4((String) request.getAttribute("pageTitle")) %>">
    <meta property="og:description" content="<%= escapeHtml4((String) request.getAttribute("pageDescription")) %>">

    <link rel="icon" href="/img/favicon.png">
    <meta name="theme-color" media="(prefers-color-scheme: light)" content="#FFB8DD" />
    <meta name="theme-color" media="(prefers-color-scheme: dark)" content="#77286E" />

    <% if ((int) request.getAttribute("refreshAfter") != 0) { %>
        <meta http-equiv="refresh" content="<%= request.getAttribute("refreshAfter") %>" >
    <% } %>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.6/dist/css/bootstrap.min.css"
        rel="stylesheet" integrity="sha384-4Q6Gf2aSP4eDXB8Miphtr37CMZZQ5oXLH2yaXMJ2w8e2ZtHTl7GptT4jmndRuHDT" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.6/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-j1CDi7MgGQ12Z7Qab0qlWQ/Qqz24Gc6BM0thvEMVjHnfYGF0rmFCozFSxQBxwHKO" crossorigin="anonymous"></script>

    <% if ((boolean) request.getAttribute("includeDownloadJS")) { %>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/downloadjs/1.4.8/download.min.js"
            integrity="sha512-WiGQZv8WpmQVRUFXZywo7pHIO0G/o3RyiAJZj8YXNN4AV7ReR1RYWVmZJ6y3H06blPcjJmG/sBpOVZjTSFFlzQ=="
            crossorigin="anonymous" referrerpolicy="no-referrer"></script>
    <% } %>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/<%= escapeHtml4((String) request.getAttribute("pageId")) %>.css">

    <script src="/js/dark-mode.js"></script>
</head>

<body class="month-<%= ZonedDateTime.now().getMonthValue() %>">
    <nav class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm">
        <div class="container-fluid">
            <h5 class="navbar-brand m-0"><a href="/"><img src="/img/maddie-avatar.png"> Maddie's Random Stuff</a></h5>

            <ul class="navbar-nav">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle <%= ((boolean) request.getAttribute("isCeleste")) ? "active" : "" %>"
                        href="#" id="celesteDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Celeste</a>

                    <ul class="dropdown-menu dropdown-menu-sm-end" aria-labelledby="celesteDropdown">
                        <li><a class="dropdown-item" href="/celeste/asset-drive">Asset Drive Browser</a></li>
                        <li><a class="dropdown-item" href="/celeste/banana-mirror-browser">Banana Mirror Browser</a></li>
                        <li><a class="dropdown-item <%= "celeste-news-network-subscription".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/news-network-subscription">#celeste_news_network Subscription</a></li>
                        <li><a class="dropdown-item <%= "collab-list".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/collab-contest-list">Collab & Contest List</a></li>
                        <li><a class="dropdown-item <%= "mod-catalog".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/custom-entity-catalog">Custom Entity Catalog</a></li>
                        <li><a class="dropdown-item <%= "direct-url-service".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/direct-link-service">Direct Link service</a></li>
                        <li><a class="dropdown-item <%= "everest-yaml-validator".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/everest-yaml-validator">everest.yaml validator</a></li>
                        <li><a class="dropdown-item" href="/celeste/file-searcher">File Searcher</a></li>
                        <li><a class="dropdown-item <%= "font-generator".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/font-generator">Font Generator</a></li>
                        <li><a class="dropdown-item" href="/celeste/graphics-dump-browser">Graphics Dump Browser</a></li>
                        <li><a class="dropdown-item" href="/lua-cutscenes-documentation/index.html" target="_blank">Lua Cutscenes Documentation</a></li>
                        <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                        <li><a class="dropdown-item <%= "mod-structure-verifier".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
                        <li><a class="dropdown-item <%= "olympus-news".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/olympus-news">Olympus News</a></li>
                        <li><a class="dropdown-item <%= "update-checker-status".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/update-checker-status">Update Checker status</a></li>
                        <li><a class="dropdown-item" href="/celeste/wipe-converter">Wipe Converter</a></li>
                    </ul>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= "discord-bots".equals(request.getAttribute("navId")) ? "active" : "" %>" href="/discord-bots">Discord Bots</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle <%= Arrays.asList("radio-lnj", "radio-lnj-playlist", "quest-mods", "quest-tools", "quest-backgrounds").contains(request.getAttribute("navId")) ? "active" : "" %>"
                        href="#" id="linksDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Links</a>

                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="linksDropdown">
                        <li><h6 class="dropdown-header">External Profiles</h6></li>
                        <li><a class="dropdown-item" href="https://gamebanana.com/members/1698143" target="_blank">&#x1F34C;&nbsp;&nbsp;GameBanana &#x2013; Celeste Mods</a></li>
                        <li><a class="dropdown-item" href="https://github.com/maddie480" target="_blank">&#x1F4BB;&nbsp;&nbsp;GitHub &#x2013; Source Code</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><h6 class="dropdown-header">Les Navets Jouables (in French)</h6></li>
                        <li><a class="dropdown-item" href="https://github.com/maddie480/BazarLNJ" target="_blank">&#x1F50E;&nbsp;&nbsp;Bazar LNJ &#x2013; Game Investigation</a></li>
                        <li><a class="dropdown-item <%= Arrays.asList("radio-lnj", "radio-lnj-playlist").contains(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/radio-lnj">&#x1F4FB;&nbsp;&nbsp;Radio LNJ &#x2013; Game Music</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><h6 class="dropdown-header">QUEST by Laupok (in French)</h6></li>
                        <li><a class="dropdown-item <%= "quest-mods".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/quest/mods">&#x2694;&#xFE0F;&nbsp;&nbsp;Quest &#x2013; Game Mods</a></li>
                        <li><a class="dropdown-item <%= "quest-tools".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/quest/tools">&#x1F527;&#xFE0F;&nbsp;&nbsp;Quest &#x2013; Modding Tools</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><h6 class="dropdown-header">Mirrored Stuff</h6></li>
                        <li><a class="dropdown-item" href="/static/celeste-fmod-project.zip" target="_blank">&#x1F3A7;&nbsp;&nbsp;Celeste FMOD Project</a></li>
                        <li><a class="dropdown-item" href="/static/unicode-mirror/emoji/charts/index.html" target="_blank">&#x1F604;&nbsp;&nbsp;Unicode Emoji Charts</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </nav>

    <div class="container">
        <c:import url="/WEB-INF/${pageId}.jsp" />
    </div>

    <% if ((boolean) request.getAttribute("crabified")) { %>
        <script src="/js/crabifier.js"></script>
    <% } %>
</body>
</html>
