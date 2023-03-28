<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page import="static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title><%= escapeHtml4((String) request.getAttribute("pageTitle")) %></title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="<%= escapeHtml4((String) request.getAttribute("pageDescription")) %>">
    <meta property="og:site_name" content="max480's Random Stuff">
    <meta property="og:title" content="<%= escapeHtml4((String) request.getAttribute("pageTitle")) %>">
    <meta property="og:description" content="<%= escapeHtml4((String) request.getAttribute("pageDescription")) %>">

    <% if ((boolean) request.getAttribute("isCeleste")) { %>
        <link rel="shortcut icon" href="/celeste/favicon.ico">
    <% } %>

    <% if ((int) request.getAttribute("refreshAfter") != 0) { %>
        <meta http-equiv="refresh" content="<%= request.getAttribute("refreshAfter") %>" >
    <% } %>

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css"
        rel="stylesheet" integrity="sha384-rbsA2VBKQhggwzxH7pPCaAqO46MgnOM80zW1RWuH61DGLwZJEdK2Kadq2F9CUG65" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-kenU1KFdBIe4zVF0s0G1M5b4hcpxyD9F7jL+jjXkk+Q2h455rYXK/7HAuoJl+0I4" crossorigin="anonymous"></script>

    <% if ((boolean) request.getAttribute("includeDownloadJS")) { %>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/downloadjs/1.4.8/download.min.js"
            integrity="sha512-WiGQZv8WpmQVRUFXZywo7pHIO0G/o3RyiAJZj8YXNN4AV7ReR1RYWVmZJ6y3H06blPcjJmG/sBpOVZjTSFFlzQ=="
            crossorigin="anonymous" referrerpolicy="no-referrer"></script>
    <% } %>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/<%= escapeHtml4((String) request.getAttribute("pageId")) %>.css">
</head>

<body>
    <nav class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm">
        <div class="container-fluid">
            <h5 class="navbar-brand m-0">max480's Random Stuff</h5>

            <ul class="navbar-nav">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle <%= ((boolean) request.getAttribute("isCeleste")) ? "active" : "" %>"
                        href="#" id="celesteDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Celeste</a>

                    <ul class="dropdown-menu dropdown-menu-sm-end" aria-labelledby="celesteDropdown">
                        <li><a class="dropdown-item" href="/celeste/banana-mirror-browser">Banana Mirror Browser</a></li>
                        <li><a class="dropdown-item <%= "celeste-news-network-subscription".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/news-network-subscription">#celeste_news_network Subscription</a></li>
                        <li><a class="dropdown-item <%= "mod-catalog".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/custom-entity-catalog">Custom Entity Catalog</a></li>
                        <li><a class="dropdown-item <%= "direct-url-service".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/direct-link-service">Direct Link service</a></li>
                        <li><a class="dropdown-item <%= "everest-yaml-validator".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/everest-yaml-validator">everest.yaml validator</a></li>
                        <li><a class="dropdown-item" href="/celeste/file-searcher">File Searcher</a></li>
                        <li><a class="dropdown-item <%= "font-generator".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/font-generator">Font Generator</a></li>
                        <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                        <li><a class="dropdown-item <%= "mod-structure-verifier".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
                        <li><a class="dropdown-item <%= "update-checker-status".equals(request.getAttribute("navId")) ? "active" : "" %>"
                            href="/celeste/update-checker-status">Update Checker status</a></li>
                        <li><a class="dropdown-item" href="/celeste/wipe-converter">Wipe Converter</a></li>
                    </ul>
                </li>
                <li class="nav-item">
                    <a class="nav-link <%= "discord-bots".equals(request.getAttribute("navId")) ? "active" : "" %>" href="/discord-bots">Discord Bots</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" id="linksDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Links</a>
                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="linksDropdown">
                        <li><a class="dropdown-item" href="https://gamebanana.com/members/1698143" target="_blank">&#x1F34C;&nbsp;&nbsp;GameBanana &#x2013; Celeste Mods</a></li>
                        <li><a class="dropdown-item" href="https://github.com/max4805" target="_blank">&#x1F4BB;&nbsp;&nbsp;GitHub &#x2013; Source Code</a></li>
                        <li><a class="dropdown-item" href="https://ko-fi.com/max480" target="_blank">&#x2615;&#xFE0F;&nbsp;&nbsp;Ko-fi &#x2013; Donate</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </nav>

    <div class="container">
        <c:import url="/WEB-INF/${pageId}.jsp" />
    </div>
</body>
</html>
