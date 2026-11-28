
(() => {
    const state = {
        products: [],
        customers: [],
        employees: [],
        activePromotions: [],
        cart: [], // { maSanPham, tenSanPham, donGia, soLuong }
        appliedVoucher: null,
        paymentMethod: 'Tien_mat',
        totals: { subTotal: 0, discount: 0, vat: 0, total: 0 }
    };

    const $ = (id) => document.getElementById(id);

    function formatVnd(amount) {
        return Number(amount || 0).toLocaleString('vi-VN') + ' đ';
    }

    async function apiRequest(path, options = {}) {
        const res = await fetch(`${API_BASE_URL}${path}`, {
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
            ...options
        });

        const text = await res.text();
        const data = text ? JSON.parse(text) : null;

        if (!res.ok) {
            throw new Error(data?.message || `Yêu cầu thất bại (HTTP ${res.status})`);
        }

        return data;
    }

    function showPosStatus(message, isError = false) {
        const el = $('posStatusRegion');

        el.textContent = message;
        el.className = isError
            ? 'rounded-xl px-4 py-3 text-sm font-semibold border bg-red-50 border-red-200 text-red-700'
            : 'rounded-xl px-4 py-3 text-sm font-semibold border bg-emerald-50 border-emerald-200 text-emerald-700';

        el.classList.remove('hidden');
    }

    function setBackendStatus(isOnline) {
        const label = $('backendStatusLabel');
        const dot = $('backendStatusDot');
        label.textContent = isOnline ? 'Đang hoạt động' : 'Mất kết nối';
        label.className = isOnline ? 'text-emerald-400 font-semibold' : 'text-red-400 font-semibold';
        dot.className = isOnline
            ? 'w-2 h-2 rounded-full bg-emerald-500'
            : 'w-2 h-2 rounded-full bg-red-500';
    }

    function attachBackdropClose(dialogEl) {
        dialogEl.addEventListener('click', (e) => {
            if (e.target !== dialogEl) return;

            const rect = dialogEl.getBoundingClientRect();
            const inside = e.clientX >= rect.left && e.clientX <= rect.right
                && e.clientY >= rect.top && e.clientY <= rect.bottom;

            if (!inside) dialogEl.close();
        });
    }

    async function loadInitialPosData() {
        let products;
        try {
            products = await apiRequest('/pos/products');
            if (!Array.isArray(products)) {
                throw new Error('Dữ liệu sản phẩm trả về không hợp lệ.');
            }
        } catch (err) {
            setBackendStatus(false);
            showPosStatus('Không thể tải sản phẩm POS: ' + err.message, true);
            return;
        }

        state.products = products;
        renderProductGrid(state.products);
        setBackendStatus(true);

        const results = await Promise.allSettled([
            apiRequest('/pos/customers'),
            apiRequest('/pos/employees'),
            apiRequest('/pos/vouchers'),
            apiRequest('/pos/brands')
        ]);
        const labels = ['khách hàng', 'nhân viên', 'voucher', 'thương hiệu'];
        const errors = [];
        const lists = results.map((result, index) => {
            if (result.status === 'fulfilled' && Array.isArray(result.value)) {
                return result.value;
            }

            const detail = result.status === 'rejected'
                ? (result.reason instanceof Error ? result.reason.message : String(result.reason))
                : 'dữ liệu trả về không hợp lệ';
            errors.push(`${labels[index]}: ${detail}`);
            return [];
        });

        const [customers, employees, vouchers, brands] = lists;
        state.customers = customers;
        state.employees = employees;
        const now = new Date();
        state.activePromotions = vouchers.filter((voucher) =>
            voucher.trangThai === 'Dang_dien_ra'
            && (!voucher.ngayBatDau || new Date(voucher.ngayBatDau) <= now)
            && (!voucher.ngayKetThuc || new Date(voucher.ngayKetThuc) >= now)
            && (!Number(voucher.soLuongPhatHanh)
                || Number(voucher.soLuongDaDung) < Number(voucher.soLuongPhatHanh))
        );

        const brandFilter = $('posBrandFilter');

        brandFilter.innerHTML = '<option value="">Tất cả thương hiệu</option>'
            + brands.map((brand) =>
                `<option value="${escapeHtml(brand.maThuongHieu)}">${escapeHtml(brand.tenThuongHieu)}</option>`
            ).join('');

        renderCustomerSelect();
        renderEmployeeSelect();
        renderActiveVoucherPills();
        recalculateTotals();

        if (errors.length) {
            showPosStatus(`Đã tải ${products.length} sản phẩm. Một số dữ liệu phụ chưa tải được: ${errors.join('; ')}.`, true);
        } else {
            $('posStatusRegion').classList.add('hidden');
        }
    }

    function renderCustomerSelect() {
        const sel = $('customerSelect');
        const currentVal = sel.value;

        sel.innerHTML = '<option value="KH_LE">Khách vãng lai (Không tích điểm)</option>'
            + state.customers.map((c) => {
                const badge = c.hangKhachHang === 'Khach_VIP'
                    ? 'VIP'
                    : (c.hangKhachHang === 'Thanh_vien' ? 'Thành viên' : 'Khách mới');

                return `<option value="${escapeHtml(c.maKhachHang)}">
                    ${escapeHtml(c.hoTen)} (${escapeHtml(c.soDienThoai)}) - ${badge} (${c.diemTichLuy}đ)
                </option>`;
            }).join('');

        if (currentVal) sel.value = currentVal;
    }

    function renderEmployeeSelect() {
        const select = $('employeeSelect');
        const previousValue = select.value;
        select.innerHTML = state.employees.map((employee) => `
            <option value="${escapeHtml(employee.maNhanVien)}">
                ${escapeHtml(employee.hoTen)} (${escapeHtml(employee.maNhanVien)})
            </option>
        `).join('');

        if (state.employees.some((employee) => employee.maNhanVien === previousValue)) {
            select.value = previousValue;
        }
        select.disabled = state.employees.length === 0;
    }

    function renderProductGrid(list) {
        const grid = $('posProductGrid');

        if (!list.length) {
            grid.innerHTML = '<div class="col-span-full py-8 text-center text-slate-400 text-sm">Chưa có sản phẩm đang kinh doanh trong danh mục POS.</div>';
            return;
        }

        grid.innerHTML = list.map((p) => {
            const outOfStock = Number(p.soLuongTon || 0) <= 0;

            return `
            <button type="button"
                    data-add-product="${escapeHtml(p.maSanPham)}"
                    ${outOfStock ? 'disabled' : ''}
                    class="text-left p-3.5 rounded-xl border ${outOfStock
                ? 'border-slate-200 bg-slate-50 opacity-60 cursor-not-allowed'
                : 'border-slate-200 bg-white hover:border-blue-500 hover:shadow-md'} transition flex flex-col justify-between gap-2">

                <div>
                    <div class="flex items-center justify-between gap-1">
                        <span class="text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded">
                            ${escapeHtml(p.tenThuongHieu)}
                        </span>

                        <span class="text-[11px] font-bold ${outOfStock ? 'text-red-600' : 'text-emerald-600'}">
                            ${outOfStock ? 'Hết hàng' : `Còn ${p.soLuongTon} máy`}
                        </span>
                    </div>

                    <p class="font-bold text-slate-900 text-sm mt-1.5 line-clamp-2">
                        ${escapeHtml(p.tenSanPham)}
                    </p>

                    <p class="text-[11px] text-slate-500 mt-0.5">
                        ${escapeHtml(p.ram)}/${escapeHtml(p.rom)} • ${escapeHtml(p.mauSac)}
                    </p>
                </div>

                <div class="flex items-center justify-between pt-1 border-t border-slate-100">
                    <strong class="text-blue-700 text-sm">${formatVnd(p.giaBan)}</strong>
                    <span class="text-xs font-bold text-slate-600">
                        ${outOfStock ? 'Tạm hết' : '+ Thêm'}
                    </span>
                </div>
            </button>`;
        }).join('');
    }

    function renderActiveVoucherPills() {
        const container = $('activeVoucherPills');

        if (!state.activePromotions.length) {
            container.innerHTML = '<span class="text-xs text-slate-400">Không có voucher đang hoạt động.</span>';
            return;
        }

        container.innerHTML = state.activePromotions.map((p) => {
            const valLabel = p.loaiGiamGia === 'Phan_tram'
                ? `-${Number(p.giaTriGiam)}%`
                : (p.loaiGiamGia === 'Qua_tang'
                    ? 'Quà tặng'
                    : `-${formatVnd(p.giaTriGiam)}`);

            return `
            <button type="button"
                    data-pick-voucher="${escapeHtml(p.codeVoucher)}"
                    title="${escapeHtml(p.tenKhuyenMai)} (Tối thiểu ${formatVnd(p.giaTriDonHangToiThieu)})"
                    class="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-blue-50 hover:text-blue-700 border border-slate-200 text-xs font-semibold transition">

                <span class="font-mono font-bold">${escapeHtml(p.codeVoucher)}</span>
                <span class="text-slate-500">(${valLabel})</span>
            </button>`;
        }).join('');
    }

    // Thêm sản phẩm vào giỏ hàng.
    // IMEI sẽ do backend tự phân bổ khi thanh toán.
    function addProductToCart(maSanPham) {
        const prod = state.products.find((p) => p.maSanPham === maSanPham);

        if (!prod || Number(prod.soLuongTon || 0) <= 0) return;

        const existingIndex = state.cart.findIndex(
            (item) => item.maSanPham === maSanPham
        );

        if (existingIndex >= 0) {
            setCartQuantity(
                existingIndex,
                state.cart[existingIndex].soLuong + 1
            );
            return;
        }

        state.cart.push({
            maSanPham: prod.maSanPham,
            tenSanPham: prod.tenSanPham,
            donGia: Number(prod.giaBan),
            soLuong: 1
        });

        renderCart();

        if (state.appliedVoucher) {
            applyVoucher(true);
        } else {
            recalculateTotals();
        }
    }

    // Cập nhật số lượng sản phẩm trong giỏ hàng.
    function setCartQuantity(index, quantity) {
        const item = state.cart[index];

        if (!item) return;

        const prod = state.products.find(
            (p) => p.maSanPham === item.maSanPham
        );

        const stockLimit = Math.max(1, Number(prod?.soLuongTon || 0));

        const normalized = Math.min(
            stockLimit,
            Math.max(1, Number.parseInt(quantity, 10) || 1)
        );

        item.soLuong = normalized;

        if (normalized !== Number(quantity) && Number(quantity) > stockLimit) {
            showPosStatus(
                `Số lượng tối đa cho ${item.tenSanPham} là ${stockLimit}.`,
                true
            );
        } else {
            $('posStatusRegion').classList.add('hidden');
        }

        renderCart();

        if (state.appliedVoucher) {
            applyVoucher(true);
        } else {
            recalculateTotals();
        }
    }

    // Hiển thị giỏ hàng.
    function renderCart() {
        $('cartItemCount').textContent = state.cart.reduce(
            (sum, item) => sum + Number(item.soLuong || 1),
            0
        );

        const container = $('posCartList');

        if (!state.cart.length) {
            container.innerHTML = `
                <div class="py-10 text-center text-slate-400 text-sm">
                    Chưa có thiết bị nào trong giỏ hàng. Bấm chọn máy ở danh mục phía trên.
                </div>`;
            return;
        }

        container.innerHTML = state.cart.map((item, idx) => {
            const stockLimit = Math.max(
                1,
                Number(state.products.find(
                    (p) => p.maSanPham === item.maSanPham
                )?.soLuongTon || 0)
            );

            const imeiControl = `
                <p class="text-[11px] text-slate-500 mt-1">
                    <i class="fa-solid fa-barcode mr-1"></i>
                    Hệ thống tự động phân bổ ${item.soLuong} IMEI khi thanh toán.
                </p>
            `;

            return `
            <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">

                <div class="min-w-0 flex-1">
                    <p class="font-bold text-slate-900 text-sm">
                        ${escapeHtml(item.tenSanPham)}
                    </p>
                    ${imeiControl}
                </div>

                <div class="flex items-center gap-3 justify-between sm:justify-end">

                    <div class="flex items-center rounded-xl border border-slate-300 bg-white overflow-hidden">

                        <button type="button"
                                data-cart-qty="${idx}"
                                data-cart-delta="-1"
                                ${item.soLuong <= 1 ? 'disabled' : ''}
                                aria-label="Giảm số lượng ${escapeHtml(item.tenSanPham)}"
                                class="w-9 h-9 text-slate-700 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed">
                            <i class="fa-solid fa-minus text-xs"></i>
                        </button>

                        <input type="number"
                               min="1"
                               max="${stockLimit}"
                               step="1"
                               value="${item.soLuong}"
                               data-cart-qty-input="${idx}"
                               aria-label="Số lượng ${escapeHtml(item.tenSanPham)}"
                               class="w-14 h-9 border-x border-slate-200 text-center text-sm font-bold text-slate-900 outline-none">

                        <button type="button"
                                data-cart-qty="${idx}"
                                data-cart-delta="1"
                                ${item.soLuong >= stockLimit ? 'disabled' : ''}
                                aria-label="Tăng số lượng ${escapeHtml(item.tenSanPham)}"
                                class="w-9 h-9 text-slate-700 hover:bg-slate-100 disabled:opacity-40 disabled:cursor-not-allowed">
                            <i class="fa-solid fa-plus text-xs"></i>
                        </button>

                    </div>

                    <strong class="text-blue-700 text-sm whitespace-nowrap">
                        ${formatVnd(item.donGia * item.soLuong)}
                    </strong>

                    <button type="button"
                            data-remove-idx="${idx}"
                            aria-label="Xóa ${escapeHtml(item.tenSanPham)}"
                            class="w-8 h-8 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 flex items-center justify-center">
                        <i class="fa-solid fa-trash-can text-xs"></i>
                    </button>

                </div>
            </div>`;
        }).join('');
    }

    // Tính tổng tiền giỏ hàng.
    function recalculateTotals() {
        const subTotal = state.cart.reduce(
            (sum, item) => sum + Number(item.donGia) * Number(item.soLuong || 1),
            0
        );

        const vatRate = Number($('vatPercentSelect').value || 0);

        let discount = 0;
        let gift = null;

        if (state.appliedVoucher && state.appliedVoucher.valid) {
            discount = Number(state.appliedVoucher.soTienGiam || 0);
            gift = state.appliedVoucher.quaTang || null;
        }

        const afterDiscount = Math.max(0, subTotal - discount);
        const vat = Math.round(afterDiscount * vatRate / 100);
        const total = afterDiscount + vat;

        state.totals = {
            subTotal,
            discount,
            vat,
            total
        };

        $('txtSubTotal').textContent = formatVnd(subTotal);
        $('txtDiscount').textContent = '-' + formatVnd(discount);
        $('txtVat').textContent = formatVnd(vat);
        $('txtTotal').textContent = formatVnd(total);

        if (gift) {
            $('giftRow').classList.remove('hidden');
            $('txtGiftLabel').textContent = gift;
        } else {
            $('giftRow').classList.add('hidden');
            $('txtGiftLabel').textContent = '-';
        }

        updateCashChange();
    }

    // Cập nhật tiền thối lại.
    function updateCashChange() {
        const given = Number($('customerGivenMoney').value || 0);
        const change = Math.max(0, given - state.totals.total);

        $('txtChangeMoney').textContent = formatVnd(change);
    }

    // Kiểm tra và áp dụng voucher.
    async function applyVoucher(silentRevalidate = false) {
        const code = $('voucherInput').value.trim();

        if (!state.cart.length) {
            state.appliedVoucher = null;
            recalculateTotals();

            if (!silentRevalidate) {
                showVoucherErrorModal(
                    'Giỏ hàng đang trống. Vui lòng chọn sản phẩm trước khi áp dụng voucher.'
                );
            }

            return;
        }

        if (!code) {
            if (!silentRevalidate) {
                showVoucherErrorModal(
                    'Vui lòng nhập hoặc chọn mã voucher cần áp dụng.'
                );
            }

            return;
        }

        try {
            const res = await apiRequest('/pos/apply-voucher', {
                method: 'POST',
                body: JSON.stringify({
                    codeVoucher: code,
                    maKhachHang: $('customerSelect').value,
                    vatPhanTram: Number($('vatPercentSelect').value || 0),

                    items: state.cart.map((c) => ({
                        maSanPham: c.maSanPham,
                        soLuong: c.soLuong,
                        donGia: c.donGia
                    }))
                })
            });

            if (res.valid) {
                state.appliedVoucher = res;

                $('voucherInput').value = res.codeVoucher;
                $('btnRemoveVoucher').classList.remove('hidden');

                const msgBox = $('voucherMessage');

                msgBox.textContent = res.message;
                msgBox.className = 'rounded-xl px-3.5 py-2.5 text-xs font-semibold border bg-emerald-50 border-emerald-200 text-emerald-700';
                msgBox.classList.remove('hidden');

                state.totals = {
                    subTotal: Number(res.tamTinh),
                    discount: Number(res.soTienGiam),
                    vat: Number(res.tienVat),
                    total: Number(res.tongThanhToan)
                };

                $('txtSubTotal').textContent = formatVnd(res.tamTinh);
                $('txtDiscount').textContent = '-' + formatVnd(res.soTienGiam);
                $('txtVat').textContent = formatVnd(res.tienVat);
                $('txtTotal').textContent = formatVnd(res.tongThanhToan);

                if (res.quaTang) {
                    $('giftRow').classList.remove('hidden');
                    $('txtGiftLabel').textContent = res.quaTang;
                } else {
                    $('giftRow').classList.add('hidden');
                }

                updateCashChange();

            } else {
                removeVoucherState();
                showVoucherErrorModal(res.message);
            }

        } catch (err) {
            removeVoucherState();
            showVoucherErrorModal(err.message);
        }
    }

    // Xóa trạng thái voucher.
    function removeVoucherState() {
        state.appliedVoucher = null;

        $('btnRemoveVoucher').classList.add('hidden');
        $('voucherMessage').classList.add('hidden');

        recalculateTotals();
    }

    function showVoucherErrorModal(message) {
        $('voucherErrorBody').textContent = message;
        $('voucherErrorDialog').showModal();
    }

    // Thanh toán POS.
    async function handleCheckout() {
        if (!state.cart.length) {
            showPosStatus(
                'Giỏ hàng đang trống. Vui lòng chọn sản phẩm cần thanh toán.',
                true
            );
            return;
        }

        if (!$('employeeSelect').value) {
            showPosStatus(
                'Không có nhân viên đang hoạt động để thực hiện giao dịch.',
                true
            );
            return;
        }

        const given = state.paymentMethod === 'Tien_mat'
            ? Number($('customerGivenMoney').value || 0)
            : state.totals.total;

        if (state.paymentMethod === 'Tien_mat' && given < state.totals.total) {
            showPosStatus(
                `Tiền khách đưa phải >= ${formatVnd(state.totals.total)}`,
                true
            );
            return;
        }

        const checkoutButton = $('btnCheckout');

        try {
            checkoutButton.disabled = true;

            const invoice = await apiRequest('/pos/checkout', {
                method: 'POST',
                body: JSON.stringify({
                    maKhachHang: $('customerSelect').value,

                    maNhanVien: $('employeeSelect').value,

                    codeVoucher: state.appliedVoucher
                        ? state.appliedVoucher.codeVoucher
                        : null,

                    phuongThucThanhToan: state.paymentMethod,
                    vatPhanTram: Number($('vatPercentSelect').value || 0),
                    tienKhachDua: given,

                    // Không gửi IMEI từ frontend.
                    // Backend tự khóa và phân bổ IMEI còn trong kho.
                    items: state.cart.map((c) => ({
                        maSanPham: c.maSanPham,
                        soLuong: c.soLuong,
                        donGia: c.donGia
                    }))
                })
            });

            renderInvoiceModal(invoice);

            state.cart = [];
            state.appliedVoucher = null;

            $('voucherInput').value = '';
            $('customerGivenMoney').value = '';

            removeVoucherState();
            renderCart();

            await loadInitialPosData();

            showPosStatus(
                `Đã xuất hóa đơn ${invoice.maHoaDon} thành công!`
            );

        } catch (err) {
            showPosStatus(err.message, true);

        } finally {
            checkoutButton.disabled = false;
        }
    }

    // Hiển thị hóa đơn sau khi thanh toán.
    function renderInvoiceModal(inv) {
        $('invCode').textContent = inv.maHoaDon;

        const linesHtml = (inv.items || []).map((it, idx) => `
            <tr class="border-b border-slate-100">
                <td class="py-2 pr-2">${idx + 1}</td>

                <td class="py-2 pr-2">
                    <div class="font-bold text-slate-900">
                        ${escapeHtml(it.tenSanPham)}
                    </div>

                    <div class="text-xs text-slate-500 font-mono">
                        IMEI: ${escapeHtml((it.danhSachImei || []).join(', ') || it.maImei || '--')} • BH ${it.thoiGianBaoHanhThang} tháng
                    </div>
                </td>

                <td class="py-2 text-right font-semibold">
                    ${formatVnd(it.donGia)}
                </td>
            </tr>
        `).join('');

        $('invoiceDetails').innerHTML = `
            <div class="text-center border-b border-slate-200 pb-3">
                <h3 class="text-lg font-extrabold text-slate-900">
                    PHONESTORE PRO - HÓA ĐƠN BÁN HÀNG
                </h3>

                <p class="text-xs text-slate-500">
                    Số HĐ: <strong>${escapeHtml(inv.maHoaDon)}</strong>
                    • Ngày: ${new Date(inv.ngayLap).toLocaleString('vi-VN')}
                </p>
            </div>

            <div class="grid grid-cols-2 gap-3 text-xs bg-slate-50 p-3 rounded-xl border border-slate-200">
                <div>
                    <span class="text-slate-500">Khách hàng:</span>
                    <strong>${escapeHtml(inv.tenKhachHang)}</strong>
                </div>

                <div>
                    <span class="text-slate-500">Thu ngân:</span>
                    <strong>${escapeHtml(inv.tenNhanVien)}</strong>
                </div>

                <div>
                    <span class="text-slate-500">Thanh toán:</span>
                    <strong>${escapeHtml(inv.phuongThucThanhToan)}</strong>
                </div>

                <div>
                    <span class="text-slate-500">Mã khuyến mãi:</span>
                    <strong>${escapeHtml(inv.codeVoucher || 'Không áp dụng')}</strong>
                </div>
            </div>

            <table class="w-full text-left">
                <thead>
                    <tr class="text-xs text-slate-500 uppercase border-b border-slate-200">
                        <th class="py-2">#</th>
                        <th class="py-2">Sản phẩm &amp; IMEI</th>
                        <th class="py-2 text-right">Đơn giá</th>
                    </tr>
                </thead>

                <tbody>${linesHtml}</tbody>
            </table>

            <div class="space-y-1.5 border-t border-slate-200 pt-3 text-sm">
                <div class="flex justify-between">
                    <span>Tổng tiền hàng:</span>
                    <strong>${formatVnd(inv.tongTienHang)}</strong>
                </div>

                <div class="flex justify-between text-emerald-700">
                    <span>Chiết khấu KM:</span>
                    <strong>-${formatVnd(inv.tienGiamGia)}</strong>
                </div>

                ${inv.quaTang ? `
                    <div class="flex justify-between text-blue-700">
                        <span>Quà tặng kèm:</span>
                        <strong>${escapeHtml(inv.quaTang)}</strong>
                    </div>
                ` : ''}

                <div class="flex justify-between">
                    <span>Thuế VAT:</span>
                    <strong>${formatVnd(inv.tienThueVat)}</strong>
                </div>

                <div class="flex justify-between text-base font-extrabold text-blue-700 pt-1 border-t border-slate-200">
                    <span>Tổng thanh toán:</span>
                    <span>${formatVnd(inv.tongThanhToan)}</span>
                </div>

                <div class="flex justify-between text-xs text-slate-500">
                    <span>Tiền khách đưa: ${formatVnd(inv.tienKhachDua)}</span>
                    <span>Tiền thối lại: ${formatVnd(inv.tienThua)}</span>
                </div>
            </div>
        `;

        $('invoiceModal').showModal();
    }

    // Mở danh sách hóa đơn gần nhất.
    async function openRecentInvoicesModal() {
        try {
            const list = await apiRequest('/pos/invoices?limit=10');

            if (!list.length) {
                $('recentInvoicesBody').innerHTML = `
                    <p class="text-center text-slate-400 py-8">
                        Chưa có hóa đơn nào.
                    </p>
                `;
            } else {
                $('recentInvoicesBody').innerHTML = `
                <table class="w-full text-left border-collapse">
                    <thead>
                        <tr class="bg-slate-50 text-xs uppercase text-slate-600 border-b border-slate-200">
                            <th class="px-3 py-2.5">Mã HĐ</th>
                            <th class="px-3 py-2.5">Thời gian</th>
                            <th class="px-3 py-2.5">Khách hàng</th>
                            <th class="px-3 py-2.5">Voucher</th>
                            <th class="px-3 py-2.5 text-right">Giảm giá</th>
                            <th class="px-3 py-2.5 text-right">Thực thu</th>
                        </tr>
                    </thead>

                    <tbody class="divide-y divide-slate-100">
                        ${list.map((inv) => `
                            <tr>
                                <td class="px-3 py-2.5 font-mono font-bold text-blue-700">
                                    ${escapeHtml(inv.maHoaDon)}
                                </td>

                                <td class="px-3 py-2.5 text-xs text-slate-600">
                                    ${new Date(inv.ngayLap).toLocaleString('vi-VN')}
                                </td>

                                <td class="px-3 py-2.5 font-semibold">
                                    ${escapeHtml(inv.tenKhachHang)}
                                </td>

                                <td class="px-3 py-2.5 font-mono text-xs">
                                    ${escapeHtml(inv.codeVoucher || '-')}
                                </td>

                                <td class="px-3 py-2.5 text-right text-emerald-700">
                                    -${formatVnd(inv.tienGiamGia)}
                                </td>

                                <td class="px-3 py-2.5 text-right font-bold text-slate-900">
                                    ${formatVnd(inv.tongThanhToan)}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>`;
            }

            $('recentInvoicesDialog').showModal();

        } catch (err) {
            showPosStatus(err.message, true);
        }
    }

    // Tìm kiếm và lọc sản phẩm.
    function filterDisplayedProducts() {
        const kw = $('posSearchInput').value.trim().toLowerCase();
        const brandId = $('posBrandFilter').value;

        const filtered = state.products.filter((p) => {
            const matchBrand = !brandId
                || String(p.maThuongHieu) === String(brandId);

            const matchKw = !kw
                || p.tenSanPham.toLowerCase().includes(kw)
                || p.maSanPham.toLowerCase().includes(kw)
                || (p.dongMay || '').toLowerCase().includes(kw);

            return matchBrand && matchKw;
        });

        renderProductGrid(filtered);
    }

    // Escape HTML để tránh chèn HTML không an toàn từ dữ liệu.
    function escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    // Khởi tạo sự kiện khi trang đã tải xong.
    document.addEventListener('DOMContentLoaded', async () => {
        attachBackdropClose($('voucherErrorDialog'));
        attachBackdropClose($('invoiceModal'));
        attachBackdropClose($('recentInvoicesDialog'));

        $('posSearchInput').addEventListener('input', filterDisplayedProducts);
        $('posBrandFilter').addEventListener('change', filterDisplayedProducts);

        $('posSearchForm').addEventListener('submit', (e) => {
            e.preventDefault();
            filterDisplayedProducts();
        });

        // Chọn sản phẩm.
        $('posProductGrid').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-add-product]');

            if (btn) {
                addProductToCart(btn.dataset.addProduct);
            }
        });

        // Các thao tác trong giỏ hàng.
        $('posCartList').addEventListener('click', (e) => {
            const qtyBtn = e.target.closest('button[data-cart-qty]');

            if (qtyBtn) {
                const idx = Number(qtyBtn.dataset.cartQty);
                const delta = Number(qtyBtn.dataset.cartDelta);

                if (state.cart[idx]) {
                    setCartQuantity(
                        idx,
                        state.cart[idx].soLuong + delta
                    );
                }

                return;
            }

            const btn = e.target.closest('button[data-remove-idx]');

            if (!btn) return;

            state.cart.splice(Number(btn.dataset.removeIdx), 1);

            renderCart();

            if (state.appliedVoucher) {
                applyVoucher(true);
            } else {
                recalculateTotals();
            }
        });

        // Thay đổi số lượng trực tiếp bằng ô nhập.
        $('posCartList').addEventListener('change', (e) => {
            const qtyInput = e.target.closest('input[data-cart-qty-input]');

            if (qtyInput) {
                setCartQuantity(
                    Number(qtyInput.dataset.cartQtyInput),
                    qtyInput.value
                );
            }
        });

        // Xóa toàn bộ giỏ hàng.
        $('btnClearCart').addEventListener('click', () => {
            state.cart = [];

            renderCart();
            removeVoucherState();
        });

        // Chọn voucher từ danh sách voucher đang hoạt động.
        $('activeVoucherPills').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-pick-voucher]');

            if (!btn) return;

            $('voucherInput').value = btn.dataset.pickVoucher;
            applyVoucher(false);
        });

        $('btnApplyVoucher').addEventListener('click', () => {
            applyVoucher(false);
        });

        $('voucherInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyVoucher(false);
            }
        });

        // Gỡ voucher.
        $('btnRemoveVoucher').addEventListener('click', () => {
            $('voucherInput').value = '';
            removeVoucherState();
        });

        // Khi thay đổi khách hàng, kiểm tra lại voucher.
        $('customerSelect').addEventListener('change', () => {
            if (state.appliedVoucher) {
                applyVoucher(true);
            }
        });

        // Khi thay đổi VAT, tính lại tổng tiền.
        $('vatPercentSelect').addEventListener('change', () => {
            if (state.appliedVoucher) {
                applyVoucher(true);
            } else {
                recalculateTotals();
            }
        });

        // Chọn phương thức thanh toán.
        document.querySelectorAll('.pay-method-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                state.paymentMethod = btn.dataset.paymethod;

                document.querySelectorAll('.pay-method-btn').forEach((b) => {
                    b.className = b === btn
                        ? 'pay-method-btn min-h-[40px] px-3 py-2 rounded-xl border-2 border-blue-600 bg-blue-50 text-blue-700 text-xs font-bold'
                        : 'pay-method-btn min-h-[40px] px-3 py-2 rounded-xl border border-slate-300 bg-white text-slate-700 text-xs font-bold';
                });

                $('cashInputGroup').classList.toggle(
                    'hidden',
                    state.paymentMethod !== 'Tien_mat'
                );
            });
        });

        $('customerGivenMoney').addEventListener('input', updateCashChange);

        // Thanh toán.
        $('btnCheckout').addEventListener('click', handleCheckout);

        // Đóng các modal.
        $('btnCloseVoucherError').addEventListener('click', () => {
            $('voucherErrorDialog').close();
        });

        $('btnCloseInvoiceModal').addEventListener('click', () => {
            $('invoiceModal').close();
        });

        $('btnDoneInvoiceModal').addEventListener('click', () => {
            $('invoiceModal').close();
        });

        // In hóa đơn.
        if ($('btnPrintInvoice')) {
            $('btnPrintInvoice').addEventListener('click', () => {
                const html = $('invoiceDetails').innerHTML;

                const w = window.open(
                    '',
                    '_blank',
                    'width=800,height=700'
                );

                if (!w) {
                    window.print();
                    return;
                }

                w.document.write(`
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head>
                        <meta charset="UTF-8">
                        <title>In hóa đơn</title>
                        <script src="https://cdn.tailwindcss.com"><\/script>
                        <style>
                            body {
                                font-family: sans-serif;
                                padding: 24px;
                                color: #0f172a;
                                background: #fff;
                            }
                        </style>
                    </head>
                    <body>
                        ${html}
                        <script>
                            window.onload = () => {
                                setTimeout(() => {
                                    window.print();
                                }, 250);
                            };
                        <\/script>
                    </body>
                    </html>
                `);

                w.document.close();
            });
        }

        // Hóa đơn gần đây.
        $('btnRecentInvoices').addEventListener(
            'click',
            openRecentInvoicesModal
        );

        $('btnCloseRecentInvoices').addEventListener('click', () => {
            $('recentInvoicesDialog').close();
        });

        // Tải dữ liệu POS ban đầu.
        await loadInitialPosData();
    });
})();
