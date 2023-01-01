<%@ page import="java.util.List, java.util.Map, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>speedrun.com mod update notifications</h1>

<% if((boolean) request.getAttribute("bad_request")) { %>
    <div class="alert alert-danger">
        Your request was invalid, please try again.
    </div>
<% } else if((boolean) request.getAttribute("bad_mod")) { %>
    <div class="alert alert-danger">
        The mod you specified does not exist. Make sure you are using the everest.yaml name of the mod.
    </div>
<% } else if((boolean) request.getAttribute("not_registered")) { %>
    <div class="alert alert-warning">
        This mod is not on the list currently! You cannot remove it from the list.
    </div>
<% } else if((boolean) request.getAttribute("already_registered")) { %>
    <div class="alert alert-info">
        This mod is already on the list!
    </div>
<% } else if((boolean) request.getAttribute("register_success")) { %>
    <div class="alert alert-success">
        The mod was successfully added to the list! You will be notified if it gets updated.
    </div>
<% } else if((boolean) request.getAttribute("unregister_success")) { %>
    <div class="alert alert-success">
        The mod was successfully removed from the list.
    </div>
<% } %>

<p>
    This page allows you to add or remove mods to be notified about when they are updated.
</p>

<p>
    Here are the mods that are currently on the list:
</p>

<ul>
    <% for (Map<String, String> mod : (List<Map<String, String>>) request.getAttribute("modList")) { %>
        <li>
            <code><%= escapeHtml4(mod.get("id")) %></code> &#x2013;
            <% if (mod.containsKey("url")) { %>
                <a href="<%= escapeHtml4(mod.get("url")) %>" target="_blank"><%= escapeHtml4(mod.get("name")) %></a>
                (current version <b><%= escapeHtml4(mod.get("version")) %></b>)
            <% } else { %>
                <span class="bad-mod">&#x26a0; Unknown mod</span>
            <% } %>
        </li>
    <% } %>
</ul>

<form method="POST">
    <div class="form-group">
        <label for="modId"><b>Mod Name (from everest.yaml)</b></label>
        <input type="text" class="form-control" id="modId" name="modId" required>
    </div>

    <input type="submit" class="btn btn-success" name="action" value="Add to List">
    <input type="submit" class="btn btn-danger" name="action" value="Remove from List">
</form>