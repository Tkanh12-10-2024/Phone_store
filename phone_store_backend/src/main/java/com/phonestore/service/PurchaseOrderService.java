package com.phonestore.service;

import com.phonestore.dto.request.PurchaseOrderCreateRequest;
import com.phonestore.dto.response.PurchaseOrderDetailResponse;
import com.phonestore.entity.*;
import com.phonestore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private PurchaseOrderDetailRepository purchaseOrderDetailRepository;

    @Autowired
    private ProductImeiRepository productImeiRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAllByOrderByNgayNhapDesc();
    }

    public PurchaseOrderDetailResponse getOrderDetail(String maPhieuNhap) {
        PurchaseOrder po = purchaseOrderRepository.findById(maPhieuNhap)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu nhập: " + maPhieuNhap));

        Supplier supplier = supplierRepository.findById(po.getMaNhaCungCap()).orElse(null);
        List<PurchaseOrderDetail> details = purchaseOrderDetailRepository.findByMaPhieuNhap(maPhieuNhap);

        List<PurchaseOrderDetailResponse.ItemDetail> itemDetails = new ArrayList<>();
        for (PurchaseOrderDetail d : details) {
            String tenSp = productRepository.findById(d.getMaSanPham())
                    .map(Product::getTenSanPham).orElse(d.getMaSanPham());

            List<String> imeis = productImeiRepository.findByMaPhieuNhap(maPhieuNhap).stream()
                    .filter(i -> i.getMaSanPham().equals(d.getMaSanPham()))
                    .map(ProductImei::getMaImei)
                    .toList();

            itemDetails.add(PurchaseOrderDetailResponse.ItemDetail.builder()
                    .detail(d)
                    .tenSanPham(tenSp)
                    .imeis(imeis)
                    .build());
        }

        return PurchaseOrderDetailResponse.builder()
                .purchaseOrder(po)
                .supplier(supplier)
                .items(itemDetails)
                .build();
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderCreateRequest request) {
        String maPhieu = request.getMaPhieuNhap();
        if (maPhieu == null || maPhieu.trim().isEmpty()) {
            maPhieu = "PN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        }

        double tongTien = 0.0;
        if (request.getItems() != null) {
            for (PurchaseOrderCreateRequest.PurchaseItemRequest item : request.getItems()) {
                tongTien += (item.getSoLuong() != null ? item.getSoLuong() : 0)
                        * (item.getDonGiaNhap() != null ? item.getDonGiaNhap() : 0.0);
            }
        }

        double daTra = request.getDaThanhToan() != null ? request.getDaThanhToan() : 0.0;
        double congNoConLai = Math.max(0, tongTien - daTra);

        // Lưu phiếu nhập
        PurchaseOrder po = PurchaseOrder.builder()
                .maPhieuNhap(maPhieu)
                .maNhaCungCap(request.getMaNhaCungCap())
                .maNhanVien(request.getMaNhanVien() != null ? request.getMaNhanVien() : "NV001")
                .ngayNhap(LocalDateTime.now())
                .tongTien(tongTien)
                .daThanhToan(daTra)
                .congNoConLai(congNoConLai)
                .trangThai("Hoan_thanh")
                .ghiChu(request.getGhiChu())
                .build();

        purchaseOrderRepository.save(po);

        // Cập nhật công nợ nhà cung cấp
        if (congNoConLai > 0 && request.getMaNhaCungCap() != null) {
            supplierRepository.findById(request.getMaNhaCungCap()).ifPresent(s -> {
                double nợMới = (s.getCongNo() != null ? s.getCongNo() : 0.0) + congNoConLai;
                s.setCongNo(nợMới);
                supplierRepository.save(s);
            });
        }

        // Xử lý từng sản phẩm trong phiếu nhập
        if (request.getItems() != null) {
            for (PurchaseOrderCreateRequest.PurchaseItemRequest item : request.getItems()) {
                double thanhTien = (item.getSoLuong() != null ? item.getSoLuong() : 0)
                        * (item.getDonGiaNhap() != null ? item.getDonGiaNhap() : 0.0);

                PurchaseOrderDetail detail = PurchaseOrderDetail.builder()
                        .maPhieuNhap(maPhieu)
                        .maSanPham(item.getMaSanPham())
                        .soLuong(item.getSoLuong())
                        .donGiaNhap(item.getDonGiaNhap())
                        .thanhTien(thanhTien)
                        .build();

                purchaseOrderDetailRepository.save(detail);

                // Cập nhật tồn kho sản phẩm và giá nhập tham khảo
                productRepository.findById(item.getMaSanPham()).ifPresent(p -> {
                    int tonCu = p.getSoLuongTon() != null ? p.getSoLuongTon() : 0;
                    p.setSoLuongTon(tonCu + item.getSoLuong());
                    p.setGiaNhapThamKhao(item.getDonGiaNhap());
                    productRepository.save(p);
                });

                // Lưu các mã IMEI vào bảng imei_san_pham
                if (item.getImeis() != null) {
                    for (String imei : item.getImeis()) {
                        String cleanImei = imei.trim();
                        if (!cleanImei.isEmpty() && !productImeiRepository.existsByMaImei(cleanImei)) {
                            ProductImei pImei = ProductImei.builder()
                                    .maImei(cleanImei)
                                    .maSanPham(item.getMaSanPham())
                                    .maPhieuNhap(maPhieu)
                                    .trangThai("Trong_kho")
                                    .ngayNhap(LocalDateTime.now())
                                    .build();
                            productImeiRepository.save(pImei);
                        }
                    }
                }
            }
        }

        return po;
    }
}
