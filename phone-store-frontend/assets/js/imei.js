// =========================================================================
// QUẢN LÝ IMEI / SERIAL - tích hợp backend (theo sequence diagram 3.3.2.4)
// Có fallback dữ liệu mẫu khi backend chưa sẵn sàng.
// =========================================================================

const API_BASE_URL = 'http://localhost:8080/api';

async function fetchAPI(endpoint, options = {}) {
    try {
        const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
        const response = await fetch(url, {
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
            ...options
        });
        if (!response.ok) return null;
        const ct = response.headers.get('content-type') || '';
        if (ct.includes('application/json')) return await response.json();
        return await response.text();
    } catch (e) {
        console.warn('API unavailable:', endpoint, e);
        return null;
    }
}

const RAW_IMEI_LIST_FALLBACK = [
    { imei: '358941091234561', model: 'iPhone 15 Pro Max 256GB', brand: 'Apple', color: 'Titan Tự Nhiên', rom: '256GB', phieuNhap: 'PN20261001_01', hoaDon: 'HD20261012_001', customer: 'Trần Văn An', status: 'Dang_bao_hanh', importDate: '01/10/2026', sellDate: '12/10/2026', warrantyExpiry: '12/10/2027' },
    { imei: '358941091234562', model: 'iPhone 15 Pro Max 256GB', brand: 'Apple', color: 'Titan Tự Nhiên', rom: '256GB', phieuNhap: 'PN20261001_01', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '01/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '358941091234563', model: 'iPhone 15 Pro Max 256GB', brand: 'Apple', color: 'Titan Tự Nhiên', rom: '256GB', phieuNhap: 'PN20261001_01', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '01/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '358941091234564', model: 'iPhone 15 Pro Max 256GB', brand: 'Apple', color: 'Titan Tự Nhiên', rom: '256GB', phieuNhap: 'PN20261001_01', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '01/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '358941091234565', model: 'iPhone 15 Pro Max 256GB', brand: 'Apple', color: 'Titan Tự Nhiên', rom: '256GB', phieuNhap: 'PN20261001_01', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '01/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '864219082345671', model: 'Samsung Galaxy S24 Ultra 256GB', brand: 'Samsung', color: 'Xám Titan', rom: '256GB', phieuNhap: 'PN20261005_02', hoaDon: 'HD20261014_002', customer: 'Nguyễn Thị Bích', status: 'Da_ban', importDate: '05/10/2026', sellDate: '14/10/2026', warrantyExpiry: '14/10/2027' },
    { imei: '864219082345672', model: 'Samsung Galaxy S24 Ultra 256GB', brand: 'Samsung', color: 'Xám Titan', rom: '256GB', phieuNhap: 'PN20261005_02', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '05/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '864219082345673', model: 'Samsung Galaxy S24 Ultra 256GB', brand: 'Samsung', color: 'Xám Titan', rom: '256GB', phieuNhap: 'PN20261005_02', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '05/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '864219082345674', model: 'Samsung Galaxy S24 Ultra 256GB', brand: 'Samsung', color: 'Xám Titan', rom: '256GB', phieuNhap: 'PN20261005_02', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '05/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '861054073456781', model: 'Xiaomi 14 256GB', brand: 'Xiaomi', color: 'Trắng Tuyết', rom: '256GB', phieuNhap: 'PN20261010_03', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '10/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '861054073456782', model: 'Xiaomi 14 256GB', brand: 'Xiaomi', color: 'Trắng Tuyết', rom: '256GB', phieuNhap: 'PN20261010_03', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '10/10/2026', sellDate: null, warrantyExpiry: null },
    { imei: '861054073456783', model: 'Xiaomi 14 256GB', brand: 'Xiaomi', color: 'Trắng Tuyết', rom: '256GB', phieuNhap: 'PN20261010_03', hoaDon: null, customer: null, status: 'Trong_kho', importDate: '10/10/2026', sellDate: null, warrantyExpiry: null }
];

let RAW_IMEI_LIST = [];
let currentFilter = 'all';
let currentSearch = '';
let isApiSource = false;

document.addEventListener('DOMContentLoaded', () => {
    reloadImeiData();
});

function formatDateDisplay(value) {
    if (!value) return null;
    if (typeof value === 'string' && /^\d{2}\/\d{2}\/\d{4}$/.test(value)) return value;
    const d = new Date(value);
    if (isNaN(d.getTime())) return String(value);
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
}

