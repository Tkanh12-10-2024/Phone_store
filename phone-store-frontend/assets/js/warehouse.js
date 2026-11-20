// ========================================================================
// WAREHOUSE & INVENTORY MANAGEMENT (UC 2.6.1 & UC 2.6.2) - NHÓM 05
// ========================================================================

const MOCK_WAREHOUSE_PRODUCTS = [
    { maSanPham: 'SP_IP15PM_256', tenSanPham: 'iPhone 15 Pro Max 256GB Titan Tự Nhiên', dongMay: 'iPhone 15 Series', mauSac: 'Titan Tự Nhiên', ram: '8GB', rom: '256GB', giaNhapThamKhao: 26500000, giaBan: 29990000, soLuongTon: 5, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_IP15PM_512', tenSanPham: 'iPhone 15 Pro Max 512GB Titan Xanh', dongMay: 'iPhone 15 Series', mauSac: 'Titan Xanh', ram: '8GB', rom: '512GB', giaNhapThamKhao: 31500000, giaBan: 35490000, soLuongTon: 2, dinhMucTonToiThieu: 3, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_IP15_128', tenSanPham: 'iPhone 15 128GB Hồng Pastel', dongMay: 'iPhone 15 Series', mauSac: 'Hồng Pastel', ram: '6GB', rom: '128GB', giaNhapThamKhao: 17200000, giaBan: 19490000, soLuongTon: 1, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_SS_S24U_256', tenSanPham: 'Samsung Galaxy S24 Ultra 256GB Xám Titan', dongMay: 'Galaxy S Series', mauSac: 'Xám Titan', ram: '12GB', rom: '256GB', giaNhapThamKhao: 24000000, giaBan: 27990000, soLuongTon: 4, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_SS_S24_256', tenSanPham: 'Samsung Galaxy S24 256GB Vàng Amber', dongMay: 'Galaxy S Series', mauSac: 'Vàng Amber', ram: '8GB', rom: '256GB', giaNhapThamKhao: 15500000, giaBan: 17990000, soLuongTon: 0, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_OPPO_R11_256', tenSanPham: 'OPPO Reno11 5G 256GB Xanh Sóng Biển', dongMay: 'Reno Series', mauSac: 'Xanh Sóng Biển', ram: '8GB', rom: '256GB', giaNhapThamKhao: 8200000, giaBan: 9990000, soLuongTon: 6, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' },
    { maSanPham: 'SP_XM_14U_512', tenSanPham: 'Xiaomi 14 Ultra 512GB Đen Nhiếp Ảnh', dongMay: 'Xiaomi Number Series', mauSac: 'Đen', ram: '16GB', rom: '512GB', giaNhapThamKhao: 26000000, giaBan: 29990000, soLuongTon: 1, dinhMucTonToiThieu: 2, trangThai: 'Dang_kinh_doanh' }
];

const MOCK_CHECKS = [
    {
        maPhieuKiemKe: 'PKK20261111_01',
        maNhanVien: 'NV001',
        ngayKiemKe: '2026-11-11T17:00:00',
        trangThai: 'Da_can_bang',
        ghiChu: 'Kiểm kê định kỳ đầu tuần khu vực iPhone'
    }
];

let warehouseProducts = [];
let warehouseChecks = [];
let currentFilter = 'all'; // 'all' hoặc 'low'
let selectedProductForMinStock = null;

document.addEventListener('DOMContentLoaded', async () => {
    await loadWarehouseData();
});

// Chuyển tab giữa Tồn kho và Kiểm kê
function switchWarehouseTab(tab) {
    const tabStock = document.getElementById('tabContentStock');
    const tabChecks = document.getElementById('tabContentChecks');
    const btnStock = document.getElementById('tabBtnStock');
    const btnChecks = document.getElementById('tabBtnChecks');

    if (tab === 'stock') {
        tabStock.classList.remove('hidden');
        tabChecks.classList.add('hidden');
        btnStock.className = "py-4 text-sm font-bold border-b-2 border-blue-600 text-blue-600 flex items-center gap-2";
        btnChecks.className = "py-4 text-sm font-semibold border-b-2 border-transparent text-slate-500 hover:text-slate-800 flex items-center gap-2";
    } else {
        tabStock.classList.add('hidden');
        tabChecks.classList.remove('hidden');
        btnChecks.className = "py-4 text-sm font-bold border-b-2 border-blue-600 text-blue-600 flex items-center gap-2";
        btnStock.className = "py-4 text-sm font-semibold border-b-2 border-transparent text-slate-500 hover:text-slate-800 flex items-center gap-2";
    }
}

