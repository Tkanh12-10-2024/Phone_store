package com.phonestore.repository;

import com.phonestore.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {
    List<Supplier> findByTrangThai(String trangThai);
    List<Supplier> findByTenNhaCungCapContainingIgnoreCaseOrSoDienThoaiContaining(String ten, String sdt);
}
