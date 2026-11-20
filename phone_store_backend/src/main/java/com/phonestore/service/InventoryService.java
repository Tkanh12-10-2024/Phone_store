package com.phonestore.service;

import com.phonestore.dto.request.InventoryCheckCreateRequest;
import com.phonestore.dto.response.InventoryCheckDetailResponse;
import com.phonestore.entity.*;
import com.phonestore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImeiRepository productImeiRepository;

    @Autowired
    private InventoryCheckRepository inventoryCheckRepository;

    @Autowired
    private InventoryCheckDetailRepository inventoryCheckDetailRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(String maSanPham) {
        return productRepository.findById(maSanPham);
    }

    public List<Product> getLowStockWarnings() {
        return productRepository.findLowStockProducts();
    }

    public Product updateMinStock(String maSanPham, Integer minStock) {
        return productRepository.findById(maSanPham).map(p -> {
            p.setDinhMucTonToiThieu(minStock != null ? minStock : 2);
            return productRepository.save(p);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + maSanPham));
    }

    public List<ProductImei> getImeisInStock(String maSanPham) {
        return productImeiRepository.findByMaSanPhamAndTrangThai(maSanPham, "Trong_kho");
    }

    public List<InventoryCheck> getAllInventoryChecks() {
        return inventoryCheckRepository.findAllByOrderByNgayKiemKeDesc();
    }

    public InventoryCheckDetailResponse getInventoryCheckDetail(String maPhieuKiemKe) {
        InventoryCheck check = inventoryCheckRepository.findById(maPhieuKiemKe)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu kiểm kê: " + maPhieuKiemKe));

        List<InventoryCheckDetail> details = inventoryCheckDetailRepository.findByMaPhieuKiemKe(maPhieuKiemKe);
        List<InventoryCheckDetailResponse.ItemDetail> itemDetails = new ArrayList<>();

        for (InventoryCheckDetail d : details) {
            String tenSp = productRepository.findById(d.getMaSanPham())
                    .map(Product::getTenSanPham).orElse(d.getMaSanPham());

            itemDetails.add(InventoryCheckDetailResponse.ItemDetail.builder()
                    .detail(d)
                    .tenSanPham(tenSp)
                    .build());
        }

        return InventoryCheckDetailResponse.builder()
                .inventoryCheck(check)
                .items(itemDetails)
                .build();
    }

    @Transactional
    public InventoryCheck createInventoryCheck(InventoryCheckCreateRequest request) {
        String maPhieu = request.getMaPhieuKiemKe();
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            maPhieu = "PKK" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        String trangThai = request.getTrangThai() != null ? request.getTrangThai() : "Da_can_bang";

        InventoryCheck check = InventoryCheck.builder()
                .maPhieuKiemKe(maPhieu)
                .maNhanVien(request.getMaNhanVien() != null ? request.getMaNhanVien() : "NV001")
                .ngayKiemKe(LocalDateTime.now())
                .trangThai(trangThai)
                .ghiChu(request.getGhiChu())
                .build();

        inventoryCheckRepository.save(check);

        if (request.getItems() != null) {
            for (InventoryCheckCreateRequest.CheckItemRequest item : request.getItems()) {
                Product p = productRepository.findById(item.getMaSanPham()).orElse(null);
                int soLuongHeThong = (p != null && p.getSoLuongTon() != null) ? p.getSoLuongTon() : 0;
                int soLuongThucTe = item.getSoLuongThucTe() != null ? item.getSoLuongThucTe() : soLuongHeThong;
                int chenhLech = soLuongThucTe - soLuongHeThong;

                InventoryCheckDetail detail = InventoryCheckDetail.builder()
                        .maPhieuKiemKe(maPhieu)
                        .maSanPham(item.getMaSanPham())
                        .soLuongHeThong(soLuongHeThong)
                        .soLuongThucTe(soLuongThucTe)
                        .chenhLech(chenhLech)
                        .lyDoChenhLech(item.getLyDoChenhLech())
                        .build();

                inventoryCheckDetailRepository.save(detail);

                // Nếu xác nhận cân bằng kho thì cập nhật tồn kho về số lượng thực tế
                if ("Da_can_bang".equalsIgnoreCase(trangThai) && p != null) {
                    p.setSoLuongTon(soLuongThucTe);
                    productRepository.save(p);
                }
            }
        }

        return check;
    }
}
