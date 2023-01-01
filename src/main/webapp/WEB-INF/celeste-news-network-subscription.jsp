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
<% } else if((boolean) request.getAttribute("already_registered")) { %>
    <div class="alert alert-info">
        Your webhook is already registered!
    </div>
<% } else if((boolean) request.getAttribute("subscribe_success")) { %>
    <div class="alert alert-success">
        Your webhook was registered successfully! You should have received the test message on it.
    </div>
<% } else if((boolean) request.getAttribute("unsubscribe_success")) { %>
    <div class="alert alert-success">
        Your webhook was unregistered successfully.
    </div>
<% } %>

<p>
    This page allows you to register a Discord webhook in order to have a copy of the <code>#celeste_news_network</code> channel from
    the <a href="https://discord.gg/celeste" target="_blank">Mt. Celeste Climbing Association</a> on your server.
    This webhook will receive all new tweets from <a href="https://twitter.com/celeste_game" target="_blank">@celeste_game</a>
    and <a href="https://twitter.com/EverestAPI" target="_blank">@EverestAPI</a>.
</p>

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

    <input type="submit" class="btn btn-success" name="action" value="Subscribe">
    <input type="submit" class="btn btn-danger" name="action" value="Unsubscribe">
</form>
