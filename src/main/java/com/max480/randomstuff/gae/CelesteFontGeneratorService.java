package com.max480.randomstuff.gae;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.tools.bmfont.BitmapFontWriter;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Servlet allowing to generate bitmap fonts for usage in Celeste (~~and any other game using the XML output of BMFont actually~~).
 */
@WebServlet(name = "CelesteFontGeneratorService", urlPatterns = {"/celeste/font-generator"})
@MultipartConfig
public class CelesteFontGeneratorService extends HttpServlet {
    private static final Logger logger = Logger.getLogger("CelesteFontGeneratorService");


    private static class AllCharactersMissingException extends Exception {
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("error", false);
        request.setAttribute("badrequest", false);
        request.setAttribute("allmissing", false);
        request.setAttribute("nothingToDo", false);

        PageRenderer.render(request, response, "font-generator", "Celeste Font Generator",
                "This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("error", false);
        request.setAttribute("badrequest", false);
        request.setAttribute("allmissing", false);
        request.setAttribute("nothingToDo", false);
        boolean sentZip = false;

        if (!ServletFileUpload.isMultipartContent(request)) {
            // if not, we stop here
            request.setAttribute("badrequest", true);
            logger.warning("Bad request");
            response.setStatus(400);
        } else {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setRepository(new File("/tmp"));
            ServletFileUpload upload = new ServletFileUpload(factory);

            // parse request
            String font = null;
            String fontFileName = null;
            String dialogFile = null;
            String method = null;

            String customFontFileName = null;
            InputStream customFontFile = null;
            try {
                for (FileItem item : upload.parseRequest(request)) {
                    if (item.isFormField()) {
                        // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
                        String fieldname = item.getFieldName();
                        String fieldvalue = item.getString();

                        if ("font".equals(fieldname)) {
                            font = fieldvalue;
                        } else if ("fontFileName".equals(fieldname)) {
                            fontFileName = fieldvalue;
                        } else if ("method".equals(fieldname)) {
                            method = fieldvalue;
                        }
                    } else {
                        // Process form file field (input type="file").
                        String fieldname = item.getFieldName();
                        InputStream filecontent = item.getInputStream();

                        if ("dialogFile".equals(fieldname)) {
                            dialogFile = IOUtils.toString(filecontent, StandardCharsets.UTF_8);
                        } else if ("fontFile".equals(fieldname)) {
                            customFontFileName = item.getName();
                            if (customFontFileName.replace("\\", "/").contains("/")) {
                                customFontFileName = customFontFileName.substring(customFontFileName.replace("\\", "/").indexOf("/") + 1);
                            }
                            customFontFile = filecontent;
                        }
                    }
                }
            } catch (FileUploadException e) {
                logger.warning("Cannot parse request: " + e);
            }

            if ("bmfont".equals(method)) {
                if (font == null || dialogFile == null) {
                    request.setAttribute("badrequest", true);
                    logger.warning("Bad request for generation through BMFont");
                    response.setStatus(400);
                } else {
                    // create the task, and redirect to the page that will allow to follow it
                    String id = runBMFontTask(font, dialogFile);
                    response.setStatus(302);
                    response.setHeader("Location", "/celeste/task-tracker/font-generate/" + id);
                    return;
                }
            } else if (font == null || fontFileName == null || dialogFile == null || hasForbiddenCharacter(fontFileName)
                    || (font.equals("custom") && (customFontFileName == null || hasForbiddenCharacter(customFontFileName)))) {

                request.setAttribute("badrequest", true);
                logger.warning("Bad request for generation through libgdx");
                response.setStatus(400);
            } else {
                try {
                    // we can try generating the font now!
                    byte[] result;
                    if (font.equals("custom")) {
                        result = generateFont(fontFileName, customFontFile, customFontFileName, dialogFile);
                    } else {
                        result = generateFont(fontFileName, font, dialogFile);
                    }

                    if (result.length == 0) {
                        // well... we didn't generate anything.
                        request.setAttribute("nothingToDo", true);
                    } else {
                        // sent the zip to the user.
                        response.setContentType("application/zip");
                        response.setContentLength(result.length);
                        response.setHeader("Content-Disposition", "attachment; filename=\"celeste-bitmap-font.zip\"");
                        IOUtils.write(result, response.getOutputStream());
                        sentZip = true;
                    }
                } catch (AllCharactersMissingException e) {
                    // all characters are missing from the font!
                    request.setAttribute("allmissing", true);

                } catch (ParserConfigurationException | SAXException | IOException e) {
                    // something blew up along the way!
                    logger.severe("Could not generate font!");
                    e.printStackTrace();

                    request.setAttribute("error", true);
                    response.setStatus(500);
                }
            }
        }

        if (!sentZip) {
            // render the HTML page.
            PageRenderer.render(request, response, "font-generator", "Celeste Font Generator",
                    "This tool allows you to generate bitmap fonts in a format appropriate for Celeste mods.");
        }
    }

