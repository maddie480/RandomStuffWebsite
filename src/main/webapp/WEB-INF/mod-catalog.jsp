<%@ page import="java.util.List, java.util.Map, ovh.maddie480.randomstuff.frontend.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<% if((boolean) request.getAttribute("error")) { %>
    <div class="alert alert-danger">
        An error occurred while loading the entity and trigger list. Please try again later.<br>
        If this keeps happening, get in touch with maddie480 on <a href="https://discord.gg/6qjaePQ" target="_blank">Discord</a>.
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

    <p>
        <a href="/celeste/custom-entity-dictionary.csv" target="_blank">A CSV entity dictionary</a> is provided as well,
        which can be used to turn technical names into the display names used in this list (for example,
        <code>FrostHelper/IceSpinner</code> &#x27A1; <code>Custom Spinner / Custom Spinner (Rainbow Spinner Texture)</code>).
    </p>

    <div class="alert alert-info">
        <p>
            This page is mostly generated automatically from entity IDs found in map editor plugins. It was last updated on
            <b><span class="timestamp-long" data-timestamp="<%= request.getAttribute("lastUpdatedTimestamp") %>"><%= request.getAttribute("lastUpdated") %></span></b>.
        </p>
        <p>
            If you are a mod maker and want to <b>rename</b> an entity appearing here, make a pull request on
            <a href="https://github.com/maddie480/RandomBackendStuff/blob/main/modcatalogdictionary.txt" target="_blank">that file</a>
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
                        <% if (effect.getValue().contains("mlp")) { %>
                            <span class="badge bg-success">
                                <a href="https://gamebanana.com/tools/12000" target="_blank" class="mlp">
                                    <abbr title="More L&#x00f6;nn Plugins (click to open GameBanana page)">MLP</abbr>
                                </a>
                            </span>
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
                        <% if (entity.getValue().contains("mlp")) { %>
                            <span class="badge bg-success">
                                <a href="https://gamebanana.com/tools/12000" target="_blank" class="mlp">
                                    <abbr title="More L&#x00f6;nn Plugins (click to open GameBanana page)">MLP</abbr>
                                </a>
                            </span>
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
                        <% if (trigger.getValue().contains("mlp")) { %>
                            <span class="badge bg-success">
                                <a href="https://gamebanana.com/tools/12000" target="_blank" class="mlp">
                                    <abbr title="More L&#x00f6;nn Plugins (click to open GameBanana page)">MLP</abbr>
                                </a>
                            </span>
                        <% } %>
                    </li>
                <% } %>
            </ul>
        <% } %>
    <% } %>
<% } %>

<script src="/js/timestamp-converter.js"></script>
