<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@page session="false"%>

<!DOCTYPE html>

<html lang="en">
<head>
    <title>Discord Bots</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="max480">
    <meta name="description" content="Read more about the Mod Structure Verifier, the Timezone Bot and the Games Bot here.">
    <meta property="og:title" content="Discord Bots">
    <meta property="og:description" content="Read more about the Mod Structure Verifier, the Timezone Bot and the Games Bot here.">

    <link rel="stylesheet"
          href="https://stackpath.bootstrapcdn.com/bootstrap/4.2.1/css/bootstrap.min.css"
          integrity="sha384-GJzZqFGwb1QTTN6wy59ffF1BuGJpLSa9DkKMp0DgiMDm4iYMj70gZWKYbI706tWS"
          crossorigin="anonymous">

    <link rel="stylesheet" href="/css/common.css">
    <link rel="stylesheet" href="/css/discord-bots.css">
</head>

<body>
<div class="container">
    <div id="nav">
        <a href="/celeste/custom-entity-catalog">Custom&nbsp;Entity&nbsp;Catalog</a> <span class="sep">|</span>
        <a href="/celeste/everest-yaml-validator">everest.yaml&nbsp;validator</a> <span class="sep">|</span>
        <a href="/celeste/update-checker-status">Update&nbsp;Checker&nbsp;status</a> <span class="sep">|</span>
        <a href="https://max480-random-stuff.herokuapp.com/celeste/banana-mirror-browser">Banana&nbsp;Mirror&nbsp;Browser</a> <span class="sep">|</span>
        <a href="/celeste/mod-structure-verifier">Mod&nbsp;Structure&nbsp;Verifier</a> <span class="sep break">|</span>
        <a href="/celeste/font-generator">Font&nbsp;Generator</a> <span class="sep">|</span>
        <a href="https://max480-random-stuff.herokuapp.com/celeste/wipe-converter">Wipe&nbsp;Converter</a> <span class="sep">|</span>
        <a href="/discord-bots" class="active">Discord&nbsp;Bots</a> <span class="sep">|</span>
        <a href="/celeste/news-network-subscription">#celeste_news_network&nbsp;Subscription</a>
    </div>

    <h1>Discord Bots</h1>

    <div class="alert alert-info">
        If you need support on those bots or want to try them out, you can <a href="https://discord.gg/59ztc8QZQ7">join the bot testing server</a>!
    </div>

    <h1 class="botname" id="timezone-bot">
        <img src="/img/timezone-bot-logo.png" class="botlogo"> Timezone Bot

        <span class="badge badge-secondary big-badge">
            <%= request.getAttribute("timezoneBotServerCount") %>
            server<%= ((int) request.getAttribute("timezoneBotServerCount")) == 1 ? "" : "s" %>
        </span>
    </h1>

    <p class="small-badge">
        <span class="badge badge-secondary">
            <%= request.getAttribute("timezoneBotServerCount") %>
            server<%= ((int) request.getAttribute("timezoneBotServerCount")) == 1 ? "" : "s" %>
        </span>
    </p>

    <p>
        This bot allows server members to grab timezone roles on your server, using slash commands. Here is the list of commands:
    </p>

    <ul>
        <li><code>/detect-timezone</code> - gives a link to <a href="/detect-timezone.html" target="_blank">a page</a> to figure out your timezone</li>
        <li>
            <code>/timezone [tz_name]</code> - sets your timezone role. You can use the following formats:
            <ul>
                <li><a href="https://en.wikipedia.org/wiki/Tz_database" target="_blank">tz database</a> time zones, like <code>Europe/Paris</code>: that's the one the <code>/detect-timezone</code> command gives you, and it will make your role change automatically when you switch between summer and winter time.</li>
                <li>timezone names, for example <code>PST</code> or <code>Pacific Standard Time</code></li>
                <li>UTC offsets, for example <code>UTC+3</code></li>
            </ul>
        </li>
        <li><code>/remove-timezone</code> - removes your timezone role</li>
        <li><code>/discord-timestamp [date_time]</code> - gives a <a href="https://discord.com/developers/docs/reference#message-formatting-timestamp-styles" target="_blank">Discord timestamp</a>, to tell a date/time to other people regardless of their timezone</li>
        <li><code>/time-for [member]</code> - gives the time it is now for another member of the server, if they have a timezone role</li>
        <li><code>/list-timezones [visibility] [names]</code> - lists the timezones of all members in the server that have timezone roles. You can pass <code>visibility = public</code> in order to have the bot response be visible to everyone in the channel.</li>
    </ul>

    <p>
        Two more commands allow admins to set the bot up, and are only accessible to members with the "Administrator" or "Manage Server" permission by default:
    </p>

    <ul>
        <li><code>/toggle-times</code> - sets whether timezone roles should show the time it is in the timezone (for example <code>Timezone UTC+01:00 (2pm)</code>) or not (for example <code>Timezone UTC+01:00</code>). Enabling this causes "role update" events to be logged hourly. This is disabled by default.</li>
        <li><code>/timezone-dropdown</code> - creates a dropdown that lets users pick a timezone role. This is useful if most members in your server have the same timezone roles. An admin can set this up in a fixed <code>#roles</code> channel, similarly to reaction roles. <a href="/discord-bots/timezone-bot/timezone-dropdown-help.html">Check this page for help with the syntax and examples.</a></li>
    </ul>

    <p>
        These slash commands issue private responses by default, so they can be used from anywhere without cluttering a channel with commands.
    </p>

    <p>
        This bot <b>requires the Manage Roles permission</b> as it creates, deletes and updates timezone roles itself as needed. No other permission is required.
    </p>

    <div class="row">
        <div class="col-md-6 col-xs-12">
            <b class="gametitle">/timezone usage:</b>
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

    <% if(((int) request.getAttribute("timezoneBotServerCount")) >= 100) { %>
        <p>
            <b>This bot cannot be invited because it hit the limit of 100 servers applied by Discord to all non-verified bots.</b>
        </p>
        <button class="btn btn-primary" disabled>Invite</button>
    <% } else { %>
        <a class="btn btn-primary" href="https://discord.com/oauth2/authorize?client_id=806514800045064213&scope=bot%20applications.commands&permissions=268435456" target="_blank">Invite</a>
    <% } %>

    <h1 class="botname margin" id="mod-structure-verifier">
        <img src="/img/mod-structure-verifier-logo.png" class="botlogo"> Mod Structure Verifier

        <span class="badge badge-secondary big-badge">
            <%= request.getAttribute("modStructureVerifierServerCount") %>
            server<%= ((int) request.getAttribute("modStructureVerifierServerCount")) == 1 ? "" : "s" %>
        </span>
    </h1>

    <p class="small-badge">
        <span class="badge badge-secondary">
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
        <img src="/img/games-bot-logo.png" class="botlogo"> Games Bot

        <span class="badge badge-secondary big-badge">
            <%= request.getAttribute("gamesBotServerCount") %>
            server<%= ((int) request.getAttribute("gamesBotServerCount")) == 1 ? "" : "s" %>
        </span>
    </h1>

    <p class="small-badge">
        <span class="badge badge-secondary">
            <%= request.getAttribute("gamesBotServerCount") %>
            server<%= ((int) request.getAttribute("gamesBotServerCount")) == 1 ? "" : "s" %>
        </span>
    </p>

    <p>
        A bot that allows you to play several 1- or 2-player games using slash commands and Discord interactions.
        You can play the 2-player games against a friend by pinging them in the command, or against the CPU.
    </p>

    <div class="row">
        <div class="col-md-6 col-xs-12">
            <p>
                <b class="gametitle">Connect 4:</b>
                <span class="lighttheme"><img src="/img/games_connect4_light.png"></span>
                <span class="darktheme"><img src="/img/games_connect4_dark.png"></span>
            </p>
        </div>
        <div class="col-md-6 col-xs-12">
            <p>
                <b class="gametitle">Minesweeper:</b>
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
                <b class="gametitle">Tic-Tac-Toe:</b>
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

    <p class="end"><i>Timezone Bot and Mod Structure Verifier logos by phant</i></p>
</div>
</body>
</html>
