package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

@WebServlet(name = "LNJEmotesService", urlPatterns = {"/lnj-emotes"})
public class LNJEmotesService extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // same emotes as https://github.com/maddie480/RandomBackendStuff/blob/main/src/main/java/ovh/maddie480/randomstuff/backend/streams/features/CustomEmotes.java
        req.setAttribute("emotes",
                Arrays.stream("""
                        1000tipla;854303924365688832
                        bigrigs;852891851002609716
                        taxi2;854315889351327754
                        verajones;852891183494856744
                        assassinscreedunity;854308657097080853
                        yourewinner;589132274138873856
                        boyard1;852956563005374523
                        boyard2;852956805007933440
                        burgerking;852894749413212190
                        cheetahmen;854307636581629992
                        chirac;852892791704649790
                        chirac2;852886742494216242
                        danyboon;854310353893326858
                        davilex;589135269219926016
                        davilex1;854312377101320192
                        davilex2;854312419173335041
                        passepartout;852896613592596520
                        hatoful;852957087607816232
                        homer;852974507744690217
                        lesvisiteurs;854317578763501588
                        lesvisiteurs2;854317610518446131
                        ljn;649739143844462593
                        lnj;649738827488952320
                        multipla;854304375941890068
                        navet;587332261817483283
                        navet2;811236063636619294
                        phoenixgames;854311078191038464
                        pizzadude;852893957053743157
                        psychokiller;852895322057605120
                        samantha;852974896216670208
                        slevy;854306615951622144
                        tanner;854309585753735168"""
                        .split("\n")
                ).sorted(Comparator.comparing(l -> l.split(";")[0])).toList());

        PageRenderer.render(req, resp, "lnj-emotes", "Emotes du chat LNJ",
                "La liste de toutes les emotes personnalisées disponibles dans le chat des lives LNJ !");
    }
}
