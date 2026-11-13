// ========================================================================
// IMPORT & SUPPLIER MANAGEMENT (UC 2.5.1 & UC 2.5.2) - NHÓM 05
// ========================================================================

// Dữ liệu dự phòng mẫu chuẩn khi chưa chạy Backend Spring Boot
const MOCK_SUPPLIERS = [
    { maNhaCungCap: 'NCC01', tenNhaCungCap: 'Công ty Cổ phần Bán lẻ Kỹ thuật số FPT (Synnex FPT)', soDienThoai: '02473006666', email: 'contact@synnexfpt.com.vn', diaChi: 'Tòa nhà FPT, Phố Duy Tân, Cầu Giấy, Hà Nội', maSoThue: '0101778161', congNo: 25000000, trangThai: 'Dang_hop_tac' },
    { maNhaCungCap: 'NCC02', tenNhaCungCap: 'Công ty Cổ phần Thế Giới Số (Digiworld Corporation)', soDienThoai: '02839290059', email: 'info@dgw.com.vn', diaChi: '195 Nguyễn Thái Bình, P. Nguyễn Thái Bình, Q.1, TP.HCM', maSoThue: '0302861742', congNo: 0, trangThai: 'Dang_hop_tac' },
    { maNhaCungCap: 'NCC03', tenNhaCungCap: 'Công ty TNHH Phân phối Synnex FPT miền Nam', soDienThoai: '02873008888', email: 'contact.hcm@synnexfpt.com', diaChi: 'Khu Công Nghệ Cao, TP. Thủ Đức, TP.HCM', maSoThue: '0301479821', congNo: 12000000, trangThai: 'Dang_hop_tac' },
    { maNhaCungCap: 'NCC04', tenNhaCungCap: 'Tổng công ty Cổ phần Bưu chính Viettel (Viettel Commerce)', soDienThoai: '02462777888', email: 'kinhdoanh@viettel.vn', diaChi: 'Tòa nhà Viettel, Số 1 Giang Văn Minh, Ba Đình, Hà Nội', maSoThue: '0104093672', congNo: 0, trangThai: 'Dang_hop_tac' }
];

const MOCK_PRODUCTS = [
    { maSanPham: 'SP_IP15PM_256', tenSanPham: 'iPhone 15 Pro Max 256GB Titan Tự Nhiên', giaNhapThamKhao: 26500000, giaBan: 29990000, soLuongTon: 5 },
    { maSanPham: 'SP_IP15PM_512', tenSanPham: 'iPhone 15 Pro Max 512GB Titan Xanh', giaNhapThamKhao: 31500000, giaBan: 35490000, soLuongTon: 2 },
    { maSanPham: 'SP_IP15_128', tenSanPham: 'iPhone 15 128GB Hồng Pastel', giaNhapThamKhao: 17200000, giaBan: 19490000, soLuongTon: 1 },
    { maSanPham: 'SP_SS_S24U_256', tenSanPham: 'Samsung Galaxy S24 Ultra 256GB Xám Titan', giaNhapThamKhao: 24000000, giaBan: 27990000, soLuongTon: 4 },
    { maSanPham: 'SP_SS_S24_256', tenSanPham: 'Samsung Galaxy S24 256GB Vàng Amber', giaNhapThamKhao: 15500000, giaBan: 17990000, soLuongTon: 0 },
    { maSanPham: 'SP_OPPO_R11_256', tenSanPham: 'OPPO Reno11 5G 256GB Xanh Sóng Biển', giaNhapThamKhao: 8200000, giaBan: 9990000, soLuongTon: 6 },
    { maSanPham: 'SP_XM_14U_512', tenSanPham: 'Xiaomi 14 Ultra 512GB Đen Nhiếp Ảnh', giaNhapThamKhao: 26000000, giaBan: 29990000, soLuongTon: 1 }
];

