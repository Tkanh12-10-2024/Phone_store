package com.phonestore.repository;

import com.phonestore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByTrangThai(String trangThai);

    @Query("SELECT p FROM Product p WHERE p.soLuongTon <= p.dinhMucTonToiThieu")
    List<Product> findLowStockProducts();
}
