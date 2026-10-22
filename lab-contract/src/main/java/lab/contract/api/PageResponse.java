package lab.contract.api;

import java.util.List;

/** 数据库分页结果：page 从 1 开始，total 为筛选后的总记录数，不是当前页条数。 */
public record PageResponse<T>(List<T> items, long page, long size, long total) {}
