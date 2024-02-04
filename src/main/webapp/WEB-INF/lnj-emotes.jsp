<%@ page import="java.util.List, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<link rel="stylesheet" href="/css/twitch-poll.css">

<h1>Emotes du chat LNJ</h1>

<table class="table table-striped mini">
    <tr class="header">
        <th>Nom</th>
        <th>Emote</th>
    </tr>

    <% for (String emote : (List<String>) request.getAttribute("emotes")) { %>
        <tr>
            <td><span class="emote-name"><%= escapeHtml4(emote.split(";")[0]) %></span></td>
            <td><img src="https://cdn.discordapp.com/emojis/<%= escapeHtml4(emote.split(";")[1]) %>.webp?size=24&quality=lossless"/></td>
        </tr>
    <% } %>
</table>
