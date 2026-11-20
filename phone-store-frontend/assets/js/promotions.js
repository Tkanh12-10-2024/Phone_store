(() => {
    const state = {
        promotions: [],
        options: { brands: [], categories: [], productLines: [] },
        pendingDeleteId: null
    };

    const $ = (id) => document.getElementById(id);

    function formatVnd(value) {
        const num = Number(value || 0);
        return num.toLocaleString('vi-VN') + ' đ';
    }

    function formatDateTimeDisplay(iso) {
        if (!iso) return '-';
        const dt = new Date(iso);
        if (Number.isNaN(dt.getTime())) return iso;
        return dt.toLocaleString('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    }

    function toDatetimeLocalValue(iso) {
        if (!iso) return '';
        const dt = new Date(iso);
        if (Number.isNaN(dt.getTime())) return '';
        const pad = (n) => String(n).padStart(2, '0');
        return `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
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

    function showLiveStatus(message, isError = false) {
        const el = $('promoLiveStatus');
        el.textContent = message;
        el.className = isError
            ? 'rounded-xl px-4 py-3 text-sm font-medium border bg-red-50 border-red-200 text-red-700'
            : 'rounded-xl px-4 py-3 text-sm font-medium border bg-emerald-50 border-emerald-200 text-emerald-700';
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

    async function loadOptions() {
        try {
            const opts = await apiRequest('/promotions/options');
            state.options = opts;

            const brandSel = $('inputThuongHieu');
            brandSel.innerHTML = '<option value="">Tất cả thương hiệu</option>'
                + (opts.brands || []).map((b) => `<option value="${b.value}">${escapeHtml(b.label)}</option>`).join('');

            const catSel = $('inputDanhMuc');
            catSel.innerHTML = '<option value="">Tất cả danh mục</option>'
                + (opts.categories || []).map((c) => `<option value="${c.value}">${escapeHtml(c.label)}</option>`).join('');

            const dl = $('productLinesList');
            dl.innerHTML = (opts.productLines || []).map((line) => `<option value="${escapeHtml(line)}"></option>`).join('');
        } catch (err) {
            console.error(err);
        }
    }

    async function loadPromotions() {
        const keyword = $('filterKeyword').value.trim();
        const status = $('filterStatus').value;
        const type = $('filterType').value;

        const qs = new URLSearchParams();
        if (keyword) qs.set('keyword', keyword);
        if (status && status !== 'ALL') qs.set('trangThai', status);
        if (type && type !== 'ALL') qs.set('loaiGiamGia', type);

        try {
            const [list, alerts] = await Promise.all([
                apiRequest(`/promotions?${qs.toString()}`),
                apiRequest('/promotions/alerts')
            ]);
            state.promotions = list || [];
            renderKpis(state.promotions, alerts || []);
            renderAlerts(alerts || []);
            renderTable(state.promotions);
        } catch (err) {
            showLiveStatus(err.message, true);
            $('promoTableBody').innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-red-500">${escapeHtml(err.message)}</td></tr>`;
        }
    }

    function renderKpis(allList, alerts) {
        $('kpiTotalPromos').textContent = allList.length;
        $('kpiActivePromos').textContent = allList.filter((p) => p.trangThai === 'Dang_dien_ra').length;
        $('kpiAlertPromos').textContent = alerts.length;
        const totalUsed = allList.reduce((acc, p) => acc + Number(p.soLuongDaDung || 0), 0);
        $('kpiTotalUsed').textContent = totalUsed.toLocaleString('vi-VN');
    }

    function renderAlerts(alerts) {
        const sec = $('promoAlertSection');
        const container = $('promoAlertCards');
        if (!alerts.length) {
            sec.classList.add('hidden');
            container.innerHTML = '';
            return;
        }
        sec.classList.remove('hidden');
        $('alertCountBadge').textContent = `${alerts.length} cảnh báo`;
        container.innerHTML = alerts.map((a) => `
            <div class="bg-white/95 border border-red-200 rounded-xl p-3.5 flex items-center justify-between gap-3">
                <div>
                    <div class="flex items-center gap-2">
                        <span class="px-2 py-0.5 rounded bg-red-100 text-red-800 font-mono text-xs font-bold">${escapeHtml(a.codeVoucher)}</span>
                        <span class="text-sm font-bold text-slate-900">${escapeHtml(a.tenKhuyenMai)}</span>
                    </div>
                    <p class="text-xs text-red-700 font-semibold mt-1">
                        <i class="fa-solid fa-triangle-exclamation mr-1"></i>${escapeHtml(a.lyDoCanhBao || 'Sắp hết hiệu lực / số lượng')}
                    </p>
                </div>
                <button type="button" data-action="edit" data-id="${escapeHtml(a.maKhuyenMai)}"
                        class="shrink-0 min-h-[34px] px-3 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-xs font-bold">
                    Gia hạn / Sửa
                </button>
            </div>
        `).join('');
    }

    function renderTable(list) {
        const tbody = $('promoTableBody');
        $('promoCountText').textContent = `${list.length} chương trình`;
        if (!list.length) {
            tbody.innerHTML = `<tr><td colspan="7" class="px-6 py-10 text-center text-slate-400">Không có chương trình khuyến mãi nào phù hợp bộ lọc.</td></tr>`;
            return;
        }

        tbody.innerHTML = list.map((p) => {
            const discountText = renderDiscountText(p);
            const scopeBadges = renderScopeText(p);
            const percentLeft = Number(p.tiLeConLai || 0);
            const barColor = p.sapHetSoLuong ? 'bg-red-600' : (percentLeft < 20 ? 'bg-amber-500' : 'bg-emerald-500');
            const rowAlertClass = p.coCanhBao ? 'bg-red-50/40 border-l-4 border-l-red-500' : 'hover:bg-slate-50/80';

            const statusBadge = renderStatusBadge(p.trangThai);
            const toggleLabel = p.trangThai === 'Tam_dung' ? 'Kích hoạt' : 'Tạm dừng';
            const toggleTarget = p.trangThai === 'Tam_dung' ? 'Dang_dien_ra' : 'Tam_dung';

            return `
            <tr class="${rowAlertClass} transition">
                <td class="px-5 py-4 align-top">
                    <div class="flex items-center gap-2 flex-wrap">
                        <span class="px-2.5 py-0.5 rounded-lg bg-blue-50 text-blue-700 border border-blue-200 font-mono text-xs font-bold">${escapeHtml(p.codeVoucher)}</span>
                        ${p.coCanhBao ? `<span class="px-2 py-0.5 rounded bg-red-100 text-red-700 text-[11px] font-bold"><i class="fa-solid fa-bell mr-1"></i>Cần chú ý</span>` : ''}
                    </div>
                    <p class="font-bold text-slate-900 mt-1">${escapeHtml(p.tenKhuyenMai)}</p>
                    <p class="text-xs text-slate-500 mt-0.5">${escapeHtml(p.moTa || '')}</p>
                </td>
                <td class="px-4 py-4 align-top">
                    <span class="text-xs font-semibold text-slate-500 block">${formatTypeLabel(p.loaiGiamGia)}</span>
                    <span class="font-extrabold text-slate-900">${discountText}</span>
                    ${p.giamToiDa ? `<span class="block text-xs text-slate-500">Tối đa: ${formatVnd(p.giamToiDa)}</span>` : ''}
                </td>
                <td class="px-4 py-4 align-top text-xs space-y-1">
                    <div><span class="text-slate-500">Đơn tối thiểu:</span> <strong>${formatVnd(p.giaTriDonHangToiThieu)}</strong></div>
                    ${scopeBadges}
                </td>
                <td class="px-4 py-4 align-top min-w-[160px]">
                    <div class="flex justify-between text-xs font-semibold mb-1">
                        <span class="${p.sapHetSoLuong ? 'text-red-700 font-bold' : 'text-slate-700'}">Còn ${p.soLuongConLai}/${p.soLuongPhatHanh}</span>
                        <span>${percentLeft}%</span>
                    </div>
                    <div class="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                        <div class="h-full ${barColor}" style="width: ${Math.min(100, Math.max(2, percentLeft))}%"></div>
                    </div>
                </td>
                <td class="px-4 py-4 align-top text-xs">
                    <div>Từ: <strong>${formatDateTimeDisplay(p.ngayBatDau)}</strong></div>
                    <div>Đến: <strong class="${p.sapHetHan ? 'text-red-700' : ''}">${formatDateTimeDisplay(p.ngayKetThuc)}</strong></div>
                    ${p.sapHetHan ? `<span class="inline-block mt-1 px-2 py-0.5 rounded bg-amber-100 text-amber-800 font-bold">Còn dưới 24h</span>` : ''}
                </td>
                <td class="px-4 py-4 align-top">${statusBadge}</td>
                <td class="px-5 py-4 align-top text-right">
                    <div class="inline-flex items-center gap-1.5">
                        <button type="button" data-action="edit" data-id="${escapeHtml(p.maKhuyenMai)}"
                                title="Sửa điều kiện"
                                class="min-h-[32px] px-2.5 py-1 bg-slate-100 hover:bg-blue-50 hover:text-blue-700 text-slate-700 rounded-lg text-xs font-semibold">
                            <i class="fa-solid fa-pen-to-square"></i> Sửa
                        </button>
                        ${p.trangThai !== 'Het_han' ? `
                        <button type="button" data-action="toggle" data-id="${escapeHtml(p.maKhuyenMai)}" data-next="${toggleTarget}"
                                class="min-h-[32px] px-2.5 py-1 bg-amber-50 hover:bg-amber-100 text-amber-800 rounded-lg text-xs font-semibold">
                            ${toggleLabel}
                        </button>` : ''}
                        <button type="button" data-action="delete" data-id="${escapeHtml(p.maKhuyenMai)}"
                                title="Ngừng / Xóa"
                                class="min-h-[32px] px-2.5 py-1 bg-red-50 hover:bg-red-100 text-red-700 rounded-lg text-xs font-semibold">
                            <i class="fa-solid fa-trash-can"></i>
                        </button>
                    </div>
                </td>
            </tr>`;
        }).join('');
    }

    function renderDiscountText(p) {
        if (p.loaiGiamGia === 'Phan_tram') return `Giảm ${Number(p.giaTriGiam)}%`;
        if (p.loaiGiamGia === 'Qua_tang') return escapeHtml(p.quaTang || 'Quà tặng kèm');
        return `Giảm ${formatVnd(p.giaTriGiam)}`;
    }

    function renderScopeText(p) {
        const tags = [];
        tags.push(p.tenThuongHieu ? `Hãng: ${escapeHtml(p.tenThuongHieu)}` : 'Mọi hãng');
        if (p.dongMayApDung) tags.push(`Dòng: ${escapeHtml(p.dongMayApDung)}`);
        if (p.tenDanhMuc) tags.push(`DM: ${escapeHtml(p.tenDanhMuc)}`);
        tags.push(`KH: ${formatTierLabel(p.doiTuongKhachHang)}`);
        return tags.map((t) => `<span class="inline-block px-2 py-0.5 bg-slate-100 text-slate-700 rounded mr-1 mb-1">${t}</span>`).join('');
    }

    function renderStatusBadge(status) {
        if (status === 'Dang_dien_ra') return '<span class="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">Đang diễn ra</span>';
        if (status === 'Tam_dung') return '<span class="px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 text-xs font-bold">Tạm dừng</span>';
        return '<span class="px-2.5 py-1 rounded-full bg-slate-200 text-slate-700 text-xs font-bold">Hết hạn</span>';
    }

    function formatTypeLabel(type) {
        if (type === 'Phan_tram') return 'Phần trăm (%)';
        if (type === 'Qua_tang') return 'Quà tặng / Combo';
        return 'Tiền mặt (VNĐ)';
    }

    function formatTierLabel(tier) {
        const map = { Tat_ca: 'Tất cả', Khach_moi: 'Khách mới', Thanh_vien: 'Thành viên', Khach_VIP: 'Khách VIP' };
        return map[tier] || 'Tất cả';
    }

    function updateDiscountTypeVisibility() {
        const type = $('inputLoaiGiam').value;
        const wrapMax = $('wrapGiamToiDa');
        const wrapGift = $('wrapQuaTang');
        const label = $('labelGiaTriGiam');

        if (type === 'Phan_tram') {
            label.textContent = 'Phần trăm giảm (%) *';
            wrapMax.classList.remove('hidden');
            wrapGift.classList.add('hidden');
        } else if (type === 'Tien_mat') {
            label.textContent = 'Số tiền giảm cố định (VNĐ) *';
            wrapMax.classList.add('hidden');
            wrapGift.classList.add('hidden');
        } else {
            label.textContent = 'Giá trị quà quy đổi (VNĐ)';
            wrapMax.classList.add('hidden');
            wrapGift.classList.remove('hidden');
        }
    }

    function openCreateModal() {
        $('promoForm').reset();
        $('promoFormError').classList.add('hidden');
        $('editingPromoId').value = '';
        $('promoModalTitle').textContent = 'Tạo chương trình khuyến mãi mới';

        const now = new Date();
        const nextMonth = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);
        $('inputNgayBatDau').value = toDatetimeLocalValue(now.toISOString());
        $('inputNgayKetThuc').value = toDatetimeLocalValue(nextMonth.toISOString());
        $('inputDonToiThieu').value = '0';
        $('inputSoLuong').value = '100';
        updateDiscountTypeVisibility();
        $('promoModal').showModal();
    }

    function openEditModal(maKhuyenMai) {
        const p = state.promotions.find((item) => item.maKhuyenMai === maKhuyenMai);
        if (!p) return;

        $('promoFormError').classList.add('hidden');
        $('editingPromoId').value = p.maKhuyenMai;
        $('promoModalTitle').textContent = `Cập nhật khuyến mãi: ${p.codeVoucher}`;
        $('inputCodeVoucher').value = p.codeVoucher || '';
        $('inputTenKm').value = p.tenKhuyenMai || '';
        $('inputLoaiGiam').value = p.loaiGiamGia || 'Phan_tram';
        $('inputGiaTriGiam').value = p.giaTriGiam ?? 0;
        $('inputGiamToiDa').value = p.giamToiDa ?? '';
        $('inputQuaTang').value = p.quaTang || '';
        $('inputDonToiThieu').value = p.giaTriDonHangToiThieu ?? 0;
        $('inputSoLuong').value = p.soLuongPhatHanh ?? 100;
        $('inputTrangThai').value = p.trangThai || 'Dang_dien_ra';
        $('inputNgayBatDau').value = toDatetimeLocalValue(p.ngayBatDau);
        $('inputNgayKetThuc').value = toDatetimeLocalValue(p.ngayKetThuc);
        $('inputThuongHieu').value = p.maThuongHieuApDung ?? '';
        $('inputDongMay').value = p.dongMayApDung || '';
        $('inputDanhMuc').value = p.maDanhMucApDung ?? '';
        $('inputDoiTuong').value = p.doiTuongKhachHang || 'Tat_ca';
        $('inputMoTa').value = p.moTa || '';

        updateDiscountTypeVisibility();
        $('promoModal').showModal();
    }

    function validatePromoFormClient() {
        const code = $('inputCodeVoucher').value.trim();
        const name = $('inputTenKm').value.trim();
        const type = $('inputLoaiGiam').value;
        const val = Number($('inputGiaTriGiam').value || 0);
        const start = $('inputNgayBatDau').value;
        const end = $('inputNgayKetThuc').value;
        const qty = Number($('inputSoLuong').value || 0);

        if (!/^[A-Za-z0-9_-]{3,50}$/.test(code)) {
            $('inputCodeVoucher').setAttribute('aria-invalid', 'true');
            return 'Mã voucher phải gồm 3-50 ký tự chữ, số hoặc dấu _/-, không chứa khoảng trắng.';
        }
        $('inputCodeVoucher').removeAttribute('aria-invalid');
        if (!name) return 'Tên chương trình khuyến mãi không được để trống.';
        if (type === 'Phan_tram' && (val <= 0 || val > 100)) return 'Phần trăm giảm phải từ 1% đến 100%.';
        if (type === 'Tien_mat' && val <= 0) return 'Số tiền giảm cố định phải lớn hơn 0đ.';
        if (type === 'Qua_tang' && !$('inputQuaTang').value.trim()) return 'Vui lòng nhập mô tả quà tặng / combo kèm theo.';
        if (qty <= 0) return 'Số lượng phát hành phải lớn hơn 0.';
        if (!start || !end) return 'Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc.';
        if (new Date(end) < new Date(start)) return 'Ngày kết thúc không được nhỏ hơn ngày bắt đầu.';
        return null;
    }

    async function handleSavePromo(e) {
        e.preventDefault();
        const errorMsg = validatePromoFormClient();
        const errBox = $('promoFormError');
        if (errorMsg) {
            errBox.textContent = errorMsg;
            errBox.classList.remove('hidden');
            return;
        }

        const editingId = $('editingPromoId').value;
        const payload = {
            codeVoucher: $('inputCodeVoucher').value.trim().toUpperCase(),
            tenKhuyenMai: $('inputTenKm').value.trim(),
            loaiGiamGia: $('inputLoaiGiam').value,
            giaTriGiam: Number($('inputGiaTriGiam').value || 0),
            giaTriDonHangToiThieu: Number($('inputDonToiThieu').value || 0),
            giamToiDa: $('inputGiamToiDa').value ? Number($('inputGiamToiDa').value) : null,
            quaTang: $('inputQuaTang').value.trim() || null,
            soLuongPhatHanh: Number($('inputSoLuong').value),
            ngayBatDau: $('inputNgayBatDau').value + ':00',
            ngayKetThuc: $('inputNgayKetThuc').value + ':00',
            maThuongHieuApDung: $('inputThuongHieu').value ? Number($('inputThuongHieu').value) : null,
            dongMayApDung: $('inputDongMay').value.trim() || null,
            maDanhMucApDung: $('inputDanhMuc').value ? Number($('inputDanhMuc').value) : null,
            doiTuongKhachHang: $('inputDoiTuong').value,
            moTa: $('inputMoTa').value.trim() || null,
            trangThai: $('inputTrangThai').value
        };

        try {
            if (editingId) {
                await apiRequest(`/promotions/${encodeURIComponent(editingId)}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showLiveStatus(`Đã cập nhật chương trình khuyến mãi ${payload.codeVoucher}.`);
            } else {
                await apiRequest('/promotions', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showLiveStatus(`Đã tạo thành công mã khuyến mãi ${payload.codeVoucher}.`);
            }
            $('promoModal').close();
            await loadPromotions();
        } catch (err) {
            errBox.textContent = err.message;
            errBox.classList.remove('hidden');
        }
    }

    async function handleToggleStatus(maKhuyenMai, nextStatus) {
        try {
            await apiRequest(`/promotions/${encodeURIComponent(maKhuyenMai)}/status`, {
                method: 'PUT',
                body: JSON.stringify({ trangThai: nextStatus })
            });
            showLiveStatus(`Đã chuyển trạng thái khuyến mãi sang ${nextStatus === 'Tam_dung' ? 'Tạm dừng' : 'Đang diễn ra'}.`);
            await loadPromotions();
        } catch (err) {
            showLiveStatus(err.message, true);
        }
    }

    function openDeleteConfirm(maKhuyenMai) {
        const promo = state.promotions.find((p) => p.maKhuyenMai === maKhuyenMai);
        if (!promo) return;
        state.pendingDeleteId = maKhuyenMai;
        $('confirmDeleteText').textContent =
            `Bạn muốn dừng/xóa voucher "${promo.codeVoucher}" (${promo.tenKhuyenMai})? `
            + `Nếu mã đã phát sinh hóa đơn bán hàng, hệ thống sẽ ngừng áp dụng (Hết hạn) để bảo toàn lịch sử.`;
        $('confirmDeleteModal').showModal();
    }

    async function handleConfirmDelete() {
        if (!state.pendingDeleteId) return;
        try {
            const res = await apiRequest(`/promotions/${encodeURIComponent(state.pendingDeleteId)}`, {
                method: 'DELETE'
            });
            $('confirmDeleteModal').close();
            state.pendingDeleteId = null;
            showLiveStatus(res.message || 'Đã xử lý khuyến mãi thành công.');
            await loadPromotions();
        } catch (err) {
            $('confirmDeleteModal').close();
            showLiveStatus(err.message, true);
        }
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
        attachBackdropClose($('promoModal'));
        attachBackdropClose($('confirmDeleteModal'));

        $('btnOpenCreateModal').addEventListener('click', openCreateModal);
        $('btnClosePromoModal').addEventListener('click', () => $('promoModal').close());
        $('btnCancelPromoModal').addEventListener('click', () => $('promoModal').close());
        $('btnCancelDelete').addEventListener('click', () => $('confirmDeleteModal').close());
        $('btnConfirmDelete').addEventListener('click', handleConfirmDelete);
        $('btnRefreshPromos').addEventListener('click', loadPromotions);
        $('inputLoaiGiam').addEventListener('change', updateDiscountTypeVisibility);
        $('promoForm').addEventListener('submit', handleSavePromo);

        $('promoFilterForm').addEventListener('submit', (e) => {
            e.preventDefault();
            loadPromotions();
        });
        $('filterStatus').addEventListener('change', loadPromotions);
        $('filterType').addEventListener('change', loadPromotions);

        document.addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-action]');
            if (!btn) return;
            const action = btn.dataset.action;
            const id = btn.dataset.id;
            if (action === 'edit') openEditModal(id);
            else if (action === 'toggle') handleToggleStatus(id, btn.dataset.next);
            else if (action === 'delete') openDeleteConfirm(id);
        });

        await loadOptions();
        await loadPromotions();
    });
})();

