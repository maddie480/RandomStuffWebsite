<%@ page import="static org.apache.commons.text.StringEscapeUtils.escapeHtml4" %>
<%@ page import="org.apache.commons.lang3.tuple.Triple" %>
<%@ page import="java.util.List" %>

<h1 class="mt-4"><%= request.getAttribute("title") %></h1>

<span class="hidden" id="program"><%= request.getParameter("program") %></span>

<p>
    wip wip wip
</p>

<div class="toggle">
    <a href="?program=everest" role="button" class="btn <%= "everest".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Everest</a>
    <a href="?program=olympus" role="button" class="btn <%= "olympus".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Olympus</a>
</div>

<table class="table">
    <tr>
        <th>Dialog ID</th>
        <th><%= request.getAttribute("leftLang") %></th>
        <th>
            <select class="form-select">
                <% for (String lang : (List<String>) request.getAttribute("langs")) { %>
                    <option value="<%= escapeHtml4(lang) %>" <%= request.getAttribute("rightLang").equals(lang) ? "selected" : "" %>>
                        <%= escapeHtml4(lang) %>
                    </option>
                <% } %>
            </select>
        </th>
    </tr>
    <% for (Triple<String, String, String> entry : (List<Triple<String, String, String>>) request.getAttribute("entries")) { %>
        <tr>
            <td><code><%= escapeHtml4(entry.getLeft()) %></code></td>
            <td>
                <% if (entry.getMiddle().isEmpty()) { %>
                    <b>Missing</b>
                <% } else { %>
                    <code><%= escapeHtml4(entry.getMiddle()) %></code>
                <% } %>
            </td>
            <td>
                <% if (entry.getRight().isEmpty()) { %>
                    <b>Missing</b>
                <% } else { %>
                    <code><%= escapeHtml4(entry.getRight()) %></code>
                <% } %>
            </td>
        </tr>
    <% } %>
</table>

<script src="/js/translation-viewer.js"></script>