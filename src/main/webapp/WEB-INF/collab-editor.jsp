<%@ page import="java.util.Arrays, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>Celeste Collab/Contest Editor</h1>

<% if((boolean) request.getAttribute("bad_request")) { %>
    <div class="alert alert-danger">
        Your request was invalid, please try again.
    </div>
<% } %>
<% if((boolean) request.getAttribute("saved")) { %>
    <div class="alert alert-success">
        Your changes were saved!
    </div>
<% } %>

<p>
    On this page, you can edit the information displayed about your collab/contest in the
    <a href="/celeste/collab-contest-list" target="_blank">Collab & Contest List</a>.
</p>

<form method="POST">
    <div class="form-group">
        <label for="name"><b>Collab Name</b></label>
        <input type="text" class="form-control" id="name" name="name" value="<%= escapeHtml4((String) request.getAttribute("name")) %>" required>
    </div>
    <div class="form-group">
        <label for="status"><b>Status</b></label>
        <select class="form-select" id="status" name="status">
            <option value="in-progress" <% if (request.getAttribute("status").equals("in-progress")) { %>selected<% } %>>In Progress</option>
            <option value="paused" <% if (request.getAttribute("status").equals("paused")) { %>selected<% } %>>Paused</option>
            <option value="released" <% if (request.getAttribute("status").equals("released")) { %>selected<% } %>>Released</option>
            <option value="cancelled" <% if (request.getAttribute("status").equals("cancelled")) { %>selected<% } %>>Cancelled</option>
            <option value="hidden" <% if (request.getAttribute("status").equals("hidden")) { %>selected<% } %>>Hidden</option>
        </select>
    </div>
    <div class="form-group">
        <label for="contact"><b>Invite or Contact (URL or text)</b></label>
        <input type="text" class="form-control" id="contact" name="contact" value="<%= escapeHtml4((String) request.getAttribute("contact")) %>" required>
    </div>

    <div class="form-group">
        <div class="form-check">
            <input class="form-check-input" type="checkbox" id="lookingForPeople" name="lookingForPeople" <% if (request.getAttribute("lookingForPeople").equals("yes")) { %>checked<% } %>>
            <label class="form-check-label" for="lookingForPeople">Open for people to join</label>
        </div>
    </div>

    <% for (String req : Arrays.asList("Mappers", "Coders", "Artists", "Musicians", "Playtesters", "Decorators", "Lobby")) { %>
        <div class="form-group">
            <label for="req<%= req %>" class="open-to"><b>Open to <%= req %></b></label>
            <div class="open-to-content">
                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" id="req<%= req %>Yes" name="req<%= req %>" value="yes"
                        <% if (request.getAttribute("req" + req).equals("yes")) { %>checked<% } %>>
                    <label class="form-check-label" for="req<%= req %>Yes">&#x2705;</label>
                </div>
                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" id="req<%= req %>Maybe" name="req<%= req %>" value="maybe"
                        <% if (request.getAttribute("req" + req).equals("maybe")) { %>checked<% } %>>
                    <label class="form-check-label" for="req<%= req %>Maybe">&#x2754;</label>
                </div>
                <div class="form-check form-check-inline">
                    <input class="form-check-input" type="radio" id="req<%= req %>No" name="req<%= req %>" value="no"
                        <% if (request.getAttribute("req" + req).equals("no")) { %>checked<% } %>>
                    <label class="form-check-label" for="req<%= req %>No">&#x274C;</label>
                </div>
            </div>
        </div>
    <% } %>
    <div class="form-group">
        <label for="reqOther"><b>Other needed people</b></label>
        <input type="text" class="form-control" id="reqOther" name="reqOther" value="<%= escapeHtml4((String) request.getAttribute("reqOther")) %>">
    </div>
    <div class="form-group">
        <label for="notes"><b>Extra notes</b></label>
        <input type="text" class="form-control" id="notes" name="notes" value="<%= escapeHtml4((String) request.getAttribute("notes")) %>">
    </div>

    <input type="submit" class="btn btn-primary" name="action" value="Update">
</form>