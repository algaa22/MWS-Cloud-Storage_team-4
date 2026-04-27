package com.mipt.team4.cloud_storage_backend.repository.payment;

import com.mipt.team4.cloud_storage_backend.model.payment.entity.PaymentTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

  List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

  List<PaymentTransaction> findByUserIdAndStatus(UUID userId, String status);
}
