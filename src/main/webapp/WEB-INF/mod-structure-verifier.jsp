<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Celeste Mod Structure Verifier</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="This tool allows you to check the structure and dependencies of your Celeste mods.">
    <meta property="og:title" content="Celeste Mod Structure Verifier">
    <meta property="og:description" content="This tool allows you to check the structure and dependencies of your Celeste mods.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link rel="stylesheet"
        href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
        integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
        crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mod-structure-verifier.css">
</head>

<body>
    <div class="container">
        <div id="nav">
            <a href="/celeste/custom-entity-catalog">Custom&nbsp;Entity&nbsp;Catalog</a> <span class="sep">|</span>
            <a href="/celeste/everest-yaml-validator">everest.yaml&nbsp;validator</a> <span class="sep">|</span>
            <a href="/celeste/update-checker-status">Update&nbsp;Checker&nbsp;status</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/celeste/banana-mirror-browser">Banana&nbsp;Mirror&nbsp;Browser</a> <span class="sep">|</span>
            <a href="/celeste/mod-structure-verifier" class="active">Mod&nbsp;Structure&nbsp;Verifier</a> <span class="sep break">|</span>
            <a href="/celeste/font-generator">Font&nbsp;Generator</a> <span class="sep">|</span>
            <a href="https://max480-random-stuff.herokuapp.com/celeste/wipe-converter">Wipe&nbsp;Converter</a> <span class="sep">|</span>
            <a href="/discord-bots">Discord&nbsp;Bots</a> <span class="sep">|</span>
            <a href="/celeste/news-network-subscription">#celeste_news_network&nbsp;Subscription</a>
        </div>

        <h1>Celeste Mod Structure Verifier</h1>

        <% if((boolean) request.getAttribute("badrequest")) { %>
            <div class="alert alert-warning">
                Your request was invalid, please try again.
            </div>
        <% } %>

        <p>
            This tool allows you to check the structure and dependencies of your Celeste mods.
        </p>

        <p>
            It checks for the following:
        </p>

        <ul>
            <li><code>everest.yaml</code> should exist and should be valid according to the everest.yaml validator</li>
            <li>all decals, stylegrounds, entities, triggers and effects should be vanilla, packaged with the mod, or from one of the everest.yaml dependencies</li>
            <li>the dialog files for vanilla languages should not contain characters that are missing from the game's font, or those extra characters should be included in the zip</li>
        </ul>

        <p>
            If you enable path checking, which is useful when you check a zip with a <b>single map</b> that is going to be included in a contest or collab, it will also check for the following:
        </p>

        <ul>
            <li>files in <code>Assets/</code>, <code>Graphics/Atlases/</code>, <code>Graphics/ColorGrading/</code> and <code>Tutorials/</code> should have this path: <code>[base path]/[asset folder name]/[subfolder]/[anything]</code></li>
            <li>XMLs in <code>Graphics/</code> should match: <code>Graphics/[asset folder name]xmls/[subfolder]/[anything].xml</code></li>
            <li>there should be exactly 1 file in the <code>Maps</code> folder, and its path should match: <code>Maps/[map folder name]/[subfolder]/[anything].bin</code></li>
            <li>if there is an `English.txt`, dialog IDs should match: <code>[asset folder name]_[anything]_[anything]</code></li>
        </ul>

        <div class="alert alert-info" id="bmfont-info">
            For faster verifying, you can use the <a href="/discord-bots#mod-structure-verifier">Mod Structure Verifier</a> Discord bot.
            If you host a contest/collab that has its own Discord server, you can invite the bot on it and preset the folder names,
            so that mappers just have to drop their zip files in a designated channel to have them verified.
        </div>

        <form method="POST" enctype="multipart/form-data">
            <div class="form-group">
                <label for="zipFile">Zip file to verify</label>
                <input type="file" class="form-control" accept=".zip" id="zipFile" name="zipFile" required>
            </div>
            <div class="form-check">
              <input class="form-check-input" type="checkbox" value="" id="checkPaths">
              <label class="form-check-label" for="checkPaths">Enable path checking</label>
            </div>

            <div id="path-checking" style="display: none">
                <div class="form-group">
                    <label for="assetFolderName">Asset folder name (alphanumeric)</label>
                    <input type="text" class="form-control" id="assetFolderName" name="assetFolderName" pattern="[A-Za-z0-9]+">
                </div>
                <div class="form-group">
                    <label for="mapFolderName">Map folder name (alphanumeric)</label>
                    <input type="text" class="form-control" id="mapFolderName" name="mapFolderName" placeholder="Same as asset folder name" pattern="[A-Za-z0-9]+">
                </div>
            </div>

            <input type="submit" class="btn btn-primary" value="Verify">
        </form>
    </div>

    <script src="/js/mod-structure-verifier.js"></script>
</body>
</html>
