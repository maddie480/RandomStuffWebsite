<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Celeste Font Generator</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.">
    <meta property="og:title" content="Celeste Font Generator">
    <meta property="og:description" content="This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link rel="stylesheet"
        href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
        integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
        crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common-v7.css">
    <link rel="stylesheet" href="/css/font-generator-v1.css">
</head>

<body>
    <div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog">Custom&nbsp;Entity&nbsp;Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml&nbsp;validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status">Update&nbsp;Checker&nbsp;status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/banana-mirror-browser">Banana&nbsp;Mirror&nbsp;Browser</a> <span class="sep break">|</span>
            <a href="/celeste/font-generator" class="active">Font&nbsp;Generator</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/wipe-converter">Wipe&nbsp;Converter</a> <span class="sep">|</span>
            <a href="/discord-bots.html">Discord&nbsp;Bots</a>
        </div>

        <h1>Celeste Font Generator</h1>

        <% if((boolean) request.getAttribute("error")) { %>
            <div class="alert alert-danger">
                The bitmap font could not be generated. Please check that your font is valid.
            </div>
        <% } else if((boolean) request.getAttribute("badrequest")) { %>
            <div class="alert alert-warning">
                Your request was invalid, please try again.
            </div>
        <% } else if((boolean) request.getAttribute("allmissing")) { %>
            <div class="alert alert-danger">
                <b>All characters are missing from the font!</b> Make sure you picked the right language.
            </div>
        <% } else if((boolean) request.getAttribute("nothingToDo")) { %>
            <div class="alert alert-success">
                <b>All the characters in your dialog file are already present in the vanilla font!</b> You have nothing to do.
            </div>
        <% } %>

        <p>
            This page allows you to generate files to import missing characters in the Celeste font for your mod,
            or to convert a completely custom font.
            In order to do this, send your dialog file here, then unzip the contents of the zip you're given to <code>Mods/yourmod/Dialog/Fonts</code>.
        </p>

        <div class="alert alert-info">
            If characters are missing from the font, you will find a <code>missing-characters.txt</code> file in the generated zip.
        </div>

        <form method="POST" enctype="multipart/form-data">
            <div class="form-group">
                <label for="fontFileName">Font file name (should be unique, for example: max480_extendedvariants_korean)</label>
                <input type="text" class="form-control" id="fontFileName" name="fontFileName" required pattern="[^/\\*?:&quot;<>|]+">
            </div>

            <div class="form-group">
                <label for="font">Font</label>
                <select class="form-control" id="font" name="font">
                    <option value="japanese">Japanese (Noto Sans CJK JP Medium)</option>
                    <option value="korean">Korean (Noto Sans CJK KR Medium)</option>
                    <option value="chinese">Simplified Chinese (Noto Sans CJK SC Medium)</option>
                    <option value="russian">Russian (Noto Sans Medium)</option>
                    <option value="renogare">Other (Renogare)</option>
                    <option value="custom">Custom Font</option>
                </select>
            </div>

            <div class="form-group" style="display: none" id="fontFileDiv">
                <label for="fontFile">Font file</label>
                <input type="file" class="form-control" accept=".ttf,.otf" id="fontFile" name="fontFile">
            </div>

            <div class="form-group">
                <label for="dialogFile">Dialog file</label>
                <input type="file" class="form-control" accept=".txt" id="dialogFile" name="dialogFile" required>
            </div>

            <input type="submit" class="btn btn-primary" value="Generate">
        </form>
    </div>

    <script src="/js/font-generator.js"></script>
</body>
</html>
