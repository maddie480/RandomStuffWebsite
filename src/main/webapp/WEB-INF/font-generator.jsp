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

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-iYQeCzEYFbKjA/T2uDLTpkwGzCiq6soy8tYaI1GyVh/UjpbCx/TYkiZhlZB6+fzT" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.1/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-u1OknCvxWvY5kfmNBILK2hRnQC3Pr17a+RTT6rIHI7NnikvbZlHgTPOOmMi466C8" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/font-generator.css">
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
                        <li><a class="dropdown-item active" href="/celeste/font-generator">Font Generator</a></li>
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

        <p>
            If characters are missing from the font, you will find a <code>missing-characters.txt</code> file in the generated zip.
        </p>

        <form method="POST" enctype="multipart/form-data">
            <div class="form-group">
                <label for="method">Generating method</label>
                <select class="form-select" id="method" name="method">
                    <option value="bmfont">BMFont (more accurate for vanilla fonts)</option>
                    <option value="libgdx">libgdx (faster, supports custom fonts)</option>
                </select>
            </div>

            <div class="alert alert-info" id="bmfont-info" style="display: none">
                For faster generation with BMFont, you can also use the <code>--generate-font [language]</code> command of the
                <a href="/discord-bots#mod-structure-verifier">Mod Structure Verifier</a> Discord bot.
                BMFont is the same tool as the one that was used to generate the fonts for vanilla Celeste.
            </div>

            <div class="form-group" id="font-file-name-field">
                <label for="fontFileName">Font file name (should be unique, for example: max480_extendedvariants_korean)</label>
                <input type="text" class="form-control" id="fontFileName" name="fontFileName" required pattern="[^/\\*?:&quot;<>|]+">
            </div>

            <div class="form-group">
                <label for="font">Font</label>
                <select class="form-select" id="font" name="font">
                    <option value="japanese">Japanese (Noto Sans CJK JP Medium)</option>
                    <option value="korean">Korean (Noto Sans CJK KR Medium)</option>
                    <option value="chinese">Simplified Chinese (Noto Sans CJK SC Medium)</option>
                    <option value="russian">Russian (Noto Sans Medium)</option>
                    <option value="renogare">Other (Renogare)</option>
                    <option value="custom" id="customfont-option">Custom Font</option>
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
