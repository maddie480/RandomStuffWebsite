<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ page import="java.util.List, java.text.DecimalFormat, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static com.max480.randomstuff.gae.UpdateCheckerStatusService.LatestUpdatesEntry"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Everest Update Checker status page</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="Check the status of the Everest Update Checker here.">
    <meta property="og:title" content="Everest Update Checker status page">
    <meta property="og:description" content="Check the status of the Everest Update Checker here.">
    <meta http-equiv="refresh" content="60" >

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-iYQeCzEYFbKjA/T2uDLTpkwGzCiq6soy8tYaI1GyVh/UjpbCx/TYkiZhlZB6+fzT" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-u1OknCvxWvY5kfmNBILK2hRnQC3Pr17a+RTT6rIHI7NnikvbZlHgTPOOmMi466C8" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/update-checker-status.css">
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
                        <li><a class="dropdown-item" href="/celeste/font-generator">Font Generator</a></li>
                        <li><a class="dropdown-item" href="/celeste/map-tree-viewer">Map Tree Viewer</a></li>
                        <li><a class="dropdown-item" href="/celeste/mod-structure-verifier">Mod Structure Verifier</a></li>
                        <li><a class="dropdown-item active" href="/celeste/update-checker-status">Update Checker status</a></li>
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
        <h1 class="mt-4">Everest Update Checker status page</h1>

        <% if ((boolean) request.getAttribute("up")) { %>
            <div class="alert alert-success"><b>The update checker is up and running!</b></div>
        <% } else { %>
            <div class="alert alert-danger">
                <b>The update checker is currently having issues.</b>
                The mod database might not be up-to-date.
            </div>
        <% } %>

        <% if (request.getAttribute("lastUpdatedAt") != null) { %>
            <p>
                The database was last updated successfully on
                <b><span class="timestamp-long" data-timestamp="<%= request.getAttribute("lastUpdatedTimestamp") %>">
                    <%= request.getAttribute("lastUpdatedAt") %>
                </span>
                (<%= request.getAttribute("lastUpdatedAgo") %>)</b>.

                <% if (((double) request.getAttribute("duration")) >= 60) { %>
                    The update check took <%= new DecimalFormat("0.0").format((double) request.getAttribute("duration") / 60.0) %> minutes.
                <% } else { %>
                    The update check took <%= new DecimalFormat("0.0").format((double) request.getAttribute("duration")) %> seconds.
                <% } %>
            </p>
        <% } %>

        <p>
            <b><%= request.getAttribute("modCount") %></b> mods are currently registered.
        </p>

        <div class="alert alert-info">
            <p>
                You can learn more about the files generated by the Everest Update Checker by checking its README
                <a href="https://github.com/max4805/EverestUpdateCheckerServer/blob/master/README.md" target="_blank">on GitHub</a>.
            </p>
            <p>
                APIs that provide mod search and sorted / filtered lists are also available,
                <a href="https://github.com/max4805/RandomStuffWebsite/blob/main/README.md#gamebanana-search-api" target="_blank">check the docs here</a>.
            </p>
            <p>
                <b>Not inspired?</b> Try <a href="/celeste/random-map" target="_blank">a random Celeste map</a>.
            </p>
        </div>

        <% if (!((List<LatestUpdatesEntry>) request.getAttribute("latestUpdates")).isEmpty()) { %>
            <h2>Latest updates</h2>

            <table class="table table-striped">
                <% for (LatestUpdatesEntry entry : (List<LatestUpdatesEntry>) request.getAttribute("latestUpdates")) { %>
                    <tr>
                        <td>
                            <% if (entry.isAddition) { %>
                                &#x2705;
                            <% } else { %>
                                &#x274c;
                            <% } %>
                        </td>
                        <td>
                            <span class="timestamp-short" data-timestamp="<%= entry.timestamp %>">
                                <%= escapeHtml4(entry.date) %>
                            </span>
                        </td>
                        <td>
                            <% if (entry.isAddition) { %>
                                <a href="<%= escapeHtml4(entry.url) %>" target="_blank"><b><%= escapeHtml4(entry.name) %></b></a>
                                was updated to version <b><%= escapeHtml4(entry.version) %></b>
                            <% } else { %>
                                <b><%= escapeHtml4(entry.name) %></b> was deleted
                            <% } %>
                        </td>
                    </tr>
                <% } %>
            </table>
        <% } %>
    </div>

    <script src="/js/timestamp-converter.js"></script>
</body>
</html>
