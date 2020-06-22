package com.github.tepagis.privatebots.api.bot;

import javax.validation.constraints.NotNull;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

public class TestUtils {

  public static final User CREATOR = new User(1, "creatorFirst", false, "creatorLast",
      "creatorUsername", null);
  public static final User USER = new User(2, "userFirst", false, "userLast", "username", null);
  public static final User USER2 = new User(3, "user2First", false, "user2Last", "user2name", null);

  @NotNull
  public static Update mockFullUpdate(RequestAccessPrivateBot bot, User user, String args) {
    addUser(bot, USER);
    addUser(bot, CREATOR);
    addUser(bot, USER2);

    Update update = Mockito.mock(Update.class);
    Mockito.when(update.hasMessage()).thenReturn(true);
    Message message = Mockito.mock(Message.class);
    Mockito.when(message.getFrom()).thenReturn(user);
    Mockito.when(message.getText()).thenReturn(args);
    Mockito.when(message.hasText()).thenReturn(true);
    Mockito.when(message.isUserMessage()).thenReturn(true);
    Mockito.when(message.getChatId()).thenReturn((long) user.getId());
    Mockito.when(update.getMessage()).thenReturn(message);
    return update;
  }

  public static void addUser(RequestAccessPrivateBot bot, User user) {
    bot.users().put(user.getId(), user);
    bot.usersByName().put(user.getUserName(), user.getId());
  }
}