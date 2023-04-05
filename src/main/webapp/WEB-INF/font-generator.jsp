<h1>Celeste Font Generator</h1>

<% if((boolean) request.getAttribute("error")) { %>
    <div class="alert alert-danger">
        The bitmap font could not be generated. Please check that your font is valid.
    </div>
<% } else if((boolean) request.getAttribute("badrequest")) { %>
    <div class="alert alert-warning">
        Your request was invalid, please try again.
    </div>
<% } else if((boolean) request.getAttribute("allmissing")) { %>
    <div class="alert alert-danger">
        <b>All characters are missing from the font!</b> Make sure you picked the right language.
    </div>
<% } else if((boolean) request.getAttribute("nothingToDo")) { %>
    <div class="alert alert-success">
        <b>All the characters in your dialog file are already present in the vanilla font!</b> You have nothing to do.
    </div>
<% } %>

<p>
    This page allows you to generate files to import missing characters in the Celeste font for your mod,
    or to convert a completely custom font.
    In order to do this, send your dialog file here, then unzip the contents of the zip you're given to <code>Mods/yourmod/Dialog/Fonts</code>.
</p>

<p>
    If characters are missing from the font, you will find a <code>missing-characters.txt</code> file in the generated zip.
</p>

<form method="POST" enctype="multipart/form-data">
    <div class="form-group">
        <label for="method">Generating method</label>
        <select class="form-select" id="method" name="method">
            <option value="bmfont">BMFont (more accurate for vanilla fonts)</option>
            <option value="libgdx">libgdx (faster, supports custom fonts)</option>
        </select>
    </div>

    <div class="alert alert-info" id="bmfont-info" style="display: none">
        For faster generation with BMFont, you can also use the <code>--generate-font [language]</code> command of the
        <a href="/discord-bots#mod-structure-verifier">Mod Structure Verifier</a> Discord bot.
        BMFont is the same tool as the one that was used to generate the fonts for vanilla Celeste.
    </div>

    <div class="form-group" id="font-file-name-field">
        <label for="fontFileName">Font file name (should be unique, for example: maddie480_extendedvariants_korean)</label>
        <input type="text" class="form-control" id="fontFileName" name="fontFileName" required pattern="[^/\\*?:&quot;<>|]+">
    </div>

    <div class="form-group">
        <label for="font">Font</label>
        <select class="form-select" id="font" name="font">
            <option value="japanese">Japanese (Noto Sans CJK JP Medium)</option>
            <option value="korean">Korean (Noto Sans CJK KR Medium)</option>
            <option value="chinese">Simplified Chinese (Noto Sans CJK SC Medium)</option>
            <option value="russian">Russian (Noto Sans Medium)</option>
            <option value="renogare">Other (Renogare)</option>
            <option value="custom" id="customfont-option">Custom Font</option>
        </select>
    </div>

    <div class="form-group" style="display: none" id="fontFileDiv">
        <label for="fontFile">Font file</label>
        <input type="file" class="form-control" accept=".ttf,.otf" id="fontFile" name="fontFile">
    </div>

    <div class="form-group">
        <label for="dialogFile">Dialog file</label>
        <input type="file" class="form-control" accept=".txt" id="dialogFile" name="dialogFile" required>
    </div>

    <input type="submit" class="btn btn-primary" value="Generate">
</form>

<script src="/js/font-generator.js"></script>
