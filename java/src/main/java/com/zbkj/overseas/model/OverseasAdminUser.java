package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * Admin account for the overseas management UI.
 * Passwords are stored as PBKDF2 hashes (never plaintext): the first
 * account (admin) is bootstrapped on startup with the ADMIN_TOKEN value
 * as its password when the table is still empty.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_admin_user")
public class OverseasAdminUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    /** pbkdf2:<iterations>:<salt b64>:<hash b64> */
    private String passwordHash;

    private String displayName;

    private Long roleId;

    private Boolean active;

    private Date createTime;

    private Date updateTime;
}