// Tải dữ liệu từ Backend
async function loadWarehouseData() {
    const pData = await fetchAPI('/inventory/products');
    warehouseProducts = (pData && Array.isArray(pData) && pData.length > 0) ? pData : [...MOCK_WAREHOUSE_PRODUCTS];

    const cData = await fetchAPI('/inventory/checks');
    warehouseChecks = (cData && Array.isArray(cData) && cData.length > 0) ? cData : [...MOCK_CHECKS];

    renderProductsTable();
    renderChecksTable();
    updateWarehouseStats();
}

function updateWarehouseStats() {
    document.getElementById('statTotalProducts').innerText = warehouseProducts.length;

    const totalQty = warehouseProducts.reduce((sum, p) => sum + (p.soLuongTon || 0), 0);
    document.getElementById('statTotalStockQty').innerText = totalQty;

    const lowStockList = warehouseProducts.filter(p => (p.soLuongTon || 0) <= (p.dinhMucTonToiThieu || 2));
    const lowCount = lowStockList.length;
    document.getElementById('statLowStockCount').innerText = lowCount;

    document.getElementById('statTotalChecks').innerText = warehouseChecks.length;

    // Hiển thị banner cảnh báo nếu có
    const banner = document.getElementById('warningBanner');
    if (lowCount > 0) {
        banner.classList.remove('hidden');
        document.getElementById('warningBannerText').innerText = 
            `Hệ thống phát hiện ${lowCount} sản phẩm có tồn kho thấp hơn hoặc bằng định mức an toàn. Hãy kiểm tra và lên kế hoạch nhập hàng!`;
    } else {
        banner.classList.add('hidden');
    }
}

