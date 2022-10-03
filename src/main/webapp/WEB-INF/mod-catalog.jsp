<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Celeste Custom Entity and Trigger List</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="A big list containing all custom entities and triggers from mods published on GameBanana.">
    <meta property="og:title" content="Celeste Custom Entity and Trigger List">
    <meta property="og:description" content="A big list containing all custom entities and triggers from mods published on GameBanana.">

    <link rel="shortcut icon" href="/celeste/favicon.ico">

    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css" rel="stylesheet"
        integrity="sha384-Zenh87qX5JnK2Jl0vWa8Ck2rdkQ2Bzep5IDxbcnCeuOxjzrPF/et3URy9Bv1WTRi" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-OERcA2EqjJCMA+/3y+gxIOqMEjwtxJY7qPCqsdltbNJuaOe923+mo//f6V8Qbsw3" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/mod-catalog.css">
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
                        <li><a class="dropdown-item active" href="/celeste/custom-entity-catalog">Custom Entity Catalog</a></li>
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
        <% if((boolean) request.getAttribute("error")) { %>
            <div class="alert alert-danger">
                An error occurred while loading the entity and trigger list. Please try again later.<br>
                If this keeps happening, get in touch with max480#4596 on <a href="https://discord.gg/6qjaePQ" target="_blank">Discord</a>.
            </div>
        <% } else { %>
            <h1>Celeste Custom Entity and Trigger List</h1>

            <p>
                Here is the list of entities and triggers that are available from helpers on GameBanana.
                You can get those by installing the helper they belong to, then restarting your map making program.
                If something looks interesting to you, check the GameBanana page to learn more and download it!
            </p>

            <p>
                <b>If you want to use one of those helpers in your map</b>, you should add it as a <i>dependency</i> using <code>everest.yaml</code>,
                to make sure people getting your map get the helper as well:
                <a href="https://github.com/EverestAPI/Resources/wiki/Mod-Structure#using-helper-mods" target="_blank">check the wiki for more details</a>.
                In the list, you will find sample <code>everest.yaml</code> files that show how to add the latest version of each mod as a dependency.
            </p>

            <p>
                Note that it is <b>not recommended</b> to use <span class="badge bg-danger">Maps</span> as dependencies, because anyone wanting to play your map
                would have to get that other map and all its dependencies as well. Maps that ship with custom entities are still included here for completeness.
            </p>

            <p>
                This list is also available <a href="/celeste/custom-entity-catalog.json" target="_blank">in JSON format</a>.
            </p>

            <div class="alert alert-info">
                <p>
                    This page is mostly generated automatically from entity IDs found in map editor plugins. It was last updated on
                    <b><span class="timestamp-long" data-timestamp="<%= request.getAttribute("lastUpdatedTimestamp") %>"><%= request.getAttribute("lastUpdated") %></span></b>.
                </p>
                <p>
                    If you are a mod maker and want to <b>rename</b> an entity appearing here, make a pull request on
                    <a href="https://github.com/max4805/RandomBackendStuff/blob/main/modcatalogdictionary.txt" target="_blank">that file</a>
                    in order to add an exception for it.
                    If you want to <b>add a button that links to your documentation</b>,
                    <a href="https://github.com/EverestAPI/Resources/wiki/Helper-Manuals" rel="noopener" target="_blank">update this GitHub wiki page</a>!
                </p>
            </div>

            <p style="margin-bottom: 0.5rem">
                <b><%= request.getAttribute("entityCount") %></b> entities, <b><%= request.getAttribute("triggerCount") %></b> triggers and
                <b><%= request.getAttribute("effectCount") %></b> effects are available through <b><%= request.getAttribute("modCount") %></b> mods:
            </p>

            <ul>
                <% for(CelesteModCatalogService.QueriedModInfo mod : (List<CelesteModCatalogService.QueriedModInfo>) request.getAttribute("mods")) { %>
                    <li>
                        <a href="#<%= CelesteModCatalogService.dasherize(mod.modName) %>"><%= escapeHtml4(mod.modName) %></a>
                        <% if(mod.categoryId == 5081) { %>
                            <span class="badge bg-success"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } else if(mod.categoryId == 6800) { %>
                            <span class="badge bg-danger"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } else { %>
                            <span class="badge bg-warning"><%= escapeHtml4(mod.categoryName) %></span>
                        <% } %>
                    </li>
                <% } %>
            </ul>

            <br>

            <% for(CelesteModCatalogService.QueriedModInfo mod : (List<CelesteModCatalogService.QueriedModInfo>) request.getAttribute("mods")) { %>
                <hr>

                <h3 id="<%= CelesteModCatalogService.dasherize(mod.modName) %>">
                    <%= escapeHtml4(mod.modName) %>
                    <% if(mod.categoryId == 5081) { %>
                        <span class="badge bg-success"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } else if(mod.categoryId == 6800) { %>
                        <span class="badge bg-danger"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } else { %>
                        <span class="badge bg-warning"><%= escapeHtml4(mod.categoryName) %></span>
                    <% } %>
                </h3>
                <p>
                    <a href="https://gamebanana.com/<%= mod.itemtype.toLowerCase() %>s/<%= mod.itemid %>" rel="noopener" target="_blank"
                        class="btn btn-outline-primary">GameBanana page</a>
                    <% for(Map.Entry<String, String> docLink : mod.documentationLinks.entrySet()) { %>
                        <a href="<%= escapeHtml4(docLink.getValue()) %>" rel="noopener" target="_blank"
                            class="btn btn-outline-secondary"><%= escapeHtml4(docLink.getKey()) %></a>
                    <% } %>
                </p>

                <% if(mod.dependentCount == 1) { %>
                    <p class="dependent-count">Used as a dependency by 1 mod.</p>
                <% } else if(mod.dependentCount > 1) { %>
                    <p class="dependent-count">Used as a dependency by <%= mod.dependentCount %> mods.</p>
                <% } %>

                <% if(mod.latestVersion != null) { %>
                    <h4>Sample everest.yaml</h4>
                    <pre><%= CelesteModCatalogService.getSampleEverestYaml(mod) %></pre>
                <% } %>

                <% if(!mod.effectList.isEmpty()) { %>
                    <h4>Effects</h4>
                    <ul>
                        <% for(Map.Entry<String, List<String>> effect : mod.effectList.entrySet()) { %>
                            <li>
                                <%= escapeHtml4(effect.getKey()) %>
                                <% if (effect.getValue().contains("ahorn")) { %>
                                    <span class="badge bg-secondary">Ahorn</span>
                                <% } %>
                                <% if (effect.getValue().contains("loenn")) { %>
                                    <span class="badge bg-primary">L&#x00f6;nn</span>
                                <% } %>
                            </li>
                        <% } %>
                    </ul>
                <% } %>
                <% if(!mod.entityList.isEmpty()) { %>
                    <h4>Entities</h4>
                    <ul>
                        <% for(Map.Entry<String, List<String>> entity : mod.entityList.entrySet()) { %>
                            <li><%= escapeHtml4(entity.getKey()) %>
                                <% if (entity.getValue().contains("ahorn")) { %>
                                    <span class="badge bg-secondary">Ahorn</span>
                                <% } %>
                                <% if (entity.getValue().contains("loenn")) { %>
                                    <span class="badge bg-primary">L&#x00f6;nn</span>
                                <% } %>
                            </li>
                        <% } %>
                    </ul>
                <% } %>
                <% if(!mod.triggerList.isEmpty()) { %>
                    <h4>Triggers</h4>
                    <ul>
                        <% for(Map.Entry<String, List<String>> trigger : mod.triggerList.entrySet()) { %>
                            <li><%= escapeHtml4(trigger.getKey()) %>
                                <% if (trigger.getValue().contains("ahorn")) { %>
                                    <span class="badge bg-secondary">Ahorn</span>
                                <% } %>
                                <% if (trigger.getValue().contains("loenn")) { %>
                                    <span class="badge bg-primary">L&#x00f6;nn</span>
                                <% } %>
                            </li>
                        <% } %>
                    </ul>
                <% } %>
            <% } %>
        <% } %>
    </div>

    <script src="/js/timestamp-converter.js"></script>
</body>
</html>
