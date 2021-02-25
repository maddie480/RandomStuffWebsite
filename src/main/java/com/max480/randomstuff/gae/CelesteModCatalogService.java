package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@WebServlet(name = "CelesteModCatalogService", loadOnStartup = 2, urlPatterns = {
        "/celeste/custom-entity-catalog", "/celeste/custom-entity-catalog-reload"})
public class CelesteModCatalogService extends HttpServlet {

    private final Logger logger = Logger.getLogger("CelesteModCatalogService");

    private static List<QueriedModInfo> modInfo = null;
    private static List<QueriedModInfo> workingModInfo = null;
    private static ZonedDateTime lastUpdated = null;

    // files that should trigger a warning when present in a mod (files that ship with Celeste or Everest)
    private final List<String> BAD_FILE_LIST = Arrays.asList("Celeste.exe",
            "CSteamworks.dll", "Celeste.Mod.mm.dll", "DotNetZip.dll", "FNA.dll", "I18N.CJK.dll", "I18N.MidEast.dll",
            "I18N.Other.dll", "I18N.Rare.dll", "I18N.West.dll", "I18N.dll", "Jdenticon.dll", "KeraLua.dll", "MMHOOK_Celeste.dll", "MojoShader.dll",
            "Mono.Cecil.Mdb.dll", "Mono.Cecil.Pdb.dll", "Mono.Cecil.Rocks.dll", "Mono.Cecil.dll", "MonoMod.RuntimeDetour.dll", "MonoMod.Utils.dll", "NLua.dll",
            "Newtonsoft.Json.dll", "SDL2.dll", "SDL2_image.dll", "Steamworks.NET.dll", "YamlDotNet.dll", "discord-rpc.dll", "fmod.dll", "fmodstudio.dll",
            "libEGL.dll", "libGLESv2.dll", "libjpeg-9.dll", "libpng16-16.dll", "lua53.dll", "steam_api.dll", "zlib1.dll", "Microsoft.Xna.Framework.dll",
            "Microsoft.Xna.Framework.Game.dll", "Microsoft.Xna.Framework.Graphics.dll");

    @Override
    public void init() {
        try {
            reloadList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Loading mod catalog failed: " + e.toString());
            workingModInfo = null;
        }
    }

    /**
     * Formats the name of a Ahorn plugin: dummyAhornName.jl => Dummy Ahorn Name
     *
     * @param input      The Ahorn file name
     * @param dictionary A list of all name overrides
     * @return The name from dictionary if present, or an automatically formatted name
     */
    public static String formatName(String input, Map<String, String> dictionary) {
        // trim the .jl
        input = input.substring(0, input.length() - 3);

        if (dictionary.containsKey(input.toLowerCase())) {
            // the plugin name is in the dictionary
            return dictionary.get(input.toLowerCase());
        }

        // replace - and _ with spaces
        input = input.replace('-', ' ').replace('_', ' ');

        // apply the spaced pascal case from Everest
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(input.charAt(i - 1)))
                builder.append(' ');

