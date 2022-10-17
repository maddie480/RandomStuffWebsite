<ul>
    <li>
        <b>
            <% if ((boolean) request.getAttribute("up")) { %>
                <span class="GreenColor">Up</span>
            <% } else { %>
                <span class="RedColor">Down</span>
            <% } %>
        </b>
    </li>

    <% if (request.getAttribute("lastCheckTimeAgo") != null) { %>
        <li>
            Last updated <b><%= request.getAttribute("lastCheckTimeAgo") %></b>
        </li>
    <% } %>

    <li>
        <b><%= request.getAttribute("modCount") %></b> mods
    </li>
</ul>