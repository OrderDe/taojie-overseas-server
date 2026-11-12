package com.zbkj.overseas.service;

import com.zbkj.overseas.request.ChatNoteRequest;
import com.zbkj.overseas.request.ChatPingRequest;
import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
import com.zbkj.overseas.request.ChatStatusRequest;
import com.zbkj.overseas.request.ChatTypingRequest;
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
    ChatStartResponse start(ChatStartRequest request, String clientIp, String userAgent);

    /** Visitor sends a message (token must match a session; rate limited per IP). */
    ChatMessageResponse sendVisitorMessage(ChatSendRequest request, String clientIp);

    /** Visitor pulls own messages, incrementally after afterId; also reports whether the agent is typing. */
    ChatPullResult listVisitorMessages(String token, long afterId);

    /** Visitor heartbeat: refresh presence and follow page moves. */
    void ping(ChatPingRequest request);

    /** Visitor "typing…" signal. */
    void visitorTyping(ChatTypingRequest request);

    /** Admin: sessions by latest activity, with last-message preview + unread count. */
    List<ChatSessionResponse> listSessionsForAdmin(String keyword);

    /** Admin: full thread; reading it marks the visitor messages as read. */
    ChatThreadResponse threadForAdmin(Long sessionId);

    /** Admin: reply as the agent. */
    ChatMessageResponse replyForAdmin(Long sessionId, ChatReplyRequest request);

    /** Admin: "typing…" signal towards the visitor. */
    void agentTyping(Long sessionId);

    /** Admin: save the internal note (agents only). */
    void updateNote(Long sessionId, ChatNoteRequest request);

    /** Admin: open/close the session. */
    void updateStatus(Long sessionId, ChatStatusRequest request);

    /** Result of a visitor pull: the new messages + the agent's typing flag. */
    record ChatPullResult(List<ChatMessageResponse> messages, boolean agentTyping) {
    }
}
