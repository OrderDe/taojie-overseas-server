package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasChatMessageDao;
import com.zbkj.overseas.dao.OverseasChatSessionDao;
import com.zbkj.overseas.model.OverseasChatMessage;
import com.zbkj.overseas.model.OverseasChatSession;
import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
import com.zbkj.overseas.response.ChatMessageResponse;
import com.zbkj.overseas.response.ChatSessionResponse;
import com.zbkj.overseas.response.ChatStartResponse;
import com.zbkj.overseas.response.ChatThreadResponse;
import com.zbkj.overseas.service.OverseasChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Live-chat service implementation.
 * Mirrors the Node backend: per-IP rate windows (10 session starts and 30
 * messages per minute), messages capped at 2000 chars, unread counts from
 * the session's lastAgentReadMsgId watermark.
 */
@Slf4j
@Service
public class OverseasChatServiceImpl implements OverseasChatService {

    private static final Set<String> LANGS = Set.of("en", "zh", "es", "fr", "pt");
    private static final int MAX_BODY = 2000;
    private static final int MAX_STARTS_PER_MINUTE = 10;
    private static final int MAX_MESSAGES_PER_MINUTE = 30;

    private final ConcurrentHashMap<String, long[]> startHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, long[]> messageHits = new ConcurrentHashMap<>();

    @Autowired
    private OverseasChatSessionDao sessionDao;

    @Autowired
    private OverseasChatMessageDao messageDao;

    @Override
    public ChatStartResponse start(ChatStartRequest request, String clientIp) {
        OverseasChatSession existing = findByToken(request == null ? null : request.getToken());
        if (existing != null) {
            return startResponse(existing);
        }
        checkRateLimit(startHits, clientIp, MAX_STARTS_PER_MINUTE);
        String name = request == null ? "" : StrUtil.trimToEmpty(request.getName());
        if (name.length() > 64) {
            name = name.substring(0, 64);
        }
        if (name.isEmpty()) {
            name = "访客";
        }
        byte[] raw = new byte[24];
        new SecureRandom().nextBytes(raw);
        OverseasChatSession session = new OverseasChatSession();
        session.setVisitorToken(HexFormat.of().formatHex(raw));
        session.setVisitorName(name);
        String lang = request == null ? null : request.getLang();
        session.setLang(lang != null && LANGS.contains(lang) ? lang : "en");
        session.setLastAgentReadMsgId(0L);
        Date now = new Date();
        session.setCreateTime(now);
        session.setLastMsgAt(now);
        sessionDao.insert(session);
        return startResponse(session);
    }

    @Override
    public ChatMessageResponse sendVisitorMessage(ChatSendRequest request, String clientIp) {
        OverseasChatSession session = findByToken(request.getToken());
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        String body = StrUtil.trimToEmpty(request.getBody());
        if (body.isEmpty()) {
            throw new OverseasException(OverseasResultCode.CHAT_MESSAGE_EMPTY);
        }
        checkRateLimit(messageHits, clientIp, MAX_MESSAGES_PER_MINUTE);
        return appendMessage(session, "visitor", body);
    }

