package com.max480.randomstuff.gae.discord;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.exceptions.SodiumException;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordProtocolHandler {
    private static final Logger logger = Logger.getLogger("DiscordProtocolHandler");

    private static final LazySodiumJava sodium = new LazySodiumJava(new SodiumJava());

    public static void warmup() {
        try {
            logger.info("Computing sha512 with sodium for warmup: " + DiscordProtocolHandler.sodium.cryptoHashSha512("warmup"));
        } catch (SodiumException e) {
            logger.log(Level.WARNING, "Sodium warmup failed! " + e);
        }
    }

    /**
     * Handles invalid signatures from Discord and ping-pongs.
     *
     * @return The request data if the request can go on, null if it was already handled.
     */
    public static JSONObject validateRequest(HttpServletRequest req, HttpServletResponse resp, String publicKey) throws IOException {
        // we want to check the Discord signature.
        byte[] body = IOUtils.toByteArray(req.getInputStream());
        String signature = req.getHeader("X-Signature-Ed25519");
        String timestamp = req.getHeader("X-Signature-Timestamp");

        byte[] timestampBytes = timestamp == null ? new byte[0] : timestamp.getBytes(StandardCharsets.UTF_8);
        byte[] signedStuff = new byte[timestampBytes.length + body.length];
        System.arraycopy(timestampBytes, 0, signedStuff, 0, timestampBytes.length);
        System.arraycopy(body, 0, signedStuff, timestampBytes.length, body.length);

        if (signature == null || timestamp == null ||
                !sodium.cryptoSignVerifyDetached(
                        hexStringToByteArray(signature),
                        signedStuff, signedStuff.length,
                        hexStringToByteArray(publicKey))) {

            // signature bad!
            logger.warning("Invalid or absent signature!");
            resp.setStatus(401);
            return null;
        } else {
            // we're good! we can go on.
            // we know we're going to answer with JSON so slap the header now.
            resp.setContentType("application/json");

            JSONObject data = new JSONObject(new String(body, StandardCharsets.UTF_8));
            logger.fine("Message contents: " + data.toString(2));

            if (data.getInt("type") == 1) {
                // ping => pong
                logger.fine("Ping => Pong");
                resp.getWriter().write("{\"type\": 1}");
                return null;
            } else {
                return data;
            }
        }
    }

    /**
     * Decodes a hex string ("28ba") to a byte array ([0x28, 0xBA]).
     *
     * @param string The string to decode
     * @return The decoded string
     */
    private static byte[] hexStringToByteArray(String string) {
        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String part = string.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(part, 16);
        }

        return bytes;
    }
}
