package com.max480.discord.slashcommandbot;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The PlanningExploit class needs to be at the same place than on the backend to allow deserialization.
 * ... and yes, it is part of a private Discord bot that goes by the very creative name of "Slash Command Bot".
 */
public class SlashCommandBot {
    public static class PlanningExploit implements Serializable {
        private static final long serialVersionUID = 56185131613831863L;

        public final List<ZonedDateTime> exploitTimes = new ArrayList<>();
        public final List<String> principalExploit = new ArrayList<>();
        public final List<String> backupExploit = new ArrayList<>();

        public final Map<String, List<Pair<ZonedDateTime, ZonedDateTime>>> holidays = new HashMap<>();
    }
}
