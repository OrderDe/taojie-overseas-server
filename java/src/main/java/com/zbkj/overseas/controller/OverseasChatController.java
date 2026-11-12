package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.ChatPingRequest;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
import com.zbkj.overseas.request.ChatTypingRequest;
import com.zbkj.overseas.response.ChatMessageResponse;
import com.zbkj.overseas.response.ChatStartResponse;
import com.zbkj.overseas.service.OverseasChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Storefront live-chat controller — public, token-based (no login).
 * The widget keeps the session token in localStorage; messages are
 * pulled incrementally with afterId.
 */
@Slf4j
@RestController
@RequestMapping("api/chat")
@Tag(name = "海外在线聊天控制器（访客端）")
public class OverseasChatController {

    @Autowired
    private OverseasChatService chatService;

    @PermitAll
    @Operation(summary = "开启/复用聊天会话")
    @PostMapping("/start")
    public CommonResult<ChatStartResponse> start(@RequestBody(required = false) ChatStartRequest request,
                                                 HttpServletRequest httpRequest) {
        return CommonResult.success(chatService.start(request, clientIp(httpRequest),
                httpRequest.getHeader("User-Agent")));
    }

    @PermitAll
    @Operation(summary = "访客发送消息")
    @PostMapping("/messages")
    public CommonResult<ChatMessageResponse> send(@Valid @RequestBody ChatSendRequest request,
                                                  HttpServletRequest httpRequest) {
        return CommonResult.success(chatService.sendVisitorMessage(request, clientIp(httpRequest)));
    }

    @PermitAll
    @Operation(summary = "访客拉取本会话消息（afterId 增量）")
    @GetMapping("/messages")
    public CommonResult<Map<String, Object>> pull(@RequestParam String token,
                                                  @RequestParam(required = false, defaultValue = "0") long afterId) {
        OverseasChatService.ChatPullResult result = chatService.listVisitorMessages(token, afterId);
        return CommonResult.success(Map.of(
                "messages", result.messages(),
                "agentTyping", result.agentTyping()));
    }

    @PermitAll
    @Operation(summary = "访客心跳（在线状态 + 当前页面）")
    @PostMapping("/ping")
    public CommonResult<Map<String, Object>> ping(@Valid @RequestBody ChatPingRequest request) {
        chatService.ping(request);
        return CommonResult.success(Map.of("ok", true));
    }

    @PermitAll
    @Operation(summary = "访客正在输入")
    @PostMapping("/typing")
    public CommonResult<Map<String, Object>> typing(@Valid @RequestBody ChatTypingRequest request) {
        chatService.visitorTyping(request);
        return CommonResult.success(Map.of("ok", true));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
