<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>#celeste_news_network Subscription Service</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="If you want the #celeste_news_network channel from Mt. Celeste Climbing Association on your server, register your Discord webhook URL on this page!">
    <meta property="og:title" content="#celeste_news_network Subscription Service">
    <meta property="og:description" content="If you want the #celeste_news_network channel from Mt. Celeste Climbing Association on your server, register your Discord webhook URL on this page!">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-Zenh87qX5JnK2Jl0vWa8Ck2rdkQ2Bzep5IDxbcnCeuOxjzrPF/et3URy9Bv1WTRi" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-OERcA2EqjJCMA+/3y+gxIOqMEjwtxJY7qPCqsdltbNJuaOe923+mo//f6V8Qbsw3" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/celeste-news-network-subscription.css">
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
                        <li><a class="dropdown-item active" href="/celeste/news-network-subscription">#celeste_news_network Subscription</a></li>
                        <li><a class="dropdown-item" href="/celeste/custom-entity-catalog">Custom Entity Catalog</a></li>
                        <li><a class="dropdown-item" href="/celeste/everest-yaml-validator">everest.yaml validator</a></li>
                        <li><a class="dropdown-item" href="/celeste/file-searcher">File Searcher</a></li>
                        <li><a class="dropdown-item" href="/celeste/font-generator">Font Generator</a></li>
                        <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                        <li><a class="dropdown-item" href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
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
        <h1>#celeste_news_network subscription service</h1>

        <% if((boolean) request.getAttribute("bad_request")) { %>
            <div class="alert alert-danger">
                Your request was invalid, please try again.
            </div>
        <% } else if((boolean) request.getAttribute("bad_webhook")) { %>
            <div class="alert alert-danger">
                Your Discord webhook link is invalid.
            </div>
        <% } else if((boolean) request.getAttribute("not_registered")) { %>
            <div class="alert alert-warning">
                Your webhook is not registered! You cannot unregister it.
            </div>
        <% } else if((boolean) request.getAttribute("already_registered")) { %>
            <div class="alert alert-info">
                Your webhook is already registered!
            </div>
        <% } else if((boolean) request.getAttribute("subscribe_success")) { %>
            <div class="alert alert-success">
                Your webhook was registered successfully! You should have received the test message on it.
            </div>
        <% } else if((boolean) request.getAttribute("unsubscribe_success")) { %>
            <div class="alert alert-success">
                Your webhook was unregistered successfully.
            </div>
        <% } %>

        <p>
            This page allows you to register a Discord webhook in order to have a copy of the <code>#celeste_news_network</code> channel from
            the <a href="https://discord.gg/celeste" target="_blank">Mt. Celeste Climbing Association</a> on your server.
            This webhook will receive all new tweets from <a href="https://twitter.com/celeste_game" target="_blank">@celeste_game</a>
            and <a href="https://twitter.com/EverestAPI" target="_blank">@EverestAPI</a>.
        </p>

        <p>
            <b><%= request.getAttribute("sub_count") %> <%= ((int) request.getAttribute("sub_count")) == 1 ? "webhook" : "webhooks" %></b> are currently registered.
        </p>

        <div class="alert alert-info">
            When registering your webhook, a message saying <i>"This webhook was registered on the #celeste_news_network subscription service!"</i> will be sent to it.
        </div>

        <p>
            You can unsubscribe by deleting the webhook, or by returning to this page and clicking "Unsubscribe".
        </p>

        <p>
            To sign up, just create a webhook on your server pointing to the channel of your choice, and paste it here:
        </p>

        <form method="POST">
            <div class="form-group">
                <label for="url"><b>Discord Webhook URL</b></label>
                <input type="text" class="form-control" id="url" name="url" required pattern="^https:\/\/discord\.com\/api\/webhooks\/[0-9]+/[A-Za-z0-9-_]+$">
            </div>

            <input type="submit" class="btn btn-success" name="action" value="Subscribe">
            <input type="submit" class="btn btn-danger" name="action" value="Unsubscribe">
        </form>
    </div>
</body>
</html>
