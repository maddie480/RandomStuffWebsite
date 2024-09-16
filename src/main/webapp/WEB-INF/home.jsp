<%@ page import="java.util.List, org.apache.commons.lang3.tuple.Pair, java.text.DecimalFormat, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<div class="home">
    <h1>Welcome!</h1>

    <div>
        Hi there! &#x1F44B; I'm <b>Maddie</b>, a backend developer from France that codes random stuff on her free time.
        And this is the website where most of said random stuff ends up. &#x1F61B;
    </div>

    <div>
        Here is a quick rundown of what I do and what you'll find on the website:
    </div>

    <h3>Celeste-related stuff</h3>

    <div>
        <b>Celeste modding</b> is the thing I'm the most involved in.
        <a href="https://www.celestegame.com/" target="_blank">Celeste</a> is a platformer about climbing a mountain and
        overcoming your inner demons.
        If you own the game on PC, you can check <a href="https://everestapi.github.io" target="_blank">the mod loader's website</a>
        to get started with modding!
    </div>

    <div>
        You can check <b><a href="https://gamebanana.com/members/1698143" target="_blank">my GameBanana profile</a></b>
        to see my Celeste mods. I don't make maps, but rather "helpers": entities and tools other people can use in their maps.
        This very website hosts the <b>Everest Update Checker</b>, which allows the mod loader to check for mod updates or install
        missing dependencies, as well as a few <b>modding tools</b>:
    </div>

    <ul>
        <li>
            <b><a href="/celeste/asset-drive">Asset Drive Browser</a>:</b> Another way to browse the
            <a href="https://drive.google.com/drive/folders/13A0feEXS3kUHb_Q4K2w4xP8DEuBlgTip" target="_blank">Celeste Asset Drive</a>,
            allowing to view the assets across all folders more easily.
        </li>
        <li>
            <b><a href="/celeste/banana-mirror-browser">Banana Mirror Browser</a>:</b> a complete mirror of
            <a href="https://gamebanana.com/games/6460" target="_blank">Celeste mods on GameBanana</a>
            that you can use if GameBanana is slow or down.
        </li>
        <li>
            <b><a href="/celeste/news-network-subscription">#celeste_news_network Subscription</a>:</b> register a Discord
            webhook there to receive posts from EXOK's Mastodon and Twitter accounts, and Celeste modding news. This service is what powers the
            <code>#celeste_news_network</code> channel in the <a href="https://discord.gg/celeste" target="_blank">Celeste Discord</a>.
        </li>
        <li>
            <b><a href="/celeste/collab-contest-list">Collab & Contest List</a>:</b> a list of Celeste modding collabs and contests,
            updated by the organizers themselves. You can check that page to find a collab/contest to join, or to check their progress!
        </li>
        <li>
            <b><a href="/celeste/custom-entity-catalog">Custom Entity Catalog</a>:</b> a huge auto-generated list of all custom entities
            provided by Celeste mods.
            Useful to answer the questions "what kinds of custom xxx exist?" or "which mod does xxx come from again?"
        </li>
        <li>
            <b><a href="/celeste/direct-link-service">Direct Link service</a>:</b> a simple service you can use to directly link to the
            download of the latest version of a mod, which will stay valid even if the mod updates.
        </li>
        <li>
            <b><a href="/celeste/everest-yaml-validator">everest.yaml validator</a>:</b> this service runs a few checks on <code>everest.yaml</code>
            files you have to put
            at the root of Celeste mods.
        </li>
        <li>
            <b><a href="/celeste/file-searcher">File Searcher</a>:</b> this tool allows you to find which mod(s) contain a file
            with a certain name or path. This can be handy if you have an unhelpful crash log that only tells you a file name!
        </li>
        <li>
            <b><a href="/celeste/font-generator">Font Generator</a>:</b> a tool that generates fonts for Celeste. The game requires fonts
            to be provided as images, and you sometimes need to provide them yourself if you want a custom font, or if you want to use characters that
            are not present in the vanilla font (which happens a lot in languages like Chinese).
        </li>
        <li>
            <b><a href="/celeste/graphics-dump-browser">Graphics Dump Browser</a>:</b> browse the
            <a href="https://drive.google.com/file/d/1ITwCI2uJ7YflAG0OwBR4uOUEJBjwTCet/view" target="_blank">Celeste Graphics Dump</a>
            online, download individual images or figure out their path.
        </li>
        <li>
            <b><a href="/lua-cutscenes-documentation/index.html" target="_blank">Lua Cutscenes Documentation</a>:</b>
            The API documentation for <a href="https://gamebanana.com/mods/53678" target="_blank">Lua Cutscenes</a>, written by Cruor.
            It is included in the Lua Cutscenes zip file, and a copy of it is hosted on this website for convenience.
        </li>
        <li>
            <b><a href="/celeste/map-tree-viewer">Map Tree Viewer</a>:</b> this tool displays a Celeste map as a tree, allowing you to see
            how those are structured, and to search for entities by name. You can also convert maps to JSON.
        </li>
        <li>
            <b><a href="/celeste/mod-structure-verifier">Mod Structure Verifier</a>:</b> a service that runs a few checks on entire mods,
            packaged as zip files. It can find missing dependencies, naming issues, missing characters in the font, and so on.
        </li>
        <li>
            <b><a href="/celeste/olympus-news">Olympus News</a>:</b> Celeste modding news that appeared in
            <a href="https://gamebanana.com/tools/6449" target="_blank">Olympus</a>, a mod manager and installer for the
            <a href="https://everestapi.github.io" target="_blank">Everest</a> mod loader.
        </li>
        <li>
            <b><a href="/celeste/update-checker-status">Update Checker status</a>:</b> exactly what it says, a status page for the Everest update checker.
            If it is down, Everest and Olympus (the mod manager) will not be aware of the latest changes on GameBanana.
            The page also displays the latest changes the update checker detected.
        </li>
        <li>
            <b><a href="/celeste/wipe-converter">Wipe Converter</a>:</b> a pretty specific tool that converts wipes (the animation
            that blacks out the screen when you die) to a format that you can use in-game with my code mod,
            <a href="https://gamebanana.com/mods/53687" target="_blank">Maddie's Helping Hand</a>.
        </li>
    </ul>

    <div>
        This website also has a few APIs, that are documented on <a href="https://github.com/maddie480/RandomStuffWebsite#gamebanana-search-api" target="_blank">GitHub</a>.
        Those are what allow the Everest mod updater, the "install missing dependencies" button, and the Olympus mod browser to work!
    </div>

    <h3>Discord bots</h3>

    <div>
        I run five small <b><a href="https://discord.com" target="_blank">Discord</a> bots</b> that you can invite in your discussion server:
    </div>

    <ul>
        <li>
            <b><a href="/discord-bots#timezone-bot">Timezone Bot</a>:</b> a bot that lets you know what time it is for other people
            or in other places in the world, with (optionally) a timezone role that you can refer to.
        </li>
        <li>
            <b><a href="/discord-bots#games-bot">Games Bot</a>:</b> a bot made as an experiment with HTTP-only bots more than anything,
            it allows you to play Connect 4, Tic-Tac-Toe, Reversi or Minesweeper with Discord interactions.
        </li>
        <li>
            <b><a href="/discord-bots#custom-slash-commands">Custom Slash Commands</a>:</b> a bot that allows you to create slash commands
            giving fixed responses. This is used by the Celeste community to answer frequently asked questions.
        </li>
        <li>
            <b><a href="/discord-bots#mod-structure-verifier">Mod Structure Verifier</a>:</b> a Celeste bot to check your mod structure and
            catch some common mistakes. It can also be used to enforce folder names, for example in the context of a collab.
            Also available online <a href="/celeste/mod-structure-verifier">on this website</a>.
        </li>
        <li>
            <b><a href="/discord-bots#bananabot">BananaBot</a>:</b> a Celeste bot that allows you to search for a mod on
            <a href="https://gamebanana.com/games/6460" target="_blank">GameBanana</a> and post its link to a channel without leaving Discord.
        </li>
    </ul>

    <h3>Les Navets Jouables (aka "messing around with obscure games")</h3>

    <div>
        <b>Les Navets Jouables</b> is a small French channel that messes around with bad games ("navet" designates bad films in French...
        and also means "turnip", so the name literally translates to "The Playable Turnips" &#x1F61B;).
        They're making <a href="https://www.youtube.com/@LesNavetsJouables" target="_blank">YouTube videos</a>, and have a
        <a href="https://twitch.tv/lesnavetsjouables" target="_blank">Twitch channel</a>.
    </div>

    <div>
        ... and I've had some fun with a few games they tested, that happen to mostly be abandonware.
        My findings and (dubious) achievements are grouped on a
        <b><a href="https://github.com/maddie480/BazarLNJ" target="_blank">dedicated GitHub repository</a></b> (in French).
        This includes console commands, stuff hidden in the games, ways to mod them (including what are probably the only mods ever made
        for <a href="https://store.steampowered.com/app/1423980/Streatham_Hill_Stories/" target="_blank">Streatham Hill Stories</a>,
        <a href="https://www.youtube.com/watch?v=_YrYAT8YtC0" target="_blank">Pizza Dude</a> (link in French)
        and <a href="https://www.gamespot.com/games/air-control-2013/" target="_blank">Air Control</a>) and some explanations for their quirks.
    </div>

    <div>
        For Pizza Dude in particular, <a href="https://www.youtube.com/watch?v=Sot_qMJoq7o" target="_blank">a video was made on the channel</a>
        (again, in French) to showcase the findings we made with some other community members.
    </div>

    <div>
        ... oh, and <a href="/radio-lnj">there's a radio too</a>. It broadcasts music from the games tested on the channel.
        And it's also not actually a radio, but more like a shared playlist that plays MP3s, but eh, close enough.
    </div>

    <h3>QUEST by Laupok</h3>

    <div>
        A French YouTuber called Laupok once released a pre-alpha version of a multiplayer Zelda-style game, with the placeholder name of... QUEST.
        A quite small community (in the double digits) formed around this game, and made several mods, since the game was rather easy to mod
        (it's mostly made of txt / bmp / wav files that can be edited).
        Today, the game was silently abandoned, the official website of the YouTuber doesn't exist anymore (the game can still be downloaded from
        <a href="/quest/download/Quest-setup.exe">this very website</a> though), and the community disappeared.
    </div>

    <div>
        Back when it was active (and before getting into Celeste), I made a mod loader and a bot to upload and list QUEST mods.
        There also was a website tied to that bot... those all still exist! I eventually made them read-only, though.
        No one is making mods anymore anyway. &#128517;
    </div>

    <div>
        You can check people's mods <a href="/quest/mods">here</a>, and modding tools <a href="/quest/tools">here</a> (in French).
        The bot was eventually reworked and made open-source
        <a href="https://github.com/maddie480/RandomBackendStuff/tree/main/src/main/java/ovh/maddie480/randomstuff/backend/discord/questcommunitybot" target="_blank">over here</a>,
        but it still cannot be invited in other servers. It has stuff like leveling, profiles, Discord presence stats, reminders, countdowns / timers...
        and amazingly enough, a handful of people <i>still</i> collect their daily rewards on the QUEST Discord server, despite <i>absolutely nothing else</i> happening there. &#x1F61B;
    </div>

    <h3>Some Stats (updated hourly)</h3>

    <div>
        <%= new DecimalFormat("#,##0").format((int) request.getAttribute("totalRequests")) %> requests were served last week, with HTTP statuses:
    </div>

    <ul>
        <% for (Pair<Integer, Integer> entry : (List<Pair<Integer, Integer>>) request.getAttribute("responseCountPerCode")) { %>
            <li>
                <% if (entry.getKey() / 100 == 2) { %>
                    <span class="text-success">
                <% } else if (entry.getKey() / 100 == 5) { %>
                    <span class="text-danger">
                <% } else if (entry.getKey() / 100 == 4) { %>
                    <span class="text-warning">
                <% } else { %>
                    <span class="text-primary">
                <% } %>
                    <%= entry.getKey() %>:
                </span>
                <%= new DecimalFormat("#,##0").format(entry.getValue()) %> request<% if (entry.getValue() != 1) { %>s<% } %>
            </li>
        <% } %>
    </ul>

    <div>
        Here is the amount of calls to each of the bots last week:
    </div>

    <ul>
        <% for (Pair<String, Integer> entry : (List<Pair<String, Integer>>) request.getAttribute("callCountPerBot")) { %>
            <li>
                <b><%= escapeHtml4(entry.getKey()) %>:</b> <%= new DecimalFormat("#,##0").format(entry.getValue()) %> call<% if (entry.getValue() != 1) { %>s<% } %>
            </li>
        <% } %>
    </ul>

    <div>
        Here is the amount of events (issues, pull requests, commits, comments) from me over the last week
        on the different GitHub repositories I contribute to:
    </div>

    <ul>
        <% for (Pair<String, Integer> entry : (List<Pair<String, Integer>>) request.getAttribute("repositoryCallCount")) { %>
            <li>
                <b>
                    <a href="https://github.com/<%= escapeHtml4(entry.getKey()) %>" target="_blank">
                        <code><%= escapeHtml4(entry.getKey()) %></code></a>:
                </b>
                <%= new DecimalFormat("#,##0").format(entry.getValue()) %> event<% if (entry.getValue() != 1) { %>s<% } %>
            </li>
        <% } %>
    </ul>

    <h3>Links</h3>

    <div>
        You can find me on:
    </div>

    <ul>
        <li><b>Discord</b>: maddie480 ~ the main way to contact me, join the <a href="https://discord.gg/celeste" target="_blank">Celeste server</a> to be able to reach me</li>
        <li><b>GitHub</b>: <a href="https://github.com/maddie480" target="_blank">maddie480</a> ~ all of my code (including this website) is there</li>
        <li><b>GameBanana</b>: <a href="https://gamebanana.com/members/1698143" target="_blank">maddie480</a> ~ all of my Celeste mods</li>
    </ul>

    <div>
        I hope you'll find this website useful, don't hesitate to report issues or to make suggestions on Discord!<br/>
        <span class="august">~ Maddie</span>
    </div>

    <h3>Credits</h3>

    <ul>
        <li>Avatar: <a href="/picrew" target="_blank">&#x5409;&#x7530;&#x541b; on Picrew</a></li>
        <li>Base theme: <a href="https://getbootstrap.com/" target="_blank">Bootstrap</a></li>
        <li>Site name font: <a href="https://www.dafont.com/fr/hey-august.font" target="_blank">Hey August by Khurasan</a></li>
        <li>Navbar and title font: <a href="https://www.dafont.com/fr/gontserrat.font" target="_blank">Gontserrat by Ospiro Enterprises</a></li>
        <li>Navbar theme: inspired by <a href="https://color.firefox.com/" target="_blank">Firefox Color</a></li>
    </ul>
</div>
