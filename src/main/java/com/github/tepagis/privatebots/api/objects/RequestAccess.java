package com.github.tepagis.privatebots.api.objects;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestAccess implements Serializable {

  private Long requesterChatId;
  private Status status;

  public RequestAccess(Long requesterChatId) {
    this.requesterChatId = requesterChatId;
    this.status = Status.PENDING;
  }

  public enum Status implements Serializable {
    PENDING,
    APPROVED,
    REJECTED,
    REVOKED
  }

}
