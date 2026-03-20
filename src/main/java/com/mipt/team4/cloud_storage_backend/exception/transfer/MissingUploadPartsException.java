package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.BitSet;
import lombok.Getter;

@Getter
public class MissingUploadPartsException extends BaseStorageException {
  private final BitSet partsBitSet;

  public MissingUploadPartsException(BitSet partsBitSet) {
    super("Failed to complete upload: missing parts", HttpResponseStatus.CONFLICT);
    this.partsBitSet = partsBitSet;
  }
}
