<%@ page import="java.util.List"%>

<% if ("font-generate".equals(request.getAttribute("type"))) { %>
    <h1>Celeste Font Generator</h1>
<% } else { %>
    <h1>Celeste Mod Structure Verifier</h1>
<% } %>

<% if((boolean) request.getAttribute("taskNotFound")) { %>
    <div class="alert alert-danger">
        <b>This task does not exist!</b> Please try running it again.
    </div>
<% } else if((boolean) request.getAttribute("fileNotFound")) { %>
    <div class="alert alert-danger">
        <b>This file does not exist!</b> Please go back to the previous page or try running the task again.
    </div>
<% } else if((boolean) request.getAttribute("taskOngoing")) { %>
    <div class="alert alert-info">
        <b>Please wait...</b> This page will refresh automatically.
        The task was started <%= request.getAttribute("taskCreatedAgo") %>.
    </div>
<% } else { %>
    <div class="alert alert-<%= request.getAttribute("taskResultType") %>">
        <%= request.getAttribute("taskResult") %>

        <% if (!((List<String>) request.getAttribute("attachments")).isEmpty()) { %>
            <div class="attachment-list">
                <% for(int i = 0; i < ((List<String>) request.getAttribute("attachments")).size(); i++) { %>
                    <a href="/celeste/task-tracker/<%= request.getAttribute("type") %>/<%= request.getAttribute("id") %>/download/<%= i %>"
                        class="btn btn-outline-dark" target="_blank">
                        &#x1F4E5; <%= ((List<String>) request.getAttribute("attachments")).get(i) %>
                    </a>
                <% } %>
            </div>
        <% } %>
    </div>
<% } %>

<% if(!((boolean) request.getAttribute("taskOngoing"))) { %>
    <div class="back-link">
        <% if ("font-generate".equals(request.getAttribute("type"))) { %>
            <a class="btn btn-outline-secondary" href="/celeste/font-generator">&#x2B05; Back to Font Generator</a>
        <% } else { %>
            <a class="btn btn-outline-secondary" href="/celeste/mod-structure-verifier">&#x2B05; Back to Mod Structure Verifier</a>
        <% } %>
    </div>
<% } %>
