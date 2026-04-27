package com.mipt.team4.cloud_storage_backend.model.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "tariff_plan", nullable = false, length = 50)
  private String tariffPlan;

  @Column(name = "amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "payment_token", nullable = false, length = 255)
  private String paymentToken;

  @Column(name = "status", length = 20)
  @Builder.Default
  private String status = "PENDING";

  @Column(name = "payment_method", length = 50)
  private String paymentMethod;

  @Column(name = "auto_renew")
  private Boolean autoRenew;

  @Column(name = "created_at")
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "storage_limit_gb")
  private Long storageLimitGb;

  @Column(name = "price")
  private BigDecimal price;

  @Column(name = "duration_days")
  private Integer durationDays;

  public void complete() {
    this.status = "COMPLETED";
    this.completedAt = LocalDateTime.now();
  }

  public void fail() {
    this.status = "FAILED";
    this.completedAt = LocalDateTime.now();
  }
}
