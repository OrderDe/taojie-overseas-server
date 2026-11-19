package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Live-chat settings as read back by the admin chat page.
 */
@Data
@Schema(name = "ChatSettingsResponse", description = "聊天设置")
public class ChatSettingsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "会话闲置多少小时后自动结束（0 = 不自动结束）")
    private Integer autoCloseHours;
}
