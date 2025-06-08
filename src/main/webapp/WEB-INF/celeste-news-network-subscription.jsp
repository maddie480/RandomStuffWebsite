<h1>#celeste_news_network subscription service</h1>

<% if((boolean) request.getAttribute("bad_request")) { %>
    <div class="alert alert-danger">
        Your request was invalid, please try again.
    </div>
<% } else if((boolean) request.getAttribute("bad_webhook")) { %>
    <div class="alert alert-danger">
        Your Discord webhook link is invalid.
    </div>
<% } else if((boolean) request.getAttribute("not_registered")) { %>
    <div class="alert alert-warning">
        Your webhook is not registered! You cannot unregister it.
    </div>
<% } else if((boolean) request.getAttribute("subscribe_success")) { %>
    <div class="alert alert-success">
        <% if((boolean) request.getAttribute("already_registered")) { %>
            Your webhook subscription has been successfully updated! You should have received the test message on it.
        <% } else { %>
            Your webhook was registered successfully! You should have received the test message on it.
        <% } %>
    </div>
<% } else if((boolean) request.getAttribute("unsubscribe_success")) { %>
    <div class="alert alert-success">
        Your webhook was unregistered successfully.
    </div>
<% } %>

<p>
    This page allows you to register a Discord webhook in order to have a copy of the <code>#celeste_news_network</code> channel from
    the <a href="https://discord.gg/celeste" target="_blank">Mt. Celeste Climbing Association</a> on your server.
</p>

<p class="last">
    This webhook can receive messages from:
</p>

<ul>
    <li>
        <b><a href="https://mastodon.exok.com/@EXOK" target="_blank">@EXOK@mastodon.exok.com</a></b> &#x2013;
        The official Mastodon account of EXOK Games, the developer of Celeste
    </li>
    <li>
        <b><a href="/celeste/olympus-news" target="_blank">Olympus News</a></b> &#x2013; Celeste modding news that appear in
        <a href="https://gamebanana.com/tools/6449" target="_blank">Olympus</a>, a mod manager and installer for
        the <a href="https://everestapi.github.io" target="_blank">Everest</a> mod loader.
        The news feed is managed by touhoe and cellularAutomaton (touhoe and cellularautomaton on Discord), and published through
        <a href="https://github.com/EverestAPI/EverestAPI.github.io/tree/main/olympusnews" target="_blank">the GitHub repository of the Everest website</a>.
    </li>
</ul>

<p>
    <b><%= request.getAttribute("sub_count") %> <%= ((int) request.getAttribute("sub_count")) == 1 ? "webhook" : "webhooks" %></b> are currently registered.
</p>

<div class="alert alert-info">
    When registering your webhook, a message saying <i>"This webhook was registered on the #celeste_news_network subscription service!"</i> will be sent to it.
</div>

<p>
    You can unsubscribe by deleting the webhook, or by returning to this page and clicking "Unsubscribe".
</p>

<p>
    To sign up, just create a webhook on your server pointing to the channel of your choice, and paste it here:
</p>

<form method="POST">
    <div class="form-group">
        <label for="url"><b>Discord Webhook URL</b></label>
        <input type="text" class="form-control" id="url" name="url" required pattern="^https:\/\/discord\.com\/api\/webhooks\/[0-9]+/[A-Za-z0-9-_]+$">
    </div>
    <b>Subscribe to:</b>
    <div class="form-check">
        <input class="form-check-input" type="checkbox" id="mastodon" name="mastodon" />
        <label class="form-check-label" for="mastodon">Mastodon</label>
    </div>
    <div class="form-check">
        <input class="form-check-input" type="checkbox" id="olympus" name="olympus" />
        <label class="form-check-label" for="olympus">Olympus News</label>
    </div>

    <p>
        <i>You can change your choices later by coming back to this page and subscribing again. This will update your subscription.</i>
    </p>

    <input type="submit" class="btn btn-success" name="action" value="Subscribe" disabled>
    <input type="submit" class="btn btn-danger" name="action" value="Unsubscribe">
</form>

<script src="/js/celeste-news-network-subscription.js"></script>