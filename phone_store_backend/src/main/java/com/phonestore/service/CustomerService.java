package com.phonestore.service;

import com.phonestore.dao.CustomerDao;
import com.phonestore.dto.request.CustomerCreateRequest;
import com.phonestore.dto.request.CustomerUpdateRequest;
import com.phonestore.dto.response.CustomerDTO;
import com.phonestore.dto.response.PurchaseHistoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerDao customerDao;

    public List<CustomerDTO> search(String keyword) {
        return customerDao.findAll(keyword);
    }

    public CustomerDTO getById(String id) {
        return customerDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
    }

    public CustomerDTO create(CustomerCreateRequest req) {
        if (req.getHoTen() == null || req.getHoTen().isBlank()) {
            throw new RuntimeException("Họ tên không được để trống.");
        }
        if (req.getSoDienThoai() == null || req.getSoDienThoai().isBlank()) {
            throw new RuntimeException("Số điện thoại không được để trống.");
        }
        return customerDao.create(req);
    }

    public CustomerDTO update(String id, CustomerUpdateRequest req) {
        customerDao.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
        if (req.getHoTen() == null || req.getHoTen().isBlank()) {
            throw new RuntimeException("Họ tên không được để trống.");
        }
        return customerDao.update(id, req);
    }

    public String deactivateOrDelete(String id) {
        customerDao.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
        return customerDao.deactivateOrDelete(id);
    }

    public List<PurchaseHistoryDTO> getPurchaseHistory(String id) {
        customerDao.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
        return customerDao.findPurchaseHistory(id);
    }
}
