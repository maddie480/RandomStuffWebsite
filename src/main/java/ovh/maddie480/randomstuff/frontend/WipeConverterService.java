package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@WebServlet(name = "WipeConverterService", urlPatterns = {"/celeste/convert-wipe"})
public class WipeConverterService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(WipeConverterService.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long startTime = System.currentTimeMillis();

            JSONArray output;
            try (InputStream is = req.getInputStream()) {
                output = new JSONArray(convertWipeToTriangles(is));
            }

            log.info("Converted wipe to triangles in {} ms", System.currentTimeMillis() - startTime);

            resp.setContentType("application/json");
            IOUtils.write(output.toString(), resp.getWriter());
        } catch (IOException e) {
            log.error("Error parsing the image", e);
            resp.setStatus(400);
            resp.setContentType("text/plain");
            IOUtils.write("Error parsing the image!", resp.getWriter());
        }
    }

    private static Color[][] readImagePixelColors(BufferedImage image) {
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;
        log.debug("Image is {}x{} w/ alpha channel = {}, should total {} px, data buffer has {} px",
                width, height, hasAlphaChannel, width * height, pixels.length / (hasAlphaChannel ? 4 : 3));

        Color[][] result = new Color[width][height];

        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
                result[col][row] = new Color(
                        (int) pixels[pixel + 3] & 0xff, // red
                        (int) pixels[pixel + 2] & 0xff, // green
                        (int) pixels[pixel + 1] & 0xff, // blue
                        (int) pixels[pixel] & 0xff // alpha
                );

                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
                result[col][row] = new Color(
                        (int) pixels[pixel + 2] & 0xff, // red
                        (int) pixels[pixel + 1] & 0xff, // green
                        (int) pixels[pixel] & 0xff // blue
                );

                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }

        return result;
    }

    /**
     * Converts a wipe image to an array of triangles
     * for usage in the game instead of loading full HD sprites.
     * <p>
     * Each triangle is an array of length 3, each point being represented by an [x, y] array.
     *
     * @param input Image to convert to triangles
     * @return An array of triangles, flattened and ready to be written to the bin file
     */
    private static List<Integer> convertWipeToTriangles(InputStream input) throws IOException {
        Path tempFile = Paths.get("/tmp/wipe_converter_" + System.currentTimeMillis());

        try (InputStream is = input;
             OutputStream os = Files.newOutputStream(tempFile)) {

            IOUtils.copy(is, os);
            log.debug("Image downloaded");
        }

        List<Integer> solution1, solution2;

        {
            Color[][] pixels;
            try (InputStream is = Files.newInputStream(tempFile)) {
                pixels = readImagePixelColors(ImageIO.read(is));
            }
            log.debug("Image read for Solution 1");

            solution1 = findTriangles(pixels, true);
            log.debug("Solution 1 computed");
        }

        {
            Color[][] pixels;
            try (InputStream is = Files.newInputStream(tempFile)) {
                pixels = readImagePixelColors(ImageIO.read(is));
            }
            log.debug("Image read for Solution 2");

            solution2 = findTriangles(pixels, false);
            log.debug("Solution 2 computed");
        }

        Files.delete(tempFile);

        return solution1.size() < solution2.size() ? solution1 : solution2;
    }

    /**
     * Runs a function on every pixel of the zone.
     *
     * @param x The left of the area to scan
     * @param y The top of the area to scan
     * @param w The width of the scan
     * @param h The height of the scan
     * @param f The function to run on every pixel of the zone
     */
    private static void scan(int x, int y, int w, int h, BiConsumer<Integer, Integer> f) {
        for (int _y = y; _y < y + h; _y++) {
            for (int _x = x; _x < x + w; _x++) {
                f.accept(_x, _y);
            }
        }
    }

    /**
     * Seeks for triangles going with this strategy:
     * - go through the image looking for a black pixel
     * - extend a rectangle from this pixel
     * - paint the rectangle white and save it in the form of 2 triangles
     * - repeat
     *
     * @param bitmap       The image to convert to triangles
     * @param verticalScan true to go through the image vertically first, false to go through it horizontally first
     * @return An array of triangles
     */
    private static List<Integer> findTriangles(Color[][] bitmap, boolean verticalScan) {
        List<Integer> result = new ArrayList<>();

        // scan the image looking for a black pixel.
        int x = 0, y = 0;
        while (x < bitmap.length && y < bitmap[0].length) {
            if (isBlack(bitmap[x][y])) {
                // black pixel! extend rectangle from here (vertically first, then horizontally first).
                Point dimensions1 = extendRectangleFrom(bitmap, x, y, true);
                Point dimensions2 = extendRectangleFrom(bitmap, x, y, false);

                // we want to keep the rectangle with the biggest area, and remove it from the image.
                Point dimensions;
                if (dimensions1.x * dimensions1.y > dimensions2.x * dimensions2.y) {
                    dimensions = dimensions1;
                } else {
                    dimensions = dimensions2;
                }

                addTrianglesFromRectangle(result, x, y, dimensions.x, dimensions.y);
                scan(x, y, dimensions.x, dimensions.y, (_x, _y) -> bitmap[_x][_y] = Color.WHITE);
            }

            if (verticalScan) {
                // move down, then right if we reached the bottom
                y++;
                if (y >= bitmap[0].length) {
                    y = 0;
                    x++;
                }
            } else {
                // move right, then down if we reached the bottom
                x++;
                if (x >= bitmap.length) {
                    x = 0;
                    y++;
                }
            }
        }

        return result;
    }

    /**
     * Extends a rectangle as far as possible from the given top-left position, until it hits a non-black pixel.
     *
     * @param bitmap       The image to convert to triangles
     * @param x            the starting X position
     * @param y            the starting Y position
     * @param verticalScan true to extend the rectangle vertically first, false to extend it horizontally first
     * @return the width and height of the rectangle found
     */
    private static Point extendRectangleFrom(Color[][] bitmap, int x, int y, boolean verticalScan) {
        int width = 1, height = 1;

        if (verticalScan) {
            // extend height, then width, while the contents of the rectangle are all black.
            while (y + height < bitmap[0].length && isBlack(bitmap[x][y + height])) {
                height++;
            }
            while (x + width < bitmap.length && rectangleIsAllBlack(bitmap, x + width, y, 1, height)) {
                width++;
            }
        } else {
            // extend width, then height, while the contents of the rectangle are all black.
            while (x + width < bitmap.length && isBlack(bitmap[x + width][y])) {
                width++;
            }
            while (y + height < bitmap[0].length && rectangleIsAllBlack(bitmap, x, y + height, width, 1)) {
                height++;
            }
        }

        return new Point(width, height);
    }

    /**
     * Checks if all the pixels in the given rectangle are black.
     *
     * @param bitmap The image to convert to triangles
     * @param x      the starting X position
     * @param y      the starting Y position
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     * @return whether all the pixels in the rectangle are black.
     */
    private static boolean rectangleIsAllBlack(Color[][] bitmap, int x, int y, int width, int height) {
        for (int _y = y; _y < y + height; _y++) {
            for (int _x = x; _x < x + width; _x++) {
                if (!isBlack(bitmap[_x][_y])) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks if the color given is black (or at least somewhat black).
     *
     * @param color the RGBA color to check
     * @return whether the given color is black
     */
    private static boolean isBlack(Color color) {
        return (
                color.getRed() < 128 && color.getGreen() < 128 && color.getBlue() < 128 && color.getAlpha() > 128
        );
    }

    /**
     * Splits a rectangle into 2 triangles.
     *
     * @param x      the starting X position
     * @param y      the starting Y position
     * @param width  the width of the rectangle
     * @param height the height of the rectangle
     */
    private static void addTrianglesFromRectangle(List<Integer> result, int x, int y, int width, int height) {
        /*
        This is the same as this, except flattened into an array:
        const tri1 = [
          [x, y],
          [x + width, y],
          [x + width, y + height],
        ];
        const tri2 = [
          [x, y],
          [x, y + height],
          [x + width, y + height],
        ];
        return [tri1, tri2];
         */
        result.add(x);
        result.add(y);
        result.add(x + width);
        result.add(y);
        result.add(x + width);
        result.add(y + height);
        result.add(x);
        result.add(y);
        result.add(x);
        result.add(y + height);
        result.add(x + width);
        result.add(y + height);
    }
}