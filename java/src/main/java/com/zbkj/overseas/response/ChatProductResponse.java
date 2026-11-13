package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Product snapshot attached to a chat session, as the admin console
 * receives it (parsed from the session's stored JSON).
 */
@Data
@Schema(name = "ChatProductResponse", description = "聊天会话附带的商品快照")
public class ChatProductResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String image;

    private String priceLabel;

    private Boolean negotiable;
}
