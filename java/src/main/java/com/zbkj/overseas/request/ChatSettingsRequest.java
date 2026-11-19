package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Live-chat settings saved from the admin chat page. Stored in the
 * site-config key-value table (key chatAutoCloseHours) but written only
 * through the chat endpoints, so the "chat" menu permission is enough —
 * no site-config field-group permission is involved.
 */
@Data
@Schema(name = "ChatSettingsRequest", description = "聊天设置")
public class ChatSettingsRequest {

    @Schema(description = "会话闲置多少小时后自动结束（0 = 不自动结束）")
    private Integer autoCloseHours;
}
