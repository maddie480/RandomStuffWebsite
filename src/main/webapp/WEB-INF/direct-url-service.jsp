<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>Celeste Direct Link service</h1>

<p>
    This page can give you direct download URLs to the latest version of a mod, based on the <code>Name</code> present in its <code>everest.yaml</code> file.
</p>
<p>
    Links given here redirect to <a href="https://0x0a.de/twoclick" target="_blank">0x0ade's Everest 2-click installer</a>, allowing you to
    install the mod directly with <a href="https://gamebanana.com/tools/6449" target="blank">Olympus</a>, or download the zip file directly,
    from either <a href="https://gamebanana.com/games/6460" target="_blank">GameBanana</a>
    or <a href="https://celestemodupdater.0x0a.de" target="_blank">0x0ade's mirror</a>.
</p>

<form method="POST">
    <div class="form-group">
        <label for="modId">Type a mod's <code>everest.yaml</code> name here:</label>
        <input type="text" class="form-control" id="modId" name="modId" required value="<%= escapeHtml4((String) request.getAttribute("typedId")) %>">
    </div>

    <input type="submit" class="btn btn-primary" value="Generate URLs">

    <% if (request.getAttribute("notfound") != null) { %>
        <div class="alert alert-danger">
            <b>The <code>everest.yaml</code> name you specified (<code><%= escapeHtml4((String) request.getAttribute("typedId")) %></code>) was not found!</b>
            Please try again.
        </div>
    <% } %>

    <% if (request.getAttribute("link") != null) { %>
        <div class="alert alert-success">
            <b>Here are your links:</b>
            <ul>
                <li>
                    Download from GameBanana:
                    <a href="/celeste/dl/<%= escapeHtml4((String) request.getAttribute("link")) %>" target="_blank">
                        https://max480.ovh/celeste/dl/<%= escapeHtml4((String) request.getAttribute("link")) %>
                    </a>
                </li>
                <li>
                    Download from 0x0ade's mirror:
                    <a href="/celeste/mirrordl/<%= escapeHtml4((String) request.getAttribute("link")) %>" target="_blank">
                        https://max480.ovh/celeste/mirrordl/<%= escapeHtml4((String) request.getAttribute("link")) %>
                    </a>
                </li>
            </ul>
        </div>
    <% } %>
</form>
