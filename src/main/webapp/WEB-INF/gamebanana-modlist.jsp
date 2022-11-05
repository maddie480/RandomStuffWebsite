<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.GameBananaArbitraryModAppService.ModInfo, static org.apache.commons.text.StringEscapeUtils.escapeHtml4, static org.apache.commons.text.StringEscapeUtils.escapeEcmaScript"%>

<%@page session="false"%>

<!-- CSS fixup due to relative font sizes behaving weird -->
<style>
    @media(max-width: 400px) {
        #tpm__sArbitraryModsModule span, #tpm__sArbitraryModsModule .PageModule {
            font-size: 12.8px;
        }
        #tpm__sArbitraryModsModule h2 {
            font-size: 16px;
        }
    }
    @media(min-width: 401px) {
        @media(max-width: 600px) {
            #tpm__sArbitraryModsModule span, #tpm__sArbitraryModsModule .PageModule {
                font-size: 14.4px;
            }
            #tpm__sArbitraryModsModule h2 {
                font-size: 18px;
            }
        }
    }
    @media(min-width: 601px) {
        #tpm__sArbitraryModsModule span, #tpm__sArbitraryModsModule .PageModule {
            font-size: 16px;
        }
        #tpm__sArbitraryModsModule h2 {
            font-size: 20px;
        }
    }
    #tpm__sArbitraryModsModule .Stats span {
        font-size: 12.8px;
    }
    #tpm__sArbitraryModsModule .Stats span {
        font-size: 12.8px;
    }
    #tpm__sArbitraryModsModule .Record .Ownership img {
        height: 32px;
        width: 32px;
        max-width: 32px;
    }
    #tpm__sArbitraryModsModule .Record .Avatar {
        width: 32px;
    }
</style>

<module class="PageModule StrangeBerryModule">
    <h2><%= escapeHtml4((String) request.getAttribute("title")) %></h2>

    <div class="Content">
        <div class="RecordsGrid">
            <% int index = 0; %>
            <% for (ModInfo modInfo : (List<ModInfo>) request.getAttribute("modList")) { %>
                <div class="Record Flow ModRecord record-<%= index++ %> ">
                    <div class="Preview">
                        <a href="<%= escapeHtml4(modInfo.url) %>" class="Preview ModPreview">
                            <img loading="lazy" class="PreviewImage" alt="<%= escapeHtml4(modInfo.name) %>" src="<%= escapeHtml4(modInfo.image) %>" decoding="async" width="220" height="90">
                        </a>

                        <div class="Ownership">
                            <a class="Avatar" href="<%= escapeHtml4(modInfo.submitterUrl) %>">
                                <img loading="lazy" src="<%= escapeHtml4(modInfo.submitterAvatar) %>" alt="<%= escapeHtml4(modInfo.submitterName) %> avatar" width="32" height="32">
                            </a>
                        </div>

                        <div class="CategoryWrapper">
                            <img loading="lazy" alt="<%= escapeHtml4(modInfo.categoryName) %> icon" class="RootCategoryIcon" src="<%= escapeHtml4(modInfo.categoryIcon) %>" width="32" height="32">
                            <spriteicon class="SubmissionType Mod"></spriteicon><img loading="lazy" alt="<%= escapeHtml4(modInfo.gameName) %> icon" class="GameIcon" src="<%= escapeHtml4(modInfo.gameIcon) %>" width="32" height="32">
                        </div>
                    </div>

                    <div class="Identifiers">
                        <a class="Name" href="<%= escapeHtml4(modInfo.url) %>">
                            <span><%= escapeHtml4(modInfo.name) %></span>
                        </a>
                    </div>

                    <div class="Stats Cluster">
                        <span class="submitted">
                            <spriteicon class="MiscIcon SubmitIcon"></spriteicon>
                            <time class="<%= modInfo.dateAddedClass %>" datetime="<%= modInfo.dateAdded %>"><%= modInfo.dateAddedRelative %></time>
                        </span>
                        <% if (modInfo.dateUpdated != null) { %>
                            <span class="updated">
                                <spriteicon class="SubnavigatorIcon UpdatesIcon"></spriteicon>
                                <time class="<%= modInfo.dateUpdatedClass %>" datetime="<%= modInfo.dateUpdated %>"><%= modInfo.dateUpdatedRelative %></time>
                            </span>
                        <% } %>
                    </div>

                    <div class="Stats Cluster">
                        <% if (!"0".equals(modInfo.likeCount)) { %>
                            <span class="likes">
                                <spriteicon class="MiscIcon LikeIcon"></spriteicon> <itemcount><%= modInfo.likeCount %></itemcount>
                            </span>
                        <% } %>

                        <% if (!"0".equals(modInfo.viewCount)) { %>
                            <span class="views">
                                <spriteicon class="MiscIcon SubscribeIcon"></spriteicon> <itemcount><%= modInfo.viewCount %></itemcount>
                            </span>
                        <% } %>

                        <% if (!"0".equals(modInfo.postCount)) { %>
                            <span class="posts">
                                <spriteicon class="SubmissionTypeSmall Post"></spriteicon> <itemcount><%= modInfo.postCount %></itemcount>
                            </span>
                        <% } %>
                    </div>
                </div>
            <% } %>
        </div>
    </div>
