package com.zbkj.overseas.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 颜色色号关系表 — replicated from the sfsy (JBolt) tinting system with the
 * eb_overseas_ table prefix; column names match the source schema.
 * Join semantics verified against the imported data:
 * carColor = car_color.id, colorNumber = color_number.id.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_color_number_relation")
public class OverseasColorNumberRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long carColor;

    private Long colorNumber;

    private String enable;
}