    @Override
    public List<ChatMessageResponse> listVisitorMessages(String token, long afterId) {
        OverseasChatSession session = findByToken(token);
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        return messageDao.selectList(new LambdaQueryWrapper<OverseasChatMessage>()
                        .eq(OverseasChatMessage::getSessionId, session.getId())
                        .gt(OverseasChatMessage::getId, Math.max(0L, afterId))
                        .orderByAsc(OverseasChatMessage::getId))
                .stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    @Override
    public List<ChatSessionResponse> listSessionsForAdmin() {
        List<OverseasChatSession> sessions = sessionDao.selectList(new LambdaQueryWrapper<OverseasChatSession>()
                .orderByDesc(OverseasChatSession::getLastMsgAt)
                .orderByDesc(OverseasChatSession::getId));
        return sessions.stream().map(this::toSessionResponse).collect(Collectors.toList());
    }

    @Override
    public ChatThreadResponse threadForAdmin(Long sessionId) {
        OverseasChatSession session = requireSession(sessionId);
        List<OverseasChatMessage> messages = messageDao.selectList(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, sessionId)
                .orderByAsc(OverseasChatMessage::getId));
        // Opening the thread marks every message in it as read by the agent.
        long maxId = messages.isEmpty() ? 0L : messages.get(messages.size() - 1).getId();
        long read = session.getLastAgentReadMsgId() == null ? 0L : session.getLastAgentReadMsgId();
        if (maxId > read) {
            session.setLastAgentReadMsgId(maxId);
            sessionDao.updateById(session);
        }
        ChatThreadResponse response = new ChatThreadResponse();
        response.setSession(toSessionResponse(session));
        response.setMessages(messages.stream().map(this::toMessageResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    public ChatMessageResponse replyForAdmin(Long sessionId, ChatReplyRequest request) {
        OverseasChatSession session = requireSession(sessionId);
        String body = StrUtil.trimToEmpty(request.getBody());
        if (body.isEmpty()) {
            throw new OverseasException(OverseasResultCode.CHAT_MESSAGE_EMPTY);
        }
        return appendMessage(session, "agent", body);
    }

    // ---- helpers ----

    private OverseasChatSession findByToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        return sessionDao.selectOne(new LambdaQueryWrapper<OverseasChatSession>()
                .eq(OverseasChatSession::getVisitorToken, token.trim()), false);
    }

    private OverseasChatSession requireSession(Long sessionId) {
        OverseasChatSession session = sessionId == null ? null : sessionDao.selectById(sessionId);
        if (session == null) {
            throw new OverseasException(OverseasResultCode.CHAT_SESSION_INVALID);
        }
        return session;
    }

    private ChatMessageResponse appendMessage(OverseasChatSession session, String sender, String body) {
        if (body.length() > MAX_BODY) {
            body = body.substring(0, MAX_BODY);
        }
        OverseasChatMessage message = new OverseasChatMessage();
        message.setSessionId(session.getId());
        message.setSender(sender);
        message.setBody(body);
        message.setCreateTime(new Date());
        messageDao.insert(message);
        session.setLastMsgAt(message.getCreateTime());
        sessionDao.updateById(session);
        return toMessageResponse(message);
    }

    private ChatStartResponse startResponse(OverseasChatSession session) {
        ChatStartResponse response = new ChatStartResponse();
        response.setSessionId(session.getId());
        response.setToken(session.getVisitorToken());
        response.setName(session.getVisitorName());
        return response;
    }

    private ChatSessionResponse toSessionResponse(OverseasChatSession session) {
        ChatSessionResponse response = new ChatSessionResponse();
        response.setId(session.getId());
        response.setVisitorName(session.getVisitorName());
        response.setLang(session.getLang());
        OverseasChatMessage last = messageDao.selectOne(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .orderByDesc(OverseasChatMessage::getId)
                .last("LIMIT 1"), false);
        response.setLastMessage(last == null ? "" : last.getBody());
        long read = session.getLastAgentReadMsgId() == null ? 0L : session.getLastAgentReadMsgId();
        Long unread = messageDao.selectCount(new LambdaQueryWrapper<OverseasChatMessage>()
                .eq(OverseasChatMessage::getSessionId, session.getId())
                .eq(OverseasChatMessage::getSender, "visitor")
                .gt(OverseasChatMessage::getId, read));
        response.setUnread(unread == null ? 0 : unread.intValue());
        response.setLastMsgAt(session.getLastMsgAt());
        response.setCreateTime(session.getCreateTime());
        return response;
    }

    private ChatMessageResponse toMessageResponse(OverseasChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSessionId(message.getSessionId());
        response.setSender(message.getSender());
        response.setBody(message.getBody());
        response.setCreateTime(message.getCreateTime());
        return response;
    }

    private void checkRateLimit(ConcurrentHashMap<String, long[]> hits, String ip, int maxPerMinute) {
        String key = ip == null ? "unknown" : ip;
        long now = System.currentTimeMillis();
        long[] slot = hits.compute(key, (k, v) -> {
            if (v == null || now - v[0] > 60_000) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        if (slot[1] > maxPerMinute) {
            throw new OverseasException(OverseasResultCode.RATE_LIMITED);
        }
    }
}