const MOCK_ORDERS = [
    {
        maPhieuNhap: 'PN20261110_01',
        maNhaCungCap: 'NCC01',
        maNhanVien: 'NV001',
        ngayNhap: '2026-11-10T09:30:00',
        tongTien: 132500000,
        daThanhToan: 132500000,
        congNoConLai: 0,
        trangThai: 'Hoan_thanh',
        ghiChu: 'Nhập lô iPhone 15 Pro Max đợt 1 tháng 11'
    },
    {
        maPhieuNhap: 'PN20261112_02',
        maNhaCungCap: 'NCC02',
        maNhanVien: 'NV001',
        ngayNhap: '2026-11-12T14:15:00',
        tongTien: 96000000,
        daThanhToan: 71000000,
        congNoConLai: 25000000,
        trangThai: 'Hoan_thanh',
        ghiChu: 'Nhập bổ sung Samsung S24 Ultra từ Digiworld'
    },
    {
        maPhieuNhap: 'PN20261113_03',
        maNhaCungCap: 'NCC04',
        maNhanVien: 'NV001',
        ngayNhap: '2026-11-13T08:45:00',
        tongTien: 49200000,
        daThanhToan: 49200000,
        congNoConLai: 0,
        trangThai: 'Hoan_thanh',
        ghiChu: 'Nhập OPPO Reno11 và Xiaomi 14 Ultra'
    }
];

let globalSuppliers = [];
let globalProducts = [];
let globalOrders = [];
let currentEditingSupplierId = null;

// Khởi chạy khi load trang
document.addEventListener('DOMContentLoaded', async () => {
    await loadInitialData();
});

// Chuyển tab giữa Phiếu nhập và Nhà cung cấp
function switchTab(tab) {
    const tabOrders = document.getElementById('tabContentOrders');
    const tabSuppliers = document.getElementById('tabContentSuppliers');
    const btnOrders = document.getElementById('tabBtnOrders');
    const btnSuppliers = document.getElementById('tabBtnSuppliers');

    if (tab === 'orders') {
        tabOrders.classList.remove('hidden');
        tabSuppliers.classList.add('hidden');
        btnOrders.className = "py-4 text-sm font-bold border-b-2 border-blue-600 text-blue-600 flex items-center gap-2";
        btnSuppliers.className = "py-4 text-sm font-semibold border-b-2 border-transparent text-slate-500 hover:text-slate-800 flex items-center gap-2";
    } else {
        tabOrders.classList.add('hidden');
        tabSuppliers.classList.remove('hidden');
        btnSuppliers.className = "py-4 text-sm font-bold border-b-2 border-blue-600 text-blue-600 flex items-center gap-2";
        btnOrders.className = "py-4 text-sm font-semibold border-b-2 border-transparent text-slate-500 hover:text-slate-800 flex items-center gap-2";
    }
}

// Nạp dữ liệu ban đầu
async function loadInitialData() {
    // 1. Tải nhà cung cấp
    const supData = await fetchAPI('/suppliers');
    globalSuppliers = (supData && Array.isArray(supData) && supData.length > 0) ? supData : [...MOCK_SUPPLIERS];

    // 2. Tải sản phẩm
    const prodData = await fetchAPI('/inventory/products');
    globalProducts = (prodData && Array.isArray(prodData) && prodData.length > 0) ? prodData : [...MOCK_PRODUCTS];

    // 3. Tải phiếu nhập
    const orderData = await fetchAPI('/purchase-orders');
    globalOrders = (orderData && Array.isArray(orderData) && orderData.length > 0) ? orderData : [...MOCK_ORDERS];

    renderOrdersTable(globalOrders);
    renderSuppliersTable(globalSuppliers);
    updateStats();
}

// Cập nhật thống kê
function updateStats() {
    document.getElementById('statTotalOrders').innerText = globalOrders.length;
    
    const totalVal = globalOrders.reduce((sum, o) => sum + (o.tongTien || 0), 0);
    document.getElementById('statTotalValue').innerText = (totalVal / 1000000).toFixed(1) + ' Tr';

    const totalDebt = globalSuppliers.reduce((sum, s) => sum + (s.congNo || 0), 0);
    document.getElementById('statTotalDebt').innerText = (totalDebt / 1000000).toFixed(1) + ' Tr';

    document.getElementById('statTotalSuppliers').innerText = globalSuppliers.filter(s => s.trangThai === 'Dang_hop_tac').length;
}

