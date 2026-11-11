package com.zbkj.overseas.service.impl;
import org.junit.jupiter.api.Test;
import cn.hutool.json.JSONUtil;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
class OverseasAdminPermissionsTest {
 @Test void parsesSuperAdministratorPermissions() {
  assertEquals(List.of("*"), OverseasAdminServiceImpl.parsePerms(JSONUtil.toJsonStr(List.of("*"))));
 }
 @Test void parsesRestrictedRolePermissions() {
  assertEquals(List.of("products", "rfq"), OverseasAdminServiceImpl.parsePerms(JSONUtil.toJsonStr(List.of("products", "rfq"))));
 }
 @Test void rejectsMalformedOrMissingPermissions() {
  assertEquals(List.of(), OverseasAdminServiceImpl.parsePerms("invalid"));
  assertEquals(List.of(), OverseasAdminServiceImpl.parsePerms(null));
 }
}