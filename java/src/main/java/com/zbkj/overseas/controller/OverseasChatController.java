package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.ChatSendRequest;
import com.zbkj.overseas.request.ChatStartRequest;
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
        return CommonResult.success(chatService.start(request, clientIp(httpRequest)));
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
        List<ChatMessageResponse> messages = chatService.listVisitorMessages(token, afterId);
        return CommonResult.success(Map.of("messages", messages));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
