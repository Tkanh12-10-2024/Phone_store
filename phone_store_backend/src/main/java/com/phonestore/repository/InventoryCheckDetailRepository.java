package com.phonestore.repository;

import com.phonestore.entity.InventoryCheckDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCheckDetailRepository extends JpaRepository<InventoryCheckDetail, Long> {
    List<InventoryCheckDetail> findByMaPhieuKiemKe(String maPhieuKiemKe);
}
