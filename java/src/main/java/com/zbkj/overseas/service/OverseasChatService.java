package com.zbkj.overseas.service;

import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
import com.zbkj.overseas.response.ChatMessageResponse;
import com.zbkj.overseas.response.ChatSessionResponse;
import com.zbkj.overseas.response.ChatStartResponse;
import com.zbkj.overseas.response.ChatThreadResponse;

import java.util.List;

/**
 * Overseas live chat — same contract as the Node backend.
 * Visitors are identified by a random session token only (no account);
 * agents read and reply from the admin console ("chat" menu permission).
 */
public interface OverseasChatService {

    /** Create a session, or return the existing one when the token is known. */
    ChatStartResponse start(ChatStartRequest request, String clientIp);

    /** Visitor sends a message (token must match a session; rate limited per IP). */
    ChatMessageResponse sendVisitorMessage(ChatSendRequest request, String clientIp);

    /** Visitor pulls own messages, incrementally after afterId. */
    List<ChatMessageResponse> listVisitorMessages(String token, long afterId);

    /** Admin: sessions by latest activity, with last-message preview + unread count. */
    List<ChatSessionResponse> listSessionsForAdmin();

    /** Admin: full thread; reading it marks the visitor messages as read. */
    ChatThreadResponse threadForAdmin(Long sessionId);

    /** Admin: reply as the agent. */
    ChatMessageResponse replyForAdmin(Long sessionId, ChatReplyRequest request);
}
