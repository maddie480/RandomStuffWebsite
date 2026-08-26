package ovh.maddie480.randomstuff.frontend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Service allowing to trigger and retrieve results of file searches across all Celeste mods.
 */
@WebServlet(name = "CelesteFileSearchService", urlPatterns = {"/celeste/file-search"})
public class CelesteFileSearchService extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(CelesteFileSearchService.class);


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String query = request.getParameter("query");
        String exactS = request.getParameter("exact");

        if (query == null || query.isEmpty() || !Arrays.asList("true", "false").contains(exactS)) {
            response.setStatus(400);
            response.setHeader("Content-Type", "text/plain");
            response.getWriter().write("'query' and 'exact' parameters are missing or invalid!");
            return;
        }
        boolean exact = "true".equals(exactS);

        List<JSONObject> results = CelesteModSearchService.database.stream()
                .map(m -> Arrays.stream(m.files)
                        .filter(f -> Arrays.stream(f.fileListing)
                                .anyMatch(path -> (exact && path.equals(query)) || (!exact && path.contains(query))))
                        .map(f -> Pair.of(m, f))
                        .toList())
                .flatMap(List::stream)
                .map(mf -> {
                    JSONObject object = new JSONObject();
                    object.put("modid", mf.getLeft().id);
                    object.put("fileid", mf.getRight().id);
                    return object;
                })
                .toList();

        JSONArray array = new JSONArray(results);

        response.setHeader("Content-Type", "application/json");
        array.write(response.getWriter());
    }
}