function mapImeiFromApi(item) {
    const model = item.tenSanPham || item.maSanPham || 'Thiết bị';
    const brand = model.split(' ')[0] || 'Khác';
    return {
        imei: item.maImei,
        model: model,
        brand: brand,
        color: item.mauSac || '-',
        rom: item.rom || '-',
        phieuNhap: item.maPhieuNhap || '-',
        hoaDon: item.maHoaDon || null,
        customer: item.maHoaDon ? 'Khách hàng (theo hóa đơn)' : null,
        status: item.trangThai || 'Trong_kho',
        importDate: formatDateDisplay(item.ngayNhap) || '-',
        sellDate: formatDateDisplay(item.ngayBan),
        warrantyExpiry: formatDateDisplay(item.hanBaoHanh),
        maSanPham: item.maSanPham || null,
        raw: item
    };
}

async function reloadImeiData() {
    showLoading();
    const data = await fetchAPI('/imeis');
    if (Array.isArray(data)) {
        RAW_IMEI_LIST = data.map(mapImeiFromApi);
        isApiSource = true;
    } else {
        RAW_IMEI_LIST = [...RAW_IMEI_LIST_FALLBACK];
        isApiSource = false;
    }
    hideLoading();
    updateCounts();
    renderTable();
}

function showLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-amber-50 border-b border-amber-200 px-6 py-2.5 flex items-center justify-between text-xs text-amber-900 transition-all duration-300';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-amber-600 text-sm';
    bannerText.textContent = 'Đang đồng bộ danh sách IMEI từ hệ thống...';

    document.getElementById('tableSkeleton').classList.remove('hidden');
    document.getElementById('tableContent').classList.add('hidden');
    document.getElementById('emptyState').classList.add('hidden');
}

function hideLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    if (isApiSource) {
        banner.className = 'bg-emerald-50 border-b border-emerald-200 px-6 py-2.5 flex items-center justify-between text-xs text-emerald-800 transition-all duration-300';
        spinner.className = 'fa-solid fa-circle-check text-emerald-600 text-sm';
        bannerText.innerHTML = `<span class="font-semibold">Đồng bộ hoàn tất:</span> Đã nạp ${RAW_IMEI_LIST.length} bản ghi IMEI từ backend.`;
    } else {
        banner.className = 'bg-amber-50 border-b border-amber-200 px-6 py-2.5 flex items-center justify-between text-xs text-amber-900 transition-all duration-300';
        spinner.className = 'fa-solid fa-triangle-exclamation text-amber-600 text-sm';
        bannerText.innerHTML = `<span class="font-semibold">Chế độ dữ liệu mẫu:</span> Không kết nối được backend, đang hiển thị ${RAW_IMEI_LIST.length} IMEI minh họa.`;
    }

    document.getElementById('tableSkeleton').classList.add('hidden');
    document.getElementById('tableContent').classList.remove('hidden');
}

function updateCounts() {
    const total = RAW_IMEI_LIST.length;
    const inStock = RAW_IMEI_LIST.filter(i => i.status === 'Trong_kho').length;
    const sold = RAW_IMEI_LIST.filter(i => i.status === 'Da_ban').length;
    const warranty = RAW_IMEI_LIST.filter(i => i.status === 'Dang_bao_hanh').length;

    const setText = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
    setText('countAll', total);
    setText('countInStock', inStock);
    setText('countSold', sold);
    setText('countWarranty', warranty);
}

function filterStatus(status) {
    currentFilter = status;
    const buttons = {
        'all': document.getElementById('btnFilterAll'),
        'Trong_kho': document.getElementById('btnFilterInStock'),
        'Da_ban': document.getElementById('btnFilterSold'),
        'Dang_bao_hanh': document.getElementById('btnFilterWarranty')
    };
    Object.keys(buttons).forEach(k => {
        const btn = buttons[k];
        if (!btn) return;
        if (k === status) {
            btn.className = 'px-3 py-2 rounded-xl bg-slate-900 text-white font-bold shadow-sm transition';
        } else {
            btn.className = 'px-3 py-2 rounded-xl bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold transition';
        }
    });
    renderTable();
}

function handleSearchImei(val) {
    currentSearch = (val || '').trim().toLowerCase();
    renderTable();
}

