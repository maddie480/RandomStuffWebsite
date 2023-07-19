<?xml version="1.0" encoding="UTF-8"?>

<%@ page contentType="application/atom+xml" %>
<%@ page import="java.util.List, com.max480.randomstuff.gae.discord.newspublisher.OlympusNews, static org.apache.commons.text.StringEscapeUtils.escapeXml10, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<feed xmlns="http://www.w3.org/2005/Atom" xml:lang="en">
    <title>Olympus News</title>
    <updated><%= ((List<OlympusNews>) request.getAttribute("news")).get(0).slug().substring(0, 10) %>T00:00:00Z</updated>
    <id>https://maddie480.ovh/celeste/olympus-news.xml</id>
    <link type="text/html" href="https://maddie480.ovh/celeste/olympus-news" rel="alternate"/>

    <% for (OlympusNews news : (List<OlympusNews>) request.getAttribute("news")) { %>
        <entry>
            <id><%= escapeXml10(news.slug()) %></id>
            <published><%= news.slug().substring(0, 10) %>T00:00:00Z</published>
            <updated><%= news.slug().substring(0, 10) %>T00:00:00Z</updated>

            <% if (news.title() != null) { %>
                <title><%= escapeXml10(news.title()) %></title>
            <% } %>

            <% if (news.link() != null) { %>
                <link rel="alternate" type="text/html" href="<%= escapeXml10(news.link()) %>" />
            <% } %>

            <% if (news.shortDescription() != null || news.longDescription() != null || news.image() != null) { %>
                <content type="html">
                    <% if (news.image() != null) { %>
                        &lt;p&gt;&lt;img src="<%= escapeXml10(news.image()) %>" /&gt;&lt;/p&gt;
                    <% } %>
                    <% if (news.shortDescription() != null) { %>
                        &lt;p&gt;<%= escapeXml10(escapeHtml4(news.shortDescription())) %>&lt;/p&gt;
                    <% } %>
                    <% if (news.shortDescription() != null && news.longDescription() != null) { %>
                        &lt;hr&gt;
                    <% } %>
                    <% if (news.longDescription() != null) { %>
                        &lt;p&gt;<%= escapeXml10(escapeHtml4(news.longDescription())) %>&lt;/p&gt;
                    <% } %>
                </content>
            <% } %>
        </entry>
    <% } %>
</feed>