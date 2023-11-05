package com.max480.randomstuff.gae.discord.timezonebot;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;

public class InteractionManager {
    /**
     * A saved timezone from a user of the Timezone Bot without roles.
     */
    public static class UserTimezone implements Serializable {
        @Serial
        private static final long serialVersionUID = 561851525612831863L;

        public String timezoneName;
        public ZonedDateTime expiresAt;
    }
}
