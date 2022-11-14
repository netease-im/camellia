package com.netease.nim.camellia.core.api;

import java.util.ArrayList;
import java.util.List;

public class PageCriteria {
    private static final int MIN_LIMIT = 0;
    private static final int MAX_LIMIT = 100;
    private static final int MIN_PAGE = 0;

    public static final String ASC_SYMBOL = "+";
    public static final String DESC_SYMBOL = "-";
    private Integer page = 1;

    private Integer limit = 30;

    private List<String> sort = new ArrayList<>();

    public PageCriteria(Integer page, Integer limit, List<String> sort) {
        if (limit != null) {
            this.setLimit(limit);
        }
        if (page != null) {
            this.setPage(page);
        }
        this.sort = sort;
    }

    public PageCriteria(Integer page, Integer limit) {
        if (limit != null) {
            this.setLimit(limit);
        }
        if (page != null) {
            this.setPage(page);
        }
    }

    public PageCriteria() {
    }

    public void setLimit(Integer limit) {
        if (limit != null && limit > MIN_LIMIT && limit <= MAX_LIMIT) {
            this.limit = limit;
        }
    }

    public void setPage(Integer page) {
        if (page != null && page > MIN_PAGE) {
            this.page = page;
        }
    }

    public Integer getPage() {
        return page;
    }

    public Integer getLimit() {
        return limit;
    }

    public List<String> getSort() {
        return sort;
    }

    public void setSort(List<String> sort) {
        this.sort = sort;
    }
}
