<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>speedrun.com mod update notifications</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="This page allows speedrun.com moderators to pick the mods they want to be notified about when they are updated.">
    <meta property="og:title" content="speedrun.com mod update notifications">
    <meta property="og:description" content="This page allows speedrun.com moderators to pick the mods they want to be notified about when they are updated.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <% if((boolean) request.getAttribute("access_forbidden")) { %>
        <link rel="stylesheet" href="/css/common.css">
        <link rel="stylesheet" href="/css/page-not-found.css">
    <% } else { %>
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/css/bootstrap.min.css" rel="stylesheet"
            integrity="sha384-iYQeCzEYFbKjA/T2uDLTpkwGzCiq6soy8tYaI1GyVh/UjpbCx/TYkiZhlZB6+fzT" crossorigin="anonymous">
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/js/bootstrap.bundle.min.js"
            integrity="sha384-u1OknCvxWvY5kfmNBILK2hRnQC3Pr17a+RTT6rIHI7NnikvbZlHgTPOOmMi466C8" crossorigin="anonymous"></script>

        <link rel="stylesheet" href="/css/common.css">
        <link rel="stylesheet" href="/css/src-mod-update-notifications.css">
    <% } %>
</head>

<body>
    <% if((boolean) request.getAttribute("access_forbidden")) { %>
        <h1>&#x274c; Unauthorized</h1>
        <a href="/">&#x2b05; Back to Home Page</a>
    <% } else { %>
        <div class="container">
            <h1>speedrun.com mod update notifications</h1>

            <% if((boolean) request.getAttribute("bad_request")) { %>
                <div class="alert alert-danger">
                    Your request was invalid, please try again.
                </div>
            <% } else if((boolean) request.getAttribute("bad_mod")) { %>
                <div class="alert alert-danger">
                    The mod you specified does not exist. Make sure you are using the everest.yaml name of the mod.
                </div>
            <% } else if((boolean) request.getAttribute("not_registered")) { %>
                <div class="alert alert-warning">
                    This mod is not on the list currently! You cannot remove it from the list.
                </div>
            <% } else if((boolean) request.getAttribute("already_registered")) { %>
                <div class="alert alert-info">
                    This mod is already on the list!
                </div>
            <% } else if((boolean) request.getAttribute("register_success")) { %>
                <div class="alert alert-success">
                    The mod was successfully added to the list! You will be notified if it gets updated.
                </div>
            <% } else if((boolean) request.getAttribute("unregister_success")) { %>
                <div class="alert alert-success">
                    The mod was successfully removed from the list.
                </div>
            <% } %>

            <p>
                This page allows you to add or remove mods to be notified about when they are updated.
            </p>

            <p>
                Here are the mods that are currently on the list:
            </p>

            <ul>
                <% for (Map<String, String> mod : (List<Map<String, String>>) request.getAttribute("modList")) { %>
                    <li>
                        <code><%= escapeHtml4(mod.get("id")) %></code> &#x2013;
                        <% if (mod.containsKey("url")) { %>
                            <a href="<%= escapeHtml4(mod.get("url")) %>" target="_blank"><%= escapeHtml4(mod.get("name")) %></a>
                            (current version <b><%= escapeHtml4(mod.get("version")) %></b>)
                        <% } else { %>
                            <span class="bad-mod">&#x26a0; Unknown mod</span>
                        <% } %>
                    </li>
                <% } %>
            </ul>

            <form method="POST">
                <div class="form-group">
                    <label for="modId"><b>Mod Name (from everest.yaml)</b></label>
                    <input type="text" class="form-control" id="modId" name="modId" required>
                </div>

                <input type="submit" class="btn btn-success" name="action" value="Add to List">
                <input type="submit" class="btn btn-danger" name="action" value="Remove from List">
            </form>
        </div>
    <% } %>
</body>
</html>
