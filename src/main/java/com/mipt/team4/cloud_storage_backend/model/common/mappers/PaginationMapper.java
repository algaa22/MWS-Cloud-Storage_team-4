package com.mipt.team4.cloud_storage_backend.model.common.mappers;

import com.mipt.team4.cloud_storage_backend.model.common.dto.PageQuery;
import com.mipt.team4.cloud_storage_backend.model.common.dto.requests.CommonPaginationParams;
import com.mipt.team4.cloud_storage_backend.model.common.dto.responses.PageResponse;
import com.mipt.team4.cloud_storage_backend.model.common.enums.SortableParam;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FilePaginationParams;
import com.mipt.team4.cloud_storage_backend.model.user.dto.requests.UserPaginationParams;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationMapper {
  public static PageQuery toPageQuery(FilePaginationParams params) {
    return createPageQuery(params.commonParams(), params.sortBy());
  }

  public static PageQuery toPageQuery(UserPaginationParams params) {
    return createPageQuery(params.commonParams(), params.sortBy());
  }

  public static Pageable toPageable(PageQuery pageQuery) {
    int number = pageQuery.offset() / pageQuery.limit();
    Sort sort = Sort.by(Sort.Direction.fromString(pageQuery.direction()), pageQuery.order());

    return PageRequest.of(number, pageQuery.limit(), sort);
  }

  public static <T> PageResponse<T> toResponse(Page<T> page) {
    return toResponse(page, page.getContent());
  }

  public static <R> PageResponse<R> toResponse(Page<?> page, List<R> content) {
    return new PageResponse<>(
        content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  private static PageQuery createPageQuery(
      CommonPaginationParams commonParams, SortableParam sortBy) {
    return new PageQuery(
        commonParams.size(),
        commonParams.page() * commonParams.size(),
        sortBy.getColumnName(),
        commonParams.direction().getName());
  }
}
