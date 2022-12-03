<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <% if ("font-generate".equals(request.getAttribute("type"))) { %>
        <title>Celeste Font Generator</title>
    <% } else { %>
        <title>Celeste Mod Structure Verifier</title>
    <% } %>

    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-Zenh87qX5JnK2Jl0vWa8Ck2rdkQ2Bzep5IDxbcnCeuOxjzrPF/et3URy9Bv1WTRi" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-OERcA2EqjJCMA+/3y+gxIOqMEjwtxJY7qPCqsdltbNJuaOe923+mo//f6V8Qbsw3" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/task-tracker.css">

    <% if((boolean) request.getAttribute("taskOngoing")) { %>
        <meta http-equiv="refresh" content="<%= request.getAttribute("refreshIn") %>" >
    <% } %>
</head>

<body>
    <nav class="navbar navbar-expand navbar-light bg-light border-bottom shadow-sm">
        <div class="container-fluid">
            <h5 class="navbar-brand m-0">max480's Random Stuff</h5>

            <ul class="navbar-nav">
                <li class="nav-item dropdown">
                    <a class="nav-link active dropdown-toggle" href="#" id="celesteDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Celeste</a>
                    <ul class="dropdown-menu dropdown-menu-sm-end" aria-labelledby="celesteDropdown">
                        <li><a class="dropdown-item" href="/celeste/banana-mirror-browser">Banana Mirror Browser</a></li>
                        <li><a class="dropdown-item" href="/celeste/news-network-subscription">#celeste_news_network Subscription</a></li>
                        <li><a class="dropdown-item" href="/celeste/custom-entity-catalog">Custom Entity Catalog</a></li>
                        <li><a class="dropdown-item" href="/celeste/everest-yaml-validator">everest.yaml validator</a></li>
                        <li><a class="dropdown-item" href="/celeste/file-searcher">File Searcher</a></li>
                        <% if ("font-generate".equals(request.getAttribute("type"))) { %>
                            <li><a class="dropdown-item active" href="/celeste/font-generator">Font Generator</a></li>
                            <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                            <li><a class="dropdown-item" href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
                        <% } else { %>
                            <li><a class="dropdown-item" href="/celeste/font-generator">Font Generator</a></li>
                            <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                            <li><a class="dropdown-item active" href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
                        <% } %>
                        <li><a class="dropdown-item" href="/celeste/update-checker-status">Update Checker status</a></li>
                        <li><a class="dropdown-item" href="/celeste/wipe-converter">Wipe Converter</a></li>
                    </ul>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="/discord-bots">Discord Bots</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" href="#" id="linksDropdown" role="button" data-bs-toggle="dropdown" aria-expanded="false">Links</a>
                    <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="linksDropdown">
                        <li><a class="dropdown-item" href="https://gamebanana.com/members/1698143" target="_blank">GameBanana</a></li>
                        <li><a class="dropdown-item" href="https://github.com/max4805" target="_blank">GitHub</a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </nav>

    <div class="container">
        <% if ("font-generate".equals(request.getAttribute("type"))) { %>
            <h1>Celeste Font Generator</h1>
        <% } else { %>
            <h1>Celeste Mod Structure Verifier</h1>
        <% } %>

        <% if((boolean) request.getAttribute("taskNotFound")) { %>
            <div class="alert alert-danger">
                <b>This task does not exist!</b> Please try running it again.
            </div>
        <% } else if((boolean) request.getAttribute("fileNotFound")) { %>
            <div class="alert alert-danger">
                <b>This file does not exist!</b> Please go back to the previous page or try running the task again.
            </div>
        <% } else if((boolean) request.getAttribute("taskOngoing")) { %>
            <div class="alert alert-info">
                <b>Please wait...</b> This page will refresh automatically.
                The task was started <%= request.getAttribute("taskCreatedAgo") %>.
            </div>
        <% } else { %>
            <div class="alert alert-<%= request.getAttribute("taskResultType") %>">
                <%= request.getAttribute("taskResult") %>

                <% if (!((List<String>) request.getAttribute("attachments")).isEmpty()) { %>
                    <div class="attachment-list">
                        <% for(int i = 0; i < ((List<String>) request.getAttribute("attachments")).size(); i++) { %>
                            <a href="/celeste/task-tracker/<%= request.getAttribute("type") %>/<%= request.getAttribute("id") %>/download/<%= i %>"
                                class="btn btn-outline-dark" target="_blank">
                                &#x1F4E5; <%= ((List<String>) request.getAttribute("attachments")).get(i) %>
                            </a>
                        <% } %>
                    </div>
                <% } %>
            </div>
        <% } %>

        <% if(!((boolean) request.getAttribute("taskOngoing"))) { %>
            <div class="back-link">
                <% if ("font-generate".equals(request.getAttribute("type"))) { %>
                    <a class="btn btn-outline-secondary" href="/celeste/font-generator">&#x2B05; Back to Font Generator</a>
                <% } else { %>
                    <a class="btn btn-outline-secondary" href="/celeste/mod-structure-verifier">&#x2B05; Back to Mod Structure Verifier</a>
                <% } %>
            </div>
        <% } %>
    </div>
</body>
</html>
