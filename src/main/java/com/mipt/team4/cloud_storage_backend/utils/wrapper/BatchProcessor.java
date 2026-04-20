package com.mipt.team4.cloud_storage_backend.utils.wrapper;

import com.mipt.team4.cloud_storage_backend.config.props.StorageProps;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BatchProcessor {
  private final StorageProps storageProps;

  public <T, ID> void scoop(
      String taskName,
      Function<Pageable, Slice<T>> fetcher,
      Function<T, ID> idExtractor,
      Consumer<T> processor) {
    Pageable pageable = createInitialPageable();

    ID lastFirstId = null;
    Slice<T> slice;

    log.info("[{}] Starting scoop processing", taskName);

    do {
      slice = fetcher.apply(pageable);

      if (!slice.hasContent()) {
        break;
      }

      ID currentFirstId = idExtractor.apply(slice.getContent().getFirst());

      if (currentFirstId.equals(lastFirstId)) {
        log.error(
            "[{}] Infinite loop detected for ID: {}. Stopping batch.", taskName, currentFirstId);
        break;
      }

      lastFirstId = currentFirstId;

      for (T item : slice) {
        try {
          processor.accept(item);
        } catch (Exception e) {
          log.error("[{}] Failed to process item {}", taskName, idExtractor.apply(item), e);
        }
      }

    } while (slice.hasContent());
  }

  public <T, ID> void scroll(
      String taskName,
      Function<Pageable, Slice<T>> fetcher,
      Function<T, ID> idExtractor,
      Consumer<T> processor) {
    Pageable pageable = createInitialPageable();
    Slice<T> slice;

    log.info("[{}] Starting scroll processing", taskName);

    do {
      slice = fetcher.apply(pageable);

      for (T item : slice) {
        try {
          processor.accept(item);
        } catch (Exception e) {
          log.error("[{}] Failed to process item {}", taskName, idExtractor.apply(item), e);
        }
      }

      pageable = slice.nextPageable();
    } while (slice.hasNext());
  }

  private Pageable createInitialPageable() {
    return PageRequest.of(0, storageProps.scheduling().pageSize());
  }
}
