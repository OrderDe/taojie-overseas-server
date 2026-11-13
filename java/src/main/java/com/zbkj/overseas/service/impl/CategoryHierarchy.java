package com.zbkj.overseas.service.impl;

import com.zbkj.overseas.model.OverseasCategory;
import java.util.*;

/** Category relationships are IDs, never inferred from display names at runtime. */
final class CategoryHierarchy {
    private CategoryHierarchy() {}
    static String parent(OverseasCategory c) { return c.getParentId() == null ? "" : c.getParentId(); }
    static Map<String, OverseasCategory> index(List<OverseasCategory> rows) {
        Map<String, OverseasCategory> out = new LinkedHashMap<>();
        rows.forEach(c -> out.put(c.getId(), c));
        return out;
    }
    static Set<String> subtree(List<OverseasCategory> rows, String id) {
        Set<String> ids = new LinkedHashSet<>();
        if (id == null || id.isBlank()) return ids;
        ids.add(id);
        boolean changed;
        do {
            changed = false;
            for (OverseasCategory c : rows) {
                if (ids.contains(parent(c)) && ids.add(c.getId())) changed = true;
            }
        } while (changed);
        return ids;
    }
    static int level(List<OverseasCategory> rows, String id) {
        return ancestors(rows, id).size();
    }
    static List<OverseasCategory> ancestors(List<OverseasCategory> rows, String id) {
        Map<String, OverseasCategory> map = index(rows);
        LinkedList<OverseasCategory> path = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        while (id != null && !id.isBlank()) {
            if (!seen.add(id)) throw new IllegalArgumentException("分类层级存在循环");
            OverseasCategory c = map.get(id);
            if (c == null) break;
            path.addFirst(c);
            id = parent(c);
        }
        return path;
    }
    static String path(List<OverseasCategory> rows, String id, String lang) {
        return String.join(" / ", ancestors(rows, id).stream().map(c -> LangUtil.pick(c.getNames(), lang)).toList());
    }
    static Set<String> visibleIds(List<OverseasCategory> rows) {
        Set<String> ids = new LinkedHashSet<>();
        for (OverseasCategory c : rows) {
            if (!Boolean.FALSE.equals(c.getIsShow())) {
                ancestors(rows, c.getId()).forEach(parent -> ids.add(parent.getId()));
            }
        }
        return ids;
    }
    static int count(List<OverseasCategory> rows, String id, Map<String, Long> directCounts) {
        long total = subtree(rows, id).stream().mapToLong(k -> directCounts.getOrDefault(k, 0L)).sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }
    static void validateMove(List<OverseasCategory> rows, String id, String parentId) {
        String target = parentId == null ? "" : parentId;
        Map<String, OverseasCategory> map = index(rows);
        if (!target.isBlank() && !map.containsKey(target)) throw new IllegalArgumentException("上级分类不存在");
        Set<String> descendants = subtree(rows, id);
        if (!target.isBlank() && descendants.contains(target)) throw new IllegalArgumentException("不能选择自身或下级分类作为上级");
        int newLevel = target.isBlank() ? 1 : level(rows, target) + 1;
        int currentLevel = map.containsKey(id) ? level(rows, id) : 1;
        int height = 0;
        for (String child : descendants) {
            if (map.containsKey(child)) height = Math.max(height, level(rows, child) - currentLevel);
        }
        if (newLevel + height > 3) throw new IllegalArgumentException("分类最多支持三级，移动后下级分类会超出三级");
    }
}