</module>

<script>
    <% index = 0; %>
    <% for (ModInfo modInfo : (List<ModInfo>) request.getAttribute("modList")) { %>
        <% int thisIndex = index++; %>

        tippy('#tpm__sArbitraryModsModule .record-<%= thisIndex %> .CategoryWrapper', {
            content: '<div class="BananaTip">'
                + '<div class="Title">Game</div>'
                + '<a href="<%= escapeEcmaScript(escapeHtml4(modInfo.gameUrl)) %>"><%= escapeEcmaScript(escapeHtml4(modInfo.gameName)) %></a>'
                + '<div class="Title">Section</div>'
                + '<a href="<%= escapeEcmaScript(escapeHtml4(modInfo.gameUrl.replace("https://gamebanana.com/games", "https://gamebanana.com/mods/games"))) %>">Mods</a>'
                + '<div class="Title">Root Category</div>'
                + '<a href="<%= escapeEcmaScript(escapeHtml4(modInfo.categoryUrl)) %>"><%= escapeEcmaScript(escapeHtml4(modInfo.categoryName)) %></a>'
                + '</div>',
            allowHTML: true,
            appendTo: document.body,
            interactive: true
        });

        tippy('#tpm__sArbitraryModsModule .record-<%= thisIndex %> .Ownership', {
            content: '<div class="BananaTip">'
                + '<div class="Title">Submitter</div>'
                + '<a href="<%= escapeEcmaScript(escapeHtml4(modInfo.submitterUrl)) %>"><%= escapeEcmaScript(escapeHtml4(modInfo.submitterName)) %></a>'
                + '</div>',
            allowHTML: true,
            appendTo: document.body,
            interactive: true
        });

        tippy('#tpm__sArbitraryModsModule .record-<%= thisIndex %> .submitted', {
            content: '<div class="BananaTip">Submitted: <%= escapeEcmaScript(escapeHtml4(modInfo.dateAdded)) %></div>',
            allowHTML: true,
            appendTo: document.body
        });
        <% if (modInfo.dateUpdated != null) { %>
            tippy('#tpm__sArbitraryModsModule .record-<%= thisIndex %> .updated', {
                content: '<div class="BananaTip">Updated: <%= escapeEcmaScript(escapeHtml4(modInfo.dateUpdated)) %></div>',
                allowHTML: true,
                appendTo: document.body
            });
        <% } %>
    <% } %>

    tippy('#tpm__sArbitraryModsModule .likes', {
        content: '<div class="BananaTip">Likes</div>',
        allowHTML: true,
        appendTo: document.body
    });
    tippy('#tpm__sArbitraryModsModule .views', {
        content: '<div class="BananaTip">Views</div>',
        allowHTML: true,
        appendTo: document.body
    });
    tippy('#tpm__sArbitraryModsModule .posts', {
        content: '<div class="BananaTip">Posts</div>',
        allowHTML: true,
        appendTo: document.body
    });

    <% if ((boolean) request.getAttribute("isMax480")) { %>
        fetch('https://max480-random-stuff.appspot.com/celeste/update-checker-status?widget=true')
            .then(r => r.text())
            .then(r => {
                $('#UpdateCheckerStatus').html(r);
                $('#UpdateCheckerStatusTitle').removeClass('hidden');
                $('#UpdateCheckerStatus').removeClass('hidden');
            });

        <% if ((boolean) request.getAttribute("avatarEnabled")) { %>
            $('#IdentityModule .Avatar img').attr('style', 'width: 128px; height: 128px;');
            $('#IdentityModule .Avatar img').attr('src', 'https://cdn.discordapp.com/attachments/445236692136230943/1009398906381140070/46407af28c31a3157f860a3357537de8.png');
        <% } %>
    <% } %>
</script>
