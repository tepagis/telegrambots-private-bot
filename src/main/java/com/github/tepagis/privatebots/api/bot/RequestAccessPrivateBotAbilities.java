package com.github.tepagis.privatebots.api.bot;

import static com.github.tepagis.privatebots.api.util.BotUtils.DEFAULT_BUNDLE_NAME;
import static com.github.tepagis.privatebots.api.util.BotUtils.getLocalizedMessage;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_APPROVE_ALREADY;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_APPROVE_SUCCESS;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_NEW_REQUEST;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_REQUESTS;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_REQUEST_REJECT_FAIL;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_REQUEST_REJECT_SUCCESS;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_REQUEST_REVOKE_FAIL;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_REQUEST_REVOKE_SUCCESS;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.CREATOR_START;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.ERROR_WRONG_USER_ID;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.PUBLIC_START;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.REQUESTER_APPROVED;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.REQUESTER_PENDING;
import static com.github.tepagis.privatebots.api.util.PrivateBotMessageCodes.REQUESTER_REJECTED;
import static org.telegram.abilitybots.api.bot.DefaultAbilities.COMMANDS;
import static org.telegram.abilitybots.api.objects.Ability.builder;
import static org.telegram.abilitybots.api.objects.Locality.USER;
import static org.telegram.abilitybots.api.objects.Privacy.CREATOR;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.fullName;
import static org.telegram.abilitybots.api.util.AbilityUtils.shortName;
import static org.telegram.abilitybots.api.util.AbilityUtils.stripTag;

import com.github.tepagis.privatebots.api.objects.RequestAccess;
import com.github.tepagis.privatebots.api.objects.RequestAccess.Status;
import java.util.List;
import java.util.stream.Collectors;
import lombok.val;
import org.apache.commons.lang3.math.NumberUtils;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.MessageContext;
import org.telegram.abilitybots.api.util.AbilityExtension;
import org.telegram.abilitybots.api.util.Pair;
import org.telegram.telegrambots.meta.api.objects.User;

public class RequestAccessPrivateBotAbilities implements AbilityExtension {

  public static final String START = "start";
  public static final String REQUEST = "request";
  public static final String REQUESTS = "requests";
  public static final String APPROVE = "approve";
  public static final String REJECT = "reject";
  public static final String REVOKE = "revoke";

  private final RequestAccessPrivateBot bot;

  public RequestAccessPrivateBotAbilities(RequestAccessPrivateBot bot) {
    this.bot = bot;
  }

  public Ability start() {
    return builder()
        .name(START)
        .locality(USER)
        .privacy(PUBLIC)
        .input(0)
        .action(ctx -> {
          val user = ctx.user();
          val name = shortName(user);

          if (user.getId() == bot.creatorId()) {
            bot.setCreatorChatId(ctx.chatId());
            send(ctx, CREATOR_START, name, REQUESTS);
            return;
          }

          if (!bot.requests().containsKey(user.getId())) {
            send(ctx, PUBLIC_START, name, REQUEST);
            return;
          }

          val request = bot.requests().get(user.getId());
          request.setRequesterChatId(ctx.chatId());
          sendToRequester(user, request);
        })
        .build();
  }

  public Ability request() {
    return builder()
        .name(REQUEST)
        .locality(USER)
        .privacy(PUBLIC)
        .info("Requests access to private functionality")
        .input(0)
        .action(ctx -> {
          val user = ctx.user();

          if (!bot.requests().containsKey(user.getId())) {
            val request = new RequestAccess(ctx.chatId());
            bot.requests().put(user.getId(), request);
            val name = shortName(user);
            sendToCreator(CREATOR_NEW_REQUEST, name, user.getId(), APPROVE, REJECT);
            send(ctx, REQUESTER_PENDING, name);
            return;
          }

          sendToRequester(user, bot.requests().get(user.getId()));
        })
        .build();
  }

  public Ability requests() {
    return builder()
        .name(REQUESTS)
        .locality(USER)
        .privacy(CREATOR)
        .input(0)
        .action(ctx -> {
          val users = bot.users();
          List<String> requests = bot.requests().entrySet().stream()
              .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
              .collect(
                  Collectors.partitioningBy(pair -> Status.PENDING.equals(pair.b().getStatus())))
              // true -> List<Pair<UserId, Request(Pending)>>
              // false -> List<Pair<UserId, Request(Another statuses)>>
              .values().stream()
              .map(entry -> entry.stream()
                  .map(pair -> {
                    val user = users.get(pair.a());
                    return String.format("%s (id: %s): %s", fullName(user), user.getId(),
                        pair.b().getStatus());
                  })
                  .collect(Collectors.joining("\n")))
              .collect(Collectors.toList());

          val pendingRequests = requests.get(1);
          val completedRequests = requests.get(0);
          send(ctx, CREATOR_REQUESTS, pendingRequests, completedRequests);
        })
        .build();
  }

