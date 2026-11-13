package com.zbkj.overseas.service.impl;
import org.junit.jupiter.api.Test;
import com.zbkj.overseas.model.OverseasCategory;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CategoryHierarchyTest {
    private List<OverseasCategory> tree() {
        return List.of(cat("a", ""),cat("b","a"),cat("c","b"),cat("d",""));
    }
    private OverseasCategory cat(String id, String parent) {
        return new OverseasCategory().setId(id).setParentId(parent).setNames(cn.hutool.json.JSONUtil.toJsonStr(Map.of("en",id,"zh",id)));
    }
    @Test void countsAndSubtreeIncludeEveryDescendantExactlyOnce() {
        assertEquals(Set.of("a","b","c"), CategoryHierarchy.subtree(tree(),"a"));
        assertEquals(8,CategoryHierarchy.count(tree(),"a",Map.of("a",1L,"b",2L,"c",5L,"d",20L)));
        assertEquals("a / b / c",CategoryHierarchy.path(tree(),"c","en"));
    }
    @Test void allowsCreatingEachOfThreeLevels() {
        assertDoesNotThrow(()->CategoryHierarchy.validateMove(tree(),"new",""));
        assertDoesNotThrow(()->CategoryHierarchy.validateMove(tree(),"new","a"));
        assertDoesNotThrow(()->CategoryHierarchy.validateMove(tree(),"new","b"));
    }
    @Test void rejectsFourthLevelAndMissingParent() {
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.validateMove(tree(),"new","c"));
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.validateMove(tree(),"new","missing"));
    }
    @Test void rejectsSelfAndDescendantParents() {
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.validateMove(tree(),"a","a"));
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.validateMove(tree(),"a","c"));
    }
    @Test void validatesEntireMovedSubtreeAndAllowsSafeMove() {
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.validateMove(tree(),"a","d"));
        assertDoesNotThrow(()->CategoryHierarchy.validateMove(tree(),"b","d"));
        assertDoesNotThrow(()->CategoryHierarchy.validateMove(tree(),"c",""));
    }
    @Test void visibleChildrenKeepAllStructuralAncestorsInStorefrontTree() {
        var root = cat("a", "").setIsShow(false);
        var child = cat("b", "a").setIsShow(true);
        var hidden = cat("d", "").setIsShow(false);
        assertEquals(Set.of("a","b"),CategoryHierarchy.visibleIds(List.of(root,child,hidden)));
    }
    @Test void cycleInCorruptedDataCannotLoopForever() {
        assertThrows(IllegalArgumentException.class,()->CategoryHierarchy.level(List.of(cat("a","b"),cat("b","a")),"a"));
        assertEquals(Set.of("a","b"),CategoryHierarchy.subtree(List.of(cat("a","b"),cat("b","a")),"a"));
    }
}