            if (i != 0 && builder.charAt(builder.length() - 1) == ' ') {
                builder.append(Character.toUpperCase(c));
            } else {
                builder.append(c);
            }
        }

        String result = builder.toString();
        result = result.substring(0, 1).toUpperCase() + result.substring(1);

        return result;
    }

    /**
     * Replaces everything that's not a letter or number with '-' in the input.
     */
    public static String dasherize(String input) {
        StringBuilder builder = new StringBuilder();

        boolean lastIsDash = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                lastIsDash = false;
                builder.append(Character.toLowerCase(c));
            } else if (!lastIsDash) {
                lastIsDash = true;
                builder.append('-');
            }
        }

        return builder.toString();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getRequestURI().equals("/celeste/custom-entity-catalog-reload")
                && ("key=" + Constants.CATALOG_RELOAD_SHARED_SECRET).equals(request.getQueryString())) {

            reloadList();
        } else if (request.getRequestURI().equals("/celeste/custom-entity-catalog")) {
            if (modInfo == null) {
                request.setAttribute("error", true);
            } else {
                request.setAttribute("error", false);
                request.setAttribute("mods", modInfo);
                request.setAttribute("lastUpdated", lastUpdated.format(
                        DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm zzz", Locale.ENGLISH)
                ));
                request.setAttribute("modCount", modInfo.size());
                request.setAttribute("entityCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.entityList.size())
                        .sum());
                request.setAttribute("triggerCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.triggerList.size())
                        .sum());
                request.setAttribute("effectCount", modInfo.stream()
                        .mapToLong(modInfo -> modInfo.effectList.size())
                        .sum());
            }
            request.getRequestDispatcher("/WEB-INF/mod-catalog.jsp").forward(request, response);
        } else {
            response.setStatus(404);
        }
    }

    private void reloadList() {
        new Thread("Custom Entity List Reload") {
            @Override
            public void run() {
                try {
                    reloadListActual();
                } catch (IOException e) {
                    logger.severe("Error while updating database: " + e.toString());
                }
            }
        }.start();
    }

    private void reloadListActual() throws IOException {
        // download the custom entity catalog dictionary.
        final Map<String, String> dictionary;
        {
            Map<String, String> tempdic = new HashMap<>();
            try {
                tempdic = Arrays.stream(IOUtils.toString(new URL("https://raw.githubusercontent.com/max4805/RandomStuffWebsite/main/modcatalogdictionary.txt"), UTF_8).split("\n"))
                        .collect(Collectors.toMap(a -> a.split("=")[0], a -> a.split("=")[1]));
            } catch (Exception e) {
                logger.warning("Could not fetch dictionary for entity names: " + e.toString());
            }
            dictionary = tempdic;
        }

        workingModInfo = new ArrayList<>();

        refreshList();

        // deal with some mods that need special display treatment by hand
        for (QueriedModInfo info : new HashSet<>(workingModInfo)) {
            if (info.itemtype.equals("Gamefile")) {
                switch (info.itemid) {
                    case 10166: // Canyon Helper: one file containing all entities
                        info.entityList.remove("canyon.jl");
                        info.entityList.add("SpinOrb.jl");
                        info.entityList.add("PushBlock.jl");
                        info.entityList.add("ToggleBlock.jl");
                        info.entityList.add("GrannyTemple.jl");
                        info.entityList.add("GrannyMonster.jl");
                        info.entityList.add("GrannyStart.jl");
                        info.entityList.add("UnbreakableStone.jl");

                        info.triggerList.remove("canyon.jl");
                        info.triggerList.add("MonsterTrigger.jl");
                        info.triggerList.add("TempleFallTrigger.jl");

                        break;

                    case 8210: // Cavern Helper: one file containing all entities
                        info.entityList.remove("cavern.jl");
                        info.entityList.add("CrystalBomb.jl");
                        info.entityList.add("IcyFloor.jl");
                        info.entityList.add("FakeCavernHeart.jl");

                        info.triggerList.remove("dialogcutscene.jl");

                        break;

                    case 8283: // Isa's Grab Bag: one file containing all entities
                        info.entityList.remove("isaentities.jl");
                        info.entityList.add("DreamSpinner.jl");
                        info.entityList.add("ColorBlock.jl");
                        info.entityList.add("ColorSwitch.jl");
                        info.entityList.add("IsaNPC.jl");

                        info.triggerList.remove("isatriggers.jl");
                        info.triggerList.add("CoreWindTrigger.jl");
                        info.triggerList.add("VariantTrigger.jl");

                        break;

                    case 7555: // Outback Helper: one file containing all entities
                        info.entityList.remove("outback.jl");
                        info.entityList.add("MovingTouchSwitch.jl");
                        info.entityList.add("Portal.jl");
                        info.entityList.add("TimedTouchSwitch.jl");

                        break;

                    case 11423: // max480's Helping Hand: common prefix to remove
                        replacePrefix(info, "maxHelpingHand");
                        break;

                    case 9486: // Extended Variant Mode: common prefix to remove
                        replacePrefix(info, "extendedVariants");
                        break;

                    case 7891: // Dialog Textbox Trigger: obsolete
                    case 10004: // Simple Cutscenes: obsolete
                        workingModInfo.remove(info);
                        break;
                }
            }

            if (info.itemtype.equals("Map")) {
                switch (info.itemid) {
                    case 204418: // Early Core: obsolete Dialog Cutscene Trigger
                        info.triggerList.remove("dialogcutscene.jl");
                        if (info.entityList.isEmpty() && info.triggerList.isEmpty() && info.effectList.isEmpty()) {
                            workingModInfo.remove(info);
                        }
                        break;
                }
            }

            if (info.itemtype.equals("Tool")) {
                switch (info.itemid) {
                    case 6970: // Ahorn Additives: blacklisted on request
                        workingModInfo.remove(info);
                        break;
                }
            }

            // sort the lists by ascending name.
            info.entityList = info.entityList.stream().map(a -> formatName(a, dictionary)).collect(Collectors.toCollection(TreeSet::new));
            info.triggerList = info.triggerList.stream().map(a -> formatName(a, dictionary)).collect(Collectors.toCollection(TreeSet::new));
            info.effectList = info.effectList.stream().map(a -> formatName(a, dictionary)).collect(Collectors.toCollection(TreeSet::new));
        }

        // sort the list by ascending name.
        workingModInfo.sort(Comparator.comparing(a -> a.modName));

        logger.info("Found " + workingModInfo.size() + " mods.");
        modInfo = workingModInfo;
        workingModInfo = null;
        lastUpdated = ZonedDateTime.now();
    }

    /**
     * Removes a prefix in all the entities/triggers/effects in a given mod.
     *
     * @param info   The mod to remove the prefix from
     * @param prefix The prefix to remove
     */
    private void replacePrefix(QueriedModInfo info, String prefix) {
        for (String s : new HashSet<>(info.entityList)) {
            if (s.startsWith(prefix)) {
                info.entityList.remove(s);
                s = s.substring(prefix.length());
                info.entityList.add(s);
            }
        }
        for (String s : new HashSet<>(info.triggerList)) {
            if (s.startsWith(prefix)) {
                info.triggerList.remove(s);
                s = s.substring(prefix.length());
                info.triggerList.add(s);
            }
        }
        for (String s : new HashSet<>(info.effectList)) {
            if (s.startsWith(prefix)) {
                info.effectList.remove(s);
                s = s.substring(prefix.length());
                info.effectList.add(s);
            }
        }
    }

    /**
     * A small object to hold an itemtype/itemid pair (this identifies a mod uniquely on GameBanana).
     */
    public static class QueriedModInfo {
        public String itemtype;
        public int itemid;
        public String modName;
        public Set<String> entityList = new TreeSet<>();
        public Set<String> triggerList = new TreeSet<>();
        public Set<String> effectList = new TreeSet<>();

        private QueriedModInfo(String itemtype, int itemid) {
            this.itemtype = itemtype;
            this.itemid = itemid;
        }
    }

    private void refreshList() throws IOException {
        // load the entire mod list
        List<String> mods;
        try (InputStream is = new URL(Constants.MOD_FILES_DATABASE_ROOT + "/list.yaml").openStream()) {
            mods = new Yaml().load(is);
        }

        for (String mod : mods) {
            // download this mod's info
            Map<String, Object> fileInfo;
            try (InputStream is = new URL(Constants.MOD_FILES_DATABASE_ROOT + "/" + mod + "/info.yaml").openStream()) {
                fileInfo = new Yaml().load(is);
            }

            // create a QueriedModInfo for it
            QueriedModInfo thisModInfo = new QueriedModInfo(mod.split("/")[0], Integer.parseInt(mod.split("/")[1]));
            thisModInfo.modName = fileInfo.get("Name").toString();
            List<String> files = (List<String>) fileInfo.get("Files");

            // only "report" (call webhooks to warn about a mod having forbidden files) once.
            boolean modWasReported = false;
            // only show files from the first version listed.
            boolean filesWereAlreadyFound = false;

            for (String file : files) {
                // download the file list for this zip
                List<String> fileList;
                try (InputStream is = new URL(Constants.MOD_FILES_DATABASE_ROOT + "/" + mod + "/" + file + ".yaml").openStream()) {
                    fileList = new Yaml().load(is);
                }

                // search for Ahorn plugins if we didn't already find them
                if (!filesWereAlreadyFound) {
                    for (String fileName : fileList) {
                        if (fileName.startsWith("Ahorn/entities/") && fileName.endsWith(".jl")) {
                            thisModInfo.entityList.add(fileName.substring(fileName.lastIndexOf("/") + 1));
                        }
                        if (fileName.startsWith("Ahorn/triggers/") && fileName.endsWith(".jl")) {
                            thisModInfo.triggerList.add(fileName.substring(fileName.lastIndexOf("/") + 1));
                        }
                        if (fileName.startsWith("Ahorn/effects/") && fileName.endsWith(".jl")) {
                            thisModInfo.effectList.add(fileName.substring(fileName.lastIndexOf("/") + 1));
                        }
                    }
                }

                // check for forbidden files if we didn't already find one
                // and any file more recent that Crowd Control (502895)
                if (Integer.parseInt(file) > 502895 && !modWasReported) {
                    modWasReported = checkForForbiddenFiles(thisModInfo, fileList);
                }

                // check if we found plugins!
                if (!thisModInfo.entityList.isEmpty() || !thisModInfo.triggerList.isEmpty() || !thisModInfo.effectList.isEmpty()) {
                    filesWereAlreadyFound = true;
                }
            }

            // add the mod to the custom entity catalog if it has any entity.
            if (filesWereAlreadyFound) {
                workingModInfo.add(thisModInfo);
            }
        }
    }

    private boolean checkForForbiddenFiles(QueriedModInfo mod, List<String> fileList) throws IOException {
        for (String entry : fileList) {
            for (String illegalFile : BAD_FILE_LIST) {
                if (entry.equalsIgnoreCase(illegalFile) || entry.toLowerCase(Locale.ROOT).endsWith("/" + illegalFile.toLowerCase(Locale.ROOT))) {
                    try {
                        WebhookExecutor.executeWebhook(Constants.MAX480_WARNING_WEBHOOK,
                                "<@354341658352943115> :warning: The mod called **" + mod.modName + "** contains a file named `" + illegalFile + "`!" +
                                        " This is illegal <:landeline:458158726558384149>\n:arrow_right: https://gamebanana.com/"
                                        + mod.itemtype.toLowerCase() + "s/" + mod.itemid);
                        WebhookExecutor.executeWebhook(Constants.COLOURSOFNOISE_WARNING_WEBHOOK,
                                ":warning: The mod called **" + mod.modName + "** contains a file named `" + illegalFile + "`!" +
                                        " This is illegal <:landeline:458158726558384149>\n:arrow_right: https://gamebanana.com/"
                                        + mod.itemtype.toLowerCase() + "s/" + mod.itemid);
                    } catch (InterruptedException e) {
                        throw new IOException("Sleep interrupted", e);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
