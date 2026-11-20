package com.phonestore.repository;

import com.phonestore.entity.ProductImei;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImeiRepository extends JpaRepository<ProductImei, String> {
    List<ProductImei> findByMaSanPhamAndTrangThai(String maSanPham, String trangThai);
    List<ProductImei> findByMaPhieuNhap(String maPhieuNhap);
    List<ProductImei> findByMaSanPham(String maSanPham);
    boolean existsByMaImei(String maImei);
}
