<%@ page import="java.util.List, java.util.Map, ovh.maddie480.randomstuff.frontend.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>Celeste Direct Link service</h1>

<p>
    This page can give you direct download URLs to the latest version of a mod, based on the <code>Name</code> present in its <code>everest.yaml</code> file.
</p>
<p>
    Links given here can redirect either to the file download directly, or to
    <a href="https://0x0a.de/twoclick" target="_blank">0x0ade's Everest 2-click installer</a>, that allows you to
    install the mod directly with <a href="https://gamebanana.com/tools/6449" target="blank">Olympus</a>.
</p>
<p>
    You can also pick whether to download the file from <a href="https://gamebanana.com/games/6460" target="_blank">GameBanana</a>
    or <a href="https://celestemodupdater.0x0a.de" target="_blank">0x0ade's mirror</a>.
</p>

<div class="alert alert-info">
    Looking for a link to download the latest Olympus or Everest version? Check the following links:
    <ul>
        <li>
            <a href="/celeste/download-olympus?branch=stable&platform=windows">https://maddie480.ovh/celeste/download-olympus?branch=stable&platform=windows</a>
            downloads the latest stable version of Olympus for Windows. Other options for branches are <code>main</code>, <code>windows-init</code> and <code>windows-split</code>,
            and other options for platforms are <code>macos</code> and <code>linux</code>.
        </li>
        <li>
            <a href="/celeste/download-everest?branch=stable">https://maddie480.ovh/celeste/download-everest?branch=stable</a>
            downloads the latest stable version of Everest. Other options for branches are <code>beta</code> and <code>dev</code>.
        </li>
    </ul>
</div>

<form method="POST">
    <div class="form-group">
        <label for="modId">Type a mod's <code>everest.yaml</code> name here (<b>make sure that uppercase/lowercase letters are correct</b>):</label>
        <input type="text" class="form-control" id="modId" name="modId" required value="<%= escapeHtml4((String) request.getAttribute("typedId")) %>">
    </div>

    <div class="form-check">
        <input type="checkbox" class="form-check-input" id="bundle" name="bundle" value="" <% if ((boolean) request.getAttribute("bundle")) { %> checked <% } %>>
        <label class="form-check-label" for="bundle">Get a zip with the mod and all of its dependencies</label>
    </div>

    <div class="form-check">
        <input type="checkbox" class="form-check-input" id="twoclick" name="twoclick" value="" <% if ((boolean) request.getAttribute("twoclick")) { %> checked <% } %>>
        <label class="form-check-label" for="twoclick">Link to 0x0ade's 2-click installer</label>
    </div>

    <div class="form-check">
        <input type="checkbox" class="form-check-input" id="mirror" name="mirror" value="" <% if ((boolean) request.getAttribute("mirror")) { %> checked <% } %>>
        <label class="form-check-label" for="mirror">Download from 0x0ade's mirror instead of GameBanana</label>
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
            <b>Here is your link:</b>
            <a href="<%= escapeHtml4((String) request.getAttribute("link")) %>" target="_blank">
                <%= escapeHtml4((String) request.getAttribute("link")) %>
            </a>
        </div>
    <% } %>
</form>

<% if (request.getAttribute("notfound") == null && request.getAttribute("link") == null) { %>
    <div class="alert alert-secondary">
        If you have an <code>everest.yaml</code> name for a mod and want to link to its <i>GameBanana page</i> (<b>not</b> a direct file download), use this address:
        <code>https://maddie480.ovh/celeste/gb?id=[everest_yaml_name]</code> (don't forget to URL-encode it).<br>
        For example: <a href="/celeste/gb?id=Monika%27s+D-Sides">https://maddie480.ovh/celeste/gb?id=Monika%27s+D-Sides</a>
    </div>
<% } %>

<script src="/js/direct-url-service.js"></script>