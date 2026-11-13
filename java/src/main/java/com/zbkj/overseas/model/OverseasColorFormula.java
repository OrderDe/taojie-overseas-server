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
 * 颜色配方表 — replicated from the sfsy (JBolt) tinting system with the
 * eb_overseas_ table prefix; column names match the source schema.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_color_formula")
public class OverseasColorFormula implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long colornumId;

    private String name;

    private String model;

    private Double dose;

    private Integer colorRank;

    private String enable;

    private String isDeleted;

    private Date createTime;

    private Date updateTime;

    private String createUserId;

    private String updateUserId;
}
