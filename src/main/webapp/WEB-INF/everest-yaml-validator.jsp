<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, com.max480.randomstuff.gae.EverestYamlValidatorService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

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

	<link rel="stylesheet"
		href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
		integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
		crossorigin="anonymous">

    <% if (request.getAttribute("parseError") != null) { %>
        <style>
            pre {
                background: #eee;
                border: solid gray 1px;
                padding: 4px;
                margin: 5px 0;
                white-space: pre-wrap;
            }
        </style>
    <% } else if (request.getAttribute("validationErrors") != null || request.getAttribute("modInfo") != null) { %>
        <style>
            ul {
                margin-bottom: 2px;
            }
        </style>
    <% } %>
</head>

<body>
	<div class="container">
	    <a href="https://github.com/EverestAPI/Resources/wiki" class="btn btn-primary" style="margin-top: 20px; margin-bottom: 20px">
	        &lt; Back to Wiki
        </a>

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
        <% } else { %>
            <div class="alert alert-success">
                If you have difficulties writing your everest.yaml, the
                <a href="https://gamebanana.com/tools/6908" target="_blank" rel="noopener">Dependency Generator</a>
                will be able to generate it for you.
            </div>

            <div class="alert alert-info">
                If you want to validate the syntax of other YAML files (like map meta.yamls), use online tools like
                <a href="https://jsonformatter.org/yaml-validator/" target="_blank" rel="noopener">this one</a>.
            </div>
        <% } %>

        <!-- Developed by max480 - version 1.3 - last updated on Dec 12, 2020 -->
        <!-- What are you doing here? :thinkeline: -->
	</div>
</body>
</html>