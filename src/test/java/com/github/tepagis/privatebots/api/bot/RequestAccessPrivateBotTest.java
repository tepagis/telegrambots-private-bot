package com.github.tepagis.privatebots.api.bot;

import static com.github.tepagis.privatebots.api.bot.RequestAccessPrivateBotExample.PRIVATE_COMMAND;
import static com.github.tepagis.privatebots.api.bot.TestUtils.CREATOR;
import static com.github.tepagis.privatebots.api.bot.TestUtils.USER;
import static com.github.tepagis.privatebots.api.bot.TestUtils.USER2;
import static com.github.tepagis.privatebots.api.bot.TestUtils.mockFullUpdate;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.telegram.abilitybots.api.db.MapDBContext.offlineInstance;
import static org.telegram.abilitybots.api.util.Pair.of;

import com.github.tepagis.privatebots.api.objects.RequestAccess;
import com.github.tepagis.privatebots.api.objects.RequestAccess.Status;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.abilitybots.api.util.Pair;
import org.telegram.telegrambots.meta.api.objects.Update;

public class RequestAccessPrivateBotTest {

  private RequestAccessPrivateBotExample bot;
  private DBContext db;
  private SilentSender silent;

  @BeforeEach
  public void setUp() {
    db = offlineInstance("db");
    bot = new RequestAccessPrivateBotExample(db);

    silent = mock(SilentSender.class);
    bot.setSender(mock(MessageSender.class));
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
        format(
            "Hello %s, to get list of requests to check use command: /%s",
            CREATOR.getFirstName(), "requests"),
        CREATOR.getId());
  }

  @Test
  public void testStartByPublic() {
    Update update = mockFullUpdate(bot, USER, "/start");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format(
            "Hello %s, it's private bot with limited access. Please use command: '/%s' to request access to private functionality.",
            USER.getFirstName(), "request"),
        USER.getId());
  }

  @Test
  public void testStartByRequestedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest();

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format(
            "Hello %s, your request to get access registered. Please wait the approval.",
            USER.getFirstName()),
        USER.getId());
  }

  @Test
  public void testStartByApprovedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest(Status.APPROVED);

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format(
            "Hello %s, your access already approved. Please use /%s to check available commands.",
            USER.getFirstName(), "commands"),
        USER.getId());
  }

  @Test
  public void testStartByRejectedUser() {
    Update update = mockFullUpdate(bot, USER, "/start");
    addRequest(Status.REJECTED);

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format(
            "Hello %s, your request to get access has been rejected.",
            USER.getFirstName()),
        USER.getId());
  }

  @Test
  public void testRequests() {
    Update update = mockFullUpdate(bot, CREATOR, "/requests");
    addRequest();
    addRequest(USER2.getId(), Status.APPROVED);

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format("Pending requests: \n%s\nHandled requests: \n%s",
            "userFirst userLast (id: 2): PENDING",
            "user2First user2Last (id: 3): APPROVED"),
        CREATOR.getId());
  }

  @Test
  public void testRequest() {
    Update update = mockFullUpdate(bot, USER, "/request");

    bot.onUpdateReceived(update);

    assertSendingMsgToUsers(
        of(
            CREATOR.getId(),
            format(
                "The user=%1$s (id=%2$s) requests access. To approve use command: /%3$s %2$s. to reject: /%4$s %2$s",
                USER.getFirstName(), USER.getId(), "approve", "reject")),
        of(
            USER.getId(),
            format(
                "Hello %s, your request to get access registered. Please wait the approval.",
                USER.getFirstName()
            ))
    );
  }

  @Test
  public void testRequestPending() {
    Update update = mockFullUpdate(bot, USER, "/request");
    addRequest();

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(
        format("Hello %s, your request to get access registered. Please wait the approval.",
            USER.getFirstName()),
        USER.getId());
  }

  @Test
  public void testApprovePending() {
    Update update = mockFullUpdate(bot, CREATOR, "/approve " + USER.getId());
    addRequest();

    bot.onUpdateReceived(update);

    assertSendingMsgToUsers(
        of(
            USER.getId(),
            format(
                "Hello %s, your access already approved. Please use /%s to check available commands.",
                USER.getFirstName(), "commands")),
        of(
            CREATOR.getId(),
            format(
                "The request from user=%s has been approved",
                USER.getId()
            ))
    );
  }

  @Test
  public void testRejectPending() {
    Update update = mockFullUpdate(bot, CREATOR, "/reject " + USER.getId());
    addRequest();

    bot.onUpdateReceived(update);

    assertSendingMsgToUsers(
        of(
            USER.getId(),
            format(
                "Hello %s, your request to get access has been rejected.",
                USER.getFirstName())),
        of(
            CREATOR.getId(),
            format(
                "The request from user=%s has been rejected. User was informed.",
                USER.getId()
            ))
    );
  }

  @Test
  public void testRevoke() {
    Update update = mockFullUpdate(bot, CREATOR, "/revoke " + USER.getId());
    addRequest(Status.APPROVED);

    bot.onUpdateReceived(update);

    assertSendingMsgToUsers(
        of(
            USER.getId(),
            format(
                "Hello %s, your request to get access has been rejected.",
                USER.getFirstName())),
        of(
            CREATOR.getId(),
            format(
                "Access has been revoked for user=%s. User was informed.",
                USER.getId()
            ))
    );
  }

  @Test
  public void testPrivateCommandByApprovedUser() {
    Update update = mockFullUpdate(bot, USER, "/" + PRIVATE_COMMAND);
    addRequest(Status.APPROVED);

    bot.onUpdateReceived(update);

    verify(silent, times(1))
        .send("Private command executed successfully", USER.getId());
  }

  @Test
  public void testPrivateCommandByCreator() {
    Update update = mockFullUpdate(bot, CREATOR, "/" + PRIVATE_COMMAND);

    bot.onUpdateReceived(update);

    verify(silent, times(1))
        .send("Private command executed successfully", CREATOR.getId());
  }

  @Test
  public void testPrivateCommandNoAccessWithoutRequest() {
    Update update = mockFullUpdate(bot, USER, "/" + PRIVATE_COMMAND);

    bot.onUpdateReceived(update);

    verify(silent, times(1))
        .send("Don't have access to private command", USER.getId());
  }

  @Test
  public void testPrivateCommandPending() {
    Update update = mockFullUpdate(bot, USER, "/" + PRIVATE_COMMAND);
    addRequest();

    bot.onUpdateReceived(update);

    verify(silent, times(1))
        .send("Don't have access to private command", USER.getId());
  }

  @SafeVarargs
  private final void assertSendingMsgToUsers(Pair<Integer, String>... expectedMessages) {
    val sendMsgCaptor = ArgumentCaptor.forClass(String.class);
    val chatIdsCaptor = ArgumentCaptor.forClass(Long.class);

    verify(silent, times(expectedMessages.length))
        .send(sendMsgCaptor.capture(), chatIdsCaptor.capture());

    val actualText = sendMsgCaptor.getAllValues();
    val actualChatIds = chatIdsCaptor.getAllValues();

    for (int i = 0; i < expectedMessages.length; i++) {
      val expectedMsg = expectedMessages[i];
      assertEquals((long) expectedMsg.a(), actualChatIds.get(i), "Wrong chatId for message=" + i);
      assertEquals(expectedMsg.b(), actualText.get(i), "Wrong text for message=" + i);
    }
  }

  private void addRequest() {
    addRequest(Status.PENDING);
  }

  private void addRequest(Status status) {
    addRequest(USER.getId(), status);
  }

  private void addRequest(Integer userId, Status status) {
    RequestAccess request = new RequestAccess((long) userId);
    request.setStatus(status);
    bot.requests().put(userId, request);
  }
}