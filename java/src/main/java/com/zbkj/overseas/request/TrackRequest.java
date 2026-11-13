package com.zbkj.overseas.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Storefront tracking beacon. type = view (default) / heartbeat / click.
 * A missing or malformed token is not an error: the server generates a
 * fresh one and returns it, mirroring the Node backend.
 */
@Data
@Schema(name = "TrackRequest", description = "访客统计上报")
public class TrackRequest {

    @Schema(description = "访客 token（浏览器 localStorage），缺失/非法时服务端生成并返回")
    private String token;

    @Schema(description = "上报类型：view（默认）/ heartbeat / click")
    private String type;

    @Schema(description = "当前页标签，如 home / catalogue / detail:商品名")
    private String page;

    @Schema(description = "来源页 document.referrer")
    private String referrer;

    @Schema(description = "界面语言")
    private String lang;

    @Schema(description = "type=click 时的点击描述，如 点商品:1K141")
    private String action;
}
