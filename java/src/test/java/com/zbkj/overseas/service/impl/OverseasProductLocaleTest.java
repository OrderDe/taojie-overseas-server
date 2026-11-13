package com.zbkj.overseas.service.impl;
import cn.hutool.json.JSONUtil;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.zbkj.overseas.model.OverseasProduct;
import static org.junit.jupiter.api.Assertions.*;
class OverseasProductLocaleTest {
    private OverseasProduct product() {
        return new OverseasProduct().setNames(JSONUtil.toJsonStr(Map.of("zh","抽纸","en","Tissues")))
            .setUnit("包").setTag("热销").setBlurb(JSONUtil.toJsonStr(Map.of("zh","柔软","en","Soft")))
            .setSpecs(JSONUtil.toJsonStr(Map.of("产地","中国")))
            .setLocalizedContent(JSONUtil.toJsonStr(Map.of("en", Map.of("unit","pack","tag","Hot","specs",Map.of("Origin","China")))));
    }
    @Test void englishResponseLocalizesAllProductText() {
        var r = new OverseasProductServiceImpl().toResponse(product(), "en");
        assertEquals("Tissues",r.getName()); assertEquals("Soft",r.getDescription());
        assertEquals("pack",r.getUnit()); assertEquals("Hot",r.getTag());
        assertEquals("China",r.getSpecs().get("Origin"));
    }
    @Test void chineseResponseRetainsSourceText() {
        var r = new OverseasProductServiceImpl().toResponse(product(), "zh");
        assertEquals("抽纸",r.getName()); assertEquals("柔软",r.getDescription());
        assertEquals("包",r.getUnit()); assertEquals("中国",r.getSpecs().get("产地"));
    }
    @Test void untranslatedProductsRemainReadable() {
        var r = new OverseasProductServiceImpl().toResponse(product().setLocalizedContent(null), "en");
        assertEquals("包",r.getUnit()); assertEquals("中国",r.getSpecs().get("产地"));
    }
}
