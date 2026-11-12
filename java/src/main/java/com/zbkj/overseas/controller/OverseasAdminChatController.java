package com.zbkj.overseas.controller;

import com.zbkj.overseas.common.result.CommonResult;
import com.zbkj.overseas.request.ChatNoteRequest;
import com.zbkj.overseas.request.ChatReplyRequest;
import com.zbkj.overseas.request.ChatStatusRequest;
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
import java.util.Map;

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

    @Operation(summary = "管理端：聊天会话列表（含未读数与访客画像，keyword 按访客名/消息内容搜索）")
    @GetMapping("/sessions")
    public CommonResult<List<ChatSessionResponse>> sessions(
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(chatService.listSessionsForAdmin(keyword));
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

    @Operation(summary = "管理端：客服正在输入")
    @PostMapping("/sessions/{id}/typing")
    public CommonResult<Map<String, Object>> typing(@PathVariable Long id) {
        chatService.agentTyping(id);
        return CommonResult.success(Map.of("ok", true));
    }

    @Operation(summary = "管理端：保存内部备注（访客不可见）")
    @PutMapping("/sessions/{id}/note")
    public CommonResult<Map<String, Object>> note(@PathVariable Long id,
                                                  @RequestBody(required = false) ChatNoteRequest request) {
        chatService.updateNote(id, request);
        String note = request == null || request.getNote() == null ? "" : request.getNote().trim();
        if (note.length() > 2000) {
            note = note.substring(0, 2000);
        }
        return CommonResult.success(Map.of("ok", true, "note", note));
    }

    @Operation(summary = "管理端：结束/重新开启会话")
    @PutMapping("/sessions/{id}/status")
    public CommonResult<Map<String, Object>> status(@PathVariable Long id,
                                                    @RequestBody(required = false) ChatStatusRequest request) {
        chatService.updateStatus(id, request);
        String status = request != null && "closed".equals(request.getStatus()) ? "closed" : "open";
        return CommonResult.success(Map.of("ok", true, "status", status));
    }
}
