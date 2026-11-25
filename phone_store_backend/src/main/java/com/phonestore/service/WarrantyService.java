package com.phonestore.service;

import com.phonestore.dao.WarrantyDao;
import com.phonestore.dto.request.WarrantyCreateRequest;
import com.phonestore.dto.request.WarrantyUpdateRequest;
import com.phonestore.dto.response.WarrantyCheckResult;
import com.phonestore.dto.response.WarrantyTicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WarrantyService {

    private final WarrantyDao warrantyDao;

    public WarrantyCheckResult checkWarranty(String imei) {
        if (imei == null || imei.isBlank()) {
            throw new RuntimeException("IMEI không được để trống.");
        }
        return warrantyDao.checkWarranty(imei.trim());
    }

    public String createTicket(WarrantyCreateRequest req) {
        if ((req.getMaKhachHang() == null || req.getMaKhachHang().isBlank())
                && (req.getMaImei() == null || req.getMaImei().isBlank())) {
            throw new RuntimeException("Mã khách hàng hoặc số IMEI không được để trống.");
        }
        if (req.getLoiPhanAnh() == null || req.getLoiPhanAnh().isBlank()) {
            throw new RuntimeException("Lỗi phản ánh không được để trống.");
        }
        return warrantyDao.createTicket(req);
    }

    public List<WarrantyTicketDTO> getAll(String keyword, String trangThai) {
        return warrantyDao.findAll(keyword, trangThai);
    }

    public WarrantyTicketDTO getById(String id) {
        return warrantyDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu bảo hành: " + id));
    }

    public void updateProgress(String id, WarrantyUpdateRequest req) {
        warrantyDao.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu bảo hành: " + id));
        warrantyDao.updateProgress(id, req);
    }

    public void closeTicket(String id, Map<String, String> body) {
        String maNhanVien = body.getOrDefault("maNhanVien", "NV001");
        warrantyDao.closeTicket(id, maNhanVien);
    }
}
