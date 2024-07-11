package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * BinToJSONService, but reversed. :p
 */
@WebServlet(name = "JSONToBinService", urlPatterns = {"/celeste/json-to-bin"})
public class JSONToBinService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(JSONToBinService.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        byte[] bin;

        try {
            JSONObject json;
            try (InputStream is = req.getInputStream()) {
                json = new JSONObject(new JSONTokener(is));
            }
            bin = toBin(json);
        } catch (Exception e) {
            log.warn("Could not convert JSON to BIN!", e);
            resp.setStatus(400);
            resp.setContentType("text/plain");
            IOUtils.write("Error parsing the JSON!", resp.getWriter());
            return;
        }

        resp.setContentType("application/octet-stream");
        resp.getOutputStream().write(bin);
    }

    private enum AttributeValueType {
        Boolean(0),
        Byte(1),
        Short(2),
        Integer(3),
        Float(4),
        FromLookup(5),
        String(6),
        LengthEncodedString(7);

        private final int value;

        AttributeValueType(int value) {
            this.value = value;
        }
    }

    private static byte[] toBin(JSONObject json) {
        long startTime = System.currentTimeMillis();

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             DataOutputStream bin = new DataOutputStream(os)) {

            writeString(json.getJSONObject("attributes").getString("Header"), bin);
            writeString(json.getJSONObject("attributes").getString("Package"), bin);

            List<String> stringLookupTable;
            { // collect all names for the lookup table
                Set<String> namesBag = new HashSet<>();
                getAllElementNames(json, namesBag);
                stringLookupTable = new ArrayList<>(namesBag);
            }

            EndianUtils.writeSwappedShort(bin, (short) stringLookupTable.size());
            for (String name : stringLookupTable) {
                writeString(name, bin);
            }

            recursiveConvert(bin, json, stringLookupTable, true);

            log.info("Converted input to BIN in {} ms", System.currentTimeMillis() - startTime);
            bin.flush();
            return os.toByteArray();
        } catch (Exception e) {
            log.warn("Could not convert JSON to BIN!", e);
            return null;
        }
    }

    private static void getAllElementNames(JSONObject current, Set<String> bag) throws IOException {
        bag.add(current.getString("name"));

        JSONObject attributes = current.getJSONObject("attributes");
        for (String attribute : attributes.keySet()) {
            bag.add(attribute);
            if (attributes.get(attribute) instanceof String string && !bag.contains(string)) {
                byte[] runLengthEncoded = toRunLengthEncodedString(string);
                if (runLengthEncoded.length >= string.getBytes(UTF_8).length && string.length() >= 5) {
                    bag.add(string);
                }
            }
        }

        for (Object o : current.getJSONArray("children")) {
            getAllElementNames((JSONObject) o, bag);
        }
    }

    // strings are encoded by C# by writing the character count in LEB128 format, then the string itself.

    private static void writeString(String string, DataOutputStream bin) throws Exception {
        byte[] stringBytes = string.getBytes(UTF_8);

        { // write LEB128-encoded number, see https://en.wikipedia.org/wiki/LEB128
            int value = stringBytes.length;
            do {
                int next = value & 0b01111111;
                value >>= 7;
                if (value != 0) {
                    // more bytes to come
                    next |= 0b10000000;
                }
                bin.writeByte(next);
            } while (value != 0);
        }

        // write the string itself now!
        bin.write(stringBytes);
    }

    private static void recursiveConvert(DataOutputStream bin, JSONObject current, List<String> stringLookupTable, boolean first) throws Exception {
        if (!first) {
            EndianUtils.writeSwappedShort(bin, (short) stringLookupTable.indexOf(current.getString("name")));
            recursiveConvertAttributes(bin, current, stringLookupTable);
        } else {
            for (int i = 0; i < 3; i++) bin.write(0);
        }

        JSONArray children = current.getJSONArray("children");
        EndianUtils.writeSwappedShort(bin, (short) children.length());
        for (Object o : children) {
            recursiveConvert(bin, (JSONObject) o, stringLookupTable, false);
        }
    }

    private static void recursiveConvertAttributes(DataOutputStream bin, JSONObject element, List<String> stringLookupTable) throws Exception {
        JSONObject attributes = element.getJSONObject("attributes");
        bin.writeByte(attributes.length());

        for (String name : attributes.keySet()) {
            EndianUtils.writeSwappedShort(bin, (short) stringLookupTable.indexOf(name));

            Object value = attributes.get(name);

            if (value instanceof Boolean bool) {
                bin.writeByte(AttributeValueType.Boolean.value);
                bin.writeBoolean(bool);
            } else if (value instanceof BigDecimal bigDecimal) {
                bin.writeByte(AttributeValueType.Float.value);
                EndianUtils.writeSwappedFloat(bin, bigDecimal.floatValue());
            } else if (value instanceof Integer integer) {
                if (integer >= 0 && integer <= 255) {
                    bin.writeByte(AttributeValueType.Byte.value);
                    bin.writeByte(integer);
                } else if (integer >= Short.MIN_VALUE && integer <= Short.MAX_VALUE) {
                    bin.writeByte(AttributeValueType.Short.value);
                    EndianUtils.writeSwappedShort(bin, integer.shortValue());
                } else {
                    bin.writeByte(AttributeValueType.Integer.value);
                    EndianUtils.writeSwappedInteger(bin, integer);
                }
            } else if (value instanceof String string) {
                byte[] runLengthEncoded = toRunLengthEncodedString(string);
                if (runLengthEncoded.length < string.getBytes(UTF_8).length) {
                    bin.writeByte(AttributeValueType.LengthEncodedString.value);
                    EndianUtils.writeSwappedShort(bin, (short) runLengthEncoded.length);
                    bin.write(runLengthEncoded);
                } else if (stringLookupTable.contains(string)) {
                    bin.writeByte(AttributeValueType.FromLookup.value);
                    EndianUtils.writeSwappedShort(bin, (short) stringLookupTable.indexOf(string));
                } else {
                    bin.writeByte(AttributeValueType.String.value);
                    writeString(string, bin);
                }
            } else {
                throw new IOException("Unrecognized value type: " + attributes.get(name).getClass());
            }
        }
    }

    private static byte[] toRunLengthEncodedString(String s) throws IOException {
        int currentCount = 0;
        int currentCodePoint = -1;

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PrimitiveIterator.OfInt iterator = s.codePoints().iterator();
            while (iterator.hasNext()) {
                int codePoint = iterator.next();
                if (codePoint == currentCodePoint && currentCount < 255) {
                    // add to the current code point
                    currentCount++;
                } else {
                    if (currentCount != 0) {
                        // write previous code point
                        os.write(currentCount);
                        os.write(currentCodePoint);
                    }

                    currentCount = 1;
                    currentCodePoint = codePoint;
                }
            }

            if (currentCount != 0) {
                // write last code point
                os.write(currentCount);
                os.write(currentCodePoint);
            }

            return os.toByteArray();
        }
    }
}
