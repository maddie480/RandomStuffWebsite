package com.max480.randomstuff.gae;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GameBananaArbitraryModAppService {
    /**
     * The settings of a user using the "Show Arbitrary Mods on Profile" app on GameBanana.
     */
    public static class ArbitraryModAppSettings implements Serializable {
        @Serial
        private static final long serialVersionUID = 56185131582831863L;

        public String key;
        public List<String> modList;
        public String title;
    }

}
