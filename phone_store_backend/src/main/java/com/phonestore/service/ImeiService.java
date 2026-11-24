package com.phonestore.service;

import com.phonestore.entity.Product;
import com.phonestore.entity.ProductImei;
import com.phonestore.repository.ProductImeiRepository;
import com.phonestore.repository.ProductRepository;
import com.phonestore.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImeiService {

    @Autowired
    private ProductImeiRepository productImeiRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    public List<Map<String, Object>> listImeis(String keyword, String trangThai) {
        List<ProductImei> imeis = productImeiRepository.findAll();
        Map<String, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getMaSanPham, p -> p, (a, b) -> a));

        String q = keyword == null ? "" : keyword.trim().toLowerCase();
        String st = trangThai == null ? "" : trangThai.trim();

        return imeis.stream()
                .filter(im -> st.isEmpty() || st.equalsIgnoreCase(im.getTrangThai()))
                .map(im -> toResponse(im, productMap.get(im.getMaSanPham())))
                .filter(m -> {
                    if (q.isEmpty()) return true;
                    return contains(m.get("maImei"), q)
                            || contains(m.get("tenSanPham"), q)
                            || contains(m.get("maSanPham"), q)
                            || contains(m.get("maPhieuNhap"), q)
                            || contains(m.get("maHoaDon"), q)
                            || contains(m.get("mauSac"), q);
                })
                .sorted(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.getOrDefault("ngayNhap", ""))).reversed())
                .collect(Collectors.toList());
    }

    public Map<String, Object> getImeiDetail(String maImei) {
        ProductImei imei = productImeiRepository.findById(maImei)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy IMEI: " + maImei));
        Product product = productRepository.findById(imei.getMaSanPham()).orElse(null);
        return toResponse(imei, product);
    }

    @Transactional
    public Map<String, Object> addImeis(String maSanPham, String maPhieuNhap, List<String> imeis) {
        if (maSanPham == null || maSanPham.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng chọn sản phẩm");
        }
        if (maPhieuNhap == null || maPhieuNhap.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập mã phiếu nhập");
        }
        if (imeis == null || imeis.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập ít nhất một mã IMEI");
        }
        productRepository.findById(maSanPham)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm: " + maSanPham));
        purchaseOrderRepository.findById(maPhieuNhap)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập: " + maPhieuNhap));

        int added = 0;
        List<String> skipped = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (String raw : imeis) {
            if (raw == null) continue;
            String imei = raw.trim();
            if (imei.isEmpty()) continue;
            if (productImeiRepository.existsByMaImei(imei)) {
                skipped.add(imei);
                continue;
            }
            ProductImei entity = ProductImei.builder()
                    .maImei(imei)
                    .maSanPham(maSanPham)
                    .maPhieuNhap(maPhieuNhap)
                    .trangThai("Trong_kho")
                    .ngayNhap(now)
                    .build();
            productImeiRepository.save(entity);
            added++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("added", added);
        result.put("skipped", skipped);
        result.put("message", "Đã thêm " + added + " IMEI" + (skipped.isEmpty() ? "" : ", bỏ qua " + skipped.size() + " IMEI trùng"));
        return result;
    }

    private Map<String, Object> toResponse(ProductImei im, Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("maImei", im.getMaImei());
        m.put("maSanPham", im.getMaSanPham());
        m.put("tenSanPham", p != null ? p.getTenSanPham() : im.getMaSanPham());
        m.put("mauSac", p != null ? p.getMauSac() : null);
        m.put("ram", p != null ? p.getRam() : null);
        m.put("rom", p != null ? p.getRom() : null);
        m.put("maPhieuNhap", im.getMaPhieuNhap());
        m.put("maHoaDon", im.getMaHoaDon());
        m.put("trangThai", im.getTrangThai());
        m.put("ngayNhap", im.getNgayNhap());
        m.put("ngayBan", im.getNgayBan());
        m.put("hanBaoHanh", im.getHanBaoHanh());
        return m;
    }

    private boolean contains(Object value, String q) {
        return value != null && String.valueOf(value).toLowerCase().contains(q);
    }
}
