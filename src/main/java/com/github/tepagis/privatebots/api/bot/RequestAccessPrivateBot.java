package com.github.tepagis.privatebots.api.bot;

import static org.telegram.abilitybots.api.db.MapDBContext.onlineInstance;

import com.github.tepagis.privatebots.api.objects.RequestAccess;
import com.github.tepagis.privatebots.api.objects.RequestAccess.Status;
import java.util.Map;
import lombok.val;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.abilitybots.api.toggle.AbilityToggle;
import org.telegram.abilitybots.api.toggle.DefaultToggle;
import org.telegram.abilitybots.api.util.AbilityExtension;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class RequestAccessPrivateBot extends AbilityBot {

  private static final String ACCESS_REQUESTS = "ACCESS_REQUESTS";

  private Long creatorChatId;

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      DBContext db, AbilityToggle toggle, DefaultBotOptions botOptions) {
    super(botToken, botUsername, db, toggle, botOptions);
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      AbilityToggle toggle, DefaultBotOptions options) {
    this(botToken, botUsername, onlineInstance(botUsername), toggle, options);
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      DBContext db, AbilityToggle toggle) {
    this(botToken, botUsername, db, toggle, new DefaultBotOptions());
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      DBContext db, DefaultBotOptions options) {
    this(botToken, botUsername, db, new DefaultToggle(), options);
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      DefaultBotOptions botOptions) {
    this(botToken, botUsername, onlineInstance(botUsername), botOptions);
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      AbilityToggle toggle) {
    this(botToken, botUsername, onlineInstance(botUsername), toggle);
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername,
      DBContext db) {
    this(botToken, botUsername, db, new DefaultToggle());
  }

  protected RequestAccessPrivateBot(String botToken, String botUsername) {
    this(botToken, botUsername, onlineInstance(botUsername));
  }

  public AbilityExtension extensions() {
    return new RequestAccessPrivateBotAbilities(this);
  }

  protected SilentSender silentSender() {
    return silent;
  }

  protected Long getCreatorChatId() {
    return creatorChatId == null ? creatorId() : creatorChatId;
  }

  protected void setCreatorChatId(Long creatorChatId) {
    this.creatorChatId = creatorChatId;
  }

  protected Map<Integer, RequestAccess> requests() {
    return db.getMap(ACCESS_REQUESTS);
  }

  protected Map<Integer, User> users() {
    return super.users();
  }

  protected Map<String, Integer> usersByName() {
    return super.userIds();
  }

  protected boolean hasAccess(Integer userId) {
    val requests = requests();
    return userId == creatorId()
      || (requests.containsKey(userId) && Status.APPROVED.equals(requests.get(userId).getStatus()));
  }
}