function escapeHtml(str) {
    return String(str == null ? '' : str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function renderTable() {
    const tbody = document.getElementById('imeiTbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    const filtered = RAW_IMEI_LIST.filter(item => {
        const matchStatus = (currentFilter === 'all') || (item.status === currentFilter);
        const matchSearch = !currentSearch ||
            (item.imei || '').toLowerCase().includes(currentSearch) ||
            (item.model || '').toLowerCase().includes(currentSearch) ||
            (item.color || '').toLowerCase().includes(currentSearch) ||
            (item.phieuNhap || '').toLowerCase().includes(currentSearch) ||
            (item.hoaDon || '').toLowerCase().includes(currentSearch) ||
            (item.customer || '').toLowerCase().includes(currentSearch) ||
            (item.maSanPham || '').toLowerCase().includes(currentSearch);
        return matchStatus && matchSearch;
    });

    if (filtered.length === 0) {
        document.getElementById('tableContent').classList.add('hidden');
        document.getElementById('emptyState').classList.remove('hidden');
        return;
    }

    document.getElementById('tableContent').classList.remove('hidden');
    document.getElementById('emptyState').classList.add('hidden');

    filtered.forEach(item => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-50 transition border-b border-slate-100';

        let statusBadge = '';
        if (item.status === 'Trong_kho') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200"><i class="fa-solid fa-box mr-1"></i>Trong kho</span>';
        } else if (item.status === 'Da_ban') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200"><i class="fa-solid fa-check mr-1"></i>Đã bán</span>';
        } else if (item.status === 'Dang_bao_hanh') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200"><i class="fa-solid fa-wrench mr-1"></i>Bảo hành</span>';
        } else {
            statusBadge = `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200">${escapeHtml(item.status)}</span>`;
        }

        tr.innerHTML = `
            <td class="py-3.5 px-4 font-mono font-bold text-slate-900 tracking-wider">
                <span class="bg-slate-100 px-2 py-1 rounded border border-slate-200">${escapeHtml(item.imei)}</span>
            </td>
            <td class="py-3.5 px-4">
                <p class="font-bold text-slate-800">${escapeHtml(item.model)}</p>
                <p class="text-xs text-slate-400">Hãng: ${escapeHtml(item.brand || '')}</p>
            </td>
            <td class="py-3.5 px-3 text-center">
                <span class="font-medium text-slate-700">${escapeHtml(item.color || '-')}</span>
                <span class="block text-xs text-slate-400 font-mono">${escapeHtml(item.rom || '-')}</span>
            </td>
            <td class="py-3.5 px-3 text-center">${statusBadge}</td>
            <td class="py-3.5 px-3 text-center font-mono font-medium text-slate-600">${escapeHtml(item.phieuNhap || '-')}</td>
            <td class="py-3.5 px-3 text-center">
                ${item.hoaDon ? `<span class="font-mono text-blue-600 font-bold">${escapeHtml(item.hoaDon)}</span><br><span class="text-xs text-slate-500">${escapeHtml(item.customer || '')}</span>` : '<span class="text-slate-400">—</span>'}
            </td>
            <td class="py-3.5 px-3 text-center">
                ${item.warrantyExpiry ? `<span class="text-rose-600 font-semibold">${escapeHtml(item.warrantyExpiry)}</span>` : '<span class="text-slate-400">Chưa kích hoạt</span>'}
            </td>
            <td class="py-3.5 px-4 text-center">
                <button onclick="inspectImei('${escapeHtml(item.imei).replace(/'/g, "\\'")}')" class="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition" title="Tra cứu máy">
                    <i class="fa-solid fa-magnifying-glass"></i> Chi tiết
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function inspectImei(imei) {
    document.getElementById('warrantySearchInput').value = imei;
    openWarrantyModal();
    doCheckWarranty();
}

function openWarrantyModal() {
    document.getElementById('warrantyModal').classList.remove('hidden');
}
function closeWarrantyModal() {
    document.getElementById('warrantyModal').classList.add('hidden');
}

async function openAddImeiModal() {
    document.getElementById('addImeiModal').classList.remove('hidden');
    const msg = document.getElementById('addImeiMessage');
    if (msg) { msg.classList.add('hidden'); msg.textContent = ''; }
    await populateAddImeiProducts();
}

function closeAddImeiModal() {
    document.getElementById('addImeiModal').classList.add('hidden');
}

async function populateAddImeiProducts() {
    const select = document.getElementById('addImeiProductSelect');
    if (!select) return;
    const products = await fetchAPI('/inventory/products');
    if (!Array.isArray(products) || products.length === 0) {
        // fallback: derive from loaded IMEI or keep placeholder
        const unique = new Map();
        RAW_IMEI_LIST.forEach(i => { if (i.maSanPham && i.model) unique.set(i.maSanPham, i.model); });
        if (unique.size === 0) {
            select.innerHTML = '<option value="">-- Không tải được sản phẩm (nhập mã thủ công khi có API) --</option>';
            return;
        }
        select.innerHTML = '<option value="">-- Chọn sản phẩm --</option>' + Array.from(unique.entries()).map(([id, name]) => `<option value="${escapeHtml(id)}">${escapeHtml(name)} (${escapeHtml(id)})</option>`).join('');
        return;
    }
    select.innerHTML = '<option value="">-- Chọn sản phẩm --</option>' + products.map(p => {
        const label = `${p.tenSanPham || p.maSanPham} (${p.maSanPham})`;
        return `<option value="${escapeHtml(p.maSanPham)}">${escapeHtml(label)}</option>`;
    }).join('');
}

function parseImeiInput(text) {
    if (!text) return [];
    return text.split(/[\n,\s;]+/).map(s => s.trim()).filter(Boolean);
}

async function submitAddImeis() {
    const productSelect = document.getElementById('addImeiProductSelect');
    const receiptInput = document.getElementById('addImeiReceiptInput');
    const listInput = document.getElementById('addImeiListInput');
    const msg = document.getElementById('addImeiMessage');
    const btn = document.getElementById('btnSaveImeis');

    const maSanPham = productSelect ? productSelect.value.trim() : '';
    const maPhieuNhap = receiptInput ? receiptInput.value.trim() : '';
    const imeis = parseImeiInput(listInput ? listInput.value : '');

    const showMsg = (text, ok) => {
        if (!msg) return;
        msg.textContent = text;
        msg.className = `text-xs rounded-lg p-2 ${ok ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-rose-50 text-rose-700 border border-rose-200'}`;
        msg.classList.remove('hidden');
    };

    if (!maSanPham) { showMsg('Vui lòng chọn sản phẩm.', false); return; }
    if (!maPhieuNhap) { showMsg('Vui lòng nhập mã phiếu nhập đã tồn tại.', false); return; }
    if (imeis.length === 0) { showMsg('Vui lòng nhập ít nhất một mã IMEI.', false); return; }

    if (btn) { btn.disabled = true; btn.textContent = 'Đang lưu...'; }
    const res = await fetchAPI('/imeis', {
        method: 'POST',
        body: JSON.stringify({ maSanPham, maPhieuNhap, imeis })
    });
    if (btn) { btn.disabled = false; btn.textContent = 'Lưu IMEI'; }

    if (res && (res.added != null || res.message)) {
        showMsg(res.message || `Đã thêm ${res.added} IMEI.`, true);
        if (listInput) listInput.value = '';
        await reloadImeiData();
    } else {
        showMsg('Không thể lưu IMEI. Vui lòng kiểm tra mã phiếu nhập/sản phẩm và kết nối backend.', false);
    }
}

