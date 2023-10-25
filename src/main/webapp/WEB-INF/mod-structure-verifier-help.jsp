<%@ page import="static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1 class="mt-4">Mod Structure Verifier Help</h1>

<div>Here is some help to solve your errors.</div>

<% if (request.getParameter("assets") != null) { %>
    <h2>You have assets that are at the wrong place</h2>

    <div>
        The bot expects your assets to be in a <pre><%= escapeHtml4(request.getParameter("collabName")) %>/[subfolder]</pre> folder.
        For example:
        <ul>
            <li>for decals: <pre>Graphics/Atlases/Gameplay/decals/<%= escapeHtml4(request.getParameter("collabName")) %>/[subfolder]/decal.png</pre></li>
            <li>for color grades: <pre>Graphics/ColorGrading/<%= escapeHtml4(request.getParameter("collabName")) %>/[subfolder]/colorgrade.png</pre></li>
            <li>for Lua cutscenes: <pre>Assets/<%= escapeHtml4(request.getParameter("collabName")) %>/[subfolder]/mycutscene.lua</pre></li>
            <li>for playback tutorials: <pre>Tutorials/<%= escapeHtml4(request.getParameter("collabName")) %>/[subfolder]/mytutorial.bin</pre></li>
        </ul>
    </div>

    <div style="margin-bottom: 20px">
        Since they cannot be in subfolders, emojis should be in
        <pre>Graphics/Atlases/Gui/emoji/<%= escapeHtml4(request.getParameter("collabName")) %>_[anything]_[anything].png</pre> instead.
    </div>

    <div>To fix this issue, create the missing subfolders and move your assets inside it.</div>

    <div>
        <b>Don't forget to update your map to reference the new paths!</b>
        For example, change the paths to the decals by right-clicking them, edit your playback tutorials, your color grade triggers, Lua cutscene triggers, etc.
    </div>
<% } %>

<% if (request.getParameter("xmls") != null) { %>
    <h2>You have XMLs that are at the wrong place</h2>

    <div>
        The bot expects your XMLs to be in the <pre>Graphics/<%= escapeHtml4(request.getParameter("collabName")) %>xmls/[subfolder]</pre> folder,
        for example <pre>Graphics/<%= escapeHtml4(request.getParameter("collabName")) %>xmls/[subfolder]/ForegroundTiles.xml</pre>.
    </div>

    <div>To fix this issue, create the missing subfolders and move your XMLs inside it. <b>Don't forget to update your map metadata in your map editor to reference the new paths!</b></div>
<% } %>

<% if (request.getParameter("nomap") != null) { %>
    <h2>There is no map in the Maps folder</h2>

    <div>
        Either you forgot to put your map in the zip, or it isn't in the <pre>Maps</pre> folder.
        <ul>
            <li>Check if your map is actually in the zip!</li>
            <li>
                Check that when opening the zip, you directly see a <pre>Maps</pre> folder, and that the map bin is in it.
                <i>If you need to open another folder before getting to <pre>Maps</pre>, that is the issue!</i>
                In that case, build your zip again by selecting everything in your mod folder (including <pre>Maps</pre>), and compressing that.
            </li>
        </ul>
    </div>
<% } %>

<% if (request.getParameter("multiplemaps") != null) { %>
    <h2>There are multiple maps in this zip</h2>

    <div>
        Check that there is a single map bin in <pre>Maps</pre>.
    </div>
<% } %>

<% if (request.getParameter("badmappath") != null) { %>
    <h2>Your map is not in the right folder</h2>

    <div>
        Make sure your map .bin file is placed in <pre>Maps/<%= escapeHtml4(request.getParameter("collabMapName")) %>/[subfolder]/mapname.bin</pre>.
    </div>
<% } %>

<% if (request.getParameter("badenglish") != null) { %>
    <h2>You have English.txt entries with invalid names</h2>

    <div>
        The bot expects all your <pre>English.txt</pre> entries to follow this format:
        <pre><%= escapeHtml4(request.getParameter("collabName")) %>_[prefix]_xxxx</pre>.
    </div>
    <div>
        To fix that, rename the entries that don't follow this format. <b>Don't forget to change anything that refers to them</b>, like dialog cutscene triggers,
        Lua cutscenes, custom tutorial birds, core messages, etc.
    </div>
    <% if (request.getParameter("collabMapName") != null) { %>
        <div>
            To be able to name your map, entries that follow this format are also accepted:
            <pre><%= escapeHtml4(request.getParameter("collabMapName")) %>_[prefix]_xxxx</pre>.
        </div>
    <% } %>
<% } %>

<% if (request.getParameter("misplacedyaml") != null) { %>
    <h2>You have an everest.yaml, but it is in a subfolder</h2>

    <div>
        This is often the sign that you compressed the folder of your mod, and not the contents of it.
        To fix this, go inside your mod folder, select everything, and compress that!
        When you open your zip, you should directly see <pre>everest.yaml</pre>, without needing to open another folder.
    </div>
<% } %>

<% if (request.getParameter("noyaml") != null) { %>
    <h2>You have no everest.yaml, please create one</h2>

    <div>
        <pre>everest.yaml</pre> is mandatory, as it specifies which helpers are required to play your map.
        You can learn more about dependencies <a target="_blank" href="https://github.com/EverestAPI/Resources/wiki/Mod-Structure#using-helper-mods">on the wiki</a>,
        and check your <pre>everest.yaml</pre> is valid using <a target="_blank" href="https://maddie480.ovh/celeste/everest-yaml-validator">this online tool</a>.
    </div>

    <div>
        Lönn allows you to generate and update your <pre>everest.yaml</pre> file in the <b>Map &gt; Dependencies</b> menu.
        If you use Ahorn, you can <a target="_blank" href="https://gamebanana.com/tools/6908">download the Dependency Generator</a> and hit "Generate everest.yaml".
    </div>
<% } %>

