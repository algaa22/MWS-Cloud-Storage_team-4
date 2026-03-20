package com.mipt.team4.cloud_storage_backend.model.share.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ShareRecipientId implements Serializable {

  @Column(name = "share_id")
  private UUID shareId;

  @Column(name = "user_id")
  private UUID userId;
}
