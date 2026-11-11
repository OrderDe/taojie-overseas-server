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
 * Admin role — a named set of menu permissions.
 * permissions is a JSON array of keys: products / categories / rfq /
 * config / users, or ["*"] for full access. The admin UI shows only the
 * tabs the role carries, and the API enforces the same keys per area.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_admin_role")
public class OverseasAdminRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    /** JSON array of permission keys */
    private String permissions;

    /** Built-in roles (the super admin) cannot be deleted or re-permissioned. */
    private Boolean builtin;

    private Date createTime;

    private Date updateTime;
}
