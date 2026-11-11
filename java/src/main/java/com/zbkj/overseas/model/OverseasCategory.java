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
 * Overseas product category.
 * Multi-language names live in a JSON column (names: {"en":..,"zh":..,...}),
 * the overseas counterpart of tjmall's eb_store_product_category + i18n needs.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_category")
public class OverseasCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /** JSON map lang -> name, e.g. {"en":"Clear Coats","zh":"清漆"} */
    private String names;

    private Integer sort;

    private Boolean isShow;

    private Date createTime;

    private Date updateTime;
}
