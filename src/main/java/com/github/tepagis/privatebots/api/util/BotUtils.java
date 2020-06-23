package com.github.tepagis.privatebots.api.util;

import static java.util.ResourceBundle.Control.FORMAT_PROPERTIES;
import static java.util.ResourceBundle.Control.getNoFallbackControl;
import static java.util.ResourceBundle.getBundle;

import com.google.common.base.Strings;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotUtils {

  public static final String DEFAULT_BUNDLE_NAME = "com.github.tepagis.privatebots.api.BotsMessages";

  /**
   * Copy of {@link org.telegram.abilitybots.api.util.AbilityUtils#getLocalizedMessage(String,
   * Locale, Object...)} with additional argument {@code bundleName} to be able to load messages
   * from different sources.
   *
   * @return localized message based on provided locale or default message.
   */
  public static String getLocalizedMessage(String bundleName,
      String messageCode, Locale locale, Object... arguments) {
    ResourceBundle bundle;
    if (locale == null) {
      bundle = getBundle(bundleName, Locale.ROOT);
    } else {
      try {
        bundle = getBundle(
            bundleName,
            locale,
            getNoFallbackControl(FORMAT_PROPERTIES));
      } catch (MissingResourceException e) {
        bundle = getBundle(bundleName, Locale.ROOT);
      }
    }
    String message = bundle.getString(messageCode);
    return MessageFormat.format(message, arguments);
  }

  public static String getLocalizedMessage(String bundleName,
      String messageCode, String languageCode, Object... arguments) {
    Locale locale =
        Strings.isNullOrEmpty(languageCode) ? null : Locale.forLanguageTag(languageCode);
    return getLocalizedMessage(bundleName, messageCode, locale, arguments);
  }

}
