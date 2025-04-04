<%@ page import="java.util.List, java.util.Map, ovh.maddie480.randomstuff.frontend.CelesteModCatalogService, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>GameBanana Arbitrary Mod App Settings</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.5/dist/css/bootstrap.min.css"
        rel="stylesheet" integrity="sha384-SgOJa3DmI69IUzQ2PVdRZhwQ+dy64/BUtbMJw1MZ8t5HZApcHrRKUc4W0kG879m7" crossorigin="anonymous">
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.5/dist/js/bootstrap.bundle.min.js"
        integrity="sha384-k6d4wzSIapyDyv1kpU366/PK5hCdSbCRGRCMv+eplOQJWyd1fbcAu9OCUj5zNLiq" crossorigin="anonymous"></script>

    <link rel="stylesheet" href="/css/gamebanana-modlist-settings.css">
</head>

<body>
    <div class="container">
        <h1>Arbitrary Mod App Settings</h1>

        <% if((boolean) request.getAttribute("invalidKey")) { %>
            <div class="alert alert-danger">
                Your key is invalid, please try again.
            </div>
        <% } else if((boolean) request.getAttribute("tooManyMods")) { %>
            <div class="alert alert-danger">
                You cannot input more than 50 mods, please try again.
            </div>
        <% } else if((boolean) request.getAttribute("appDisabled")) { %>
            <div class="alert alert-danger">
                You don't have the App installed!
            </div>
        <% } else if((boolean) request.getAttribute("invalidMods")) { %>
            <div class="alert alert-danger">
                Some of the mods you added are withheld, trashed, private or do not exist, please try again.
            </div>
        <% } else if((boolean) request.getAttribute("saved")) { %>
            <div class="alert alert-success">
                Your changes were saved!
            </div>
        <% } %>

        <form method="POST">
            <% if ((boolean) request.getAttribute("isInDatabase")) { %>
                <div class="form-group">
                    <% if (((String) request.getAttribute("initialKey")).isEmpty()) { %>
                        <label for="key">Secret key</label>
                        <input type="text" class="form-control" id="key" name="key" required value="<%= escapeHtml4((String) request.getAttribute("typedKey")) %>">
                        <p>
                            If you forgot your key, uninstall the App for a day to wipe your settings, then install it again.
                        </p>
                    <% } else { %>
                        <label for="keyfield">Secret key</label>
                        <input type="text" class="form-control" id="keyfield" name="keyfield" disabled value="<%= escapeHtml4((String) request.getAttribute("initialKey")) %>">
                        <input type="hidden" id="key" name="key" value="<%= escapeHtml4((String) request.getAttribute("initialKey")) %>">
                        <p>
                            <b>Write down your key:</b> you will need it to edit the mods you are displaying.
                        </p>
                    <% } %>
                </div>
            <% } %>

            <div class="form-group">
                <label for="title">Section Title</label>
                <input type="text" class="form-control" id="title" name="title" required value="<%= escapeHtml4((String) request.getAttribute("title")) %>">
            </div>

            <div class="form-group">
                <label for="modlist">Mod List (for example: <code>1337,5442</code> - don't put spaces!)</label>
                <input type="text" class="form-control" id="modlist" name="modlist" required pattern="([0-9]+,)*[0-9]+" value="<%= escapeHtml4((String) request.getAttribute("modList")) %>">
            </div>

            <input type="submit" class="btn btn-primary" value="Save">
        </form>
    </div>
</body>
</html>
