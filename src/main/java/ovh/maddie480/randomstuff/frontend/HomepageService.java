package ovh.maddie480.randomstuff.frontend;

import com.google.common.collect.ImmutableMap;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The "servlet" that serves the home page.
 * Since the home page is handled by {@link StaticAssetsAndRouteNotFoundServlet}, that one is the actual servlet,
 * and invokes this method.
 */
public class HomepageService {
    static void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        Map<String, Object> allTheStats;
        try (InputStream is = Files.newInputStream(Paths.get("/shared/weekly-stats.yaml"))) {
            allTheStats = YamlUtil.load(is);
        }

        request.setAttribute("totalRequests", ((Map<Integer, Integer>) allTheStats.get("responseCountPerCode"))
                .values().stream().mapToInt(i -> i).sum());

        request.setAttribute("responseCountPerCode", toSortedListOfPairs(
                (Map<Integer, Integer>) allTheStats.get("responseCountPerCode"),
                Comparator.comparingInt(Pair::getKey)
        ));

        request.setAttribute("callCountPerBot", toSortedListOfPairs(
                ImmutableMap.of(
                        "Custom Slash Commands", (int) allTheStats.get("customSlashCommandsUsage"),
                        "Games Bot", (int) allTheStats.get("gamesBotUsage"),
                        "Timezone Bot (without roles)", (int) allTheStats.get("timezoneBotLiteUsage"),
                        "Timezone Bot (with roles)", (int) allTheStats.get("timezoneBotFullUsage"),
                        "Mod Structure Verifier", (int) allTheStats.get("modStructureVerifierUsage"),
                        "BananaBot", (int) allTheStats.get("bananaBotUsage")
                ),
                Comparator.<Pair<String, Integer>>comparingInt(Pair::getValue).reversed()
        ));

        request.setAttribute("repositoryCallCount", toSortedListOfPairs(
                (Map<String, Integer>) allTheStats.get("githubActionsPerRepository"),
                Comparator.<Pair<String, Integer>>comparingInt(Pair::getValue).reversed()
        ));

        request.setAttribute("gitlabEventCount", allTheStats.get("gitlabActionsCount"));

        PageRenderer.render(request, response, "home", "Maddie's Random Stuff",
                "The website where Maddie throws all of her random stuff, I guess.");
    }

    private static <T, U> List<Pair<T, U>> toSortedListOfPairs(Map<T, U> map, Comparator<Pair<T, U>> comparator) {
        List<Pair<T, U>> result = new ArrayList<>(map.size());
        for (Map.Entry<T, U> entry : map.entrySet()) {
            result.add(Pair.of(entry));
        }
        result.sort(comparator);
        return result;
    }
}
