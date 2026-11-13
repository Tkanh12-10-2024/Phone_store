// =========================================================================
// SCRIPT XỬ LÝ QUẢN LÝ IMEI / SERIAL (SUNO / POS365)
// Dữ liệu đồng bộ với CSDL: quanly_cuahang_dienthoai (bảng imei_san_pham)
// =========================================================================

const RAW_IMEI_LIST = [
    // 5 máy iPhone 15 Pro Max (PN20261001_01)
    {
        imei: '358941091234561',
        model: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        phieuNhap: 'PN20261001_01',
        hoaDon: 'HD20261012_001',
        customer: 'Trần Văn An',
        status: 'Dang_bao_hanh', // Đang trong phiếu bảo hành PBH20261016_01
        importDate: '01/10/2026',
        sellDate: '12/10/2026',
        warrantyExpiry: '12/10/2027'
    },
    {
        imei: '358941091234562',
        model: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        phieuNhap: 'PN20261001_01',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '01/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '358941091234563',
        model: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        phieuNhap: 'PN20261001_01',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '01/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '358941091234564',
        model: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        phieuNhap: 'PN20261001_01',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '01/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '358941091234565',
        model: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        phieuNhap: 'PN20261001_01',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '01/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },

    // 4 máy Samsung Galaxy S24 Ultra (PN20261005_02)
    {
        imei: '864219082345671',
        model: 'Samsung Galaxy S24 Ultra 256GB',
        brand: 'Samsung',
        color: 'Xám Titan',
        rom: '256GB',
        phieuNhap: 'PN20261005_02',
        hoaDon: 'HD20261014_002',
        customer: 'Nguyễn Thị Bích',
        status: 'Da_ban',
        importDate: '05/10/2026',
        sellDate: '14/10/2026',
        warrantyExpiry: '14/10/2027'
    },
    {
        imei: '864219082345672',
        model: 'Samsung Galaxy S24 Ultra 256GB',
        brand: 'Samsung',
        color: 'Xám Titan',
        rom: '256GB',
        phieuNhap: 'PN20261005_02',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '05/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '864219082345673',
        model: 'Samsung Galaxy S24 Ultra 256GB',
        brand: 'Samsung',
        color: 'Xám Titan',
        rom: '256GB',
        phieuNhap: 'PN20261005_02',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '05/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '864219082345674',
        model: 'Samsung Galaxy S24 Ultra 256GB',
        brand: 'Samsung',
        color: 'Xám Titan',
        rom: '256GB',
        phieuNhap: 'PN20261005_02',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '05/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },

    // 3 máy Xiaomi 14 (PN20261010_03)
    {
        imei: '861054073456781',
        model: 'Xiaomi 14 256GB',
        brand: 'Xiaomi',
        color: 'Trắng Tuyết',
        rom: '256GB',
        phieuNhap: 'PN20261010_03',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '10/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '861054073456782',
        model: 'Xiaomi 14 256GB',
        brand: 'Xiaomi',
        color: 'Trắng Tuyết',
        rom: '256GB',
        phieuNhap: 'PN20261010_03',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '10/10/2026',
        sellDate: null,
        warrantyExpiry: null
    },
    {
        imei: '861054073456783',
        model: 'Xiaomi 14 256GB',
        brand: 'Xiaomi',
        color: 'Trắng Tuyết',
        rom: '256GB',
        phieuNhap: 'PN20261010_03',
        hoaDon: null,
        customer: null,
        status: 'Trong_kho',
        importDate: '10/10/2026',
        sellDate: null,
        warrantyExpiry: null
    }
];

let currentFilter = 'all';
let currentSearch = '';

document.addEventListener('DOMContentLoaded', () => {
    reloadImeiData();
});

function reloadImeiData() {
    showLoading();
    setTimeout(() => {
        hideLoading();
        updateCounts();
        renderTable();
    }, 1100);
}

function showLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-amber-50 border-b border-amber-200 px-6 py-2.5 flex items-center justify-between text-xs text-amber-900 transition-all duration-300';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-amber-600 text-sm';
    bannerText.textContent = 'Đang đồng bộ danh sách IMEI từ MySQL `quanly_cuahang_dienthoai`...';

    document.getElementById('tableSkeleton').classList.remove('hidden');
    document.getElementById('tableContent').classList.add('hidden');
    document.getElementById('emptyState').classList.add('hidden');
}

function hideLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-emerald-50 border-b border-emerald-200 px-6 py-2.5 flex items-center justify-between text-xs text-emerald-800 transition-all duration-300';
    spinner.className = 'fa-solid fa-circle-check text-emerald-600 text-sm';
    bannerText.innerHTML = '<span class="font-semibold">Đồng bộ hoàn tất:</span> Đã nạp thành công 12 thiết bị IMEI từ CSDL.';

    document.getElementById('tableSkeleton').classList.add('hidden');
    document.getElementById('tableContent').classList.remove('hidden');
}

function updateCounts() {
    const total = RAW_IMEI_LIST.length;
    const inStock = RAW_IMEI_LIST.filter(i => i.status === 'Trong_kho').length;
    const sold = RAW_IMEI_LIST.filter(i => i.status === 'Da_ban').length;
    const warranty = RAW_IMEI_LIST.filter(i => i.status === 'Dang_bao_hanh').length;

    document.getElementById('countAll').textContent = total;
    document.getElementById('countInStock').textContent = inStock;
    document.getElementById('countSold').textContent = sold;
    document.getElementById('countWarranty').textContent = warranty;
}

