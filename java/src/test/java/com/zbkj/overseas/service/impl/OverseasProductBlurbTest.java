package com.zbkj.overseas.service.impl;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OverseasProductBlurbTest {
    @Test void plainDescriptionIsStoredAsJsonAndReadsBackWithoutEscaping() {
        String text = "测试 " + (char)34 + "引号" + (char)34 + (char)10 + "第二行" + (char)92 + "路径";
        String json = LangUtil.withAdminBlurb(null, text);
        assertTrue(JSONUtil.isTypeJSONObject(json));
        assertEquals(text, LangUtil.pick(json, "en"));
        assertEquals(text, LangUtil.pick(json, "zh"));
    }
    @Test void emptyAndNullDescriptionsRemainValidJson() {
        for (String text : new String[]{null, ""}) {
            String json = LangUtil.withAdminBlurb(null, text);
            assertTrue(JSONUtil.isTypeJSONObject(json));
            assertEquals("", LangUtil.pick(json, "en"));
        }
    }
    @Test void savingUnchangedDescriptionPreservesAllTranslations() {
        String existing = JSONUtil.toJsonStr(Map.of("en", "Drink", "zh", "饮料", "fr", "Boisson"));
        assertEquals(LangUtil.toStringMap(existing), LangUtil.toStringMap(LangUtil.withAdminBlurb(existing, "Drink")));
    }
    @Test void editedDescriptionRetainsOtherLanguages() {
        String existing = JSONUtil.toJsonStr(Map.of("en", "Old", "zh", "旧", "fr", "Ancien"));
        String json = LangUtil.withAdminBlurb(existing, "新简介");
        assertEquals("新简介", LangUtil.pick(json, "en"));
        assertEquals("新简介", LangUtil.pick(json, "zh"));
        assertEquals("Ancien", LangUtil.pick(json, "fr"));
    }
}
