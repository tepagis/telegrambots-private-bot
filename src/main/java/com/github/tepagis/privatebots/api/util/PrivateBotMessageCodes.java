package com.github.tepagis.privatebots.api.util;

public interface PrivateBotMessageCodes {

  String PUBLIC_START = "privatebot.public.start";
  String CREATOR_START = "privatebot.creator.start";
  String CREATOR_NEW_REQUEST = "privatebot.creator.new_request";
  String CREATOR_REQUESTS = "privatebot.creator.requests";
  String CREATOR_APPROVE_ALREADY = "privatebot.creator.approve.already";
  String CREATOR_APPROVE_SUCCESS = "privatebot.creator.approve.success";
  String CREATOR_REQUEST_REJECT_FAIL = "privatebot.creator.reject.fail";
  String CREATOR_REQUEST_REJECT_SUCCESS = "privatebot.creator.reject.success";
  String CREATOR_REQUEST_REVOKE_FAIL = "privatebot.creator.revoke.fail";
  String CREATOR_REQUEST_REVOKE_SUCCESS = "privatebot.creator.revoke.success";
  String REQUESTER_PENDING = "privatebot.requester.pending";
  String REQUESTER_APPROVED = "privatebot.requester.approved";
  String REQUESTER_REJECTED = "privatebot.requester.rejected";
  String ERROR_WRONG_USER_ID = "privatebot.error.wrong_user_id";

}
