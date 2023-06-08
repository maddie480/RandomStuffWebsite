<%@ page import="java.util.Arrays, java.util.List, org.json.JSONObject, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>Celeste Collab & Contest List</h1>

<p>
    Here is some info about ongoing Celeste collabs and contests, updated by their managers.
</p>

<div class="alert alert-info">
    To add your own, reach out to Maddie (maddie480 on the <a href="https://discord.gg/celeste" target="_blank">Mt. Celeste Climbing Association</a> server),
    and she will give you a private link to add and update the status of your own collab.
</div>

<div class="accordion" id="collab-list">
    <% int id = 0; %>
    <% for (JSONObject collab : (List<JSONObject>) request.getAttribute("collabs")) { %>
        <div class="accordion-item">
            <h2 class="accordion-header" id="heading-<%= id %>">
                <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" data-bs-target="#collab-<%= id %>" aria-expanded="false" aria-controls="collab-<%= id %>">
                    <%= escapeHtml4(collab.getString("name")) %>

                    <% if ("in-progress".equals(collab.getString("status"))) { %>
                        <span class="badge bg-primary">In Progress</span>
                    <% } else if ("released".equals(collab.getString("status"))) { %>
                        <span class="badge bg-success">Released</span>
                    <% } else if ("paused".equals(collab.getString("status"))) { %>
                        <span class="badge bg-warning">Paused</span>
                    <% } else if ("cancelled".equals(collab.getString("status"))) { %>
                        <span class="badge bg-danger">Cancelled</span>
                    <% } %>

                    <% if ("yes".equals(collab.getString("lookingForPeople"))) { %>
                        <span class="badge bg-secondary">Open</span>
                    <% } %>
                </button>
            </h2>

            <div id="collab-<%= id %>" class="accordion-collapse collapse" aria-labelledby="heading-<%= id %>" data-bs-parent="#collab-list">
                <div class="accordion-body">
                    <div><b>Open to:</b></div>
                    <% for (String req : Arrays.asList("Mappers", "Coders", "Artists", "Musicians", "Playtesters", "Decorators", "Lobby")) { %>
                        <div>
                            <% if ("yes".equals(collab.getString("req" + req))) { %>
                                &#x2705;
                            <% } else if ("maybe".equals(collab.getString("req" + req))) { %>
                                &#x2754;
                            <% } else if ("no".equals(collab.getString("req" + req))) { %>
                                &#x274C;
                            <% } %>
                            <%= req %>
                        </div>
                    <% } %>

                    <% if (!collab.getString("reqOther").isBlank()) { %>
                        &#x2705; <%= escapeHtml4(collab.getString("reqOther")) %>
                    <% } %>

                    <% if (!collab.getString("notes").isBlank()) { %>
                        <div class="description">
                            <%= collab.getString("notes") %>
                        </div>
                    <% } %>

                    <div class="contact">
                        <% if (collab.getString("contact").startsWith("http")) { %>
                            <a href="<%= escapeHtml4(collab.getString("contact")) %>" class="btn btn-success" target="_blank">Join</a>
                        <% } else { %>
                            <b>Contact:</b> <%= escapeHtml4(collab.getString("contact")) %>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>
        <% id++; %>
    <% } %>
</div>
