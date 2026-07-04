<%@ page import="static org.apache.commons.text.StringEscapeUtils.escapeHtml4" %>
<%@ page import="org.apache.commons.lang3.tuple.Triple" %>
<%@ page import="java.util.List" %>

<h1 class="mt-4"><%= request.getAttribute("title") %></h1>

<span class="hidden" id="program"><%= request.getParameter("program") %></span>

<p>
    This page shows the translations for <%= request.getAttribute("programName") %> in English and
    in the language of your choice, side-to-side. This can be used for proofreading, or
    to find out which dialog keys are untranslated.
</p>

<p>
    <% if (request.getParameter("program").equals("everest")) { %>
        In order to update these translations, edit
        <a href="https://github.com/EverestAPI/Everest/tree/dev/Celeste.Mod.mm/Content/Dialog" target="_blank">the Everest dialog files</a>
        and submit a pull request.
    <% } else if (request.getParameter("program").equals("olympus")) { %>
    In order to update these translations, edit
    <a href="https://github.com/EverestAPI/Olympus/blob/main/src/lang.lua" target="_blank">the language file</a> and/or
    <a href="https://github.com/EverestAPI/Olympus/blob/main/sharp/CmdUpdateAllMods.cs#L16" target="_blank">the mod updater translation map</a>
    and submit a pull request.
    <% } else if (request.getParameter("program").equals("cu2")) { %>
    In order to update these translations, edit
    <a href="https://github.com/EverestAPI/CelesteCollabUtils2/tree/master/Dialog" target="_blank">the dialog files</a>
    and submit a pull request.
    <% } else if (request.getParameter("program").equals("evm")) { %>
    In order to update these translations, edit
    <a href="https://github.com/maddie480/ExtendedVariantMode/tree/master/Dialog" target="_blank">the dialog files</a>
    and submit a pull request.
    <% } %>
</p>

<div class="toggle">
    <a href="?program=everest" role="button" class="btn <%= "everest".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Everest</a>
    <a href="?program=olympus" role="button" class="btn <%= "olympus".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Olympus</a>
    <a href="?program=cu2" role="button" class="btn <%= "cu2".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Collab Utils 2</a>
    <a href="?program=evm" role="button" class="btn <%= "evm".equals(request.getParameter("program")) ? "btn-primary" : "btn-link" %>">Extended Variant Mode</a>
</div>

<table class="table">
    <tr>
        <th>Dialog ID</th>
        <th><%= request.getAttribute("leftLang") %></th>
        <th>
            <select class="form-select">
                <% for (String lang : (List<String>) request.getAttribute("langs")) { %>
                    <option value="<%= escapeHtml4(lang) %>" <%= request.getAttribute("rightLang").equals(lang) ? "selected" : "" %>>
                        <%= escapeHtml4(lang) %>
                    </option>
                <% } %>
            </select>
        </th>
    </tr>
    <% for (Triple<String, String, String> entry : (List<Triple<String, String, String>>) request.getAttribute("entries")) { %>
        <tr>
            <td><code><%= escapeHtml4(entry.getLeft()) %></code></td>
            <td>
                <% if (entry.getMiddle().isEmpty()) { %>
                    <b>Missing</b>
                <% } else { %>
                    <code><%= escapeHtml4(entry.getMiddle()) %></code>
                <% } %>
            </td>
            <td>
                <% if (entry.getRight().isEmpty()) { %>
                    <b>Missing</b>
                <% } else { %>
                    <code><%= escapeHtml4(entry.getRight()) %></code>
                <% } %>
            </td>
        </tr>
    <% } %>
</table>

<script src="/js/translation-viewer.js"></script>