// Format tiền tệ VNĐ
function formatCurrency(num) {
    if (num == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(num);
}

// Format ngày giờ
function formatDateTime(dtStr) {
    if (!dtStr) return '-';
    const d = new Date(dtStr);
    return d.toLocaleString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// Render bảng phiếu nhập
function renderOrdersTable(orders) {
    const tbody = document.getElementById('purchaseOrdersTableBody');
    if (!tbody) return;

    if (orders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="p-6 text-center text-slate-400">Chưa có phiếu nhập nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = orders.map(o => {
        const sup = globalSuppliers.find(s => s.maNhaCungCap === o.maNhaCungCap);
        const supName = sup ? sup.tenNhaCungCap : o.maNhaCungCap;
        const debtClass = (o.congNoConLai > 0) ? 'text-amber-600 font-bold' : 'text-slate-400';

        return `
            <tr class="hover:bg-slate-50 transition border-b border-slate-100">
                <td class="p-3 font-mono font-semibold text-blue-600">${o.maPhieuNhap}</td>
                <td class="p-3 font-medium text-slate-800 max-w-[220px] truncate" title="${supName}">${supName}</td>
                <td class="p-3 text-slate-600">${o.maNhanVien || 'NV001'}</td>
                <td class="p-3 text-slate-500">${formatDateTime(o.ngayNhap)}</td>
                <td class="p-3 text-right font-semibold text-slate-800">${formatCurrency(o.tongTien)}</td>
                <td class="p-3 text-right text-emerald-600 font-medium">${formatCurrency(o.daThanhToan)}</td>
                <td class="p-3 text-right ${debtClass}">${formatCurrency(o.congNoConLai)}</td>
                <td class="p-3 text-center">
                    <span class="px-2.5 py-1 text-xs rounded-full font-semibold bg-emerald-100 text-emerald-700">
                        ${o.trangThai || 'Hoàn thành'}
                    </span>
                </td>
                <td class="p-3 text-center">
                    <button onclick="viewOrderDetail('${o.maPhieuNhap}')" class="px-2.5 py-1 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded text-xs font-semibold inline-flex items-center gap-1">
                        <i class="fa-solid fa-eye"></i> Xem Chi Tiết
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// Render bảng nhà cung cấp
function renderSuppliersTable(suppliers) {
    const tbody = document.getElementById('suppliersTableBody');
    if (!tbody) return;

    if (suppliers.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="p-6 text-center text-slate-400">Không tìm thấy nhà cung cấp nào.</td></tr>`;
        return;
    }

    tbody.innerHTML = suppliers.map(s => {
        const isAction = s.trangThai === 'Dang_hop_tac';
        const statusBadge = isAction
            ? `<span class="px-2.5 py-0.5 text-xs rounded-full font-semibold bg-emerald-100 text-emerald-700">Đang hợp tác</span>`
            : `<span class="px-2.5 py-0.5 text-xs rounded-full font-semibold bg-slate-100 text-slate-500">Ngừng hợp tác</span>`;

        return `
            <tr class="hover:bg-slate-50 transition border-b border-slate-100">
                <td class="p-3 font-mono font-semibold text-slate-700">${s.maNhaCungCap}</td>
                <td class="p-3 font-semibold text-slate-800">${s.tenNhaCungCap}</td>
                <td class="p-3 text-slate-600 font-mono">${s.soDienThoai}</td>
                <td class="p-3 text-slate-500">${s.email || '-'}</td>
                <td class="p-3 text-slate-600 max-w-[200px] truncate" title="${s.diaChi}">${s.diaChi}</td>
                <td class="p-3 text-right font-semibold text-amber-600">${formatCurrency(s.congNo)}</td>
                <td class="p-3 text-center">${statusBadge}</td>
                <td class="p-3 text-center space-x-1">
                    <button onclick="openEditSupplierModal('${s.maNhaCungCap}')" class="p-1.5 text-blue-600 hover:bg-blue-50 rounded" title="Chỉnh sửa">
                        <i class="fa-solid fa-pen-to-square"></i>
                    </button>
                    <button onclick="deleteSupplier('${s.maNhaCungCap}')" class="p-1.5 text-rose-500 hover:bg-rose-50 rounded" title="Ngừng hợp tác">
                        <i class="fa-solid fa-ban"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

// Lọc phiếu nhập
function filterOrders() {
    const q = document.getElementById('searchOrderInput').value.toLowerCase().trim();
    if (!q) {
        renderOrdersTable(globalOrders);
        return;
    }
    const filtered = globalOrders.filter(o => {
        const sup = globalSuppliers.find(s => s.maNhaCungCap === o.maNhaCungCap);
        const supName = sup ? sup.tenNhaCungCap.toLowerCase() : '';
        return o.maPhieuNhap.toLowerCase().includes(q) || supName.includes(q);
    });
    renderOrdersTable(filtered);
}

// Lọc nhà cung cấp
function filterSuppliers() {
    const q = document.getElementById('searchSupplierInput').value.toLowerCase().trim();
    if (!q) {
        renderSuppliersTable(globalSuppliers);
        return;
    }
    const filtered = globalSuppliers.filter(s =>
        s.tenNhaCungCap.toLowerCase().includes(q) ||
        s.maNhaCungCap.toLowerCase().includes(q) ||
        s.soDienThoai.includes(q)
    );
    renderSuppliersTable(filtered);
}

// ========================================================================
// MODAL LẬP PHIẾU NHẬP MỚI VÀ QUÉT MÃ IMEI (UC 2.5.2.1 & 2.5.2.1.1)
// ========================================================================
function openCreateOrderModal() {
    const select = document.getElementById('newOrderSupplier');
    select.innerHTML = globalSuppliers
        .filter(s => s.trangThai === 'Dang_hop_tac')
        .map(s => `<option value="${s.maNhaCungCap}">${s.maNhaCungCap} - ${s.tenNhaCungCap}</option>`)
        .join('');

    const now = new Date();
    const codeSuffix = now.getFullYear().toString() +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') + '_' +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0') +
        String(now.getSeconds()).padStart(2, '0');
    
    document.getElementById('newOrderCode').value = 'PN' + codeSuffix;
    document.getElementById('newOrderPaid').value = 0;
    document.getElementById('newOrderNote').value = '';

    const container = document.getElementById('orderItemsContainer');
    container.innerHTML = '';
    addOrderItemRow(); // Thêm sẵn 1 dòng mặt hàng đầu tiên

    calcOrderTotals();
    document.getElementById('createOrderModal').classList.remove('hidden');
}

function closeCreateOrderModal() {
    document.getElementById('createOrderModal').classList.add('hidden');
}

// Thêm một dòng mặt hàng nhập
let itemRowCounter = 0;
function addOrderItemRow() {
    itemRowCounter++;
    const rowId = 'itemRow_' + itemRowCounter;
    const container = document.getElementById('orderItemsContainer');

    const productOptions = globalProducts.map(p =>
        `<option value="${p.maSanPham}" data-price="${p.giaNhapThamKhao}">${p.maSanPham} - ${p.tenSanPham}</option>`
    ).join('');

    const defaultPrice = globalProducts.length > 0 ? globalProducts[0].giaNhapThamKhao : 0;

    const div = document.createElement('div');
    div.id = rowId;
    div.className = "p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-3 relative";

    div.innerHTML = `
        <button type="button" onclick="removeOrderItemRow('${rowId}')" class="absolute top-3 right-3 text-rose-500 hover:text-rose-700 text-sm font-semibold">
            <i class="fa-solid fa-trash-can"></i> Xóa mặt hàng
        </button>
        <div class="grid grid-cols-1 md:grid-cols-3 gap-3 pr-8">
            <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Sản Phẩm (*)</label>
                <select class="item-product w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white" onchange="onProductSelectChange('${rowId}')">
                    ${productOptions}
                </select>
            </div>
            <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Số Lượng Nhập (*)</label>
                <input type="number" min="1" value="2" class="item-qty w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white" oninput="onQtyChange('${rowId}')">
            </div>
            <div>
                <label class="block text-xs font-semibold text-slate-700 mb-1">Đơn Giá Nhập (₫) (*)</label>
                <input type="number" min="0" value="${defaultPrice}" class="item-price w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white" oninput="calcOrderTotals()">
            </div>
        </div>

        <!-- VÙNG QUÉT / NHẬP MÃ IMEI (UC 2.5.2.1.1) -->
        <div class="bg-white p-3 rounded-lg border border-slate-200">
            <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                    <i class="fa-solid fa-barcode text-blue-600"></i>
                    Quét / Nhập danh sách mã IMEI (mỗi mã 1 dòng hoặc cách nhau bởi dấu phẩy)
                </span>
                <button type="button" onclick="generateSampleImeis('${rowId}')" class="text-xs bg-indigo-50 text-indigo-600 hover:bg-indigo-100 px-2.5 py-1 rounded font-medium">
                    <i class="fa-solid fa-wand-magic-sparkles"></i> Tạo IMEI mẫu hợp lệ
                </button>
            </div>
            <textarea class="item-imeis w-full px-3 py-2 border border-slate-200 rounded text-xs font-mono" rows="2" placeholder="Ví dụ: 354928110293841, 354928110293842"></textarea>
            <p class="text-[11px] text-slate-400 mt-1">Lưu ý: Số lượng mã IMEI phải khớp với số lượng nhập của sản phẩm.</p>
        </div>
    `;

    container.appendChild(div);
    generateSampleImeis(rowId);
    calcOrderTotals();
}

function removeOrderItemRow(rowId) {
    const el = document.getElementById(rowId);
    if (el) el.remove();
    calcOrderTotals();
}

function onProductSelectChange(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;
    const select = row.querySelector('.item-product');
    const selectedOpt = select.options[select.selectedIndex];
    const price = selectedOpt.getAttribute('data-price') || 0;
    row.querySelector('.item-price').value = price;
    generateSampleImeis(rowId);
    calcOrderTotals();
}

function onQtyChange(rowId) {
    generateSampleImeis(rowId);
    calcOrderTotals();
}

// Sinh mã IMEI mẫu hợp lệ (15 chữ số bắt đầu bằng 35, 86...)
function generateSampleImeis(rowId) {
    const row = document.getElementById(rowId);
    if (!row) return;
    const qty = parseInt(row.querySelector('.item-qty').value) || 1;
    const imeis = [];
    const prefix = '35' + Math.floor(100000 + Math.random() * 900000);
    for (let i = 0; i < qty; i++) {
        const suffix = String(Math.floor(1000000 + Math.random() * 9000000));
        imeis.push(prefix + suffix);
    }
    row.querySelector('.item-imeis').value = imeis.join('\n');
}

// Tính tổng tiền & công nợ
function calcOrderTotals() {
    let total = 0;
    const rows = document.querySelectorAll('#orderItemsContainer > div');
    rows.forEach(r => {
        const qty = parseInt(r.querySelector('.item-qty').value) || 0;
        const price = parseFloat(r.querySelector('.item-price').value) || 0;
        total += qty * price;
    });

    document.getElementById('newOrderTotalDisplay').value = formatCurrency(total);
    const paid = parseFloat(document.getElementById('newOrderPaid').value) || 0;
    const debt = Math.max(0, total - paid);
    document.getElementById('newOrderDebtDisplay').value = formatCurrency(debt);
}

// Submit tạo phiếu nhập
async function submitCreatePurchaseOrder() {
    const code = document.getElementById('newOrderCode').value.trim();
    const supId = document.getElementById('newOrderSupplier').value;
    const note = document.getElementById('newOrderNote').value.trim();
    const paid = parseFloat(document.getElementById('newOrderPaid').value) || 0;

    const rows = document.querySelectorAll('#orderItemsContainer > div');
    if (rows.length === 0) {
        alert('Vui lòng thêm ít nhất một mặt hàng nhập!');
        return;
    }

    const items = [];
    for (let r of rows) {
        const spId = r.querySelector('.item-product').value;
        const qty = parseInt(r.querySelector('.item-qty').value) || 0;
        const price = parseFloat(r.querySelector('.item-price').value) || 0;
        const imeiRaw = r.querySelector('.item-imeis').value;

        const imeiList = imeiRaw.split(/[\n,]/)
            .map(s => s.trim())
            .filter(s => s.length > 0);

        if (imeiList.length !== qty) {
            alert(`Sản phẩm [${spId}]: Số lượng IMEI (${imeiList.length}) chưa khớp với số lượng nhập (${qty})!`);
            return;
        }

        items.push({
            maSanPham: spId,
            soLuong: qty,
            donGiaNhap: price,
            imeis: imeiList
        });
    }

    const payload = {
        maPhieuNhap: code,
        maNhaCungCap: supId,
        maNhanVien: 'NV001',
        ghiChu: note,
        daThanhToan: paid,
        items: items
    };

    // Gọi API Backend
    const res = await fetchAPI('/purchase-orders', {
        method: 'POST',
        body: JSON.stringify(payload)
    });

    if (res) {
        alert('Lập phiếu nhập kho thành công và đã cập nhật tồn kho!');
        closeCreateOrderModal();
        await loadInitialData();
    } else {
        // Fallback lưu local mock nếu backend chưa chạy
        const tongTien = items.reduce((sum, it) => sum + it.soLuong * it.donGiaNhap, 0);
        const newOrder = {
            maPhieuNhap: code,
            maNhaCungCap: supId,
            maNhanVien: 'NV001',
            ngayNhap: new Date().toISOString(),
            tongTien: tongTien,
            daThanhToan: paid,
            congNoConLai: Math.max(0, tongTien - paid),
            trangThai: 'Hoan_thanh',
            ghiChu: note
        };
        globalOrders.unshift(newOrder);

        // Cập nhật công nợ NCC
        const s = globalSuppliers.find(x => x.maNhaCungCap === supId);
        if (s) s.congNo = (s.congNo || 0) + (tongTien - paid);

        // Tăng tồn kho
        items.forEach(it => {
            const p = globalProducts.find(x => x.maSanPham === it.maSanPham);
            if (p) p.soLuongTon = (p.soLuongTon || 0) + it.soLuong;
        });

        alert('Lập phiếu nhập thành công (Dữ liệu đã cập nhật trên giao diện)!');
        closeCreateOrderModal();
        renderOrdersTable(globalOrders);
        renderSuppliersTable(globalSuppliers);
        updateStats();
    }
}

// ========================================================================
// XEM CHI TIẾT PHIẾU NHẬP (UC 2.5.2.2)
// ========================================================================
async function viewOrderDetail(maPhieuNhap) {
    const detailData = await fetchAPI(`/purchase-orders/${maPhieuNhap}`);

    let po, sup, items;
    if (detailData && detailData.purchaseOrder) {
        po = detailData.purchaseOrder;
        sup = detailData.supplier;
        items = detailData.items;
    } else {
        // Mock fallback
        po = globalOrders.find(o => o.maPhieuNhap === maPhieuNhap);
        sup = globalSuppliers.find(s => s.maNhaCungCap === po?.maNhaCungCap);
        items = [
            {
                detail: { maSanPham: 'SP_IP15PM_256', soLuong: 5, donGiaNhap: 26500000, thanhTien: 132500000 },
                tenSanPham: 'iPhone 15 Pro Max 256GB Titan Tự Nhiên',
                imeis: ['354928110293841', '354928110293842', '354928110293843', '354928110293844', '354928110293845']
            }
        ];
    }

    if (!po) return;

    document.getElementById('viewOrderTitle').innerText = 'Chi Tiết Phiếu Nhập: ' + po.maPhieuNhap;
    document.getElementById('viewOrderSubtitle').innerText = 'Ngày nhập: ' + formatDateTime(po.ngayNhap);

    const supName = sup ? sup.tenNhaCungCap : po.maNhaCungCap;
    document.getElementById('viewOrderInfo').innerHTML = `
        <div>
            <span class="text-slate-400 block">Nhà cung cấp:</span>
            <span class="font-bold text-slate-800">${supName}</span>
        </div>
        <div>
            <span class="text-slate-400 block">Người lập phiếu:</span>
            <span class="font-semibold text-slate-800">${po.maNhanVien || 'NV001'}</span>
        </div>
        <div>
            <span class="text-slate-400 block">Tổng thanh toán:</span>
            <span class="font-bold text-blue-600">${formatCurrency(po.tongTien)}</span>
        </div>
        <div>
            <span class="text-slate-400 block">Công nợ còn lại:</span>
            <span class="font-bold text-amber-600">${formatCurrency(po.congNoConLai)}</span>
        </div>
    `;

    document.getElementById('viewOrderItemsList').innerHTML = items.map(it => {
        const imeisHtml = (it.imeis && it.imeis.length > 0)
            ? it.imeis.map(im => `<span class="px-2 py-0.5 bg-slate-100 text-slate-700 rounded font-mono text-[11px] border border-slate-200">${im}</span>`).join(' ')
            : `<span class="text-slate-400 text-xs">Chưa có mã IMEI</span>`;

        return `
            <div class="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <div class="flex items-center justify-between text-sm">
                    <span class="font-bold text-slate-800">${it.tenSanPham || it.detail.maSanPham}</span>
                    <span class="font-semibold text-blue-600">${formatCurrency(it.detail.thanhTien)}</span>
                </div>
                <div class="text-xs text-slate-500 flex gap-4">
                    <span>Số lượng: <strong class="text-slate-700">${it.detail.soLuong}</strong></span>
                    <span>Đơn giá nhập: <strong class="text-slate-700">${formatCurrency(it.detail.donGiaNhap)}</strong></span>
                </div>
                <div>
                    <span class="text-[11px] font-semibold text-slate-500 block mb-1">Danh sách IMEI sản phẩm:</span>
                    <div class="flex flex-wrap gap-1.5">${imeisHtml}</div>
                </div>
            </div>
        `;
    }).join('');

    document.getElementById('viewOrderModal').classList.remove('hidden');
}

function closeViewOrderModal() {
    document.getElementById('viewOrderModal').classList.add('hidden');
}

// ========================================================================
// QUẢN LÝ NHÀ CUNG CẤP (UC 2.5.1)
// ========================================================================
function openCreateSupplierModal() {
    currentEditingSupplierId = null;
    document.getElementById('supplierModalTitle').innerText = 'Thêm Nhà Cung Cấp Mới';
    document.getElementById('supCode').value = 'NCC0' + (globalSuppliers.length + 1);
    document.getElementById('supCode').readOnly = false;
    document.getElementById('supName').value = '';
    document.getElementById('supPhone').value = '';
    document.getElementById('supTax').value = '';
    document.getElementById('supEmail').value = '';
    document.getElementById('supAddress').value = '';

    document.getElementById('supplierModal').classList.remove('hidden');
}

function openEditSupplierModal(id) {
    const s = globalSuppliers.find(x => x.maNhaCungCap === id);
    if (!s) return;
    currentEditingSupplierId = id;
    document.getElementById('supplierModalTitle').innerText = 'Cập Nhật Nhà Cung Cấp';
    document.getElementById('supCode').value = s.maNhaCungCap;
    document.getElementById('supCode').readOnly = true;
    document.getElementById('supName').value = s.tenNhaCungCap;
    document.getElementById('supPhone').value = s.soDienThoai;
    document.getElementById('supTax').value = s.maSoThue || '';
    document.getElementById('supEmail').value = s.email || '';
    document.getElementById('supAddress').value = s.diaChi;

    document.getElementById('supplierModal').classList.remove('hidden');
}

function closeSupplierModal() {
    document.getElementById('supplierModal').classList.add('hidden');
}

async function submitSupplier() {
    const code = document.getElementById('supCode').value.trim();
    const name = document.getElementById('supName').value.trim();
    const phone = document.getElementById('supPhone').value.trim();
    const tax = document.getElementById('supTax').value.trim();
    const email = document.getElementById('supEmail').value.trim();
    const address = document.getElementById('supAddress').value.trim();

    if (!code || !name || !phone) {
        alert('Vui lòng nhập đầy đủ Mã NCC, Tên NCC và Số điện thoại!');
        return;
    }

    const payload = {
        maNhaCungCap: code,
        tenNhaCungCap: name,
        soDienThoai: phone,
        maSoThue: tax,
        email: email,
        diaChi: address,
        trangThai: 'Dang_hop_tac'
    };

    if (currentEditingSupplierId) {
        // Cập nhật
        const res = await fetchAPI(`/suppliers/${currentEditingSupplierId}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        if (res) {
            alert('Cập nhật nhà cung cấp thành công!');
        } else {
            const idx = globalSuppliers.findIndex(x => x.maNhaCungCap === currentEditingSupplierId);
            if (idx !== -1) globalSuppliers[idx] = { ...globalSuppliers[idx], ...payload };
        }
    } else {
        // Thêm mới
        const res = await fetchAPI('/suppliers', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        if (res) {
            alert('Thêm nhà cung cấp thành công!');
        } else {
            globalSuppliers.push({ ...payload, congNo: 0 });
        }
    }

    closeSupplierModal();
    renderSuppliersTable(globalSuppliers);
    updateStats();
}

async function deleteSupplier(id) {
    if (!confirm(`Bạn có chắc chắn muốn ngừng hợp tác với nhà cung cấp [${id}]?`)) return;

    await fetchAPI(`/suppliers/${id}`, { method: 'DELETE' });

    const s = globalSuppliers.find(x => x.maNhaCungCap === id);
    if (s) s.trangThai = 'Ngung_hop_tac';

    renderSuppliersTable(globalSuppliers);
    updateStats();
}
