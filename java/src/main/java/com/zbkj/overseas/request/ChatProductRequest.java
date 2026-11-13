package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Product snapshot a visitor attaches to a chat session by tapping 询价
 * on a product card / detail page. All fields are display values as the
 * visitor saw them (name and price label already localised/formatted).
 */
@Data
@Schema(name = "ChatProductRequest", description = "聊天会话附带的商品快照")
public class ChatProductRequest {

    @Schema(description = "商品 id")
    private String id;

    @Schema(description = "商品显示名（访客当前语言）")
    private String name;

    @Schema(description = "商品图片 URL，可空")
    private String image;

    @Schema(description = "价格显示文本（如 ¥104.89），面议商品为空")
    private String priceLabel;

    @Schema(description = "是否面议")
    private Boolean negotiable;
}
