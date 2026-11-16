(() => {
    const state = {
        products: [],
        customers: [],
        activePromotions: [],
        cart: [], // { maSanPham, tenSanPham, donGia, soLuong, maImei, availableImeis }
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
        try {
            const [products, customers, promos, opts] = await Promise.all([
                apiRequest('/pos/products'),
                apiRequest('/pos/customers'),
                apiRequest('/promotions?trangThai=Dang_dien_ra'),
                apiRequest('/promotions/options')
            ]);

            state.products = products || [];
            state.customers = customers || [];
            state.activePromotions = promos || [];

            const brandFilter = $('posBrandFilter');
            brandFilter.innerHTML = '<option value="">Tất cả thương hiệu</option>'
                + (opts.brands || []).map((b) => `<option value="${b.value}">${escapeHtml(b.label)}</option>`).join('');

            renderCustomerSelect();
            renderProductGrid(state.products);
            renderActiveVoucherPills();
            recalculateTotals();
        } catch (err) {
            showPosStatus('Không thể kết nối API POS: ' + err.message, true);
        }
    }

    function renderCustomerSelect() {
        const sel = $('customerSelect');
        const currentVal = sel.value;
        sel.innerHTML = '<option value="KH_LE">Khách vãng lai (Không tích điểm)</option>'
            + state.customers.map((c) => {
                const badge = c.hangKhachHang === 'Khach_VIP' ? 'VIP' : (c.hangKhachHang === 'Thanh_vien' ? 'Thành viên' : 'Khách mới');
                return `<option value="${escapeHtml(c.maKhachHang)}">${escapeHtml(c.hoTen)} (${escapeHtml(c.soDienThoai)}) - ${badge} (${c.diemTichLuy}đ)</option>`;
            }).join('');
        if (currentVal) sel.value = currentVal;
    }

    function renderProductGrid(list) {
        const grid = $('posProductGrid');
        if (!list.length) {
            grid.innerHTML = '<div class="col-span-full py-8 text-center text-slate-400 text-sm">Không tìm thấy thiết bị phù hợp.</div>';
            return;
        }
        grid.innerHTML = list.map((p) => {
            const outOfStock = Number(p.soLuongTon || 0) <= 0 || !(p.imeis && p.imeis.length);
            return `
            <button type="button" data-add-product="${escapeHtml(p.maSanPham)}" ${outOfStock ? 'disabled' : ''}
                    class="text-left p-3.5 rounded-xl border ${outOfStock ? 'border-slate-200 bg-slate-50 opacity-60 cursor-not-allowed' : 'border-slate-200 bg-white hover:border-blue-500 hover:shadow-md'} transition flex flex-col justify-between gap-2">
                <div>
                    <div class="flex items-center justify-between gap-1">
                        <span class="text-[11px] font-bold text-blue-700 bg-blue-50 px-2 py-0.5 rounded">${escapeHtml(p.tenThuongHieu)}</span>
                        <span class="text-[11px] font-bold ${outOfStock ? 'text-red-600' : 'text-emerald-600'}">
                            ${outOfStock ? 'Hết hàng' : `Còn ${p.soLuongTon} máy`}
                        </span>
                    </div>
                    <p class="font-bold text-slate-900 text-sm mt-1.5 line-clamp-2">${escapeHtml(p.tenSanPham)}</p>
                    <p class="text-[11px] text-slate-500 mt-0.5">${escapeHtml(p.ram)}/${escapeHtml(p.rom)} • ${escapeHtml(p.mauSac)}</p>
                </div>
                <div class="flex items-center justify-between pt-1 border-t border-slate-100">
                    <strong class="text-blue-700 text-sm">${formatVnd(p.giaBan)}</strong>
                    <span class="text-xs font-bold text-slate-600">${outOfStock ? 'Tạm hết' : '+ Thêm'}</span>
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
                : (p.loaiGiamGia === 'Qua_tang' ? 'Quà tặng' : `-${formatVnd(p.giaTriGiam)}`);
            return `
            <button type="button" data-pick-voucher="${escapeHtml(p.codeVoucher)}"
                    title="${escapeHtml(p.tenKhuyenMai)} (Tối thiểu ${formatVnd(p.giaTriDonHangToiThieu)})"
                    class="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-blue-50 hover:text-blue-700 border border-slate-200 text-xs font-semibold transition">
                <span class="font-mono font-bold">${escapeHtml(p.codeVoucher)}</span>
                <span class="text-slate-500">(${valLabel})</span>
            </button>`;
        }).join('');
    }

    function addProductToCart(maSanPham) {
        const prod = state.products.find((p) => p.maSanPham === maSanPham);
        if (!prod || !prod.imeis || !prod.imeis.length) return;

        const usedImeis = new Set(state.cart.map((c) => c.maImei));
        const nextImei = prod.imeis.find((im) => !usedImeis.has(im));
        if (!nextImei) {
            showPosStatus(`Tất cả IMEI khả dụng của ${prod.tenSanPham} đã có trong giỏ hàng.`, true);
            return;
        }

        state.cart.push({
            maSanPham: prod.maSanPham,
            tenSanPham: prod.tenSanPham,
            donGia: Number(prod.giaBan),
            soLuong: 1,
            maImei: nextImei,
            availableImeis: prod.imeis
        });

        renderCart();
        if (state.appliedVoucher) {
            applyVoucher(true);
        } else {
            recalculateTotals();
        }
    }

    function renderCart() {
        $('cartItemCount').textContent = state.cart.length;
        const container = $('posCartList');
        if (!state.cart.length) {
            container.innerHTML = '<div class="py-10 text-center text-slate-400 text-sm">Chưa có thiết bị nào trong giỏ hàng. Bấm chọn máy ở danh mục phía trên.</div>';
            return;
        }

        container.innerHTML = state.cart.map((item, idx) => {
            const imeiOptions = item.availableImeis.map((im) =>
                `<option value="${escapeHtml(im)}" ${im === item.maImei ? 'selected' : ''}>${escapeHtml(im)}</option>`
            ).join('');
            return `
            <div class="p-3.5 rounded-xl bg-slate-50 border border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div class="min-w-0 flex-1">
                    <p class="font-bold text-slate-900 text-sm">${escapeHtml(item.tenSanPham)}</p>
                    <div class="flex items-center gap-2 mt-1">
                        <label for="cartImei_${idx}" class="text-[11px] font-bold text-slate-500">IMEI:</label>
                        <select id="cartImei_${idx}" data-imei-idx="${idx}"
                                class="px-2 py-1 bg-white border border-slate-300 rounded-lg text-xs font-mono">
                            ${imeiOptions}
                        </select>
                    </div>
                </div>
                <div class="flex items-center gap-3 justify-between sm:justify-end">
                    <strong class="text-blue-700 text-sm">${formatVnd(item.donGia)}</strong>
                    <button type="button" data-remove-idx="${idx}" aria-label="Xóa ${escapeHtml(item.tenSanPham)}"
                            class="w-8 h-8 rounded-lg bg-red-50 hover:bg-red-100 text-red-600 flex items-center justify-center">
                        <i class="fa-solid fa-trash-can text-xs"></i>
                    </button>
                </div>
            </div>`;
        }).join('');
    }

    function recalculateTotals() {
        const subTotal = state.cart.reduce((sum, item) => sum + Number(item.donGia) * Number(item.soLuong || 1), 0);
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

        state.totals = { subTotal, discount, vat, total };

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

    function updateCashChange() {
        const given = Number($('customerGivenMoney').value || 0);
        const change = Math.max(0, given - state.totals.total);
        $('txtChangeMoney').textContent = formatVnd(change);
    }

    async function applyVoucher(silentRevalidate = false) {
        const code = $('voucherInput').value.trim();
        if (!state.cart.length) {
            state.appliedVoucher = null;
            recalculateTotals();
            if (!silentRevalidate) {
                showVoucherErrorModal('Giỏ hàng đang trống. Vui lòng chọn sản phẩm trước khi áp dụng voucher.');
            }
            return;
        }
        if (!code) {
            if (!silentRevalidate) {
                showVoucherErrorModal('Vui lòng nhập hoặc chọn mã voucher cần áp dụng.');
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
                // UC 2.14.2.4: Keep order total untouched & display explicit error alert
                removeVoucherState();
                showVoucherErrorModal(res.message);
            }
        } catch (err) {
            removeVoucherState();
            showVoucherErrorModal(err.message);
        }
    }

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

    async function handleCheckout() {
        if (!state.cart.length) {
            showPosStatus('Giỏ hàng đang trống. Vui lòng chọn sản phẩm cần thanh toán.', true);
            return;
        }

        const given = state.paymentMethod === 'Tien_mat'
            ? Number($('customerGivenMoney').value || 0)
            : state.totals.total;

        if (state.paymentMethod === 'Tien_mat' && given < state.totals.total) {
            showPosStatus(`Tiền khách đưa phải >= ${formatVnd(state.totals.total)}`, true);
            return;
        }

        try {
            const invoice = await apiRequest('/pos/checkout', {
                method: 'POST',
                body: JSON.stringify({
                    maKhachHang: $('customerSelect').value,
                    maNhanVien: 'NV003',
                    codeVoucher: state.appliedVoucher ? state.appliedVoucher.codeVoucher : null,
                    phuongThucThanhToan: state.paymentMethod,
                    vatPhanTram: Number($('vatPercentSelect').value || 0),
                    tienKhachDua: given,
                    items: state.cart.map((c) => ({
                        maSanPham: c.maSanPham,
                        maImei: c.maImei,
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
            showPosStatus(`Đã xuất hóa đơn ${invoice.maHoaDon} thành công!`);
        } catch (err) {
            showPosStatus(err.message, true);
        }
    }

    function renderInvoiceModal(inv) {
        $('invCode').textContent = inv.maHoaDon;
        const linesHtml = (inv.items || []).map((it, idx) => `
            <tr class="border-b border-slate-100">
                <td class="py-2 pr-2">${idx + 1}</td>
                <td class="py-2 pr-2">
                    <div class="font-bold text-slate-900">${escapeHtml(it.tenSanPham)}</div>
                    <div class="text-xs text-slate-500 font-mono">IMEI: ${escapeHtml(it.maImei)} • BH ${it.thoiGianBaoHanhThang} tháng</div>
                </td>
                <td class="py-2 text-right font-semibold">${formatVnd(it.donGia)}</td>
            </tr>`).join('');

        $('invoiceDetails').innerHTML = `
            <div class="text-center border-b border-slate-200 pb-3">
                <h3 class="text-lg font-extrabold text-slate-900">PHONESTORE PRO - HÓA ĐƠN BÁN HÀNG</h3>
                <p class="text-xs text-slate-500">Số HĐ: <strong>${escapeHtml(inv.maHoaDon)}</strong> • Ngày: ${new Date(inv.ngayLap).toLocaleString('vi-VN')}</p>
            </div>
            <div class="grid grid-cols-2 gap-3 text-xs bg-slate-50 p-3 rounded-xl border border-slate-200">
                <div><span class="text-slate-500">Khách hàng:</span> <strong>${escapeHtml(inv.tenKhachHang)}</strong></div>
                <div><span class="text-slate-500">Thu ngân:</span> <strong>${escapeHtml(inv.tenNhanVien)}</strong></div>
                <div><span class="text-slate-500">Thanh toán:</span> <strong>${escapeHtml(inv.phuongThucThanhToan)}</strong></div>
                <div><span class="text-slate-500">Mã khuyến mãi:</span> <strong>${escapeHtml(inv.codeVoucher || 'Không áp dụng')}</strong></div>
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
                <div class="flex justify-between"><span>Tổng tiền hàng:</span><strong>${formatVnd(inv.tongTienHang)}</strong></div>
                <div class="flex justify-between text-emerald-700"><span>Chiết khấu KM:</span><strong>-${formatVnd(inv.tienGiamGia)}</strong></div>
                ${inv.quaTang ? `<div class="flex justify-between text-blue-700"><span>Quà tặng kèm:</span><strong>${escapeHtml(inv.quaTang)}</strong></div>` : ''}
                <div class="flex justify-between"><span>Thuế VAT:</span><strong>${formatVnd(inv.tienThueVat)}</strong></div>
                <div class="flex justify-between text-base font-extrabold text-blue-700 pt-1 border-t border-slate-200">
                    <span>Tổng thanh toán:</span><span>${formatVnd(inv.tongThanhToan)}</span>
                </div>
                <div class="flex justify-between text-xs text-slate-500">
                    <span>Tiền khách đưa: ${formatVnd(inv.tienKhachDua)}</span>
                    <span>Tiền thối lại: ${formatVnd(inv.tienThua)}</span>
                </div>
            </div>`;
        $('invoiceModal').showModal();
    }

    async function openRecentInvoicesModal() {
        try {
            const list = await apiRequest('/pos/invoices?limit=10');
            if (!list.length) {
                $('recentInvoicesBody').innerHTML = '<p class="text-center text-slate-400 py-8">Chưa có hóa đơn nào.</p>';
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
                            <td class="px-3 py-2.5 font-mono font-bold text-blue-700">${escapeHtml(inv.maHoaDon)}</td>
                            <td class="px-3 py-2.5 text-xs text-slate-600">${new Date(inv.ngayLap).toLocaleString('vi-VN')}</td>
                            <td class="px-3 py-2.5 font-semibold">${escapeHtml(inv.tenKhachHang)}</td>
                            <td class="px-3 py-2.5 font-mono text-xs">${escapeHtml(inv.codeVoucher || '-')}</td>
                            <td class="px-3 py-2.5 text-right text-emerald-700">-${formatVnd(inv.tienGiamGia)}</td>
                            <td class="px-3 py-2.5 text-right font-bold text-slate-900">${formatVnd(inv.tongThanhToan)}</td>
                        </tr>`).join('')}
                    </tbody>
                </table>`;
            }
            $('recentInvoicesDialog').showModal();
        } catch (err) {
            showPosStatus(err.message, true);
        }
    }

    function filterDisplayedProducts() {
        const kw = $('posSearchInput').value.trim().toLowerCase();
        const brandId = $('posBrandFilter').value;
        const filtered = state.products.filter((p) => {
            const matchBrand = !brandId || String(p.maThuongHieu) === String(brandId);
            const matchKw = !kw
                || p.tenSanPham.toLowerCase().includes(kw)
                || p.maSanPham.toLowerCase().includes(kw)
                || (p.dongMay || '').toLowerCase().includes(kw);
            return matchBrand && matchKw;
        });
        renderProductGrid(filtered);
    }

    function escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

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

        $('posProductGrid').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-add-product]');
            if (btn) addProductToCart(btn.dataset.addProduct);
        });

        $('posCartList').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-remove-idx]');
            if (!btn) return;
            state.cart.splice(Number(btn.dataset.removeIdx), 1);
            renderCart();
            if (state.appliedVoucher) applyVoucher(true);
            else recalculateTotals();
        });

        $('posCartList').addEventListener('change', (e) => {
            const sel = e.target.closest('select[data-imei-idx]');
            if (!sel) return;
            const idx = Number(sel.dataset.imeiIdx);
            if (state.cart[idx]) state.cart[idx].maImei = sel.value;
        });

        $('btnClearCart').addEventListener('click', () => {
            state.cart = [];
            renderCart();
            removeVoucherState();
        });

        $('activeVoucherPills').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-pick-voucher]');
            if (!btn) return;
            $('voucherInput').value = btn.dataset.pickVoucher;
            applyVoucher(false);
        });

        $('btnApplyVoucher').addEventListener('click', () => applyVoucher(false));
        $('voucherInput').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyVoucher(false);
            }
        });
        $('btnRemoveVoucher').addEventListener('click', () => {
            $('voucherInput').value = '';
            removeVoucherState();
        });

        $('customerSelect').addEventListener('change', () => {
            if (state.appliedVoucher) applyVoucher(true);
        });
        $('vatPercentSelect').addEventListener('change', () => {
            if (state.appliedVoucher) applyVoucher(true);
            else recalculateTotals();
        });

        document.querySelectorAll('.pay-method-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                state.paymentMethod = btn.dataset.paymethod;
                document.querySelectorAll('.pay-method-btn').forEach((b) => {
                    b.className = b === btn
                        ? 'pay-method-btn min-h-[40px] px-3 py-2 rounded-xl border-2 border-blue-600 bg-blue-50 text-blue-700 text-xs font-bold'
                        : 'pay-method-btn min-h-[40px] px-3 py-2 rounded-xl border border-slate-300 bg-white text-slate-700 text-xs font-bold';
                });
                $('cashInputGroup').classList.toggle('hidden', state.paymentMethod !== 'Tien_mat');
            });
        });

        $('customerGivenMoney').addEventListener('input', updateCashChange);
        $('btnCheckout').addEventListener('click', handleCheckout);
        $('btnCloseVoucherError').addEventListener('click', () => $('voucherErrorDialog').close());
        $('btnCloseInvoiceModal').addEventListener('click', () => $('invoiceModal').close());
        $('btnDoneInvoiceModal').addEventListener('click', () => $('invoiceModal').close());
        if ($('btnPrintInvoice')) {
            $('btnPrintInvoice').addEventListener('click', () => {
                const html = $('invoiceDetails').innerHTML;
                const w = window.open('', '_blank', 'width=800,height=700');
                if (!w) { window.print(); return; }
                w.document.write(`<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><title>In hóa đơn</title>
                    <script src="https://cdn.tailwindcss.com"><\/script>
                    <style>body{font-family:sans-serif;padding:24px;color:#0f172a;background:#fff;}</style>
                    </head><body>${html}<script>window.onload=()=>{setTimeout(()=>{window.print();},250);};<\/script></body></html>`);
                w.document.close();
            });
        }
        $('btnRecentInvoices').addEventListener('click', openRecentInvoicesModal);
        $('btnCloseRecentInvoices').addEventListener('click', () => $('recentInvoicesDialog').close());

        await loadInitialPosData();
    });
})();