  public Ability approve() {
    return builder()
        .name(APPROVE)
        .locality(USER)
        .privacy(CREATOR)
        .info("Approves request to access from user")
        .input(1)
        .action(ctx -> {
          val userId = parseUserId(ctx.firstArg());
          if (!bot.requests().containsKey(userId)) {
            send(ctx, ERROR_WRONG_USER_ID, ctx.firstArg());
            return;
          }

          val request = bot.requests().get(userId);
          if (Status.APPROVED.equals(request.getStatus())) {
            send(ctx, CREATOR_APPROVE_ALREADY, ctx.firstArg());
            return;
          }

          request.setStatus(Status.APPROVED);

          val requester = bot.users().get(userId);
          sendToRequester(requester, request);

          send(ctx, CREATOR_APPROVE_SUCCESS, ctx.firstArg());
        })
        .build();
  }

  public Ability reject() {
    return builder()
        .name(REJECT)
        .locality(USER)
        .privacy(CREATOR)
        .info("Rejects request to access from user")
        .input(1)
        .action(ctx -> {
          val userId = parseUserId(ctx.firstArg());
          if (!bot.requests().containsKey(userId)) {
            send(ctx, ERROR_WRONG_USER_ID, ctx.firstArg());
            return;
          }

          val request = bot.requests().get(userId);
          if (Status.APPROVED.equals(request.getStatus())) {
            send(ctx, CREATOR_REQUEST_REJECT_FAIL, REVOKE, ctx.firstArg());
            return;
          }

          request.setStatus(Status.REJECTED);

          sendToRequester(bot.users().get(userId), request);
          send(ctx, CREATOR_REQUEST_REJECT_SUCCESS, ctx.firstArg());
        })
        .build();
  }

  public Ability revoke() {
    return builder()
        .name(REVOKE)
        .locality(USER)
        .privacy(CREATOR)
        .info("Revokes access for user")
        .input(1)
        .action(ctx -> {
          val userId = parseUserId(ctx.firstArg());
          if (!bot.requests().containsKey(userId)) {
            send(ctx, ERROR_WRONG_USER_ID, ctx.firstArg());
            return;
          }

          val request = bot.requests().get(userId);
          if (!Status.APPROVED.equals(request.getStatus())) {
            send(ctx, CREATOR_REQUEST_REVOKE_FAIL, REVOKE, ctx.firstArg());
            return;
          }

          request.setStatus(Status.REVOKED);

          sendToRequester(bot.users().get(userId), request);
          send(ctx, CREATOR_REQUEST_REVOKE_SUCCESS, ctx.firstArg());
        })
        .build();
  }

  private Integer parseUserId(String arg) {
    return NumberUtils.isDigits(arg)
        ? Integer.parseInt(arg)
        : bot.usersByName().get(stripTag(arg));
  }

  private void sendToRequester(User user, RequestAccess request) {
    val name = shortName(user);
    val langCode = user.getLanguageCode();
    switch (request.getStatus()) {
      case APPROVED:
        send(request.getRequesterChatId(), langCode, REQUESTER_APPROVED, name, COMMANDS);
        break;
      case PENDING:
        send(request.getRequesterChatId(), langCode, REQUESTER_PENDING, name);
        break;
      default:
        send(request.getRequesterChatId(), langCode, REQUESTER_REJECTED, name);
        break;
    }
  }

  protected void sendToCreator(String msgCode, Object... args) {
    val creator = bot.users().get(bot.creatorId());
    send(bot.getCreatorChatId(), creator.getLanguageCode(), msgCode, args);
  }

  protected void send(MessageContext ctx, String msgCode, Object... args) {
    send(ctx.chatId(), ctx.user().getLanguageCode(), msgCode, args);
  }

  protected void send(Long chatId, String langCode, String msgCode, Object... args) {
    val msg = getLocalizedMessage(DEFAULT_BUNDLE_NAME, msgCode, langCode, args);
    bot.silentSender().send(msg, chatId);
  }
}
