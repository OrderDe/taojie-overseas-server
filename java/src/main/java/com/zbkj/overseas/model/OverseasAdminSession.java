package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Login session for the admin UI. The token is what the UI sends as
 * x-admin-token after login (the shared ADMIN_TOKEN itself keeps working
 * as a break-glass root credential, checked before sessions).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_admin_session")
public class OverseasAdminSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "token", type = IdType.INPUT)
    private String token;

    private Long userId;

    /** Epoch milliseconds; expired sessions resolve to "not logged in". */
    private Long expiresAt;
}
