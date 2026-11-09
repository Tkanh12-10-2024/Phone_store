// =========================================================================
// SCRIPT XỬ LÝ DỮ LIỆU & HIỆU ỨNG LOADING CHO DASHBOARD (Sapo / KiotViet)
// Tương thích với dữ liệu CSDL: quanly_cuahang_dienthoai
// =========================================================================

let revenueChartInstance = null;
let brandChartInstance = null;

// Dữ liệu mô phỏng chính xác từ Database MySQL quanly_cuahang_dienthoai
const DB_MOCK_DATA = {
    kpis: {
        totalRevenue: 55480500, // 28.490.500 (HD1) + 26.990.000 (HD2)
        totalOrders: 2,
        totalPhonesSold: 2,
        stockAlertCount: 4
    },
    revenueTrends: {
        labels: ['01/10', '03/10', '06/10', '09/10', '12/10 (HD01)', '14/10 (HD02)', '16/10', '18/10 (Hôm nay)'],
        data: [0, 0, 0, 0, 28490500, 26990000, 0, 0]
    },
    brandShare: {
        labels: ['Apple (iPhone)', 'Samsung (Galaxy)'],
        data: [28490500, 26990000],
        colors: ['#3b82f6', '#6366f1']
    },
    bestSellers: [
        {
            name: 'iPhone 15 Pro Max 256GB Titan Tự Nhiên',
            brand: 'Apple',
            soldCount: 1,
            revenue: '29.990.000 đ',
            stock: 4,
            badgeClass: 'bg-blue-100 text-blue-700'
        },
        {
            name: 'Samsung Galaxy S24 Ultra 256GB Xám Titan',
            brand: 'Samsung',
            soldCount: 1,
            revenue: '27.990.000 đ',
            stock: 3,
            badgeClass: 'bg-indigo-100 text-indigo-700'
        }
    ],
    stockAlerts: [
        {
            name: 'iPhone 15 Pro Max 512GB Titan Đen',
            stock: 0,
            minStock: 2,
            status: 'Hết hàng',
            statusClass: 'bg-rose-100 text-rose-700 border-rose-200'
        },
        {
            name: 'OPPO Reno11 5G 256GB Xanh Sóng Biển',
            stock: 0,
            minStock: 4,
            status: 'Hết hàng',
            statusClass: 'bg-rose-100 text-rose-700 border-rose-200'
        },
        {
            name: 'Samsung Galaxy Z Flip5 256GB Xanh Mint',
            stock: 0,
            minStock: 2,
            status: 'Hết hàng',
            statusClass: 'bg-rose-100 text-rose-700 border-rose-200'
        },
        {
            name: 'Samsung Galaxy S24 Ultra 256GB Xám Titan',
            stock: 3,
            minStock: 3,
            status: 'Sắp hết hàng',
            statusClass: 'bg-amber-100 text-amber-700 border-amber-200'
        }
    ]
};

// Hàm định dạng tiền tệ VNĐ
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

// Khởi chạy khi tài liệu tải xong
document.addEventListener('DOMContentLoaded', () => {
    reloadDashboardData();
});

// Hàm mô phỏng quá trình kết nối API / Database và hiển thị dữ liệu
function reloadDashboardData() {
    // 1. Chuyển sang trạng thái Đang tải (Loading Skeleton)
    showLoadingState();

    // 2. Mô phỏng độ trễ truy vấn CSDL (1.2 giây)
    setTimeout(() => {
        // 3. Ẩn Skeleton và Render dữ liệu thật
        hideLoadingState();
        renderKPIs();
        renderCharts();
        renderTables();
        updateSyncTime();
    }, 1200);
}

function showLoadingState() {
    // Banner trạng thái
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-blue-50 border-b border-blue-200 px-6 py-2.5 flex items-center justify-between text-xs text-blue-800 transition-all duration-300';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-blue-600 text-sm';
    bannerText.textContent = 'Đang đồng bộ dữ liệu từ CSDL MySQL `quanly_cuahang_dienthoai` (XAMPP)...';

    // Bật Skeletons, ẩn Content
    document.getElementById('kpiRevenueSkeleton').classList.remove('hidden');
    document.getElementById('kpiRevenue').classList.add('hidden');

    document.getElementById('kpiOrdersSkeleton').classList.remove('hidden');
    document.getElementById('kpiOrders').classList.add('hidden');

    document.getElementById('kpiPhonesSkeleton').classList.remove('hidden');
    document.getElementById('kpiPhones').classList.add('hidden');

    document.getElementById('kpiAlertSkeleton').classList.remove('hidden');
    document.getElementById('kpiAlert').classList.add('hidden');

    document.getElementById('chartSkeleton').classList.remove('hidden');
    document.getElementById('chartContainer').classList.add('hidden');

    document.getElementById('doughnutSkeleton').classList.remove('hidden');
    document.getElementById('doughnutContainer').classList.add('hidden');

    document.getElementById('tableBestSellerSkeleton').classList.remove('hidden');
    document.getElementById('tableBestSellerContent').classList.add('hidden');

    document.getElementById('tableStockAlertSkeleton').classList.remove('hidden');
    document.getElementById('tableStockAlertContent').classList.add('hidden');
}

