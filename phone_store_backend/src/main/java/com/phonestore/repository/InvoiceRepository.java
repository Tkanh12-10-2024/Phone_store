package com.phonestore.repository;

import com.phonestore.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'COMPLETED'")
    Double getTotalRevenue();

    long countByStatus(String status);

    List<Invoice> findTop5ByOrderByIdDesc();
}