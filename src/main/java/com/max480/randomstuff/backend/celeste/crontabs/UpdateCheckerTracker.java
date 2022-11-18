package com.max480.randomstuff.backend.celeste.crontabs;

import java.io.Serializable;
import java.util.Map;

/**
 * The ModInfo class needs to be at the same place as on the backend to allow deserialization.
 */
public class UpdateCheckerTracker {
    public static class ModInfo implements Serializable {
        private static final long serialVersionUID = -2184804878021343630L;

        public final String type;
        public final int id;
        public final int likes;
        public final int views;
        public final int downloads;
        public final int categoryId;
        public final int createdDate;
        public final Map<String, Object> fullInfo;

        private ModInfo(String type, int id, int likes, int views, int downloads, int categoryId, int createdDate, Map<String, Object> fullInfo) {
            this.type = type;
            this.id = id;
            this.likes = likes;
            this.views = views;
            this.downloads = downloads;
            this.categoryId = categoryId;
            this.createdDate = createdDate;
            this.fullInfo = fullInfo;
        }
    }
}
