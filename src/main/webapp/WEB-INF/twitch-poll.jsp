<%@ page import="java.util.List, org.json.JSONObject, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1><%= escapeHtml4((String) request.getAttribute("title")) %></h1>

<p class="instructions">
    Pour voter, envoyez l'une des réponses dans le chat.
    Vous pouvez envoyer un autre message pour changer votre choix.
</p>

<table class="table table-striped" id="table" data-poll-uuid="<%= escapeHtml4((String) request.getAttribute("uuid")) %>">
    <% for (String choice : (List<String>) request.getAttribute("answers")) { %>
        <tr>
            <td data-choice="<%= escapeHtml4(choice) %>">
                <span class="choice-name"><%= escapeHtml4(choice) %></span>
                &#x2013;
                <span class="quantity">--</span>
                <div class="bar" style="width: 0"></div>
            </td>
        </tr>
    <% } %>
</table>

<script src="/js/twitch-poll.js"></script>
