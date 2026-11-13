package com.zbkj.overseas.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.overseas.common.exception.OverseasException;
import com.zbkj.overseas.common.result.CommonResultCode;
import com.zbkj.overseas.common.result.OverseasResultCode;
import com.zbkj.overseas.dao.OverseasCustomPageDao;
import com.zbkj.overseas.model.OverseasCustomPage;
import com.zbkj.overseas.request.CustomPageSaveRequest;
import com.zbkj.overseas.response.CustomPageResponse;
import com.zbkj.overseas.response.CustomPageSummaryResponse;
import com.zbkj.overseas.service.OverseasCustomPageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom page service implementation. Authoring guards mirror the
 * profile-content sanitizers in OverseasSiteConfigServiceImpl (lang maps
 * keep non-blank strings only, truncated; images keep http(s)/protocol-
 * relative/site paths; lists are capped), so a junk or hostile payload
 * can neither break the storefront page view nor bloat the table.
 */
@Slf4j
@Service
public class OverseasCustomPageServiceImpl extends ServiceImpl<OverseasCustomPageDao, OverseasCustomPage>
        implements OverseasCustomPageService {

    private static final String SLUG_REGEX = "^[a-z0-9][a-z0-9-]{1,60}$";
    private static final DateTimeFormatter ISO_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // One sample page on first boot (only while the table is empty) so
    // the owner sees the feature working end to end; a normal row she can
    // edit or delete in the admin. Same content as the Node backend seed.
    @PostConstruct
    public void seedSamplePageIfEmpty() {
        try {
            if (count() > 0) {
                return;
            }
            CustomPageSaveRequest seed = new CustomPageSaveRequest();
            seed.setSlug("cases");
            seed.setTitles(langMap("客户案例", "Cases"));
            seed.setEnabled(true);
            seed.setSort(1);
            String imgPrefix = "https://youjia-oss.youjiakj.com/williscarpaint/Upload/20260524/";
            CustomPageSaveRequest.Section s1 = new CustomPageSaveRequest.Section();
            s1.setTitle(langMap("我们的工厂", "Our Factory"));
            s1.setBody(langMap(
                    "我们拥有专业的生产设备与经验丰富的生产团队。\n\n产品质量有保障，发货速度快！",
                    "WE HAVE PROFESSIONAL PRODUCTION EQUIPMENT AND EXPERIENCED MANUFACTURERS.\n\nTHE QUALITY OF THE PRODUCT IS GUARANTEED, AND THE SHIPMENT SPEED IS FAST!"));
            s1.setImage(imgPrefix + "1779626894357.jpg");
            CustomPageSaveRequest.Section s2 = new CustomPageSaveRequest.Section();
            s2.setTitle(langMap("仓储与发货", "Warehousing & Shipping"));
            s2.setBody(langMap(
                    "专业的生产设备与经验丰富的生产团队，产品质量有保障，发货速度快。",
                    "Professional production equipment and an experienced team — guaranteed product quality and fast shipment."));
            s2.setImage(imgPrefix + "1779626894649.jpg");
            seed.setSections(List.of(s1, s2));
            seed.setGallery(List.of(
                    imgPrefix + "1779626894497.jpg",
                    imgPrefix + "1779626894850.jpg",
                    imgPrefix + "1779626894947.jpg"));
            saveForAdmin(seed, null);
            log.info("seeded sample custom page: cases");
        } catch (Exception e) {
            log.warn("custom page sample seed skipped: {}", e.getMessage());
        }
    }

    private static Map<String, String> langMap(String zh, String en) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("zh", zh);
        map.put("en", en);
        return map;
    }

    // ---- storefront ----

    @Override
    public List<CustomPageSummaryResponse> listForFront() {
        return list(new LambdaQueryWrapper<OverseasCustomPage>()
                .eq(OverseasCustomPage::getIsShow, true)
                .orderByAsc(OverseasCustomPage::getSort)
                .orderByAsc(OverseasCustomPage::getId))
                .stream().map(p -> {
                    CustomPageSummaryResponse r = new CustomPageSummaryResponse();
                    r.setSlug(p.getSlug());
                    r.setTitles(LangUtil.toStringMap(p.getTitles()));
                    return r;
                }).collect(Collectors.toList());
    }

    @Override
    public CustomPageResponse getForFront(String slug) {
        OverseasCustomPage page = getOne(new LambdaQueryWrapper<OverseasCustomPage>()
                .eq(OverseasCustomPage::getSlug, slug)
                .eq(OverseasCustomPage::getIsShow, true), false);
        if (page == null) {
            throw new OverseasException(OverseasResultCode.PAGE_NOT_FOUND);
        }
        return toResponse(page);
    }

    // ---- admin ----

    @Override
    public List<CustomPageResponse> listForAdmin() {
        return list(new LambdaQueryWrapper<OverseasCustomPage>()
                .orderByAsc(OverseasCustomPage::getSort)
                .orderByAsc(OverseasCustomPage::getId))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public CustomPageResponse saveForAdmin(CustomPageSaveRequest request, Long id) {
        String slug = request.getSlug() == null ? "" : request.getSlug().trim();
        if (!slug.matches(SLUG_REGEX)) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(),
                    "slug 须为 2-61 位小写字母、数字或连字符，且以字母或数字开头");
        }
        Map<String, String> titles = sanitizeLangMap(request.getTitles(), 300);
        if (titles.isEmpty()) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "页面标题至少填一个语言");
        }
        OverseasCustomPage clash = getOne(new LambdaQueryWrapper<OverseasCustomPage>()
                .eq(OverseasCustomPage::getSlug, slug)
                .ne(id != null, OverseasCustomPage::getId, id), false);
        if (clash != null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "slug 已存在：" + slug);
        }
        OverseasCustomPage page;
        if (id == null) {
            page = new OverseasCustomPage();
            page.setCreateTime(new Date());
        } else {
            page = getById(id);
            if (page == null) {
                throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "页面不存在");
            }
        }
        page.setSlug(slug);
        page.setTitles(JSONUtil.toJsonStr(titles));
        page.setSections(JSONUtil.toJsonStr(sanitizeSections(request.getSections())));
        page.setGallery(JSONUtil.toJsonStr(sanitizeImageList(request.getGallery())));
        page.setIsShow(!Boolean.FALSE.equals(request.getEnabled()));
        page.setSort(request.getSort() == null ? 0 : request.getSort());
        page.setUpdateTime(new Date());
        saveOrUpdate(page);
        return toResponse(page);
    }

    @Override
    public void deleteForAdmin(Long id) {
        if (getById(id) == null) {
            throw new OverseasException(CommonResultCode.VALIDATE_FAILED.getCode(), "页面不存在");
        }
        removeById(id);
    }

    // ---- mapping + guards (mirror OverseasSiteConfigServiceImpl) ----

    private CustomPageResponse toResponse(OverseasCustomPage page) {
        CustomPageResponse r = new CustomPageResponse();
        r.setId(page.getId());
        r.setSlug(page.getSlug());
        r.setTitles(LangUtil.toStringMap(page.getTitles()));
        r.setSections(parseSections(page.getSections()));
        r.setGallery(parseGallery(page.getGallery()));
        r.setEnabled(Boolean.TRUE.equals(page.getIsShow()));
        r.setSort(page.getSort() == null ? 0 : page.getSort());
        r.setUpdatedAt(page.getUpdateTime() == null ? ""
                : ISO_SECONDS.format(page.getUpdateTime().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()));
        return r;
    }

    private static List<CustomPageSaveRequest.Section> parseSections(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, CustomPageSaveRequest.Section.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<String> parseGallery(String json) {
        if (StrUtil.isBlank(json)) {
            return List.of();
        }
        try {
            return JSONUtil.toList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<CustomPageSaveRequest.Section> sanitizeSections(List<CustomPageSaveRequest.Section> raw) {
        List<CustomPageSaveRequest.Section> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (CustomPageSaveRequest.Section s : raw) {
            if (s == null || out.size() >= 30) {
                continue;
            }
            CustomPageSaveRequest.Section clean = new CustomPageSaveRequest.Section();
            clean.setTitle(sanitizeLangMap(s.getTitle(), 300));
            clean.setBody(sanitizeLangMap(s.getBody(), 5000));
            clean.setImage(sanitizeImageUrl(s.getImage()));
            out.add(clean);
        }
        return out;
    }

    private static Map<String, String> sanitizeLangMap(Map<String, String> raw, int maxLen) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, String> e : raw.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            String v = e.getValue().trim();
            if (!v.isEmpty()) {
                out.put(e.getKey(), truncate(v, maxLen));
            }
        }
        return out;
    }

    private static List<String> sanitizeImageList(List<String> raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String u : raw) {
            if (u != null && !u.trim().isEmpty() && out.size() < 30) {
                out.add(truncate(u.trim(), 1000));
            }
        }
        return out;
    }

    private static String sanitizeImageUrl(String raw) {
        if (raw == null) {
            return "";
        }
        String u = truncate(raw.trim(), 1000);
        return u.startsWith("http://") || u.startsWith("https://") || u.startsWith("//") || u.startsWith("/") ? u : "";
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