function filterStatus(status) {
    currentFilter = status;
    
    // Đổi màu tab đang chọn
    const buttons = {
        'all': document.getElementById('btnFilterAll'),
        'Trong_kho': document.getElementById('btnFilterInStock'),
        'Da_ban': document.getElementById('btnFilterSold'),
        'Dang_bao_hanh': document.getElementById('btnFilterWarranty')
    };

    Object.keys(buttons).forEach(k => {
        if (k === status) {
            buttons[k].className = 'px-3 py-2 rounded-xl bg-slate-900 text-white font-bold shadow-sm transition';
        } else {
            buttons[k].className = 'px-3 py-2 rounded-xl bg-slate-100 text-slate-700 hover:bg-slate-200 font-semibold transition';
        }
    });

    renderTable();
}

function handleSearchImei(val) {
    currentSearch = val.trim().toLowerCase();
    renderTable();
}

function renderTable() {
    const tbody = document.getElementById('imeiTbody');
    tbody.innerHTML = '';

    const filtered = RAW_IMEI_LIST.filter(item => {
        const matchStatus = (currentFilter === 'all') || (item.status === currentFilter);
        const matchSearch = !currentSearch || 
            item.imei.toLowerCase().includes(currentSearch) ||
            item.model.toLowerCase().includes(currentSearch) ||
            item.color.toLowerCase().includes(currentSearch) ||
            (item.hoaDon && item.hoaDon.toLowerCase().includes(currentSearch)) ||
            (item.customer && item.customer.toLowerCase().includes(currentSearch));

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

        // Badge trạng thái
        let statusBadge = '';
        if (item.status === 'Trong_kho') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 border border-emerald-200"><i class="fa-solid fa-box mr-1"></i>Trong kho</span>';
        } else if (item.status === 'Da_ban') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 border border-blue-200"><i class="fa-solid fa-check mr-1"></i>Đã bán</span>';
        } else if (item.status === 'Dang_bao_hanh') {
            statusBadge = '<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 border border-amber-200"><i class="fa-solid fa-wrench mr-1"></i>Bảo hành</span>';
        }

        tr.innerHTML = `
            <td class="py-3.5 px-4 font-mono font-bold text-slate-900 tracking-wider">
                <span class="bg-slate-100 px-2 py-1 rounded border border-slate-200">${item.imei}</span>
            </td>
            <td class="py-3.5 px-4">
                <p class="font-bold text-slate-800">${item.model}</p>
                <p class="text-xs text-slate-400">Hãng: ${item.brand}</p>
            </td>
            <td class="py-3.5 px-3 text-center">
                <span class="font-medium text-slate-700">${item.color}</span>
                <span class="block text-xs text-slate-400 font-mono">${item.rom}</span>
            </td>
            <td class="py-3.5 px-3 text-center">${statusBadge}</td>
            <td class="py-3.5 px-3 text-center font-mono font-medium text-slate-600">${item.phieuNhap}</td>
            <td class="py-3.5 px-3 text-center">
                ${item.hoaDon ? `<span class="font-mono text-blue-600 font-bold">${item.hoaDon}</span><br><span class="text-xs text-slate-500">${item.customer}</span>` : '<span class="text-slate-400">—</span>'}
            </td>
            <td class="py-3.5 px-3 text-center">
                ${item.warrantyExpiry ? `<span class="text-rose-600 font-semibold">${item.warrantyExpiry}</span>` : '<span class="text-slate-400">Chưa kích hoạt</span>'}
            </td>
            <td class="py-3.5 px-4 text-center">
                <button onclick="inspectImei('${item.imei}')" class="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition" title="Tra cứu máy">
                    <i class="fa-solid fa-magnifying-glass"></i> Chi tiết
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

// Mở modal tra cứu bảo hành với dữ liệu của 1 IMEI
function inspectImei(imei) {
    document.getElementById('warrantySearchInput').value = imei;
    doCheckWarranty();
    openWarrantyModal();
}

function openWarrantyModal() {
    document.getElementById('warrantyModal').classList.remove('hidden');
}
function closeWarrantyModal() {
    document.getElementById('warrantyModal').classList.add('hidden');
}

function openAddImeiModal() {
    document.getElementById('addImeiModal').classList.remove('hidden');
}
function closeAddImeiModal() {
    document.getElementById('addImeiModal').classList.add('hidden');
}

function doCheckWarranty() {
    const inputVal = document.getElementById('warrantySearchInput').value.trim();
    const item = RAW_IMEI_LIST.find(i => i.imei === inputVal);

    if (item) {
        document.getElementById('resModel').textContent = item.model;
        document.getElementById('resImei').textContent = item.imei;
        document.getElementById('resColor').textContent = item.color;
        document.getElementById('resCustomer').textContent = item.customer || 'Chưa bán (Thuộc kho)';
        document.getElementById('resInvoice').textContent = item.hoaDon || 'Chưa xuất bán';
        document.getElementById('resBuyDate').textContent = item.sellDate || 'Chưa bán';
        document.getElementById('resExpireDate').textContent = item.warrantyExpiry || '12 tháng kể từ ngày bán';

        if (item.status === 'Dang_bao_hanh') {
            document.getElementById('resStatusBadge').className = 'px-2 py-0.5 rounded-full font-bold bg-amber-100 text-amber-800';
            document.getElementById('resStatusBadge').textContent = 'Đang tiếp nhận bảo hành';
        } else if (item.status === 'Da_ban') {
            document.getElementById('resStatusBadge').className = 'px-2 py-0.5 rounded-full font-bold bg-emerald-100 text-emerald-800';
            document.getElementById('resStatusBadge').textContent = 'Còn hạn bảo hành';
        } else {
            document.getElementById('resStatusBadge').className = 'px-2 py-0.5 rounded-full font-bold bg-slate-100 text-slate-700';
            document.getElementById('resStatusBadge').textContent = 'Hàng mới trong kho';
        }
    } else {
        alert('Không tìm thấy thông tin của mã IMEI này trên CSDL!');
    }
}
