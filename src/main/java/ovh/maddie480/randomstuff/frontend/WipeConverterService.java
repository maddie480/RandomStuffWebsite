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
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * Converts a wipe image to an array of triangles
     * for usage in the game instead of loading full HD sprites.
     * <p>
     * Each triangle is an array of length 3, each point being represented by an [x, y] array.
     *
     * @param input Image to convert to triangles
     * @return An array of triangles, flattened and ready to be written to the bin file
     */
    private static int[] convertWipeToTriangles(InputStream input) throws IOException {
        BufferedImage image = ImageIO.read(input);

        int[] solution1 = findTrianglesAndFlatten(deepCopy(image), true);
        int[] solution2 = findTrianglesAndFlatten(image, false);

        return solution1.length < solution2.length ? solution1 : solution2;
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
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

    private static int[] findTrianglesAndFlatten(BufferedImage bitmap, boolean verticalScan) {
        List<List<Point>> triangles = findTriangles(bitmap, verticalScan);

        // double-flatten (triangles => points => coordinates) to end up with a coordinate list
        int[] result = new int[triangles.size() * 3 /* points in triangle */ * 2 /* coordinates */];
        int index = 0;

        for (List<Point> triangle : triangles) {
            for (Point point : triangle) {
                result[index++] = point.x;
                result[index++] = point.y;
            }
        }

        return result;
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
    private static List<List<Point>> findTriangles(BufferedImage bitmap, boolean verticalScan) {
        List<List<Point>> result = new LinkedList<>();

        // scan the image looking for a black pixel.
        int x = 0, y = 0;
        while (x < bitmap.getWidth() && y < bitmap.getHeight()) {
            if (isBlack(bitmap.getRGB(x, y))) {
                // black pixel! extend rectangle from here (vertically first, then horizontally first).
                Point dimensions1 = extendRectangleFrom(bitmap, x, y, true);
                Point dimensions2 = extendRectangleFrom(bitmap, x, y, false);

                // we want to keep the rectangle with the biggest area, and remove it from the image.
                List<List<Point>> definitiveTriangles;

                Point dimensions;
                if (dimensions1.x * dimensions1.y > dimensions2.x * dimensions2.y) {
                    dimensions = dimensions1;
                } else {
                    dimensions = dimensions2;
                }

                definitiveTriangles = getTrianglesFromRectangle(x, y, dimensions.x, dimensions.y);
                scan(x, y, dimensions1.x, dimensions1.y, (_x, _y) -> {
                    Color origColor = new Color(bitmap.getRGB(_x, _y), true);
                    bitmap.setRGB(_x, _y, new Color(origColor.getRed(), origColor.getGreen(), origColor.getBlue(), 0).getRGB());
                });

                // and save them to the list.
                result.addAll(definitiveTriangles);
            }

            if (verticalScan) {
                // move down, then right if we reached the bottom
                y++;
                if (y >= bitmap.getHeight()) {
                    y = 0;
                    x++;
                }
            } else {
                // move right, then down if we reached the bottom
                x++;
                if (x >= bitmap.getWidth()) {
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
    private static Point extendRectangleFrom(BufferedImage bitmap, int x, int y, boolean verticalScan) {
        int width = 1, height = 1;

        if (verticalScan) {
            // extend height, then width, while the contents of the rectangle are all black.
            while (y + height < bitmap.getHeight() && isBlack(bitmap.getRGB(x, y))) {
                height++;
            }
            while (x + width < bitmap.getWidth() && rectangleIsAllBlack(bitmap, x + width, y, 1, height)) {
                width++;
            }
        } else {
            // extend width, then height, while the contents of the rectangle are all black.
            while (x + width < bitmap.getWidth() && isBlack(bitmap.getRGB(x, y))) {
                width++;
            }
            while (y + height < bitmap.getHeight() && rectangleIsAllBlack(bitmap, x, y + height, width, 1)) {
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
    private static boolean rectangleIsAllBlack(BufferedImage bitmap, int x, int y, int width, int height) {
        AtomicBoolean allBlack = new AtomicBoolean(true);
        scan(x, y, width, height, (_x, _y) -> {
            if (!isBlack(bitmap.getRGB(_x, _y))) {
                allBlack.set(false);
            }
        });

        return allBlack.get();
    }

    /**
     * Checks if the color given is black (or at least somewhat black).
     *
     * @param colorRaw the RGBA color to check
     * @return whether the given color is black
     */
    private static boolean isBlack(int colorRaw) {
        Color color = new Color(colorRaw, true);

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
     * @return a list containing 2 triangles
     */
    private static List<List<Point>> getTrianglesFromRectangle(int x, int y, int width, int height) {
        List<Point> tri1 = Arrays.asList(
                new Point(x, y),
                new Point(x + width, y),
                new Point(x + width, y + height)
        );
        List<Point> tri2 = Arrays.asList(
                new Point(x, y),
                new Point(x, y + height),
                new Point(x + width, y + height)
        );
        return Arrays.asList(tri1, tri2);
    }
}