package com.github.tepagis.privatebots.api.bot;

import static com.github.tepagis.privatebots.api.bot.TestUtils.CREATOR;
import static com.github.tepagis.privatebots.api.bot.TestUtils.USER;
import static com.github.tepagis.privatebots.api.bot.TestUtils.mockFullUpdate;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.telegram.abilitybots.api.db.MapDBContext.offlineInstance;

import com.github.tepagis.privatebots.api.objects.RequestAccess;
import com.github.tepagis.privatebots.api.objects.RequestAccess.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Update;

public class RequestAccessPrivateBotTest {

  private RequestAccessPrivateBotExample bot;
  private DBContext db;
  private MessageSender sender;
  private SilentSender silent;

  @BeforeEach
  public void setUp() {
    db = offlineInstance("db");
    bot = new RequestAccessPrivateBotExample(db);

    sender = mock(MessageSender.class);
    silent = mock(SilentSender.class);
    bot.setSender(sender);
    bot.setSilentSender(silent);
  }

  @AfterEach
  public void tearDown() throws Exception {
    db.clear();
    db.close();
  }

  @Test
  public void testStartByCreator() {
    Update update = mockFullUpdate(bot, CREATOR, "/start");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        "Hello creatorFirst, to get list of requests to check use command: /requests",
        CREATOR.getId());
  }

  @Test
  public void testStartByPublic() {
    Update update = mockFullUpdate(bot, USER, "/start");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        "Hello userFirst, it's private bot with limited access. Please use command: '/request' to request access to private functionality.",
        USER.getId());
  }

  @Test
  public void testStartByRequestedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest();

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        "Hello userFirst, your request to get access registered. Please wait the approval.",
        USER.getId());
  }

  @Test
  public void testStartByApprovedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest(Status.APPROVED);

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        "Hello userFirst, your access already approved. Please use /commands to check available commands.",
        USER.getId());
  }

  @Test
  public void testStartByRejectedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest(Status.REJECTED);

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        "Hello userFirst, your request to get access has been rejected.",
        USER.getId());
  }

  private void addRequest() {
    addRequest(Status.PENDING);
  }

  private void addRequest(Status status) {
    RequestAccess request = new RequestAccess((long) USER.getId());
    request.setStatus(status);
    bot.requests().put(USER.getId(), request);
  }
}