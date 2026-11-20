
package com.phonestore.repository;

import com.phonestore.entity.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietGioHangRepository
        extends JpaRepository<ChiTietGioHang, Integer> {

    Optional<ChiTietGioHang> findByMaGioHangAndMaSanPham(
            Integer maGioHang,
            String maSanPham
    );

    List<ChiTietGioHang> findByMaGioHang(Integer maGioHang);
}
