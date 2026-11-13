package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * One row of the admin formula page wrapper — the admin CRUD endpoints
 * return rows as maps so the JSON matches the Node backend exactly
 * (camelCase keys, id serialised as a string, enable as a boolean).
 * Page shape mirrors the Node { list, total, page, pageSize }.
 */
@Data
@Schema(name = "FormulaAdminPageResponse", description = "配方后台分页列表")
public class FormulaAdminPageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Map<String, Object>> list;

    private Long total;

    private Integer page;

    private Integer pageSize;
}