    private String runBMFontTask(String language, String dialogFile) throws IOException {
        String id = UUID.randomUUID().toString();

        // store timestamp marker (this will save that the task exists, and the timestamp at which it started)
        Path file = Paths.get("/shared/temp/font-generate/" + id + "-timestamp.txt");
        Files.write(file, Long.toString(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));

        // store dialog file
        file = Paths.get("/shared/temp/font-generate/" + id + ".txt");
        Files.write(file, dialogFile.getBytes(StandardCharsets.UTF_8));

        // generate message payload
        JSONObject message = new JSONObject();
        message.put("taskType", "fontGenerate");
        message.put("language", language);
        message.put("fileName", "font-generate-" + id + ".txt");

        // publish the message to the backend!
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("backend", 4480));
            try (OutputStream os = socket.getOutputStream()) {
                IOUtils.write(message.toString(), os, StandardCharsets.UTF_8);
            }
        }

        logger.info("Emitted message to handle font generation task " + id + "!");
        return id;
    }

    /**
     * Checks if a file name contains characters that are forbidden in file names.
     */
    private static boolean hasForbiddenCharacter(String name) {
        return name.contains("/") || name.contains("\\") || name.contains("*") || name.contains("?")
                || name.contains(":") || name.contains("\"") || name.contains("<") || name.contains(">")
                || name.contains("|") || name.contains("\r") || name.contains("\n");
    }

    /**
     * Generates a font from a custom font provided by input stream.
     *
     * @param fontFileName         The base name for the fnt and png files
     * @param font                 The font to use as an input stream
     * @param uploadedFontFileName The name of the font that is provided as an input stream
     * @param dialogFile           The dialog file to take characters from
     * @return The zip containing all the files, as a byte array. Can be empty if no charater was exported
     * @throws AllCharactersMissingException If all characters that are not in existingCodes are not in the font either.
     */
    private static byte[] generateFont(String fontFileName, InputStream font, String uploadedFontFileName, String dialogFile)
            throws IOException, AllCharactersMissingException {

        // create a temp dir to dump stuff to.
        final Path tempDirectory = Files.createTempDirectory("celeste-font-generator-");

        // write the font to that folder.
        final File fontFile = tempDirectory.resolve(uploadedFontFileName).toFile();
        try (OutputStream os = Files.newOutputStream(fontFile.toPath())) {
            IOUtils.copy(font, os);
        }

        return generateFontAndBuildZip(fontFileName + "_image", fontFileName + ".fnt", fontFile, dialogFile, new HashSet<>(), tempDirectory);
    }

    /**
     * Generates a font from one of Celeste's base fonts (that depends on language).
     * Only characters absent from the game's font are exported.
     *
     * @param fontFileName The base name for the png files
     * @param language     One of "russian", "japanese", "korean", "chinese" or "renogare" to pick the font to use and the name for the fnt file
     * @param dialogFile   The dialog file to take characters from
     * @return The zip containing all the files, as a byte array. Can be empty if no charater was exported
     * @throws AllCharactersMissingException If all characters that are not in existingCodes are not in the font either.
     */
    private static byte[] generateFont(String fontFileName, String language, String dialogFile)
            throws IOException, ParserConfigurationException, SAXException, AllCharactersMissingException {

        String fontName;
        String vanillaFntName;
        switch (language) {
            case "russian":
                fontName = "Noto Sans Med.ttf";
                vanillaFntName = "russian.fnt";
                break;
            case "japanese":
                fontName = "Noto Sans CJK JP Medium.otf";
                vanillaFntName = "japanese.fnt";
                break;
            case "korean":
                fontName = "Noto Sans CJK KR Medium.otf";
                vanillaFntName = "korean.fnt";
                break;
            case "chinese":
                fontName = "Noto Sans CJK SC Medium.otf";
                vanillaFntName = "chinese.fnt";
                break;
            default:
                fontName = "Renogare.otf";
                vanillaFntName = "renogare64.fnt";
                break;
        }

        // create a temp dir to dump stuff to.
        final Path tempDirectory = Files.createTempDirectory("celeste-font-generator-");

        // extract the font from classpath to that folder.
        final File fontFile = tempDirectory.resolve(fontName).toFile();
        try (InputStream is = CelesteFontGeneratorService.class.getClassLoader().getResourceAsStream("font-generator/fonts/" + fontName);
             OutputStream os = Files.newOutputStream(fontFile.toPath())) {

            IOUtils.copy(is, os);
        }

        // extract the font XML from classpath to that folder.
        final File fontXml = tempDirectory.resolve(vanillaFntName).toFile();
        try (InputStream is = CelesteFontGeneratorService.class.getClassLoader().getResourceAsStream("font-generator/vanilla/" + vanillaFntName);
             OutputStream os = Files.newOutputStream(fontXml.toPath())) {

            IOUtils.copy(is, os);
        }

        // parse the XML and delete it
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(fontXml);
        Files.delete(fontXml.toPath());

        // get the list of existing codes
        Set<Integer> existingCodes = new HashSet<>();
        NodeList chars = document.getElementsByTagName("char");
        for (int i = 0; i < chars.getLength(); i++) {
            Node charItem = chars.item(i);
            existingCodes.add(Integer.parseInt(charItem.getAttributes().getNamedItem("id").getNodeValue()));
        }

        return generateFontAndBuildZip(fontFileName, vanillaFntName, fontFile, dialogFile, existingCodes, tempDirectory);
    }

    /**
     * Generates a font from a font file on disk, builds a zip, deletes the temp directory, and returns it as a byte array.
     *
     * @param fontFileName  The base name for the png files
     * @param fntName       The name for the fnt file
     * @param font          The font file
     * @param dialogFile    The dialog file to take characters from
     * @param existingCodes Code points for the characters to exclude from the export
     * @param tempDirectory The working directory where files should be written.
     * @return The zip containing all the files, as a byte array. Can be empty if no charater was exported
     * @throws AllCharactersMissingException If all characters that are not in existingCodes are not in the font either.
     */
    private static byte[] generateFontAndBuildZip(String fontFileName, String fntName, File font, String dialogFile,
                                                  Set<Integer> existingCodes, Path tempDirectory) throws IOException, AllCharactersMissingException {
        // generate the bitmap font!
        try {
            String missingCharacters = generateFont(fontFileName, fntName, font, dialogFile, existingCodes);

            if (missingCharacters == null) {
                // all characters are already in the vanilla font!
                FileUtils.deleteDirectory(tempDirectory.toFile());
                return new byte[0];
            } else if (!missingCharacters.isEmpty()) {
                // write the missing characters to missing-characters.txt
                FileUtils.writeStringToFile(tempDirectory.resolve("missing-characters.txt").toFile(),
                        missingCharacters, StandardCharsets.UTF_8);
            }
        } catch (AllCharactersMissingException e) {
            // all characters are missing from the font!
            FileUtils.deleteDirectory(tempDirectory.toFile());
            throw e;
        }

        // delete the font
        Files.delete(font.toPath());

        // and zip the whole thing.
        return zipAndDeleteTempDirectory(tempDirectory);
    }

    /**
     * Generates a font from a font file on disk.
     *
     * @param fontFileName  The base name for the png files
     * @param fntName       The name for the fnt file
     * @param font          The font file
     * @param dialogFile    The dialog file to take characters from
     * @param existingCodes Code points for the characters to exclude from the export
     * @return the list of characters missing from the font, or null if all characters already were in existingCodes.
     * @throws AllCharactersMissingException If all characters that are not in existingCodes are not in the font either.
     */
    private static String generateFont(String fontFileName, String fntName, File font, String dialogFile, Set<Integer> existingCodes)
            throws IOException, AllCharactersMissingException {
        // take all characters that do not exist and jam them all into a single string
        final String missingCharacters = dialogFile.codePoints()
                .filter(c -> !existingCodes.contains(c))
                .mapToObj(c -> new String(new int[]{c}, 0, 1))
                .distinct()
                .filter(s -> s.matches("\\P{C}")) // not control characters!
                .collect(Collectors.joining());

        if (missingCharacters.isEmpty()) {
            // nothing to do! all characters are already in the font.
            return null;
        }

        final Path directory = font.toPath().getParent().toAbsolutePath();

        // generate the font using libgdx
        Semaphore waitUntilFinished = new Semaphore(0);
        AtomicBoolean failure = new AtomicBoolean(false);
        AtomicBoolean empty = new AtomicBoolean(false);
        List<String> missingCharactersInFont = new ArrayList<>();

        final HeadlessApplication app = new HeadlessApplication(new ApplicationAdapter() {
            public void create() {
                try {
                    BitmapFontWriter.FontInfo info = new BitmapFontWriter.FontInfo();
                    info.padding = new BitmapFontWriter.Padding(1, 1, 1, 1);
                    info.face = font.getName().substring(0, font.getName().lastIndexOf("."));
                    info.size = 64;
                    info.aa = 4;
                    info.spacing.horizontal = 1;
                    info.spacing.vertical = 1;

                    FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
                    param.size = 43;
                    param.spaceX = 1;
                    param.spaceY = 1;
                    param.characters = missingCharacters;
                    param.packer = new HackPixmapPacker(256, 256, Pixmap.Format.RGBA8888, 1, false, new HackSkylineStrategy());

                    FreeTypeFontGenerator generator = new FreeTypeFontGenerator(new NoMapFileHandle(font));
                    FreeTypeFontGenerator.FreeTypeBitmapFontData data = generator.generateData(param);

                    final Array<PixmapPacker.Page> pages = param.packer.getPages();
                    Pixmap[] pix = new Pixmap[pages.size];
                    for (int i = 0; i < pages.size; i++) {
                        pix[i] = pages.get(i).getPixmap();
                    }

                    if (pix.length == 0) {
                        empty.set(true);
                        waitUntilFinished.release();
                        return;
                    }

                    BitmapFontWriter.setOutputFormat(BitmapFontWriter.OutputFormat.XML);
                    BitmapFontWriter.writeFont(data, pix, Gdx.files.absolute(directory.resolve(fontFileName + ".fnt").toString()), info);

                    generator.dispose();
                    data.dispose();
                    param.packer.dispose();

                    // find out characters that were missing from the font.
                    List<Character> missingChars = new ArrayList<>();
                    for (char c : missingCharacters.toCharArray()) {
                        if (data.getGlyph(c) == null) {
                            missingChars.add(c);
                        }
                    }
                    char[] missingCharsArray = new char[missingChars.size()];
                    for (int i = 0; i < missingCharsArray.length; i++) {
                        missingCharsArray[i] = missingChars.get(i);
                    }
                    missingCharactersInFont.add(new String(missingCharsArray));

                    waitUntilFinished.release();
                } catch (Exception e) {
                    logger.severe("Could not generate font");
                    e.printStackTrace();
                    failure.set(true);
                    waitUntilFinished.release();
                }
            }
        });

        waitUntilFinished.acquireUninterruptibly();
        app.exit();
        if (empty.get()) {
            throw new AllCharactersMissingException();
        }
        if (failure.get()) {
            throw new IOException("Generating font failed!");
        }

        // rename the fnt file as required
        Files.move(directory.resolve(fontFileName + ".fnt"), directory.resolve(fntName));

        // and... done!
        return missingCharactersInFont.get(0);
    }

    /**
     * Zips the given directory, then deletes it. Does not support subfolders, but it is not needed anyway.
     */
    private static byte[] zipAndDeleteTempDirectory(Path tempDirectory) throws IOException {
        // zip the whole folder.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutput = new ZipOutputStream(output)) {
            for (File f : tempDirectory.toFile().listFiles()) {
                zipOutput.putNextEntry(new ZipEntry(f.getName()));
                try (FileInputStream fileInput = new FileInputStream(f)) {
                    IOUtils.copy(fileInput, zipOutput);
                }
            }
        }

        // delete it.
        FileUtils.deleteDirectory(tempDirectory.toFile());

        // and... done!
        return output.toByteArray();
    }

    /**
     * The map() method from FileHandle seems to be causing the file to always stay open, preventing its cleanup...
     * so we want to nope it out.
     * GdxRuntimeException is expected by FreeTypeFontGenerator, and the fallback it uses does not keep the file open.
     */
    private static class NoMapFileHandle extends FileHandle {
        public NoMapFileHandle(File file) {
            super(file);
        }

        public ByteBuffer map() {
            throw new GdxRuntimeException("nope");
        }
    }

    /*
     * The following is pretty hacky:
     * basically, FreeTypeFontGenerator always expects characters to be added to the last page, except the strategy
     * can add those to previous pages if it fits.
     * So, we temporarily remove pages from the pixmap packer until FreeTypeFontGenerator reads it, so that glyphs
     * get associated to the right page in the fnt file.
     * ... yeah.
     */

    private static class HackSkylineStrategy extends PixmapPacker.SkylineStrategy {
        @Override
        public PixmapPacker.Page pack(PixmapPacker packer, String name, Rectangle rect) {
            PixmapPacker.Page p = super.pack(packer, name, rect);

            int pageIndex = packer.getPages().indexOf(p, true);
            if (packer.getPages().size - 1 > pageIndex) {
                // character wasn't added to the last page! remove pages until we get it.
                Array<PixmapPacker.Page> pages = new Array<>(packer.getPages());
                while (packer.getPages().size - 1 > pageIndex) {
                    packer.getPages().pop();
                }
                // save the actual page list to restore it later.
                ((HackPixmapPacker) packer).actualPages = pages;
            }

            return p;
        }
    }

    private static class HackPixmapPacker extends PixmapPacker {
        private Array<Page> actualPages;

        public HackPixmapPacker(int pageWidth, int pageHeight, Pixmap.Format pageFormat, int padding, boolean duplicateBorder, PackStrategy packStrategy) {
            super(pageWidth, pageHeight, pageFormat, padding, duplicateBorder, packStrategy);
        }

        @Override
        public Array<Page> getPages() {
            if (actualPages == null) {
                return super.getPages();
            } else {
                // restore the actual list of pages, and return the faked one.
                Array<Page> result = new Array<>(super.getPages());
                super.getPages().clear();
                super.getPages().addAll(actualPages);
                actualPages = null;
                return result;
            }
        }
    }
}
