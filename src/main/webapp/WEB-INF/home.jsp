<%@ page import="java.util.List, org.apache.commons.lang3.tuple.Pair, java.text.DecimalFormat, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<div class="home">
    <h1>Welcome!</h1>

    <div>
        Hi there! &#x1F44B; I'm <b>Maddie</b>, a backend developer from France that codes random stuff on her free time.
        And this is the website where most of said random stuff ends up. &#x1F61B;
    </div>

    <h3>So, what can I find here?</h3>

    <div>
        This website has a lot of tools and APIs related to <b>Celeste modding</b>.
        <a href="https://www.celestegame.com/" target="_blank">Celeste</a> is a platformer about climbing a mountain and
        overcoming your inner demons.
        If you own the game on PC, you can check <a href="https://everestapi.github.io" target="_blank">the mod loader's website</a>
        to get started with modding!
    </div>

    <ul>
        <li>
            <b><a href="/celeste/banana-mirror-browser">Banana Mirror Browser</a>:</b> a complete mirror of
            <a href="https://gamebanana.com/games/6460" target="_blank">Celeste mods on GameBanana</a>
            that you can use if GameBanana is slow or down.
        </li>
        <li>
            <b><a href="/celeste/news-network-subscription">#celeste_news_network Subscription</a>:</b> register a Discord
            webhook there to receive posts from EXOK's Mastodon account, and Celeste modding news. This service is what powers the
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

    <div>
        You can also find information on the five <b><a href="/discord-bots">Discord bots</a></b> I made here, and invites for them.
        Some of them are HTTP-based, and are run on this website directly!
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

    <% if (((int) request.getAttribute("gitlabEventCount")) > 0) { %>
        <% int gitlabEventCount = (int) request.getAttribute("gitlabEventCount"); %>
        <div>
            In addition, I made
            <b><%= new DecimalFormat("#,##0").format(gitlabEventCount) %></b> event<% if (gitlabEventCount != 1) { %>s<% } %>
            happen last week at my work's GitLab repositories. Nothing to show here though, they're private. &#x1F61B;
        </div>
    <% } %>

    <h2>And who are you exactly?</h2>

    <div>
        Just a girl in her late twenties that likes programming, video games and silly stuff. &#x1F61D;
        I've got a bit of a hyperfocus on Celeste as you can see here, but I sometimes mess with obscure glitchy games,
        leading to rather <i>interesting</i> things like <a href="/vids/pizza-dude.webm" target="_blank">stacking cars</a> or
        <a href="/vids/forklift-racer.webm" target="_blank">shooting forklifts into space</a>.
    </div>

    <div>
        Maintaining all of that stuff takes time, but this is what I like doing &#x1F604;
        And at the end of the day, I don't really play video games that much, even Celeste. &#x1F914;
    </div>

    <div>
        As far as programming languages go, I'm using:
    </div>

    <ul>
        <li><b>Java</b> for the backend and part of the frontend</li>
        <li><b>VueJS</b> for the more dynamic parts of the frontend</li>
        <li><b>C#</b> for Celeste modding, as this game runs on the .NET Framework</li>
        <li><b>PHP</b> with Symfony at work</li>
    </ul>

    <div>
        I also messed around in other languages like Ruby or Python. I don't have a real preference, as long as the language allows
        me to do what I want. &#x1F61B;
    </div>

    <div>
        You can find me on Discord, I'm <b>maddie480</b> and I hang out quite a bit on the
        <a href="https://discord.gg/celeste" target="_blank">Celeste server</a>.
        I'm also on <a href="https://github.com/maddie480" target="_blank">GitHub</a>, and my Celeste mods are showcased
        on <a href="https://gamebanana.com/members/1698143" target="_blank">GameBanana</a>!
    </div>

    <div class="text-secondary">
        You might find quite a few references to my old nickname, max480, scattered everywhere.
        This refers to me as well!
    </div>

    <div>
        I hope you'll find this website useful, don't hesitate to report issues or to make suggestions on Discord! &#x2764;&#xFE0F;<br/>
        <span class="august">~ Maddie</span>
    </div>

    <h2>Credits</h2>

    <ul>
        <li>Avatar: <a href="/picrew" target="_blank">&#x5409;&#x7530;&#x541b; on Picrew</a></li>
        <li>Base theme: <a href="https://getbootstrap.com/" target="_blank">Bootstrap</a></li>
        <li>Site name font: <a href="https://www.dafont.com/fr/hey-august.font" target="_blank">Hey August by Khurasan</a></li>
        <li>Navbar and title font: <a href="https://www.dafont.com/fr/gontserrat.font" target="_blank">Gontserrat by Ospiro Enterprises</a></li>
        <li>Navbar theme: inspired by <a href="https://color.firefox.com/" target="_blank">Firefox Color</a></li>
    </ul>
</div>