function hideLoadingState() {
    // Banner thành công
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-emerald-50 border-b border-emerald-200 px-6 py-2.5 flex items-center justify-between text-xs text-emerald-800 transition-all duration-300';
    spinner.className = 'fa-solid fa-circle-check text-emerald-600 text-sm';
    bannerText.innerHTML = '<span class="font-semibold">Đồng bộ thành công!</span> Dữ liệu thời gian thực từ CSDL <span class="font-mono font-bold">quanly_cuahang_dienthoai</span> đã sẵn sàng.';

    // Ẩn Skeletons, hiện Content
    document.getElementById('kpiRevenueSkeleton').classList.add('hidden');
    document.getElementById('kpiRevenue').classList.remove('hidden');

    document.getElementById('kpiOrdersSkeleton').classList.add('hidden');
    document.getElementById('kpiOrders').classList.remove('hidden');

    document.getElementById('kpiPhonesSkeleton').classList.add('hidden');
    document.getElementById('kpiPhones').classList.remove('hidden');

    document.getElementById('kpiAlertSkeleton').classList.add('hidden');
    document.getElementById('kpiAlert').classList.remove('hidden');

    document.getElementById('chartSkeleton').classList.add('hidden');
    document.getElementById('chartContainer').classList.remove('hidden');

    document.getElementById('doughnutSkeleton').classList.add('hidden');
    document.getElementById('doughnutContainer').classList.remove('hidden');

    document.getElementById('tableBestSellerSkeleton').classList.add('hidden');
    document.getElementById('tableBestSellerContent').classList.remove('hidden');

    document.getElementById('tableStockAlertSkeleton').classList.add('hidden');
    document.getElementById('tableStockAlertContent').classList.remove('hidden');
}

function renderKPIs() {
    document.getElementById('kpiRevenue').textContent = formatCurrency(DB_MOCK_DATA.kpis.totalRevenue);
    document.getElementById('kpiOrders').textContent = `${DB_MOCK_DATA.kpis.totalOrders} hóa đơn`;
    document.getElementById('kpiPhones').textContent = `${DB_MOCK_DATA.kpis.totalPhonesSold} máy`;
    document.getElementById('kpiAlert').textContent = `${DB_MOCK_DATA.kpis.stockAlertCount} sản phẩm`;
}

function renderCharts() {
    // 1. Biểu đồ đường doanh thu
    const ctxRevenue = document.getElementById('revenueChart').getContext('2d');
    if (revenueChartInstance) {
        revenueChartInstance.destroy();
    }

    revenueChartInstance = new Chart(ctxRevenue, {
        type: 'line',
        data: {
            labels: DB_MOCK_DATA.revenueTrends.labels,
            datasets: [{
                label: 'Doanh thu thực tế (VNĐ)',
                data: DB_MOCK_DATA.revenueTrends.data,
                borderColor: '#2563eb',
                backgroundColor: 'rgba(37, 99, 235, 0.1)',
                borderWidth: 2.5,
                fill: true,
                tension: 0.35,
                pointBackgroundColor: '#2563eb',
                pointRadius: 5,
                pointHoverRadius: 7
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            return ' Doanh thu: ' + formatCurrency(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return (value / 1000000) + ' Tr';
                        }
                    },
                    grid: { color: '#f1f5f9' }
                },
                x: {
                    grid: { display: false }
                }
            }
        }
    });

    // 2. Biểu đồ Donut Hãng
    const ctxBrand = document.getElementById('brandDoughnutChart').getContext('2d');
    if (brandChartInstance) {
        brandChartInstance.destroy();
    }

    brandChartInstance = new Chart(ctxBrand, {
        type: 'doughnut',
        data: {
            labels: DB_MOCK_DATA.brandShare.labels,
            datasets: [{
                data: DB_MOCK_DATA.brandShare.data,
                backgroundColor: DB_MOCK_DATA.brandShare.colors,
                hoverOffset: 4,
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            cutout: '70%'
        }
    });
}

function renderTables() {
    // Bảng Top sản phẩm bán chạy
    const bestSellerTbody = document.getElementById('bestSellerTbody');
    bestSellerTbody.innerHTML = '';
    DB_MOCK_DATA.bestSellers.forEach(item => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-slate-50 transition';
        tr.innerHTML = `
            <td class="py-3 px-4 font-semibold text-slate-800">${item.name}</td>
            <td class="py-3 px-3 text-center">
                <span class="px-2 py-0.5 rounded-full text-xs font-semibold ${item.badgeClass}">${item.brand}</span>
            </td>
            <td class="py-3 px-3 text-center font-bold text-slate-900">${item.soldCount}</td>
            <td class="py-3 px-3 text-right font-semibold text-emerald-600">${item.revenue}</td>
            <td class="py-3 px-4 text-center">
                <span class="px-2 py-0.5 bg-slate-100 text-slate-700 rounded font-mono font-bold">${item.stock}</span>
            </td>
        `;
        bestSellerTbody.appendChild(tr);
    });

    // Bảng Cảnh báo tồn kho
    const stockAlertTbody = document.getElementById('stockAlertTbody');
    stockAlertTbody.innerHTML = '';
    DB_MOCK_DATA.stockAlerts.forEach(item => {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-amber-50/30 transition';
        tr.innerHTML = `
            <td class="py-3 px-4 font-semibold text-slate-800">${item.name}</td>
            <td class="py-3 px-3 text-center font-bold text-slate-900">${item.stock}</td>
            <td class="py-3 px-3 text-center text-slate-500">${item.minStock}</td>
            <td class="py-3 px-4 text-center">
                <span class="px-2 py-0.5 rounded-full text-xs font-semibold border ${item.statusClass}">${item.status}</span>
            </td>
        `;
        stockAlertTbody.appendChild(tr);
    });
}

function updateSyncTime() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('vi-VN') + ' ' + now.toLocaleDateString('vi-VN');
    document.getElementById('syncTimestamp').textContent = `Vừa cập nhật: ${timeStr}`;
}
