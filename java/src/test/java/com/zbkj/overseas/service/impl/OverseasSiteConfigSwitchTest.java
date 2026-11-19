package com.zbkj.overseas.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.zbkj.overseas.dao.OverseasSiteConfigDao;
import com.zbkj.overseas.model.OverseasSiteConfig;
import com.zbkj.overseas.request.SiteConfigRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evidence chain for the site-config display switches (ZenTao #24/#25):
 * an admin PUT that turns showRfq / showContact off must come back from
 * getConfigMap() as real Boolean FALSE values (never the string "false",
 * which the storefront would read as truthy). The DAO is an in-memory
 * proxy, so the test exercises the real updateConfig → storage →
 * parseValue path without a database.
 *
 * Also covers the batch-2 additions on the same service: the hero
 * multi-language maps (#26) round-trip, and nav item `names` are kept
 * while non-registry language codes and bad jump types are dropped (#22).
 */
class OverseasSiteConfigSwitchTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                OverseasSiteConfig.class);
    }

    private OverseasSiteConfigServiceImpl newService(Map<String, OverseasSiteConfig> store) {
        OverseasSiteConfigDao dao = (OverseasSiteConfigDao) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{OverseasSiteConfigDao.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "insert": {
                            OverseasSiteConfig row = (OverseasSiteConfig) args[0];
                            store.put(row.getConfigKey(), row);
                            return 1;
                        }
                        case "updateById": {
                            OverseasSiteConfig row = (OverseasSiteConfig) args[0];
                            store.put(row.getConfigKey(), row);
                            return 1;
                        }
                        case "insertOrUpdate": {
                            // ServiceImpl.saveOrUpdate() delegates to this
                            // BaseMapper method and unboxes its Boolean
                            // result — returning null here NPEs the test,
                            // so store the row and report success.
                            OverseasSiteConfig row = (OverseasSiteConfig) args[0];
                            store.put(row.getConfigKey(), row);
                            return true;
                        }
                        case "selectById":
                            return store.get(String.valueOf(args[0]));
                        case "selectList":
                            return new ArrayList<>(store.values());
                        case "selectCount":
                            return (long) store.size();
                        case "toString":
                            return "in-memory-site-config-dao";
                        default:
                            return null;
                    }
                });
        OverseasSiteConfigServiceImpl service = new OverseasSiteConfigServiceImpl();
        try {
            Field field = com.baomidou.mybatisplus.extension.service.impl.ServiceImpl.class
                    .getDeclaredField("baseMapper");
            field.setAccessible(true);
            field.set(service, dao);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return service;
    }

    @Test
    void switchesRoundTripAsRealBooleans() {
        OverseasSiteConfigServiceImpl service = newService(new ConcurrentHashMap<>());
        // Defaults before anything is stored.
        Map<String, Object> before = service.getConfigMap();
        assertEquals(Boolean.TRUE, before.get("showRfq"));
        assertEquals(Boolean.TRUE, before.get("showContact"));

        SiteConfigRequest request = new SiteConfigRequest();
        request.setShowRfq(false);
        request.setShowContact(false);
        request.setShowCart(true);
        request.setShowDealer(true);
        request.setShowPrices(false);
        Map<String, Object> after = service.updateConfig(request);

        assertInstanceOf(Boolean.class, after.get("showRfq"));
        assertEquals(Boolean.FALSE, after.get("showRfq"));
        assertEquals(Boolean.FALSE, after.get("showContact"));
        assertEquals(Boolean.TRUE, after.get("showCart"));
        assertEquals(Boolean.TRUE, after.get("showDealer"));
        assertEquals(Boolean.FALSE, after.get("showPrices"));

        // And a fresh read (the storefront's GET /api/site-config path)
        // sees the same stored values.
        Map<String, Object> reread = service.getConfigMap();
        assertEquals(Boolean.FALSE, reread.get("showRfq"));
        assertEquals(Boolean.FALSE, reread.get("showContact"));
    }

    @Test
    void heroI18nMapsRoundTrip() {
        OverseasSiteConfigServiceImpl service = newService(new ConcurrentHashMap<>());
        SiteConfigRequest request = new SiteConfigRequest();
        request.setHeroTitleI18n(new LinkedHashMap<>(Map.of("zh", "大标题", "en", "Big Title")));
        request.setAnnouncementI18n(new LinkedHashMap<>(Map.of("zh", "公告", "en", "Notice")));
        Map<String, Object> after = service.updateConfig(request);
        assertEquals(Map.of("zh", "大标题", "en", "Big Title"), after.get("heroTitleI18n"));
        assertEquals(Map.of("zh", "公告", "en", "Notice"), after.get("announcementI18n"));
    }

    @Test
    void navItemNamesAreKeptAndSanitised() {
        OverseasSiteConfigServiceImpl service = newService(new ConcurrentHashMap<>());
        SiteConfigRequest.NavItem item = new SiteConfigRequest.NavItem();
        item.setLabel("涂料专区");
        item.setType("evil");
        Map<String, String> names = new LinkedHashMap<>();
        names.put("zh", "涂料专区");
        names.put("en", "Coatings");
        names.put("xx", "bogus-language");
        item.setNames(names);
        SiteConfigRequest request = new SiteConfigRequest();
        request.setNavItems(List.of(item));

        Map<String, Object> after = service.updateConfig(request);
        @SuppressWarnings("unchecked")
        List<SiteConfigRequest.NavItem> saved =
                (List<SiteConfigRequest.NavItem>) after.get("navItems");
        assertEquals(1, saved.size());
        assertEquals("涂料专区", saved.get(0).getNames().get("zh"));
        assertEquals("Coatings", saved.get(0).getNames().get("en"));
        assertFalse(saved.get(0).getNames().containsKey("xx"));
        assertEquals("", saved.get(0).getType());
        assertTrue(saved.get(0).getNames().size() == 2);
    }
}
