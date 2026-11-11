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
 * Key-value site configuration — mirrors tjmall's eb_system_config pattern.
 * Keys match the PC frontend's pc-config.json fields one-to-one.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_site_config")
public class OverseasSiteConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;

    private String configValue;

    private Date updateTime;
}