function formatCurrency(num) {
    if (num == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
}

function formatDateTime(dtStr) {
    if (!dtStr) return '-';
    const d = new Date(dtStr);
    return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// Render bảng sản phẩm tồn kho
function renderProductsTable() {
    const tbody = document.getElementById('productsTableBody');
    if (!tbody) return;

    let displayList = [...warehouseProducts];
    if (currentFilter === 'low') {
        displayList = displayList.filter(p => (p.soLuongTon || 0) <= (p.dinhMucTonToiThieu || 2));
    }

    const q = document.getElementById('searchProductInput')?.value.toLowerCase().trim();
    if (q) {
        displayList = displayList.filter(p =>
            p.tenSanPham.toLowerCase().includes(q) ||
            p.maSanPham.toLowerCase().includes(q) ||
            (p.mauSac && p.mauSac.toLowerCase().includes(q))
        );
    }

    if (displayList.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="p-6 text-center text-slate-400">Không có sản phẩm nào phù hợp.</td></tr>`;
        return;
    }

    tbody.innerHTML = displayList.map(p => {
        const ton = p.soLuongTon || 0;
        const minStock = p.dinhMucTonToiThieu != null ? p.dinhMucTonToiThieu : 2;
        const isWarning = ton <= minStock;

        let statusBadge;
        if (ton === 0) {
            statusBadge = `<span class="px-2.5 py-0.5 text-xs rounded-full font-bold bg-rose-100 text-rose-700 animate-pulse">Hết hàng</span>`;
        } else if (isWarning) {
            statusBadge = `<span class="px-2.5 py-0.5 text-xs rounded-full font-bold bg-amber-100 text-amber-700">Sắp hết (Cảnh báo)</span>`;
        } else {
            statusBadge = `<span class="px-2.5 py-0.5 text-xs rounded-full font-semibold bg-emerald-100 text-emerald-700">Dồi dào</span>`;
        }

        const stockColor = isWarning ? 'text-rose-600 font-extrabold' : 'text-slate-800 font-bold';

        return `
            <tr class="hover:bg-slate-50 transition border-b border-slate-100 ${isWarning ? 'bg-amber-50/30' : ''}">
                <td class="p-3 font-mono font-semibold text-blue-600 text-xs">${p.maSanPham}</td>
                <td class="p-3 font-bold text-slate-800">
                    <div>${p.tenSanPham}</div>
                </td>
                <td class="p-3 text-slate-600 text-xs">${p.dongMay} <br><span class="text-slate-400">Màu: ${p.mauSac}</span></td>
                <td class="p-3 text-slate-600 text-xs font-mono">${p.ram} / ${p.rom}</td>
                <td class="p-3 text-right font-medium text-slate-700">${formatCurrency(p.giaNhapThamKhao)}</td>
                <td class="p-3 text-center ${stockColor} text-base">${ton}</td>
                <td class="p-3 text-center font-semibold text-slate-600">
                    <button onclick="openMinStockModal('${p.maSanPham}')" class="px-2 py-0.5 rounded bg-slate-100 hover:bg-blue-50 hover:text-blue-600 transition" title="Bấm để chỉnh sửa định mức">
                        ${minStock} <i class="fa-solid fa-pen text-[10px] ml-1 opacity-60"></i>
                    </button>
                </td>
                <td class="p-3 text-center">${statusBadge}</td>
                <td class="p-3 text-center space-x-1">
                    <button onclick="viewProductImeis('${p.maSanPham}', '${p.tenSanPham}')" class="px-2.5 py-1 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 rounded text-xs font-semibold inline-flex items-center gap-1">
                        <i class="fa-solid fa-barcode"></i> Mã IMEI
                    </button>
                    <button onclick="openMinStockModal('${p.maSanPham}')" class="px-2 py-1 bg-slate-100 text-slate-600 hover:bg-slate-200 rounded text-xs font-semibold" title="Thiết lập định mức">
                        <i class="fa-solid fa-sliders"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterProducts() {
    renderProductsTable();
}

function setFilterCategory(cat) {
    currentFilter = cat;
    const btnAll = document.getElementById('filterBtnAll');
    const btnLow = document.getElementById('filterBtnLow');

    if (cat === 'all') {
        btnAll.className = "px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-50 text-blue-700 border border-blue-200";
        btnLow.className = "px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600 hover:bg-rose-50 hover:text-rose-600 border border-slate-200";
    } else {
        btnLow.className = "px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200";
        btnAll.className = "px-3 py-1.5 rounded-lg text-xs font-semibold bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-blue-600 border border-slate-200";
    }
    renderProductsTable();
}

function filterOnlyWarnings() {
    setFilterCategory('low');
    switchWarehouseTab('stock');
}

// ========================================================================
// THIẾT LẬP ĐỊNH MỨC TỒN KHO TỐI THIỂU (UC 2.6.1.1)
// ========================================================================
function openMinStockModal(maSanPham) {
    const p = warehouseProducts.find(x => x.maSanPham === maSanPham);
    if (!p) return;

    selectedProductForMinStock = p;
    document.getElementById('minStockProductName').innerText = p.tenSanPham;
    document.getElementById('minStockProductCode').innerText = p.maSanPham;
    document.getElementById('inputMinStock').value = p.dinhMucTonToiThieu != null ? p.dinhMucTonToiThieu : 2;

    document.getElementById('minStockModal').classList.remove('hidden');
}

function closeMinStockModal() {
    document.getElementById('minStockModal').classList.add('hidden');
}

async function submitMinStock() {
    if (!selectedProductForMinStock) return;
    const minVal = parseInt(document.getElementById('inputMinStock').value);
    if (isNaN(minVal) || minVal < 0) {
        alert('Vui lòng nhập định mức hợp lệ (>= 0)!');
        return;
    }

    const res = await fetchAPI(`/inventory/products/${selectedProductForMinStock.maSanPham}/min-stock`, {
        method: 'PUT',
        body: JSON.stringify({ dinhMucTonToiThieu: minVal })
    });

    if (res) {
        selectedProductForMinStock.dinhMucTonToiThieu = minVal;
        alert('Cập nhật định mức tồn kho tối thiểu thành công!');
    } else {
        selectedProductForMinStock.dinhMucTonToiThieu = minVal;
        alert('Đã cập nhật định mức tồn kho tối thiểu!');
    }

    closeMinStockModal();
    renderProductsTable();
    updateWarehouseStats();
}

// ========================================================================
// XEM DANH SÁCH MÃ IMEI TRONG KHO
// ========================================================================
async function viewProductImeis(maSanPham, tenSanPham) {
    document.getElementById('viewImeisTitle').innerText = 'IMEI Trong Kho: ' + maSanPham;
    document.getElementById('viewImeisSubtitle').innerText = tenSanPham;

    const imeisData = await fetchAPI(`/inventory/products/${maSanPham}/imeis`);
    const container = document.getElementById('imeisContainer');

    let imeis = [];
    if (imeisData && Array.isArray(imeisData) && imeisData.length > 0) {
        imeis = imeisData;
    } else {
        // Fallback mock
        const p = warehouseProducts.find(x => x.maSanPham === maSanPham);
        const count = p ? p.soLuongTon : 0;
        for (let i = 1; i <= count; i++) {
            imeis.push({
                maImei: '35' + (9000000000000 + i * 12345),
                trangThai: 'Trong_kho',
                ngayNhap: new Date().toISOString()
            });
        }
    }

    if (imeis.length === 0) {
        container.innerHTML = `<p class="text-center text-slate-400 py-6 text-sm">Hiện tại không có máy nào trong kho cho sản phẩm này.</p>`;
    } else {
        container.innerHTML = imeis.map((im, idx) => `
            <div class="p-3 bg-slate-50 border border-slate-200 rounded-lg flex items-center justify-between text-xs">
                <div class="flex items-center gap-2">
                    <span class="w-6 h-6 rounded bg-blue-100 text-blue-700 font-bold flex items-center justify-center text-[10px]">${idx + 1}</span>
                    <span class="font-mono font-bold text-slate-800 text-sm tracking-wide">${im.maImei}</span>
                </div>
                <div class="flex items-center gap-2">
                    <span class="px-2 py-0.5 rounded bg-emerald-100 text-emerald-700 font-semibold text-[11px]">${im.trangThai || 'Trong_kho'}</span>
                </div>
            </div>
        `).join('');
    }

    document.getElementById('viewImeisModal').classList.remove('hidden');
}

function closeViewImeisModal() {
    document.getElementById('viewImeisModal').classList.add('hidden');
}

// ========================================================================
// KIỂM KÊ KHO HÀNG (UC 2.6.2, UC 2.6.2.1, UC 2.6.2.2 & UC 2.6.2.3)
// ========================================================================
function renderChecksTable() {
    const tbody = document.getElementById('checksTableBody');
    if (!tbody) return;

    if (warehouseChecks.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="p-6 text-center text-slate-400">Chưa có đợt kiểm kê nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = warehouseChecks.map(c => `
        <tr class="hover:bg-slate-50 transition border-b border-slate-100">
            <td class="p-3 font-mono font-semibold text-blue-600">${c.maPhieuKiemKe}</td>
            <td class="p-3 font-semibold text-slate-700">${c.maNhanVien || 'NV001'}</td>
            <td class="p-3 text-slate-500">${formatDateTime(c.ngayKiemKe)}</td>
            <td class="p-3 text-slate-600 max-w-[250px] truncate">${c.ghiChu || '-'}</td>
            <td class="p-3 text-center">
                <span class="px-2.5 py-1 text-xs rounded-full font-semibold ${c.trangThai === 'Da_can_bang' ? 'bg-emerald-100 text-emerald-700' : 'bg-amber-100 text-amber-700'}">
                    ${c.trangThai === 'Da_can_bang' ? 'Đã cân bằng kho' : 'Chờ xử lý'}
                </span>
            </td>
            <td class="p-3 text-center">
                <button onclick="viewCheckDetail('${c.maPhieuKiemKe}')" class="px-2.5 py-1 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded text-xs font-semibold inline-flex items-center gap-1">
                    <i class="fa-solid fa-eye"></i> Xem Chi Tiết
                </button>
            </td>
        </tr>
    `).join('');
}

function filterChecks() {
    const q = document.getElementById('searchCheckInput').value.toLowerCase().trim();
    const tbody = document.getElementById('checksTableBody');
    if (!q) {
        renderChecksTable();
        return;
    }
    const filtered = warehouseChecks.filter(c =>
        c.maPhieuKiemKe.toLowerCase().includes(q) ||
        (c.maNhanVien && c.maNhanVien.toLowerCase().includes(q))
    );
    tbody.innerHTML = filtered.map(c => `
        <tr class="hover:bg-slate-50 transition border-b border-slate-100">
            <td class="p-3 font-mono font-semibold text-blue-600">${c.maPhieuKiemKe}</td>
            <td class="p-3 font-semibold text-slate-700">${c.maNhanVien || 'NV001'}</td>
            <td class="p-3 text-slate-500">${formatDateTime(c.ngayKiemKe)}</td>
            <td class="p-3 text-slate-600">${c.ghiChu || '-'}</td>
            <td class="p-3 text-center">
                <span class="px-2.5 py-1 text-xs rounded-full font-semibold bg-emerald-100 text-emerald-700">
                    ${c.trangThai || 'Đã cân bằng'}
                </span>
            </td>
            <td class="p-3 text-center">
                <button onclick="viewCheckDetail('${c.maPhieuKiemKe}')" class="px-2.5 py-1 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded text-xs font-semibold">
                    Xem
                </button>
            </td>
        </tr>
    `).join('');
}

// Mở modal tạo phiếu kiểm kê mới (Tải toàn bộ sản phẩm hiện có để đếm)
function openCreateCheckModal() {
    const now = new Date();
    const codeSuffix = now.getFullYear().toString() +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') + '_' +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0');

    document.getElementById('newCheckCode').value = 'PKK' + codeSuffix;
    document.getElementById('newCheckNote').value = '';

    const tbody = document.getElementById('checkItemsTableBody');
    tbody.innerHTML = warehouseProducts.map((p, idx) => {
        const sysQty = p.soLuongTon || 0;
        return `
            <tr id="checkRow_${idx}" class="hover:bg-slate-50">
                <td class="p-3">
                    <span class="font-bold text-slate-800 text-xs block">${p.tenSanPham}</span>
                    <span class="font-mono text-[11px] text-blue-600">${p.maSanPham}</span>
                    <input type="hidden" class="check-product-id" value="${p.maSanPham}">
                </td>
                <td class="p-3 text-center font-bold text-slate-700 check-sys-qty" data-val="${sysQty}">
                    ${sysQty}
                </td>
                <td class="p-3 text-center">
                    <input type="number" min="0" value="${sysQty}" 
                           class="w-20 px-2 py-1 border border-slate-300 rounded text-center font-bold text-sm bg-white check-actual-qty" 
                           oninput="onActualQtyChange(${idx})">
                </td>
                <td class="p-3 text-center font-bold text-sm check-diff-display text-slate-400">
                    0
                </td>
                <td class="p-3">
                    <input type="text" placeholder="Lý do nếu lệch..." 
                           class="w-full px-2 py-1 border border-slate-200 rounded text-xs bg-white check-reason">
                </td>
            </tr>
        `;
    }).join('');

    document.getElementById('createCheckModal').classList.remove('hidden');
}

function closeCreateCheckModal() {
    document.getElementById('createCheckModal').classList.add('hidden');
}

function onActualQtyChange(idx) {
    const row = document.getElementById('checkRow_' + idx);
    if (!row) return;

    const sysQty = parseInt(row.querySelector('.check-sys-qty').getAttribute('data-val')) || 0;
    const actualQty = parseInt(row.querySelector('.check-actual-qty').value) || 0;
    const diff = actualQty - sysQty;

    const diffDisplay = row.querySelector('.check-diff-display');
    if (diff === 0) {
        diffDisplay.innerText = '0';
        diffDisplay.className = "p-3 text-center font-bold text-sm check-diff-display text-slate-400";
    } else if (diff > 0) {
        diffDisplay.innerText = '+' + diff;
        diffDisplay.className = "p-3 text-center font-bold text-sm check-diff-display text-emerald-600";
    } else {
        diffDisplay.innerText = String(diff);
        diffDisplay.className = "p-3 text-center font-bold text-sm check-diff-display text-rose-600";
    }
}

// Lưu phiếu kiểm kê
async function submitCreateInventoryCheck() {
    const code = document.getElementById('newCheckCode').value.trim();
    const note = document.getElementById('newCheckNote').value.trim();
    const status = document.getElementById('newCheckStatus').value;

    const rows = document.querySelectorAll('#checkItemsTableBody tr');
    const items = [];

    for (let r of rows) {
        const spId = r.querySelector('.check-product-id').value;
        const actualQty = parseInt(r.querySelector('.check-actual-qty').value) || 0;
        const reason = r.querySelector('.check-reason').value.trim();

        items.push({
            maSanPham: spId,
            soLuongThucTe: actualQty,
            lyDoChenhLech: reason
        });
    }

    const payload = {
        maPhieuKiemKe: code,
        maNhanVien: 'NV001',
        trangThai: status,
        ghiChu: note,
        items: items
    };

    const res = await fetchAPI('/inventory/checks', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (res) {
        alert('Lập phiếu kiểm kê thành công và đã cân bằng tồn kho!');
        closeCreateCheckModal();
        await loadWarehouseData();
    } else {
        // Fallback lưu local mock
        const newCheck = {
            maPhieuKiemKe: code,
            maNhanVien: 'NV001',
            ngayKiemKe: new Date().toISOString(),
            trangThai: status,
            ghiChu: note
        };
        warehouseChecks.unshift(newCheck);

        // Nếu cân bằng kho: cập nhật tồn kho sản phẩm
        if (status === 'Da_can_bang') {
            items.forEach(it => {
                const p = warehouseProducts.find(x => x.maSanPham === it.maSanPham);
                if (p) p.soLuongTon = it.soLuongThucTe;
            });
        }

        alert('Lập phiếu kiểm kê thành công (Đã cân bằng số lượng trên giao diện)!');
        closeCreateCheckModal();
        renderProductsTable();
        renderChecksTable();
        updateWarehouseStats();
    }
}

// Xem chi tiết phiếu kiểm kê
async function viewCheckDetail(maPhieuKiemKe) {
    const detailData = await fetchAPI(`/inventory/checks/${maPhieuKiemKe}`);

    let check, items;
    if (detailData && detailData.inventoryCheck) {
        check = detailData.inventoryCheck;
        items = detailData.items;
    } else {
        check = warehouseChecks.find(c => c.maPhieuKiemKe === maPhieuKiemKe);
        items = [
            {
                detail: { maSanPham: 'SP_IP15PM_256', soLuongHeThong: 5, soLuongThucTe: 5, chenhLech: 0, lyDoChenhLech: 'Khớp số lượng' },
                tenSanPham: 'iPhone 15 Pro Max 256GB Titan Tự Nhiên'
            },
            {
                detail: { maSanPham: 'SP_IP15_128', soLuongHeThong: 2, soLuongThucTe: 1, chenhLech: -1, lyDoChenhLech: '1 máy đem trưng bày trải nghiệm' },
                tenSanPham: 'iPhone 15 128GB Hồng Pastel'
            }
        ];
    }

    if (!check) return;

    document.getElementById('viewCheckTitle').innerText = 'Phiếu Kiểm Kê: ' + check.maPhieuKiemKe;
    document.getElementById('viewCheckSubtitle').innerText = 'Thời gian kiểm: ' + formatDateTime(check.ngayKiemKe);

    document.getElementById('viewCheckInfo').innerHTML = `
        <div>
            <span class="text-slate-400 block">Nhân viên kiểm:</span>
            <span class="font-bold text-slate-800">${check.maNhanVien || 'NV001'}</span>
        </div>
        <div>
            <span class="text-slate-400 block">Trạng thái:</span>
            <span class="font-bold text-emerald-600">${check.trangThai || 'Da_can_bang'}</span>
        </div>
        <div>
            <span class="text-slate-400 block">Ghi chú:</span>
            <span class="font-medium text-slate-700">${check.ghiChu || '-'}</span>
        </div>
    `;

    document.getElementById('viewCheckItemsList').innerHTML = `
        <table class="w-full text-left text-xs">
            <thead class="bg-slate-100 text-slate-600 uppercase font-semibold">
                <tr>
                    <th class="p-2.5">Sản Phẩm</th>
                    <th class="p-2.5 text-center">Tồn Hệ Thống</th>
                    <th class="p-2.5 text-center">Thực Tế</th>
                    <th class="p-2.5 text-center">Chênh Lệch</th>
                    <th class="p-2.5">Lý Do</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 bg-white">
                ${items.map(it => {
                    const diff = it.detail.chenhLech;
                    const diffColor = diff === 0 ? 'text-slate-400' : (diff > 0 ? 'text-emerald-600 font-bold' : 'text-rose-600 font-bold');
                    return `
                        <tr>
                            <td class="p-2.5 font-medium text-slate-800">${it.tenSanPham || it.detail.maSanPham}</td>
                            <td class="p-2.5 text-center font-bold text-slate-600">${it.detail.soLuongHeThong}</td>
                            <td class="p-2.5 text-center font-bold text-blue-700">${it.detail.soLuongThucTe}</td>
                            <td class="p-2.5 text-center ${diffColor}">${diff > 0 ? '+' + diff : diff}</td>
                            <td class="p-2.5 text-slate-500 italic">${it.detail.lyDoChenhLech || '-'}</td>
                        </tr>
                    `;
                }).join('')}
            </tbody>
        </table>
    `;

    document.getElementById('viewCheckModal').classList.remove('hidden');
}

function closeViewCheckModal() {
    document.getElementById('viewCheckModal').classList.add('hidden');
}
