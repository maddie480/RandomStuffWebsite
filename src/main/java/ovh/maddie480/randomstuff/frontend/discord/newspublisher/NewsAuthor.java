package ovh.maddie480.randomstuff.frontend.discord.newspublisher;

import java.util.Locale;

public record NewsAuthor(int id, String username) {
    public String email() {
        return id + "+" + username.toLowerCase(Locale.ROOT) + "@users.noreply.github.com";
    }
}
