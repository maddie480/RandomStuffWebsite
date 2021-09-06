<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

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

	<link rel="stylesheet"
		href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
		integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
		crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common-v6.css">
</head>

<body>
	<div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog">Custom Entity Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status" class="active">Update Checker status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/banana-mirror-browser">Banana Mirror Browser</a> <span class="sep">|</span>
            <a href="/celeste/font-generator">Font Generator</a>
        </div>

        <h1 class="mt-4">Everest Update Checker status page</h1>

        <% if ((boolean) request.getAttribute("up")) { %>
            <div class="alert alert-success"><b>The update checker is up and running!</b></div>
        <% } else { %>
            <div class="alert alert-danger">
                <b>The update checker is currently having issues.</b>
                The mod database might not be up-to-date.
            </div>
        <% } %>

        <% if (request.getAttribute("lastUpdated") != null) { %>
            <p>
                The database was last updated successfully on <b><%= request.getAttribute("lastUpdated") %></b>.
                The update check took <%= request.getAttribute("duration") %> seconds.
            </p>
        <% } %>

        <% if (request.getAttribute("lastUpdateFound") != null) { %>
            <p>
                A mod update was last found on <b><%= request.getAttribute("lastUpdateFound") %></b>.
            </p>
        <% } %>

        <b><%= request.getAttribute("modCount") %></b> mods are currently registered.

        <!-- Developed by max480 - version 1.0 - last updated on Mar 20, 2021 -->
        <!-- What are you doing here? :thinkeline: -->
	</div>
</body>
</html>