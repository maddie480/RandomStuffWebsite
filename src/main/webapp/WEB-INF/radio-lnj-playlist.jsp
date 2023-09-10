<%@ page import="java.util.List, org.json.JSONObject, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1>Radio LNJ&nbsp;&#x2013; Playlist</h1>

<p class="header">
    <b><%= request.getAttribute("elementCount") %></b> éléments dans la playlist,
    durée totale <b><%= request.getAttribute("totalDuration") %></b>
</p>

<table class="table table-striped">
    <% for (JSONObject track : (List<JSONObject>) request.getAttribute("elements")) { %>
        <tr>
            <td>
                <audio controls src="<%= escapeHtml4(track.getString("path")) %>" preload="none"></audio>
                <span class="track-name"><%= escapeHtml4(track.getString("trackName")) %></span>
            </td>
            <td>
                <%= escapeHtml4(track.getString("trackName")) %>
            </td>
        </tr>
    <% } %>
</table>

<script src="/js/radio-lnj-playlist.js"></script>
