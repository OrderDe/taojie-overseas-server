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
 * 车型库 — replicated from the sfsy (JBolt) tinting system with the
 * eb_overseas_ table prefix; column names match the source schema.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_car_model")
public class OverseasCarModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String brand;

    private Long brandId;

    private String model;

    private Integer sortRank;

    private String enable;

    private Date updateTime;

    private String isDeleted;

    private Date createTime;

    private String createUserId;

    private String updateUserId;
}
