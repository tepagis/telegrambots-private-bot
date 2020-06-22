package com.github.tepagis.privatebots.api.bot;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.telegram.abilitybots.api.objects.Ability.builder;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;

import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;

public class RequestAccessPrivateBotExample extends RequestAccessPrivateBot {

  public static final String PRIVATE_COMMAND = "private";

  RequestAccessPrivateBotExample(DBContext db) {
    super(EMPTY, EMPTY, db);
  }

  @Override
  public int creatorId() {
    return TestUtils.CREATOR.getId();
  }

  public void setSender(MessageSender sender) {
    this.sender = sender;
  }

  public void setSilentSender(SilentSender silent) {
    this.silent = silent;
  }

  public Ability privateCommand() {
    return builder()
        .name(PRIVATE_COMMAND)
        .locality(USER)
        .privacy(PUBLIC)
        .input(0)
        .action(ctx -> {
          if (!hasAccess(ctx.user().getId())) {
            silentSender().send("Don't have access to private command", ctx.chatId());
          }

          silentSender().send("Private command executed successfully", ctx.chatId());
        })
        .build();
  }
}
