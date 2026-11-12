package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.response.ChatMessageResponse;
import com.zbkj.overseas.response.ChatSessionResponse;
import com.zbkj.overseas.response.ChatThreadResponse;
import com.zbkj.overseas.service.OverseasChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin live-chat controller. Every path sits under /api/admin/chat/**, so
 * AdminTokenInterceptor guards them with the "chat" menu permission —
 * same contract as the Node backend.
 */
@Slf4j
@RestController
@RequestMapping("api/admin/chat")
@Tag(name = "海外在线聊天控制器（管理端）")
public class OverseasAdminChatController {

    @Autowired
    private OverseasChatService chatService;

    @Operation(summary = "管理端：聊天会话列表（含未读数）")
    @GetMapping("/sessions")
    public CommonResult<List<ChatSessionResponse>> sessions() {
        return CommonResult.success(chatService.listSessionsForAdmin());
    }

    @Operation(summary = "管理端：会话全部消息（读取即标记已读）")
    @GetMapping("/sessions/{id}/messages")
    public CommonResult<ChatThreadResponse> messages(@PathVariable Long id) {
        return CommonResult.success(chatService.threadForAdmin(id));
    }

    @Operation(summary = "管理端：以客服身份回复")
    @PostMapping("/sessions/{id}/messages")
    public CommonResult<ChatMessageResponse> reply(@PathVariable Long id,
                                                   @Valid @RequestBody ChatReplyRequest request) {
        return CommonResult.success(chatService.replyForAdmin(id, request));
    }
}
