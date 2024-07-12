<h1>Discord Bots</h1>

<div>
    I am currently hosting 5 publicly available Discord bots and applications:
    <ul>
        <li><a href="#timezone-bot">Timezone Bot</a></li>
        <li><a href="#mod-structure-verifier">Mod Structure Verifier</a></li>
        <li><a href="#games-bot">Games Bot</a></li>
        <li><a href="#custom-slash-commands">Custom Slash Commands</a></li>
        <li><a href="#bananabot">BananaBot</a></li>
    </ul>
</div>

<div>
    Check <a href="/discord-bots/terms-and-privacy">this page</a> for Terms of Service and more information about collected data.
</div>

<div>
    All bot command descriptions and private responses are available <b>in English and French</b>.
    The language will be picked automatically depending on your Discord settings.
</div>

<div class="alert alert-info">
    If you need support on those bots or want to try them out, you can <a href="https://discord.gg/PdyfMaq9Vq">join the bot testing server</a>!
</div>

<h1 class="botname" id="timezone-bot">
    <img src="/img/timezone-bot-logo.png" class="botlogo"> Timezone Bot
</h1>

<h2>
    Without timezone roles
    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("timezoneBotLiteServerCount") %>
        server<%= ((int) request.getAttribute("timezoneBotLiteServerCount")) == 1 ? "" : "s" %>
        &#x2013;
        <%= request.getAttribute("timezoneBotLiteUserCount") %>
        user<%= ((int) request.getAttribute("timezoneBotLiteUserCount")) == 1 ? "" : "s" %>
    </span>
