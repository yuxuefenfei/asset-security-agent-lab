package lab.contract.api;

import java.util.List;

public record PageResponse<T>(List<T> items, long page, long size, long total) {}
