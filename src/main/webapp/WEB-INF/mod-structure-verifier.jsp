<%@ page import="java.util.List, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

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
    <p>
        <b>File upload on the website can be quite slow.</b> To reduce the file size, you can delete the Audio folder from the zip you upload.
    </p>

    <p>
        For faster verifying, you can use the <a href="/discord-bots#mod-structure-verifier">Mod Structure Verifier</a> Discord bot.
        If you host a contest/collab that has its own Discord server, you can invite the bot on it and preset the folder names,
        so that mappers just have to drop their zip files in a designated channel to have them verified.
    </p>
</div>

<form method="POST" action="/celeste/mod-structure-verifier" enctype="multipart/form-data" id="verify-form">
    <div class="form-group">
        <label for="zipFile">Zip file to verify</label>
        <input type="file" class="form-control" accept=".zip" id="zipFile" name="zipFile" required>
    </div>
    <div class="form-check">
      <input class="form-check-input" type="checkbox" value="" id="checkPaths"
       <% if (request.getParameter("assetFolderName") != null || request.getParameter("mapFolderName") != null) { %>
          checked
       <% } %>>
      <label class="form-check-label" for="checkPaths">Enable path checking</label>
    </div>

    <div id="path-checking" style="display: none">
        <div class="form-group">
            <label for="assetFolderName">Asset folder name (alphanumeric)</label>
            <input type="text" class="form-control" id="assetFolderName" name="assetFolderName" pattern="[A-Za-z0-9]+"
                <% if (request.getParameter("assetFolderName") != null) { %>
                    value="<%= escapeHtml4(request.getParameter("assetFolderName")) %>"
                <% } %>>
        </div>
        <div class="form-group">
            <label for="mapFolderName">Map folder name (alphanumeric)</label>
            <input type="text" class="form-control" id="mapFolderName" name="mapFolderName" placeholder="Same as asset folder name" pattern="[A-Za-z0-9]+"
                <% if (request.getParameter("mapFolderName") != null) { %>
                    value="<%= escapeHtml4(request.getParameter("mapFolderName")) %>"
                <% } %>>
        </div>

        <div class="alert alert-info">
            If you want to reuse those settings frequently, you can use a direct link to have them filled out by default:
            <a class="btn btn-light" href="#" id="copyUrl">Copy URL</a>
        </div>
    </div>

    <input type="submit" class="btn btn-primary" value="Verify" id="submit-button">
</form>

<script src="/js/mod-structure-verifier.js"></script>