</h2>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("timezoneBotLiteServerCount") %>
        server<%= ((int) request.getAttribute("timezoneBotLiteServerCount")) == 1 ? "" : "s" %>
        &#x2013;
        <%= request.getAttribute("timezoneBotLiteUserCount") %>
        user<%= ((int) request.getAttribute("timezoneBotLiteUserCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    You can install this bot to your Discord account, and you'll be able to get Discord timestamps and check the time it is in
    other parts of the world, from any server or DM!
</p>
<p>
    You can also invite the bot to your server to let everyone there use it, and see each other's local time as well.
</p>

<p>
    Here is a list of commands:
</p>

<ul>
    <li><code>/detect-timezone</code> - gives a link to <a href="/discord-bots/timezone-bot/detect-timezone" target="_blank">a page</a> to figure out your timezone</li>
    <li>
        <code>/set-timezone [tz_name]</code> - saves your timezone for other commands. You should use the format given to you by <code>/detect-timezone</code>
        (for example <code>Europe/Paris</code>) because it handles switching between summer and winter time nicely, but you can use "UTC+3" or "EST" as well.
    </li>
    <li><code>/remove-timezone</code> - deletes your timezone from the bot's database</li>
    <li><code>/discord-timestamp [date_time]</code> - gives a <a href="https://discord.com/developers/docs/reference#message-formatting-timestamp-styles" target="_blank">Discord timestamp</a>, to tell a date/time to other people regardless of their timezone</li>
    <li><code>/world-clock [place]</code> - gives the time it is in another place in the world (a city or a country) and the difference with the time it is for you</li>
    <li>
        <code>/time-for [member]</code> <i>(only available on servers the bot was added to)</i> - gives the time it is now for another member of the server (if they have a timezone configured) and the difference with the time it is for you.
        You can also see someone's local time by <b>right-clicking on them</b> (or tapping them on mobile), then selecting <b>Apps &gt; Get Local Time</b>.
    </li>
    <li><code>/list-timezones</code> <i>(only available on servers the bot was added to)</i> - lists all members on the server with a timezone configured, sorting them by timezone with local time for each</li>
</ul>

<p>
    These slash commands issue private responses, so they can be used from anywhere without cluttering a channel with commands.
</p>

<div class="row">
    <div class="col-md-6 col-xs-12">
        <b class="gametitle">/set-timezone usage:</b>
        <p class="lighttheme"><img src="/img/example_timezone_bot_lighttheme.png"></p>
        <p class="darktheme"><img src="/img/example_timezone_bot.png"></p>

        <p><b>/time-for usage:</b></p>
        <p class="lighttheme"><img src="/img/time_for_light.png"></p>
        <p class="darktheme"><img src="/img/time_for_dark.png"></p>
    </div>

    <div class="col-md-6 col-xs-12">
        <b class="gametitle">/discord-timestamp usage:</b>
        <p class="lighttheme"><img src="/img/discord_timestamp_light.png"></p>
        <p class="darktheme"><img src="/img/discord_timestamp_dark.png"></p>
    </div>
</div>

<p class="credits">
    The timezone names recognized by <code>/set-timezone</code> are provided by <a href="https://en.wikipedia.org/wiki/Tz_database" target="_blank">the tz database</a>
    and <a href="https://www.timeanddate.com/time/zones/" target="_blank">timeanddate.com</a>.
    The <code>/world-clock</code> command uses <a href="https://nominatim.openstreetmap.org/" target="_blank">Nominatim from OpenStreetMap</a> for geocoding,
    and <a href="https://timezonedb.com/" target="_blank">TimeZoneDB.com</a> to turn the resulting position into a timezone.
</p>

<a class="btn btn-primary" href="https://discord.com/api/oauth2/authorize?client_id=1021154491040534649" target="_blank">Invite</a>

<h2 class="space">
    With timezone roles
    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("timezoneBotFullServerCount") %>
        server<%= ((int) request.getAttribute("timezoneBotFullServerCount")) == 1 ? "" : "s" %>
    </span>
</h2>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("timezoneBotFullServerCount") %>
        server<%= ((int) request.getAttribute("timezoneBotFullServerCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    In addition to the above, this bot assigns <b>timezone roles</b> to the users on your server.
    As such, it <b>requires the Manage Roles permission</b> as it creates, deletes and updates timezone roles itself as needed.
    It is also limited to 100 servers due to Discord having denied verification to exceed that.
</p>

<p>
    Two more commands allow admins to set the bot up, and are only accessible to members with the "Administrator" or "Manage Server" permission by default:
</p>

<ul>
    <li><code>/toggle-times</code> - sets whether timezone roles should show the time it is in the timezone (for example <code>Timezone UTC+01:00 (2pm)</code>) or not (for example <code>Timezone UTC+01:00</code>). Enabling this causes "role update" events to be logged hourly. This is disabled by default.</li>
    <li><code>/timezone-dropdown</code> - creates a dropdown that lets users pick a timezone role. This is useful if most members in your server have the same timezone roles. An admin can set this up in a fixed <code>#roles</code> channel, similarly to reaction roles. <a href="/discord-bots/timezone-bot/timezone-dropdown-help">Check this page for help with the syntax and examples.</a></li>
</ul>

<p>
    Here is what the timezone roles look like:
</p>

<div class="row">
    <div class="col-md-6 col-xs-12">
        <b class="gametitle">If /toggle-times is enabled:</b>
        <p class="lighttheme"><img src="/img/timezone_role_with_times_lighttheme.png"></p>
        <p class="darktheme"><img src="/img/timezone_role_with_times.png"></p>
    </div>

    <div class="col-md-6 col-xs-12">
        <b class="gametitle">If /toggle-times is disabled:</b>
        <p class="lighttheme"><img src="/img/timezone_role_no_time_lighttheme.png"></p>
        <p class="darktheme"><img src="/img/timezone_role_no_time.png"></p>
    </div>
</div>

<% if(((int) request.getAttribute("timezoneBotFullServerCount")) >= 100) { %>
    <p>
        <b>This bot cannot be invited because it hit the limit of 100 servers applied by Discord to all non-verified bots.</b>
    </p>
    <button class="btn btn-primary" disabled>Invite</button>
<% } else { %>
    <a class="btn btn-primary" href="https://discord.com/oauth2/authorize?client_id=806514800045064213&scope=bot%20applications.commands&permissions=268435456" target="_blank">Invite</a>
<% } %>

<h1 class="botname margin" id="mod-structure-verifier">
    <img src="/img/mod-structure-verifier-logo.png" class="botlogo"> Mod Structure Verifier

    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("modStructureVerifierServerCount") %>
        server<%= ((int) request.getAttribute("modStructureVerifierServerCount")) == 1 ? "" : "s" %>
    </span>
</h1>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("modStructureVerifierServerCount") %>
        server<%= ((int) request.getAttribute("modStructureVerifierServerCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    This is a bot that downloads zips from Discord attachments or Google Drive links posted in a specific channel,
    and check if they are properly structured Celeste mods for <a href="https://github.com/EverestAPI/Everest" target="_blank">Everest</a>.
    This is useful to enforce a structure for collabs/contests, or just to check for some stuff like missing dependencies.
</p>

<p>
    Server admins and moderators with the "Manage Server" permission can set up channels where other users can verify mods.
    The bot will ignore messages in other channels. 3 setups are available:
</p>
<ul>
    <li>
        <code>--setup-fixed-names</code>: the admin setting up the bot chooses what the assets folder should be called, and all zips posted in the channel
        are checked with those settings. This setup is useful for servers dedicated to a collab/contest in particular.
    </li>
    <li>
        <code>--setup-free-names</code>: the user can pick the assets folder name themselves by running the <code>--verify</code> command in the dedicated channel
        with the folder name as a parameter, and the zip / Google Drive link attached. This is useful on servers where people organize their
        own collabs/contests, since they can tell participants the command to run, without needing an admin to set up the bot.
    </li>
    <li>
        <code>--setup-no-name</code>: the bot won't check folder names at all, and will only check if the everest.yaml is valid and missing entities,
        triggers, effects, stylegrounds and decals. This is useful to allow people to check their own mods.
    </li>
</ul>

<p>
    You can combine <code>--setup-free-names</code> with one of the 2 other setups in the same channel: the bot will pick depending on whether the user
    uses <code>--verify</code> or not.
</p>

<p>
    After setting the bot up, the users will be able to have their zips checked by just dropping the zip or the Google Drive link in the channel you designated
    (they will have to use the <code>--verify</code> command if you set up the bot with <code>--setup-free-names</code>).
</p>

<p>
    If you have a dialog file in a language like Japanese or Korean, you might be using characters that are absent from the vanilla game's font.
    The Mod Structure Verifier checks for that, and gives you <b>a command to generate the missing characters</b>: <code>--generate-font [language]</code>.
    Send this command along with your dialog file to get a zip with all the files you need to integrate the characters you use into the game's font!
    <i><code>language</code> should be one of <code>chinese</code>, <code>japanese</code>, <code>korean</code>, <code>renogare</code> or <code>russian</code>.</i>
</p>

<div class="row">
    <div class="col-md-6 col-xs-12">
        <b class="gametitle">Example of a mod passing verification:</b>
        <p class="lighttheme"><img src="/img/mod-structure-verifier-ok-light.png"></p>
        <p class="darktheme"><img src="/img/mod-structure-verifier-ok-dark.png"></p>
    </div>

    <div class="col-md-6 col-xs-12">
        <b class="gametitle">Example of a mod failing verification:</b>
        <p class="lighttheme"><img src="/img/mod-structure-verifier-ko-light.png"></p>
        <p class="darktheme"><img src="/img/mod-structure-verifier-ko-dark.png"></p>
    </div>
</div>

<% if(((int) request.getAttribute("modStructureVerifierServerCount")) >= 100) { %>
    <p>
        <b>This bot cannot be invited because it hit the limit of 100 servers applied by Discord to all non-verified bots.</b>
    </p>
    <button class="btn btn-primary" disabled>Invite</button>
<% } else { %>
    <a class="btn btn-primary" href="https://discord.com/oauth2/authorize?client_id=809572233953542154&scope=bot&permissions=52288" target="_blank">Invite</a>
<% } %>

<h1 class="botname margin" id="games-bot">
    <img src="/img/games-bot-logo.png" class="botlogo extra-margin"> Games Bot

    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("gamesBotServerCount") %>
        server<%= ((int) request.getAttribute("gamesBotServerCount")) == 1 ? "" : "s" %>
    </span>
</h1>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("gamesBotServerCount") %>
        server<%= ((int) request.getAttribute("gamesBotServerCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    This bot allows you to play several 1- or 2-player games using slash commands and Discord interactions.
    You can play the 2-player games against a friend by pinging them in the command, or against the CPU.
</p>

<div class="row">
    <div class="col-md-6 col-xs-12">
        <p>
            <b class="gametitle">Connect 4 (<code>/puissance4</code> in French):</b>
            <span class="lighttheme"><img src="/img/games_connect4_light.png"></span>
            <span class="darktheme"><img src="/img/games_connect4_dark.png"></span>
        </p>
    </div>
    <div class="col-md-6 col-xs-12">
        <p>
            <b class="gametitle">Minesweeper (<code>/d&eacute;mineur</code> in French):</b>
            <span class="lighttheme"><img src="/img/games_minesweeper_light.png"></span>
            <span class="darktheme"><img src="/img/games_minesweeper_dark.png"></span>
        </p>
    </div>
    <div class="col-md-6 col-xs-12">
        <p>
            <b class="gametitle">Reversi:</b>
            <span class="lighttheme"><img src="/img/games_reversi_light.png"></span>
            <span class="darktheme"><img src="/img/games_reversi_dark.png"></span>
        </p>
    </div>
    <div class="col-md-6 col-xs-12">
        <p>
            <b class="gametitle">Tic-Tac-Toe (<code>/morpion</code> in French):</b>
            <span class="lighttheme"><img src="/img/games_tictactoe_light.png"></span>
            <span class="darktheme"><img src="/img/games_tictactoe_dark.png"></span>
        </p>
    </div>
</div>

<p>
    To start playing, <b>use a slash command</b> or <b>right-click on another server member</b>:
</p>

<div class="row">
    <div class="col-md-6 col-xs-12 mb-4">
        <span class="lighttheme"><img src="/img/slash_command_light.png"></span>
        <span class="darktheme"><img src="/img/slash_command_dark.png"></span>
    </div>
    <div class="col-md-6 col-xs-12 mb-4">
        <span class="lighttheme"><img src="/img/user_command_light.png"></span>
        <span class="darktheme"><img src="/img/user_command_dark.png"></span>
    </div>
</div>

<a class="btn btn-primary" href="https://discord.com/api/oauth2/authorize?client_id=890556635091697665&scope=applications.commands" target="_blank">Invite</a>

<h1 class="botname margin" id="custom-slash-commands">
    <img src="/img/custom-slash-commands-logo.png" class="botlogo extra-margin"> Custom Slash Commands

    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("customSlashCommandsServerCount") %>
        server<%= ((int) request.getAttribute("customSlashCommandsServerCount")) == 1 ? "" : "s" %>
    </span>
</h1>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("customSlashCommandsServerCount") %>
        server<%= ((int) request.getAttribute("customSlashCommandsServerCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    This application allows you to add custom slash commands that send out fixed responses of your choice on your server.
</p>

<p>
    To set it up, <b>use the <code>/addc</code> command</b>:
</p>

<p>
    <span class="lighttheme"><img src="/img/create_command_light.png"></span>
    <span class="darktheme"><img src="/img/create_command_dark.png"></span>
</p>

<p>
    <i><code>is_public</code> controls whether the bot response should be visible to everyone (<code>is_public = true</code>), or only to the person that ran the command (<code>is_public = false</code>).</i>
</p>

<p>
    Then, <b>anyone on the server can see and use the command</b>:
</p>

<div class="row">
    <div class="col-md-6 col-xs-12 mb-4">
        <span class="lighttheme"><img src="/img/command_suggestion_light.png"></span>
        <span class="darktheme"><img src="/img/command_suggestion_dark.png"></span>
    </div>
    <div class="col-md-6 col-xs-12 mb-4">
        <span class="lighttheme"><img src="/img/command_response_light.png"></span>
        <span class="darktheme"><img src="/img/command_response_dark.png"></span>
    </div>
</div>

<p>
    Alternatively, you can <b>write up a message and turn it into a custom slash command</b> by right-clicking on it:
</p>

<p>
    <span class="lighttheme framed"><img src="/img/message_command_light.png"></span>
    <span class="darktheme framed"><img src="/img/message_command_dark.png"></span>
</p>

<p>
    Once created, you can edit commands with <code>/editc</code>, remove them with <code>/removec</code>,
    and list all defined commands with <code>/clist</code>.
</p>

<p>
    You can also set up custom slash commands that respond with <b>Discord embeds</b>:
</p>

<div class="row">
    <div class="col-md-6 col-xs-12 mb-4">
        <span class="lighttheme"><img src="/img/embed_setup_light.png"></span>
        <span class="darktheme"><img src="/img/embed_setup_dark.png"></span>
    </div>
    <div class="col-md-6 col-xs-12 mb-4">
        <b class="gametitle">Result:</b>
        <span class="lighttheme"><img src="/img/embed_result_light.png"></span>
        <span class="darktheme"><img src="/img/embed_result_dark.png"></span>
    </div>
</div>

<div>
    <b>Hints:</b>
    <ul>
        <li>
            You can manage who can use commands (including the management commands like <code>/addc</code>) and in which channels by going to
            <b>Server Settings &gt; Integrations &gt; Custom Slash Commands</b>.
            By default, members with Administrator or Manage Server permissions can use the management commands like <code>/addc</code>, and everyone can use the created custom commands.
        </li>
        <li>
            You can customize the text of your links by writing something like this in the slash command response:
            <br>
            <code>You can invite the bot [here](https://discord.com/api/oauth2/authorize?client_id=992122149764608041&scope=applications.commands)!</code>
            <br>
            This will appear like this:
            "You can invite the bot <a href="https://discord.com/api/oauth2/authorize?client_id=992122149764608041&scope=applications.commands" target="_blank">here</a>!"
        </li>
        <li>
            If you include a user or role mention in a slash command response, nobody will actually be pinged when the command is used.
        </li>
        <li>
            You can include line breaks in answers by using <code>\n</code> (for example <code>line1\nline2</code>).
        </li>
        <li>
            If you create a command with an already existing name, the existing command will be replaced.
        </li>
    </ul>
</div>

<a class="btn btn-primary" href="https://discord.com/api/oauth2/authorize?client_id=992122149764608041&scope=applications.commands" target="_blank">Invite</a>

<h1 class="botname margin" id="bananabot">
    <img src="/img/bananabot-logo.png" class="botlogo extra-margin"> BananaBot

    <span class="badge bg-secondary big-badge">
        <%= request.getAttribute("bananaBotServerCount") %>
        server<%= ((int) request.getAttribute("bananaBotServerCount")) == 1 ? "" : "s" %>
    </span>
</h1>

<p class="small-badge">
    <span class="badge bg-secondary">
        <%= request.getAttribute("bananaBotServerCount") %>
        server<%= ((int) request.getAttribute("bananaBotServerCount")) == 1 ? "" : "s" %>
    </span>
</p>

<p>
    An unofficial bot that allows you to search Celeste mods on GameBanana. The command is <code>/gamebanana-search [query]</code>.
    The search results are private, and a button allows you to make it public once you found the mod you want to share.
</p>

<p>
    Here is a video demo:
</p>

<p class="framed">
    <span class="lighttheme">
        <video controls preload="none" muted poster="/img/bananabot-poster-light.png">
            <source src="vids/bananabot_demo_light.webm" type="video/webm">
        </video>
    </span>
    <span class="darktheme">
        <video controls preload="none" muted poster="/img/bananabot-poster-dark.png">
            <source src="vids/bananabot_demo_dark.webm" type="video/webm">
        </video>
    </span>
</p>

<a class="btn btn-primary" href="https://discord.com/api/oauth2/authorize?client_id=1097127282872692789&scope=applications.commands" target="_blank">Invite</a>

<p class="end"><i>Timezone Bot and Mod Structure Verifier logos by phant</i></p>
