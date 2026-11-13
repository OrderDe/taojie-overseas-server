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
 * 车辆颜色 — replicated from the sfsy (JBolt) tinting system with the
 * eb_overseas_ table prefix; column names match the source schema.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_car_color")
public class OverseasCarColor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long brandId;

    private Long modelId;

    private String carColor;

    private String factoryColor;

    private String colorNum;

    private String cardNum;

    private String carImage;

    private String enable;

    private String isDeleted;

    private Date createTime;

    private Date updateTime;

    private String createUserId;

    private String updateUserId;
}
