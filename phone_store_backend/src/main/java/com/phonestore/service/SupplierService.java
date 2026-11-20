package com.phonestore.service;

import com.phonestore.entity.Supplier;
import com.phonestore.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(String id) {
        return supplierRepository.findById(id);
    }

    public List<Supplier> searchSuppliers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return supplierRepository.findAll();
        }
        return supplierRepository.findByTenNhaCungCapContainingIgnoreCaseOrSoDienThoaiContaining(keyword, keyword);
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(String id, Supplier updated) {
        return supplierRepository.findById(id).map(existing -> {
            existing.setTenNhaCungCap(updated.getTenNhaCungCap());
            existing.setSoDienThoai(updated.getSoDienThoai());
            existing.setEmail(updated.getEmail());
            existing.setDiaChi(updated.getDiaChi());
            existing.setMaSoThue(updated.getMaSoThue());
            if (updated.getCongNo() != null) {
                existing.setCongNo(updated.getCongNo());
            }
            if (updated.getTrangThai() != null) {
                existing.setTrangThai(updated.getTrangThai());
            }
            return supplierRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp với mã: " + id));
    }

    public void deleteSupplier(String id) {
        supplierRepository.findById(id).ifPresent(s -> {
            // Chuyển trạng thái sang Ngung_hop_tac thay vì hard delete để tránh lỗi khóa ngoại
            s.setTrangThai("Ngung_hop_tac");
            supplierRepository.save(s);
        });
    }
}
