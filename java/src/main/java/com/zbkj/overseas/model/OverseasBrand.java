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
 * Overseas product brand — the counterpart of plat/tjmall's brand module
 * (brandAddApi / brandListApi ...). Multi-language names live in a JSON
 * column, same as categories; products reference a brand by id.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_brand")
public class OverseasBrand implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** JSON map lang -> name, e.g. {"en":"Kapci","zh":"卡普奇"} */
    private String names;

    private String logo;

    private Integer sort;

    private Boolean isShow;

    private Date createTime;

    private Date updateTime;
}
