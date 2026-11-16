(() => {
    const state = {
        activeTab: 'tab-revenue',
        revChart: null,
        prodChart: null,
        lastPreview: null,
        sortState: {}
    };

    const $ = (id) => document.getElementById(id);
    const collator = new Intl.Collator('vi', { numeric: true, sensitivity: 'base' });

    function formatVnd(val) {
        return Number(val || 0).toLocaleString('vi-VN') + ' đ';
    }

    function toIsoDate(d) {
        const pad = (n) => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }

    async function apiRequest(path, options = {}) {
        const res = await fetch(`${API_BASE_URL}${path}`, {
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
            ...options
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : null;
        if (!res.ok) {
            throw new Error(data?.message || `Lỗi API (${res.status})`);
        }
        return data;
    }

    function showStatus(msg, isError = false) {
        const el = $('reportsLiveStatus');
        el.textContent = msg;
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

    // Accessible Tabs management with hidden="until-found"
    function activateTab(tabId) {
        state.activeTab = tabId;
        const tabs = document.querySelectorAll('button[role="tab"]');
        tabs.forEach((tab) => {
            const selected = tab.id === tabId;
            tab.setAttribute('aria-selected', String(selected));
            const panel = $(tab.getAttribute('aria-controls'));
            if (panel) {
                if (selected) panel.removeAttribute('hidden');
                else panel.setAttribute('hidden', 'until-found');
            }
        });

        if (tabId === 'tab-revenue') loadRevenueStats();
        else if (tabId === 'tab-products') loadProductStats();
        else if (tabId === 'tab-inventory') loadInventoryStats();
        else if (tabId === 'tab-export') loadReportHistory();
    }

    async function loadSharedDropdowns() {
        try {
            const filters = await apiRequest('/statistics/inventory/filters');
            const brandsHtml = '<option value="">Tất cả thương hiệu</option>'
                + (filters.brands || []).map((b) => `<option value="${b.value}">${escapeHtml(b.label)}</option>`).join('');

            $('prodBrandFilter').innerHTML = brandsHtml;
            $('invBrand').innerHTML = brandsHtml;
            $('repBrand').innerHTML = brandsHtml;

            $('invLine').innerHTML = '<option value="">Tất cả dòng máy</option>'
                + (filters.dongMay || []).map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
            $('invRam').innerHTML = '<option value="">Tất cả RAM</option>'
                + (filters.ram || []).map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
            $('invRom').innerHTML = '<option value="">Tất cả ROM</option>'
                + (filters.rom || []).map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
            $('invColor').innerHTML = '<option value="">Tất cả màu</option>'
                + (filters.mauSac || []).map((v) => `<option value="${escapeHtml(v)}">${escapeHtml(v)}</option>`).join('');
        } catch (err) {
            showStatus('Không tải được danh mục lọc: ' + err.message, true);
        }
    }

    // ================= UC-25 REVENUE =================
    async function loadRevenueStats() {
        const from = $('revFromDate').value;
        const to = $('revToDate').value;
        const groupBy = $('revGroupBy').value;
        const errEl = $('revDateError');

        if (from && to && new Date(to) < new Date(from)) {
            $('revFromDate').setAttribute('aria-invalid', 'true');
            $('revToDate').setAttribute('aria-invalid', 'true');
            errEl.textContent = 'Khoảng thời gian không hợp lệ: Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.';
            errEl.classList.remove('hidden');
            return;
        }
        $('revFromDate').removeAttribute('aria-invalid');
        $('revToDate').removeAttribute('aria-invalid');
        errEl.classList.add('hidden');

        try {
            const qs = new URLSearchParams({ groupBy });
            if (from) qs.set('tuNgay', from);
            if (to) qs.set('denNgay', to);
            const data = await apiRequest(`/statistics/revenue?${qs.toString()}`);

            $('revNetKpi').textContent = formatVnd(data.doanhThuThucThu);
            const growth = Number(data.tyLeTangTruongPhanTram || 0);
            const sign = growth > 0 ? '+' : '';
            $('revGrowthKpi').textContent = `So với kỳ trước: ${sign}${growth}%`;
            $('revGrowthKpi').className = `text-xs font-bold mt-1 ${growth >= 0 ? 'text-emerald-600' : 'text-red-600'}`;

            $('revGrossKpi').textContent = formatVnd(data.doanhThuGop);
            $('revDiscountKpi').textContent = `Chiết khấu KM: -${formatVnd(data.tongChietKhau)}`;
            $('revCogsKpi').textContent = formatVnd(data.tongTienVon);
            $('revOrderCountKpi').textContent = `${data.tongHoaDon} hóa đơn • ${data.tongSanPhamBan} máy`;
            $('revProfitKpi').textContent = formatVnd(data.loiNhuanGop);

            const emptyBox = $('revEmptyAlert');
            if (data.emptyMessage) {
                emptyBox.textContent = data.emptyMessage;
                emptyBox.classList.remove('hidden');
            } else {
                emptyBox.classList.add('hidden');
            }

            renderRevenueChart(data.series || []);
            renderRevenueTable(data.series || []);
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    function renderRevenueChart(series) {
        const ctx = $('revenueGrowthChart');
        if (state.revChart) state.revChart.destroy();
        state.revChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: series.map((s) => s.nhanThoiGian),
                datasets: [
                    {
                        label: 'Doanh thu thực thu (đ)',
                        data: series.map((s) => Number(s.doanhThuThucThu || 0)),
                        backgroundColor: 'rgba(37, 99, 235, 0.85)',
                        borderRadius: 6
                    },
                    {
                        label: 'Lợi nhuận gộp (đ)',
                        data: series.map((s) => Number(s.loiNhuanGop || 0)),
                        backgroundColor: 'rgba(16, 185, 129, 0.85)',
                        borderRadius: 6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    tooltip: {
                        callbacks: {
                            afterBody: (items) => {
                                const idx = items[0]?.dataIndex ?? 0;
                                const pt = series[idx];
                                if (!pt) return '';
                                return `Số HĐ: ${pt.soHoaDon} | Tăng trưởng: ${pt.tangTruongPhanTram}%`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderRevenueTable(series) {
        const tbody = $('revTableBody');
        if (!series.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="px-6 py-8 text-center text-slate-400">Không có doanh thu phát sinh trong khoảng thời gian này.</td></tr>';
            return;
        }
        tbody.innerHTML = series.map((pt) => {
            const g = Number(pt.tangTruongPhanTram || 0);
            return `
            <tr class="hover:bg-slate-50">
                <td class="px-4 py-3 font-semibold text-slate-900" data-value="${escapeHtml(pt.nhanThoiGian)}">${escapeHtml(pt.nhanThoiGian)}</td>
                <td class="px-4 py-3 text-right" data-value="${pt.soHoaDon}">${pt.soHoaDon}</td>
                <td class="px-4 py-3 text-right" data-value="${pt.doanhThuGop}">${formatVnd(pt.doanhThuGop)}</td>
                <td class="px-4 py-3 text-right text-amber-700" data-value="${pt.chietKhau}">-${formatVnd(pt.chietKhau)}</td>
                <td class="px-4 py-3 text-right font-bold text-blue-700" data-value="${pt.doanhThuThucThu}">${formatVnd(pt.doanhThuThucThu)}</td>
                <td class="px-4 py-3 text-right" data-value="${pt.tienVon}">${formatVnd(pt.tienVon)}</td>
                <td class="px-4 py-3 text-right font-bold text-emerald-700" data-value="${pt.loiNhuanGop}">${formatVnd(pt.loiNhuanGop)}</td>
                <td class="px-4 py-3 text-right font-bold ${g >= 0 ? 'text-emerald-600' : 'text-red-600'}">${g > 0 ? '+' : ''}${g}%</td>
            </tr>`;
        }).join('');
    }

    // ================= UC-26 PRODUCT STATS =================
    async function loadProductStats() {
        const brand = $('prodBrandFilter').value;
        const from = $('prodFromDate').value;
        const to = $('prodToDate').value;
        try {
            const qs = new URLSearchParams();
            if (brand) qs.set('maThuongHieu', brand);
            if (from) qs.set('tuNgay', from);
            if (to) qs.set('denNgay', to);
            const data = await apiRequest(`/statistics/products?${qs.toString()}`);

            $('prodTotalSummary').textContent = `Tổng xuất bán: ${data.tongSoLuongBan} máy • ${formatVnd(data.tongDoanhThu)}`;
            const emptyBox = $('prodEmptyAlert');
            if (data.emptyMessage) {
                emptyBox.textContent = data.emptyMessage;
                emptyBox.classList.remove('hidden');
            } else {
                emptyBox.classList.add('hidden');
            }

            renderTopProductsChart(data.bestSellers || []);
            renderBestSellerTable(data.bestSellers || []);
            renderStagnantTable(data.stagnantProducts || []);
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    function renderTopProductsChart(list) {
        const ctx = $('topProductsChart');
        if (state.prodChart) state.prodChart.destroy();
        const top = list.slice(0, 6);
        state.prodChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: top.map((p) => p.tenSanPham),
                datasets: [{
                    label: 'Số lượng đã bán (máy)',
                    data: top.map((p) => p.soLuongDaBan),
                    backgroundColor: ['#2563eb', '#0ea5e9', '#10b981', '#f59e0b', '#8b5cf6', '#64748b'],
                    borderRadius: 6
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }

    function renderBestSellerTable(list) {
        const tbody = $('bestSellerBody');
        if (!list.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-slate-400">Chưa có phát sinh giao dịch bán hàng đối với nhóm sản phẩm này.</td></tr>';
            return;
        }
        tbody.innerHTML = list.map((p, idx) => `
            <tr class="hover:bg-slate-50">
                <td class="px-4 py-3 font-extrabold ${idx === 0 ? 'text-amber-600' : 'text-slate-500'}">#${idx + 1}</td>
                <td class="px-4 py-3">
                    <div class="font-bold text-slate-900">${escapeHtml(p.tenSanPham)}</div>
                    <div class="text-xs text-slate-500">${escapeHtml(p.tenThuongHieu)} • ${escapeHtml(p.ram)}/${escapeHtml(p.rom)}</div>
                </td>
                <td class="px-4 py-3 text-right font-extrabold text-blue-700">${p.soLuongDaBan}</td>
                <td class="px-4 py-3 text-right font-semibold">${formatVnd(p.doanhThu)}</td>
                <td class="px-4 py-3 text-right font-bold text-emerald-700">${formatVnd(p.loiNhuan)}</td>
                <td class="px-4 py-3 text-right font-semibold">${p.tonKhoHienTai}</td>
            </tr>`).join('');
    }

    function renderStagnantTable(list) {
        const tbody = $('stagnantBody');
        if (!list.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="px-4 py-8 text-center text-slate-400">Không có sản phẩm tồn kho lâu (&gt; 60 ngày).</td></tr>';
            return;
        }
        tbody.innerHTML = list.map((p) => {
            const over60 = Number(p.soNgayTonKho || 0) > 60;
            return `
            <tr class="${over60 ? 'bg-amber-50/40' : ''}">
                <td class="px-4 py-3 font-bold text-slate-900">${escapeHtml(p.tenSanPham)}</td>
                <td class="px-4 py-3 text-xs text-slate-600">${escapeHtml(p.tenThuongHieu)} • ${escapeHtml(p.dongMay)}</td>
                <td class="px-4 py-3 text-right font-bold">${p.soLuongTon}</td>
                <td class="px-4 py-3 text-right">
                    <span class="px-2 py-0.5 rounded-full text-xs font-bold ${over60 ? 'bg-red-100 text-red-800' : 'bg-slate-100 text-slate-700'}">
                        ${p.soNgayTonKho} ngày
                    </span>
                </td>
                <td class="px-4 py-3 text-right">${p.soLuongBanTrongKy}</td>
                <td class="px-4 py-3 text-right font-bold text-amber-800">${formatVnd(p.giaTriTonKho)}</td>
            </tr>`;
        }).join('');
    }

    // ================= UC-27 INVENTORY STATS =================
    async function loadInventoryStats() {
        const qs = new URLSearchParams();
        if ($('invBrand').value) qs.set('maThuongHieu', $('invBrand').value);
        if ($('invLine').value) qs.set('dongMay', $('invLine').value);
        if ($('invRam').value) qs.set('ram', $('invRam').value);
        if ($('invRom').value) qs.set('rom', $('invRom').value);
        if ($('invColor').value) qs.set('mauSac', $('invColor').value);
        if ($('invKeyword').value.trim()) qs.set('keyword', $('invKeyword').value.trim());
        qs.set('onlyAlert', String($('invOnlyAlert').checked));

        try {
            const data = await apiRequest(`/statistics/inventory?${qs.toString()}`);
            $('invKpiModels').textContent = data.tongMaSanPham;
            $('invKpiStock').textContent = data.tongSoLuongTon;
            $('invKpiReserved').textContent = data.tongDangGiuCho;
            $('invKpiAvailable').textContent = data.tongKhaDung;
            $('invKpiValue').textContent = formatVnd(data.tongGiaTriTonKho);

            const alertSec = $('invAlertSection');
            const alertList = $('invAlertList');
            const alerts = data.alerts || [];
            if (!alerts.length) {
                alertSec.classList.add('hidden');
            } else {
                alertSec.classList.remove('hidden');
                alertList.innerHTML = alerts.map((a) => `
                    <div class="bg-white border border-red-200 rounded-lg px-3.5 py-2.5 flex items-center justify-between gap-2 text-xs">
                        <span class="font-bold text-red-800">${escapeHtml(a.canhBaoThongDiep)}</span>
                        <span class="shrink-0 font-mono font-bold text-red-700">Tồn: ${a.soLuongTon} / Min: ${a.dinhMucTonToiThieu}</span>
                    </div>`).join('');
            }

            renderInventoryTable(data.items || []);
            renderMovementsTable(data.movements || []);
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    function renderInventoryTable(items) {
        const tbody = $('invTableBody');
        if (!items.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="px-4 py-8 text-center text-slate-400">Không tìm thấy mẫu thiết bị phù hợp điều kiện lọc.</td></tr>';
            return;
        }
        tbody.innerHTML = items.map((it) => {
            const isOut = it.mucDoCanhBao === 'Hết hàng';
            const isLow = it.mucDoCanhBao === 'Sắp hết hàng';
            const badge = isOut
                ? '<span class="px-2.5 py-1 rounded-full bg-red-100 text-red-800 text-xs font-bold">Hết hàng</span>'
                : (isLow
                    ? '<span class="px-2.5 py-1 rounded-full bg-amber-100 text-amber-800 text-xs font-bold">Sắp hết hàng</span>'
                    : '<span class="px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold">Bình thường</span>');
            return `
            <tr class="${(isOut || isLow) ? 'bg-red-50/30' : 'hover:bg-slate-50'}">
                <td class="px-4 py-3">
                    <div class="font-bold text-slate-900">${escapeHtml(it.tenSanPham)}</div>
                    <div class="text-xs font-mono text-slate-500">${escapeHtml(it.maSanPham)}</div>
                </td>
                <td class="px-4 py-3 text-xs text-slate-600">${escapeHtml(it.tenThuongHieu)} • ${escapeHtml(it.dongMay)}<br>${escapeHtml(it.ram)}/${escapeHtml(it.rom)} • ${escapeHtml(it.mauSac)}</td>
                <td class="px-4 py-3 text-right font-extrabold text-slate-900">${it.soLuongTon}</td>
                <td class="px-4 py-3 text-right font-semibold text-amber-700">${it.soLuongGiuCho}</td>
                <td class="px-4 py-3 text-right font-bold text-emerald-700">${it.soLuongKhaDung}</td>
                <td class="px-4 py-3 text-right text-slate-600">${it.dinhMucTonToiThieu}</td>
                <td class="px-4 py-3 text-right font-bold">${formatVnd(it.giaTriTonKho)}</td>
                <td class="px-4 py-3">${badge}</td>
            </tr>`;
        }).join('');
    }

    function renderMovementsTable(movements) {
        const tbody = $('invMovementsBody');
        if (!movements.length) {
            tbody.innerHTML = '<tr><td colspan="7" class="px-4 py-8 text-center text-slate-400">Chưa có biến động xuất nhập kho.</td></tr>';
            return;
        }
        tbody.innerHTML = movements.map((m) => {
            const isIn = m.loaiBienDong === 'NHAP_KHO';
            return `
            <tr>
                <td class="px-4 py-3 text-xs text-slate-600">${new Date(m.thoiGian).toLocaleString('vi-VN')}</td>
                <td class="px-4 py-3">
                    <span class="px-2 py-0.5 rounded text-xs font-bold ${isIn ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'}">
                        ${isIn ? 'Nhập kho' : 'Xuất bán'}
                    </span>
                </td>
                <td class="px-4 py-3 font-mono text-xs font-bold">${escapeHtml(m.maChungTu)}</td>
                <td class="px-4 py-3 font-semibold">${escapeHtml(m.tenSanPham)}</td>
                <td class="px-4 py-3 text-right font-extrabold ${m.thayDoiSoLuong > 0 ? 'text-emerald-700' : 'text-red-700'}">
                    ${m.thayDoiSoLuong > 0 ? '+' : ''}${m.thayDoiSoLuong}
                </td>
                <td class="px-4 py-3 text-xs">${escapeHtml(m.nhanVienThucHien)}</td>
                <td class="px-4 py-3 text-xs text-slate-500">${escapeHtml(m.ghiChu)}</td>
            </tr>`;
        }).join('');
    }

    // ================= UC-28 REPORT BUILDER & EXPORT =================
    function buildReportPayload(format) {
        return {
            loaiBaoCao: $('repTemplate').value,
            tuNgay: $('repFromDate').value || null,
            denNgay: $('repToDate').value || null,
            maThuongHieu: $('repBrand').value ? Number($('repBrand').value) : null,
            maNhanVien: 'NV001',
            dinhDangXuat: format,
            ghiChu: $('repNote').value.trim() || null
        };
    }

    async function handlePreviewReport(e) {
        if (e) e.preventDefault();
        try {
            const preview = await apiRequest('/reports/preview', {
                method: 'POST',
                body: JSON.stringify(buildReportPayload('PDF'))
            });
            state.lastPreview = preview;
            renderInlinePreview(preview);
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    function renderInlinePreview(p) {
        $('inlinePreviewBox').classList.remove('hidden');
        $('previewTitle').textContent = p.tieuDeBaoCao;
        $('previewMeta').textContent = `${p.phamViLoc} • Người lập: ${p.nguoiLap} • Ngày lập: ${new Date(p.ngayLap).toLocaleString('vi-VN')}`;

        $('previewThead').innerHTML = `
            <tr class="bg-slate-900 text-white text-xs uppercase">
                ${(p.columns || []).map((c) => `<th scope="col" class="px-4 py-3 font-bold">${escapeHtml(c)}</th>`).join('')}
            </tr>`;

        const rows = p.rows || [];
        $('previewTbody').innerHTML = rows.length
            ? rows.map((line) => `<tr class="hover:bg-slate-50">${line.map((cell) => `<td class="px-4 py-2.5">${escapeHtml(cell)}</td>`).join('')}</tr>`).join('')
            : `<tr><td colspan="${(p.columns || []).length || 1}" class="px-4 py-8 text-center text-slate-400">Không có dữ liệu phát sinh trong kỳ báo cáo.</td></tr>`;

        const summaryEntries = Object.entries(p.summary || {});
        $('previewSummaryGrid').innerHTML = summaryEntries.map(([k, v]) => `
            <div class="p-3 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center text-xs">
                <span class="font-semibold text-slate-600">${escapeHtml(k)}</span>
                <strong class="text-slate-900">${escapeHtml(v)}</strong>
            </div>`).join('');
    }

    function openA4Modal() {
        const p = state.lastPreview;
        if (!p) return;
        const rows = p.rows || [];
        $('a4DialogBody').innerHTML = `
            <div class="flex justify-between items-start border-b-2 border-slate-900 pb-4">
                <div>
                    <h3 class="text-base font-extrabold text-blue-800">CỬA HÀNG ĐIỆN THOẠI PHONESTORE PRO</h3>
                    <p class="text-xs text-slate-600">273 An Dương Vương, Q.5, TP.HCM • Hotline: 1900 8888</p>
                </div>
                <div class="text-right text-xs text-slate-600">
                    <p>Mẫu: <strong>${escapeHtml(p.loaiBaoCao)}</strong></p>
                    <p>Ngày in: ${new Date(p.ngayLap).toLocaleString('vi-VN')}</p>
                </div>
            </div>
            <div class="text-center space-y-1">
                <h3 class="text-lg font-extrabold text-slate-900 uppercase">${escapeHtml(p.tieuDeBaoCao)}</h3>
                <p class="text-xs text-slate-600">${escapeHtml(p.phamViLoc)}</p>
            </div>
            <table class="w-full border-collapse border border-slate-300 text-xs">
                <thead>
                <tr class="bg-slate-100">
                    ${(p.columns || []).map((c) => `<th class="border border-slate-300 px-2.5 py-2 text-left font-bold">${escapeHtml(c)}</th>`).join('')}
                </tr>
                </thead>
                <tbody>
                ${rows.length
                    ? rows.map((line) => `<tr>${line.map((cell) => `<td class="border border-slate-300 px-2.5 py-1.5">${escapeHtml(cell)}</td>`).join('')}</tr>`).join('')
                    : `<tr><td colspan="${(p.columns || []).length || 1}" class="border border-slate-300 px-4 py-6 text-center text-slate-400">Không có dữ liệu</td></tr>`}
                </tbody>
            </table>
            <div class="border border-slate-300 rounded-lg p-3 bg-slate-50 space-y-1 text-xs">
                ${Object.entries(p.summary || {}).map(([k, v]) => `<div class="flex justify-between"><span>${escapeHtml(k)}:</span><strong>${escapeHtml(v)}</strong></div>`).join('')}
            </div>
            <div class="grid grid-cols-2 gap-8 pt-6 text-center text-xs">
                <div>
                    <p class="font-bold">Người lập báo cáo</p>
                    <p class="text-slate-500 italic">(Ký, ghi rõ họ tên)</p>
                    <p class="mt-12 font-bold">${escapeHtml(p.nguoiLap)}</p>
                </div>
                <div>
                    <p class="font-bold">Quản lý phê duyệt</p>
                    <p class="text-slate-500 italic">(Ký, đóng dấu)</p>
                    <p class="mt-12 text-slate-500">Ngày ..... tháng ..... năm .....</p>
                </div>
            </div>`;
        $('a4PreviewDialog').showModal();
    }

    async function downloadBinaryReport(type) {
        const format = type === 'excel' ? 'EXCEL' : 'PDF';
        const endpoint = type === 'excel' ? '/reports/export/excel' : '/reports/export/pdf';
        try {
            const res = await fetch(`${API_BASE_URL}${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(buildReportPayload(format))
            });
            if (!res.ok) {
                const errText = await res.text();
                let msg = `Xuất báo cáo thất bại (${res.status})`;
                try { msg = JSON.parse(errText).message || msg; } catch (_) {}
                throw new Error(msg);
            }
            const blob = await res.blob();
            const dispo = res.headers.get('Content-Disposition') || '';
            const match = /filename="?([^"]+)"?/i.exec(dispo);
            const filename = match ? match[1] : `baocao.${type === 'excel' ? 'xlsx' : 'pdf'}`;

            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);

            showStatus(`Đã xuất báo cáo ${filename} và ghi nhận vào nhật ký hệ thống.`);
            await loadReportHistory();
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    async function loadReportHistory() {
        try {
            const logs = await apiRequest('/reports/history?limit=15');
            const tbody = $('reportHistoryBody');
            if (!logs.length) {
                tbody.innerHTML = '<tr><td colspan="7" class="px-4 py-8 text-center text-slate-400">Chưa có lịch sử xuất báo cáo.</td></tr>';
                return;
            }
            tbody.innerHTML = logs.map((l) => `
                <tr class="hover:bg-slate-50">
                    <td class="px-4 py-3 font-mono text-xs text-slate-500">#${l.maBaoCao}</td>
                    <td class="px-4 py-3 font-bold text-slate-900">${escapeHtml(l.loaiBaoCao)}</td>
                    <td class="px-4 py-3">
                        <span class="px-2 py-0.5 rounded text-xs font-bold ${l.dinhDangXuat === 'EXCEL' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'}">
                            ${escapeHtml(l.dinhDangXuat)}
                        </span>
                    </td>
                    <td class="px-4 py-3 text-xs font-semibold">${escapeHtml(l.tenNhanVienLap)}</td>
                    <td class="px-4 py-3 text-xs text-slate-600">${escapeHtml(l.tieuChiLoc || '-')}</td>
                    <td class="px-4 py-3 font-mono text-xs text-blue-700">${escapeHtml(l.duongDanFile || '-')}</td>
                    <td class="px-4 py-3 text-xs text-slate-500">${new Date(l.ngayLap).toLocaleString('vi-VN')}</td>
                </tr>`).join('');
        } catch (err) {
            showStatus(err.message, true);
        }
    }

    function sortTable(tableId, colIdx) {
        const table = $(tableId);
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        if (rows.length <= 1) return;

        const key = `${tableId}:${colIdx}`;
        const nextDir = state.sortState[key] === 'ascending' ? 'descending' : 'ascending';
        state.sortState[key] = nextDir;

        table.querySelectorAll('thead th').forEach((th, idx) => {
            if (th.hasAttribute('aria-sort')) {
                th.setAttribute('aria-sort', idx === colIdx ? nextDir : 'none');
            }
        });

        rows.sort((a, b) => {
            const va = a.children[colIdx]?.dataset.value ?? a.children[colIdx]?.textContent.trim() ?? '';
            const vb = b.children[colIdx]?.dataset.value ?? b.children[colIdx]?.textContent.trim() ?? '';
            const na = Number(va);
            const nb = Number(vb);
            let cmp;
            if (!Number.isNaN(na) && !Number.isNaN(nb) && va !== '' && vb !== '') {
                cmp = na - nb;
            } else {
                cmp = collator.compare(va, vb);
            }
            return nextDir === 'ascending' ? cmp : -cmp;
        });

        rows.forEach((r) => tbody.appendChild(r));
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
        attachBackdropClose($('a4PreviewDialog'));

        const today = new Date();
        const sixtyDaysAgo = new Date(today.getTime() - 60 * 24 * 60 * 60 * 1000);
        const ninetyDaysAgo = new Date(today.getTime() - 90 * 24 * 60 * 60 * 1000);

        $('revFromDate').value = toIsoDate(sixtyDaysAgo);
        $('revToDate').value = toIsoDate(today);
        $('prodFromDate').value = toIsoDate(ninetyDaysAgo);
        $('prodToDate').value = toIsoDate(today);
        $('repFromDate').value = toIsoDate(sixtyDaysAgo);
        $('repToDate').value = toIsoDate(today);

        // Tabs click & keyboard arrow support
        const tabButtons = Array.from(document.querySelectorAll('button[role="tab"]'));
        tabButtons.forEach((btn, idx) => {
            btn.addEventListener('click', () => activateTab(btn.id));
            btn.addEventListener('keydown', (e) => {
                if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    const next = tabButtons[(idx + 1) % tabButtons.length];
                    next.focus();
                    activateTab(next.id);
                } else if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    const prev = tabButtons[(idx - 1 + tabButtons.length) % tabButtons.length];
                    prev.focus();
                    activateTab(prev.id);
                }
            });
        });

        $('btnReloadActiveTab').addEventListener('click', () => activateTab(state.activeTab));
        $('revFilterForm').addEventListener('submit', (e) => { e.preventDefault(); loadRevenueStats(); });
        $('revGroupBy').addEventListener('change', loadRevenueStats);
        $('prodFilterForm').addEventListener('submit', (e) => { e.preventDefault(); loadProductStats(); });
        $('prodBrandFilter').addEventListener('change', loadProductStats);
        $('invFilterForm').addEventListener('submit', (e) => { e.preventDefault(); loadInventoryStats(); });
        $('invOnlyAlert').addEventListener('change', loadInventoryStats);
        $('reportConfigForm').addEventListener('submit', handlePreviewReport);
        $('btnOpenA4Modal').addEventListener('click', openA4Modal);
        $('btnCloseA4Modal').addEventListener('click', () => $('a4PreviewDialog').close());
        $('btnExportExcel').addEventListener('click', () => downloadBinaryReport('excel'));
        $('btnExportPdf').addEventListener('click', () => downloadBinaryReport('pdf'));

        document.addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-sort-table]');
            if (btn) sortTable(btn.dataset.sortTable, Number(btn.dataset.col));
        });

        await loadSharedDropdowns();
        await loadRevenueStats();
        await handlePreviewReport();
    });
})();

