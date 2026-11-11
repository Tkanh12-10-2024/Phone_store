package com.phonestore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", unique = true, nullable = false)
    private String invoiceCode;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status")
    private String status; // 'COMPLETED', 'CANCELLED', 'PENDING'
}