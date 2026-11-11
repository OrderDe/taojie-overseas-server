package com.zbkj.overseas.common.page;

import com.github.pagehelper.PageInfo;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Pagination wrapper — mirrors tjMall-common com.zbkj.common.page.CommonPage
 * ({ page, limit, totalPage, total, list }), built from PageHelper's PageInfo.
 */
@Data
public class CommonPage<T> {
    private Integer page = 1;
    private Integer limit = 20;
    private Integer totalPage = 0;
    private Integer total = 0;
    private List<T> list = new ArrayList<>();

    public static <T> CommonPage<T> restPage(PageInfo<T> pageInfo) {
        CommonPage<T> result = new CommonPage<>();
        result.setPage(pageInfo.getPageNum());
        result.setLimit(pageInfo.getPageSize());
        result.setTotalPage(pageInfo.getPages());
        result.setTotal((int) pageInfo.getTotal());
        result.setList(pageInfo.getList());
        return result;
    }

    public static <T> CommonPage<T> restPage(List<T> list, int page, int limit, long total) {
        CommonPage<T> result = new CommonPage<>();
        result.setPage(page);
        result.setLimit(limit);
        result.setTotal((int) total);
        result.setTotalPage(limit > 0 ? (int) Math.ceil((double) total / limit) : 0);
        result.setList(list);
        return result;
    }
}
