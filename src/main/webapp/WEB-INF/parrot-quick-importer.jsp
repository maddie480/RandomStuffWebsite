<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<%@ page import="java.util.Map, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <title>Parrot Quick Importer Online</title>

        <link rel="shortcut icon" href="https://cultofthepartyparrot.com/still/parrots/parrot.png">

        <script src="https://code.jquery.com/jquery-3.6.0.slim.min.js"
                integrity="sha256-u7e5khyithlIdTpu22PHhENmPcRdFiHRjhAuHcs05RI="
                crossorigin="anonymous">
        </script>

    <link rel="stylesheet" href="/css/parrot-quick-importer.css">
    </head>

    <body>
        <div class="content">
            <h1>Parrot Quick Importer Online</h1>

            <form>
                <label for="discord">Discord</label>
                <input id="discord" type="radio" name="type" value="discord" checked="checked">

                <label for="gitlab">GitLab</label>
                <input id="gitlab" type="radio" name="type" value="gitlab">

                <label for="mattermost">Mattermost</label>
                <input id="mattermost" type="radio" name="type" value="mattermost">

                <input id="recherche" type="text" placeholder="Rechercher un parrot">
            </form>

            <% for (Map.Entry<String, String> parrot : ((Map<String, String>) request.getAttribute("parrots")).entrySet()) { %>
            <a class="target" title="<%= escapeHtml4(parrot.getKey()) %>" data-target="<%= escapeHtml4(parrot.getValue()) %>">
                <img src="<%= escapeHtml4(parrot.getValue()) %>" height="32" width="32">
            </a>
            <% } %>
        </div>

        <script type="text/javascript" src="/js/parrot-script.js"></script>
    </body>
</html>