<% if (request.getParameter("yamlinvalid") != null) { %>
    <h2>Your everest.yaml seems to have problems</h2>

    <div>
        The bot sent your <pre>everest.yaml</pre> to <a target="_blank" href="https://maddie480.ovh/celeste/everest-yaml-validator">the validator</a>
        and that tool found issues. Send it there yourself to get all the details!
    </div>
<% } %>

<% if (request.getParameter("multiyaml") != null) { %>
    <h2>Your everest.yaml declares multiple mods</h2>

    <div>
        Your <pre>everest.yaml</pre> gives multiple IDs to your mod. You should avoid this, since it causes issues with mod loading and updating, and the only reason why you
        would want to do this is having multiple DLLs in the same mod zip (which you can usually also avoid &#x1f605;).
    </div>

    <div>
        This can also happen when the indentation of your <pre>everest.yaml</pre> file is wrong:
    </div>
    <pre class="codeblock">
- Name: MyMod
Version: 1.0.0
Dependencies:
- Name: Everest
Version: 1.2803.0
- Name: MaxHelpingHand
Version: 1.21.1</pre>
    <div>
        &#x2b06; This <pre>everest.yaml</pre> declares that there are 2 mods in this zip: <pre>MyMod</pre> (depending on <pre>Everest</pre>) and <pre>MaxHelpingHand</pre>.
        In order to have a mod that depends on <pre>MaxHelpingHand</pre> instead, align it properly to have it inside <pre>Dependencies</pre>:
    </div>
    <pre class="codeblock">
- Name: MyMod
Version: 1.0.0
Dependencies:
- Name: Everest
Version: 1.2803.0
- Name: MaxHelpingHand
Version: 1.21.1</pre>
    <div>
        You can send your <pre>everest.yaml</pre> to <a target="_blank" href="https://maddie480.ovh/celeste/everest-yaml-validator">the validator</a>
        to have more details about how it is interpreted!
    </div>
<% } %>

<% if (request.getParameter("missingassets") != null) { %>
    <h2>You use missing decals / parallax stylegrounds in your map</h2>

    <div>
        One of the decals or parallax stylegrounds you're using could not be found anywhere. This can happen if:
        <ul>
            <li>You used a custom decal/styleground from another map by accident: use something else!</li>
            <li>You used your own custom assets, but forgot to include them in your map zip: package them!</li>
            <li>You used a custom asset from a helper but it isn't in your <pre>everest.yaml</pre>: add it there!</li>
            <li>You used a decal/styleground but then moved it to another folder or renamed it: fix the paths in your map (in the styleground window, or by right-clicking decals).</li>
        </ul>
    </div>
<% } %>

<% if (request.getParameter("missingentities") != null) { %>
    <h2>You use missing entities / triggers / effects in your map</h2>

    <div>
        One of the entities, triggers or effects you use couldn't be found anywhere. This can happen if:
        <ul>
            <li>
                You used a custom asset from a helper but it isn't in your <pre>everest.yaml</pre>: add it there or regenerate your <pre>everest.yaml</pre> using the
                <a target="_blank" href="https://gamebanana.com/tools/6908">Dependency Generator</a>.
            </li>
            <li>
                The asset is not publicly available, for example packaged with your map or in a WIP/private helper:
                this can cause issues in the context of collabs/contests, get in touch with the organizers.
            </li>
        </ul>
    </div>
    <div><i>Note: "Effects" refer to the Effects tab of the Stylegrounds window in Ahorn here. Effects are listed together with parallax stylegrounds in Lönn.</i></div>
<% } %>

<% if (request.getParameter("missingfonts") != null) { %>
    <h2>You use characters that are missing from the game's font</h2>

    <div>
        The game does not ship with complete fonts, because that is not necessary and would unnecessarily take space:
        Celeste only uses a small subset of those characters, in particular for languages like Chinese or Korean.
    </div>

    <div>
        Some of the characters you used in your dialogue are not included in the game, so you need to ship them with your mod,
        otherwise they will be missing when you see that dialogue in-game.
        These extra characters take the form of a .fnt file defining the characters,
        and one or multiple .png files containing the characters themselves.
    </div>

    <div>
        You can use <a target="_blank" href="/celeste/font-generator">this tool</a> in order to generate those files
        with only the missing characters. After generating the font, include it in your mod zip like indicated by the tool.
    </div>

    <div>
        You can also run the <pre>--generate-font [language]</pre> command with your dialog file attached to have the Mod Structure Verifier generate those files.
        <pre>language</pre> should be one of <pre>chinese</pre>, <pre>japanese</pre>, <pre>korean</pre>, <pre>renogare</pre> or <pre>russian</pre>.
    </div>

    <div>
        <i>If you already ship the missing characters with your mod,</i> check that they are at the right place (<pre>Mods/yourmod/Dialog/Fonts</pre>),
        with a correct name (one of <pre>chinese.fnt</pre>, <pre>japanese.fnt</pre>, <pre>korean.fnt</pre>, <pre>russian.fnt</pre> or <pre>renogare64.fnt</pre>),
        and check that the .fnt file is valid (it should be in XML format). More details can be found
        <a href="https://github.com/EverestAPI/Resources/wiki/Adding-Custom-Dialogue#custom-font-loading" target="_blank">on the wiki</a>.
    </div>
<% } %>

<div style="height: 30px"/>