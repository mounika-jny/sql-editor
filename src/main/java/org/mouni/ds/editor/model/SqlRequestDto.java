package org.mouni.ds.editor.model;

import jakarta.validation.constraints.NotBlank;

public class SqlRequestDto {
    @NotBlank
    private String query;

    // Optional paging for SELECT
    private Integer page; // zero-based
    private Integer size; // default 20

    public SqlRequestDto() {}

    public SqlRequestDto(String query, Integer page, Integer size) {
        this.query = query;
        this.page = page;
        this.size = size;
    }

    public String getQuery() {
        return query;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
