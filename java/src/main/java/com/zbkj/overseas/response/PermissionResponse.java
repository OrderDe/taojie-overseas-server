package com.zbkj.overseas.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * One menu-permission definition (key + display label) for the role editor.
 */
@Data
@AllArgsConstructor
@Schema(name = "PermissionResponse", description = "菜单权限定义")
public class PermissionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;

    private String label;
}
