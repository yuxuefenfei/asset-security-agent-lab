package lab.contract.api;

import java.util.List;

/**
 * 数据库分页结果；只传输当前页记录和筛选后的总数，不将全表一次性返回。
 *
 * @param <T> 当前列表每一项的数据类型
 * @param items 当前页记录列表
 * @param page 当前页码，从 1 开始
 * @param size 每页请求容量，最后一页的实际条数可以更少
 * @param total 符合筛选条件的总记录数，用于计算总页数
 */
public record PageResponse<T>(List<T> items, long page, long size, long total) {}
