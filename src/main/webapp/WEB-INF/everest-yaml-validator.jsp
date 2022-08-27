<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, com.max480.randomstuff.gae.EverestYamlValidatorService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
	<title>everest.yaml validator</title>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<meta name="author" content="max480">
	<meta name="description" content="Check if your everest.yaml is valid by sending it on this page.">
	<meta property="og:title" content="everest.yaml validator">
	<meta property="og:description" content="Check if your everest.yaml is valid by sending it on this page.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.0/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-gH2yIJqKdNHPEq0n4Mqa/HGKIhSkIHeL5AyhkYV8i59U5AR6csBvApHHNl/vI1Bx" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.0/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-A3rJD856KowSb7dwlZdYEkO39Gagi7vIsF0jrRAoQmDKKtQBHUuLZ9AsSv4jD4Xa" crossorigin="anonymous"></script>

    <% if (request.getAttribute("latestVersionsYaml") != null) { %>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/downloadjs/1.4.8/download.min.js"
            integrity="sha512-WiGQZv8WpmQVRUFXZywo7pHIO0G/o3RyiAJZj8YXNN4AV7ReR1RYWVmZJ6y3H06blPcjJmG/sBpOVZjTSFFlzQ=="
            crossorigin="anonymous" referrerpolicy="no-referrer"></script>
    <% } %>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/everest-yaml-validator.css">
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
                        <li><a class="dropdown-item active" href="/celeste/everest-yaml-validator">everest.yaml validator</a></li>
                        <li><a class="dropdown-item" href="/celeste/font-generator">Font Generator</a></li>
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
        <h1>everest.yaml validator</h1>

        Want to know if your everest.yaml is valid? Send it here, and this service will check:
        <ul>
            <li>If the file is a valid YAML file, and it has all required fields</li>
            <li>If all dependencies exist in the dependency downloader database</li>
        </ul>

        <form method="POST" enctype="multipart/form-data">
            <input type="file" accept=".yaml,.yml" id="file" name="file" required>
            <input type="submit" class="btn btn-primary" value="Validate!">
        </form>

        <br/>

        <% if (request.getAttribute("parseError") != null) { %>
            <div class="alert alert-danger">
                <b>Your everest.yaml file is not valid.</b> An error occurred while parsing it:
                <pre><%= escapeHtml4((String) request.getAttribute("parseError")) %></pre>
                <p class="error-description">
                    This probably means your file is not valid YAML or that it does not match the structure of an everest.yaml file.
                    Make sure it looks like this one, use spaces (not tabs) and check text is aligned in the same way:
                </p>

<!-- pre blocks are sensitive to indentation so it has to be over there on the left aaaa -->
<pre>
- Name: YourModName
  Version: 1.0.0
  Dependencies:
    - Name: DependencyName1
      Version: 1.0.0
    - Name: DependencyName2
      Version: 1.0.0
</pre>
            </div>
        <% } else if (request.getAttribute("validationErrors") != null) { %>
            <div class="alert alert-warning">
                <b>There are issues with your everest.yaml file:</b>
                <ul>
                    <% for (String issue : (List<String>) request.getAttribute("validationErrors")) { %>
                        <li><%= escapeHtml4(issue) %></li>
                    <% } %>
                </ul>
            </div>
        <% } else if (request.getAttribute("modInfo") != null) { %>
            <% List<EverestYamlValidatorService.EverestModuleMetadata> modsInfo = (List<EverestYamlValidatorService.EverestModuleMetadata>) request.getAttribute("modInfo"); %>
            <div class="alert alert-success">
                <b>Your everest.yaml file seems valid!</b>

                <% for (EverestYamlValidatorService.EverestModuleMetadata modInfo : modsInfo) { %>
                    <%= modsInfo.size() == 1 ? "Your mod is called" : "It contains a mod called" %>
                    <b><%= escapeHtml4(modInfo.Name) %></b> version <%= escapeHtml4(modInfo.Version) %> and depends on:
                    <ul>
                        <% for (EverestYamlValidatorService.EverestModuleMetadata dependency : modInfo.Dependencies) { %>
                            <li>
                                <b><%= escapeHtml4(dependency.Name) %></b> version <%= escapeHtml4(dependency.Version) %>
                                <i>(latest version is <%= escapeHtml4(dependency.LatestVersion) %>)</i>
                            </li>
                        <% } %>
                        <% for (EverestYamlValidatorService.EverestModuleMetadata dependency : modInfo.OptionalDependencies) { %>
                            <li>
                                <i>Optional:</i> <b><%= escapeHtml4(dependency.Name) %></b> version <%= escapeHtml4(dependency.Version) %>
                                <i>(latest version is <%= escapeHtml4(dependency.LatestVersion) %>)</i>
                            </li>
                        <% } %>
                        <% if (modInfo.Dependencies.isEmpty() && modInfo.OptionalDependencies.isEmpty()) { %>
                            <li>Nothing!</li>
                        <% } %>
                    </ul>
                <% } %>
            </div>

            <% if (request.getAttribute("latestVersionsYaml") != null) { %>
                <button id="download-latest-versions-yaml" class="btn btn-outline-dark">
                    &#x1F4E5; Download everest.yaml with updated dependencies
                </button>
            <% } %>
        <% } else { %>
            <div class="alert alert-success">
                If you have difficulties writing your everest.yaml, the
                <a href="https://gamebanana.com/tools/6908" target="_blank" rel="noopener">Dependency Generator</a>
                will be able to generate it for you.
            </div>

            <div class="alert alert-info">
                <b>This page only validates the everest.yaml files you place at the root of your mod.</b>
                If you want to validate the syntax of other YAML files (like map meta.yamls), use online tools like
                <a href="https://jsonformatter.org/yaml-validator/" target="_blank" rel="noopener">this one</a>.
            </div>
        <% } %>

        <% if (request.getAttribute("latestVersionsYaml") != null) { %>
            <script nonce="<%= request.getAttribute("nonce") %>">
                document.getElementById("download-latest-versions-yaml").addEventListener("click", function() {
                    const yamlContents = "<%= escapeEcmaScript((String) request.getAttribute("latestVersionsYaml")) %>";
                    download(yamlContents, "everest.yaml", "text/yaml");
                });
            </script>
        <% } %>
	</div>
</body>
</html>
