<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ page import="java.util.List, java.util.Map, com.max480.randomstuff.gae.GameBananaArbitraryModAppService.ModInfo, static org.apache.commons.text.StringEscapeUtils.escapeHtml4"%>

<%@page session="false"%>

<module class="PageModule MultipleRowListModule MemberProfileModule SubmissionsListModule">
    <h2><%= escapeHtml4((String) request.getAttribute("title")) %></h2>

    <div class="Content">
        <records class="Grid">
            <columnHeadings>
                <columnHeading></columnHeading>
                <columnHeading>Submission</columnHeading>
                <columnHeading>Stats</columnHeading>
                <columnHeading>Submitter</columnHeading>
            </columnHeadings>

            <% for (ModInfo modInfo : (List<ModInfo>) request.getAttribute("modList")) { %>
                <record>
                    <recordCell class="Preview">
                        <div class="Preview ModPreview">
                            <a href="<%= escapeHtml4(modInfo.url) %>">
                            <img class="PreviewImage lazy" alt="<%= escapeHtml4(modInfo.name) %>" data-src="<%= escapeHtml4(modInfo.image) %>"/></a>
                        </div>
                    </recordCell>
                    <recordCell class="Identifiers">
                        <a class="Name" href="<%= escapeHtml4(modInfo.url) %>">
                            <span><%= escapeHtml4(modInfo.name) %></span>
                        </a>
                    </recordCell>

                    <recordCell class="Stats">
                        <statWrapper class="DateAdded"><spriteIcon class="MiscIcon SubmitIcon"></spriteIcon>
                            <timeSince title="<%= escapeHtml4(modInfo.dateAdded) %>" class="SinceAdded <%= escapeHtml4(modInfo.dateAddedClass) %>">
                                <%= escapeHtml4(modInfo.dateAddedRelative) %>
                            </timeSince>
                        </statWrapper>

                        <% if (modInfo.dateUpdated != null) { %>
                            <statWrapper class="DateUpdated">
                                <spriteIcon class="SubnavigatorIcon UpdatesIcon"></spriteIcon>
                                <timeSince title="<%= escapeHtml4(modInfo.dateUpdated) %>" class="SinceUpdated <%= escapeHtml4(modInfo.dateUpdatedClass) %>">
                                    <%= escapeHtml4(modInfo.dateUpdatedRelative) %>
                                </timeSince>
                            </statWrapper>
                        <% } %>
                    </recordCell>

                    <recordCell class="Category">
                        <img alt="<%= escapeHtml4(modInfo.gameName) %> icon" class="GameIcon lazy" data-src="<%= escapeHtml4(modInfo.gameIcon) %>"/>
                    </recordCell>
                    <div class="BananaTip">
                        <div class="Title">Game</div>
                        <a href="<%= escapeHtml4(modInfo.gameUrl) %>">
                            <%= escapeHtml4(modInfo.gameName) %>
                        </a>
                    </div>

                    <recordCell class="Ownership">
                        <img alt="avatar" class="AvatarIcon lazy" data-src="<%= escapeHtml4(modInfo.submitterAvatar) %>"/>
                        <div class="BananaTip">
                            <div class="Title">Submitter</div>
                            <a class="MemberLink" href="<%= escapeHtml4(modInfo.submitterUrl) %>">
                                <%= escapeHtml4(modInfo.submitterName) %>
                            </a>
                        </div>
                    </recordCell>
                </record>
            <% } %>
        </records>
    </div>
</module>

<script>
    $(".BananaTip").joUninited("joBindBananaTip").joBindBananaTip();
    <% if ("1698143".equals(request.getAttribute("memberId"))) { %>
        $('#SubmissionsListModule record:has(spriteicon.App)').hide();
        fetch('https://max480-random-stuff.appspot.com/celeste/update-checker-status?widget=true')
            .then(r => r.text())
            .then(r => $('#UpdateCheckerStatus').html(r))
            .catch(() => {
                $('#UpdateCheckerStatusTitle').hide();
                $('#UpdateCheckerStatus').hide();
            })
    <% } %>
</script>
