package th.mfu.config;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** Looks up UI texts (messages*.properties) in the language of the current request. */
@Component
public class Messages {

    private final MessageSource source;

    public Messages(MessageSource source) {
        this.source = source;
    }

    public String get(String code, Object... args) {
        return source.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public boolean isEnglish() {
        return "en".equals(LocaleContextHolder.getLocale().getLanguage());
    }
}
