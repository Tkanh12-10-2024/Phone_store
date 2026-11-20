package com.phonestore.repository;

import com.phonestore.entity.InventoryCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCheckRepository extends JpaRepository<InventoryCheck, String> {
    List<InventoryCheck> findAllByOrderByNgayKiemKeDesc();
}
