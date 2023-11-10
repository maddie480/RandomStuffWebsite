<%@ page import="java.util.List, java.time.LocalDate, java.time.format.DateTimeFormatter, java.time.format.FormatStyle, ovh.maddie480.randomstuff.frontend.discord.newspublisher.OlympusNews, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<h1 class="mt-4">Olympus News</h1>

<p>
    These are the Celeste modding news that appeared in <a href="https://gamebanana.com/tools/6449" target="_blank">Olympus</a>,
    a mod manager and installer for the <a href="https://everestapi.github.io" target="_blank">Everest</a> mod loader.
    The news feed is managed by Nyan, touhoe and cellularAutomaton (ppnyan, touhoe and cellularautomaton on Discord), and published through
    <a href="https://github.com/EverestAPI/EverestAPI.github.io/tree/main/olympusnews" target="_blank">the GitHub repository of the Everest website</a>.
</p>

<p>
    This news feed is also available <a href="/celeste/olympus-news.json" target="_blank">in JSON format</a>
    and <a href="/celeste/olympus-news.xml" target="_blank">as an RSS / Atom feed</a>.
</p>

<div class="row">
    <% for (OlympusNews news : (List<OlympusNews>) request.getAttribute("news")) { %>
        <div class="col-xl-4 col-md-6 col-sm-12">
            <div class="card">
                <% if (news.image() != null) { %>
                    <img src="<%= escapeHtml4(news.image()) %>" class="card-img-top">
                <% } %>

                <div class="card-body">
                    <% if (news.title() != null) { %>
                        <h5 class="card-title"><%= escapeHtml4(news.title()) %></h5>
                    <% } %>
                    <p class="card-text">
                        <i>
                            Published on
                            <%= escapeHtml4(LocalDate.parse(news.slug().substring(0, 10)).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))) %>
                        </i>

                        <% if (news.shortDescription() != null) { %>
                            <span class="news-description">
                                <%= escapeHtml4(news.shortDescription()) %></p>
                            </span>
                        <% } %>
                    </p>
                    <% if (news.link() != null) { %>
                        <a href="<%= escapeHtml4(news.link()) %>" target="_blank" class="btn btn-primary">Open in new tab</a>
                    <% } %>
                    <% if (news.longDescription() != null) { %>
                        <!-- Button trigger modal -->
                        <button type="button" class="btn btn-secondary" data-bs-toggle="modal" data-bs-target="#modal-<%= escapeHtml4(news.slug()) %>">
                            View more
                        </button>

                        <!-- Modal -->
                        <div class="modal fade" id="modal-<%= escapeHtml4(news.slug()) %>" tabindex="-1" aria-labelledby="#modal-label-<%= escapeHtml4(news.slug()) %>" aria-hidden="true">
                            <div class="modal-dialog modal-lg">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h1 class="modal-title fs-5" id="#modal-label-<%= escapeHtml4(news.slug()) %>"><%= escapeHtml4(news.title()) %></h1>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                    </div>
                                    <div class="modal-body">
                                        <%= escapeHtml4(news.longDescription()) %>
                                    </div>
                                    <div class="modal-footer">
                                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    <% } %>
                </div>
            </div>
        </div>
    <% } %>
</div>

<% int currentPage = (int) request.getAttribute("page"); %>
<% int pageCount = (int) request.getAttribute("pageCount"); %>

<div class="paginator" v-if="this.totalCount > 0">
    <% if (currentPage > 1) { %>
        <a class="btn btn-outline-secondary" href="?page=1">&lt;&lt;</a>
        <a class="btn btn-outline-secondary" href="?page=<%= currentPage - 1 %>">&lt;</a>
    <% } else { %>
        <button class="btn btn-outline-secondary" disabled>&lt;&lt;</button>
        <button class="btn btn-outline-secondary" disabled>&lt;</button>
    <% } %>

    <%= currentPage %> / <%= pageCount %>

    <% if (currentPage < pageCount) { %>
        <a class="btn btn-outline-secondary" href="?page=<%= currentPage + 1 %>">&gt;</a>
        <a class="btn btn-outline-secondary" href="?page=<%= pageCount %>">&gt;&gt;</a>
    <% } else { %>
        <button class="btn btn-outline-secondary" disabled>&gt;</button>
        <button class="btn btn-outline-secondary" disabled>&gt;&gt;</button>
    <% } %>
</div>