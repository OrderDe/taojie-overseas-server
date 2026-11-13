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
 * Custom page — an admin-authored standalone page (title + image/text
 * sections + gallery) that a top-navigation item can point at. The slug
 * is the page's stable public handle: nav items store it, so it is
 * validated strictly and stays unique across the table.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("eb_overseas_custom_page")
public class OverseasCustomPage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** Public handle, ^[a-z0-9][a-z0-9-]{1,60}$, unique. */
    private String slug;

    /** JSON map lang -> page title. */
    private String titles;

    /** JSON array of {title:{lang:text}, body:{lang:text}, image}. */
    private String sections;

    /** JSON array of image URLs. */
    private String gallery;

    private Integer sort;

    private Boolean isShow;

    private Date createTime;

    private Date updateTime;
}
