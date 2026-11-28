
package com.phonestore.dto.pos;

import java.math.BigDecimal;

/**
 * DTO trả kết quả kiểm tra và áp dụng voucher tại POS.
 */
public class PosVoucherResponse {

    private boolean valid;

    private String codeVoucher;

    private BigDecimal tamTinh;

    private BigDecimal soTienGiam;

    private BigDecimal tienVat;

    private BigDecimal tongThanhToan;

    private String quaTang;

    private String message;

    private String errorCode;

    public PosVoucherResponse() {
    }

    public PosVoucherResponse(
            boolean valid,
            String codeVoucher,
            BigDecimal tamTinh,
            BigDecimal soTienGiam,
            BigDecimal tienVat,
            BigDecimal tongThanhToan,
            String quaTang,
            String message,
            String errorCode
    ) {
        this.valid = valid;
        this.codeVoucher = codeVoucher;
        this.tamTinh = tamTinh;
        this.soTienGiam = soTienGiam;
        this.tienVat = tienVat;
        this.tongThanhToan = tongThanhToan;
        this.quaTang = quaTang;
        this.message = message;
        this.errorCode = errorCode;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCodeVoucher() {
        return codeVoucher;
    }

    public void setCodeVoucher(String codeVoucher) {
        this.codeVoucher = codeVoucher;
    }

    public BigDecimal getTamTinh() {
        return tamTinh;
    }

    public void setTamTinh(BigDecimal tamTinh) {
        this.tamTinh = tamTinh;
    }

    public BigDecimal getSoTienGiam() {
        return soTienGiam;
    }

    public void setSoTienGiam(BigDecimal soTienGiam) {
        this.soTienGiam = soTienGiam;
    }

    public BigDecimal getTienVat() {
        return tienVat;
    }

    public void setTienVat(BigDecimal tienVat) {
        this.tienVat = tienVat;
    }

    public BigDecimal getTongThanhToan() {
        return tongThanhToan;
    }

    public void setTongThanhToan(BigDecimal tongThanhToan) {
        this.tongThanhToan = tongThanhToan;
    }

    public String getQuaTang() {
        return quaTang;
    }

    public void setQuaTang(String quaTang) {
        this.quaTang = quaTang;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