async function doCheckWarranty() {
    const inputEl = document.getElementById('warrantySearchInput');
    const inputVal = inputEl ? inputEl.value.trim() : '';
    if (!inputVal) return;

    let item = RAW_IMEI_LIST.find(i => (i.imei || '').toLowerCase() === inputVal.toLowerCase());
    // Try backend detail if not found locally or to refresh
    const apiDetail = await fetchAPI(`/imeis/${encodeURIComponent(inputVal)}`);
    if (apiDetail && apiDetail.maImei) {
        item = mapImeiFromApi(apiDetail);
        // merge/update cache
        const idx = RAW_IMEI_LIST.findIndex(i => i.imei === item.imei);
        if (idx >= 0) RAW_IMEI_LIST[idx] = item; else RAW_IMEI_LIST.unshift(item);
        updateCounts();
        renderTable();
    }

    if (item) {
        setTextSafe('resModel', item.model || '-');
        setTextSafe('resImei', item.imei || '-');
        setTextSafe('resColor', item.color || '-');
        setTextSafe('resCustomer', item.customer || (item.hoaDon ? 'Theo hóa đơn ' + item.hoaDon : 'Chưa bán (Thuộc kho)'));
        setTextSafe('resInvoice', item.hoaDon || 'Chưa xuất bán');
        setTextSafe('resBuyDate', item.sellDate || 'Chưa bán');
        setTextSafe('resExpireDate', item.warrantyExpiry || (item.status === 'Trong_kho' ? '12 tháng kể từ ngày bán' : '-'));

        const badge = document.getElementById('resStatusBadge');
        if (badge) {
            if (item.status === 'Dang_bao_hanh') {
                badge.className = 'px-2 py-0.5 rounded-full font-bold bg-amber-100 text-amber-800';
                badge.textContent = 'Đang tiếp nhận bảo hành';
            } else if (item.status === 'Da_ban') {
                badge.className = 'px-2 py-0.5 rounded-full font-bold bg-emerald-100 text-emerald-800';
                badge.textContent = 'Còn hạn bảo hành';
            } else {
                badge.className = 'px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-700';
                badge.textContent = 'Hàng mới trong kho';
            }
        }
    } else {
        alert('Không tìm thấy thông tin của mã IMEI này trên hệ thống!');
    }
}

function setTextSafe(id, text) {
    const el = document.getElementById(id);
    if (el) el.textContent = text;
}
