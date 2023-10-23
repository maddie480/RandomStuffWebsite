package com.max480.randomstuff.gae.quest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Background {
    public final String fileName;
    public final String nameUrlDecoded;
    public String author;
    public final String price;
    public final boolean bought;

    public Background(String line, boolean bought) throws UnsupportedEncodingException {
        String priceCut = line.split(";")[2];
        long priceLong = Long.parseLong(priceCut.substring(0, priceCut.length() - 4));

        fileName = line;
        String name = line.split(";")[0];
        nameUrlDecoded = URLDecoder.decode(name, StandardCharsets.UTF_8);

        author = line.split(";")[1];

        this.bought = bought;
        this.price = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.FRENCH)).format(priceLong) +
                (priceLong == 1 ? " pièce" : " pièces");
    }
}
