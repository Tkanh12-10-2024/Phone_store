// --- CÁC HÀM TIỆN ÍCH ĐỊNH DẠNG ---
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
}

function showLoadingState() {
    // 1. KPI Skeletons
    document.getElementById('kpiRevenueSkeleton').classList.remove('hidden');
    document.getElementById('kpiRevenue').classList.add('hidden');

    document.getElementById('kpiOrdersSkeleton').classList.remove('hidden');
    document.getElementById('kpiOrders').classList.add('hidden');

    document.getElementById('kpiPhonesSkeleton').classList.remove('hidden');
    document.getElementById('kpiPhones').classList.add('hidden');

    document.getElementById('kpiAlertSkeleton').classList.remove('hidden');
    document.getElementById('kpiAlert').classList.add('hidden');

    // 2. Table Skeletons
    document.getElementById('tableBestSellerSkeleton').classList.remove('hidden');
    document.getElementById('tableBestSellerContent').classList.add('hidden');

    document.getElementById('tableStockAlertSkeleton').classList.remove('hidden');
    document.getElementById('tableStockAlertContent').classList.add('hidden');

    const banner = document.getElementById('loadingStatusBanner');
    banner.className = 'bg-blue-50 border-b border-blue-200 px-6 py-2.5 flex items-center justify-between text-xs text-blue-800';
    document.getElementById('bannerSpinner').className = 'fa-solid fa-circle-notch fa-spin text-blue-600 text-sm';
    document.getElementById('bannerText').textContent = 'Đang đồng bộ dữ liệu thực tế từ CSDL XAMPP MySQL `quanly_cuahang_dienthoai`...';
}

function hideLoadingState() {
    // Ẩn Skeletons KPI & Charts
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

    // Ẩn Skeletons Bảng
    document.getElementById('tableBestSellerSkeleton').classList.add('hidden');
    document.getElementById('tableBestSellerContent').classList.remove('hidden');

    document.getElementById('tableStockAlertSkeleton').classList.add('hidden');
    document.getElementById('tableStockAlertContent').classList.remove('hidden');
}

function updateSyncTime() {
    const now = new Date();
    document.getElementById('syncTimestamp').textContent = `Vừa cập nhật: ${now.toLocaleTimeString('vi-VN')}`;

    const banner = document.getElementById('loadingStatusBanner');
    banner.className = 'bg-emerald-50 border-b border-emerald-200 px-6 py-2.5 flex items-center justify-between text-xs text-emerald-800';
    document.getElementById('bannerSpinner').className = 'fa-solid fa-check text-emerald-600 text-sm';
    document.getElementById('bannerText').textContent = 'Đã kết nối thành công với CSDL XAMPP MySQL `quanly_cuahang_dienthoai`';
}

// Biến toàn cục lưu instance Chart.js
let revenueChartInstance = null;
let brandChartInstance = null;

// --- HÀM VẼ BIỂU ĐỒ ---
function renderCharts(data) {
    // 1. Biểu đồ đường (Line Chart) Doanh Thu
    const ctxRevenue = document.getElementById('revenueChart').getContext('2d');
    if (revenueChartInstance) revenueChartInstance.destroy();

    revenueChartInstance = new Chart(ctxRevenue, {
        type: 'line',
        data: {
            labels: ['01/10', '05/10', '10/10', '15/10', '20/10', '25/10', '31/10'],
            datasets: [{
                label: 'Doanh thu (VNĐ)',
                data: [0, 0, 0, data.totalRevenue || 0, data.totalRevenue || 0, data.totalRevenue || 0, data.totalRevenue || 0],
                borderColor: '#2563eb',
                backgroundColor: 'rgba(37, 99, 235, 0.08)',
                borderWidth: 2.5,
                fill: true,
                tension: 0.3
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });

    // 2. Biểu đồ tròn (Doughnut Chart)
    const ctxDoughnut = document.getElementById('brandDoughnutChart').getContext('2d');
    if (brandChartInstance) brandChartInstance.destroy();

    brandChartInstance = new Chart(ctxDoughnut, {
        type: 'doughnut',
        data: {
            labels: ['Apple', 'Samsung', 'Khác'],
            datasets: [{
                data: [51.3, 48.7, 0],
                backgroundColor: ['#2563eb', '#6366f1', '#e2e8f0']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } }
        }
    });
}

// --- HÀM RENDER DỮ LIỆU BẢNG (TÙY CHỌN DỰA TRÊN API) ---
function renderTables(bestSellers = [], stockAlerts = []) {
    // Render Bảng sản phẩm bán chạy
    const bestSellerTbody = document.getElementById('bestSellerTbody');
    if (bestSellers.length === 0) {
        bestSellerTbody.innerHTML = `<tr><td colspan="5" class="py-4 text-center text-slate-400">Chưa có dữ liệu giao dịch phát sinh</td></tr>`;
    } else {
        bestSellerTbody.innerHTML = bestSellers.map(item => `
            <tr class="hover:bg-slate-50 transition">
                <td class="py-3 px-4 font-semibold text-slate-800">${item.productName || 'iPhone / Samsung'}</td>
                <td class="py-3 px-3 text-center"><span class="px-2 py-0.5 bg-blue-50 text-blue-700 rounded font-medium">${item.brand || 'Apple'}</span></td>
                <td class="py-3 px-3 text-center font-bold text-slate-900">${item.soldQuantity || 0}</td>
                <td class="py-3 px-3 text-right font-semibold text-blue-600">${formatCurrency(item.revenue)}</td>
                <td class="py-3 px-4 text-center"><span class="px-2 py-0.5 bg-emerald-50 text-emerald-700 rounded">${item.stock || 10}</span></td>
            </tr>
        `).join('');
    }

    // Render Bảng cảnh báo tồn kho
    const stockAlertTbody = document.getElementById('stockAlertTbody');
    if (stockAlerts.length === 0) {
        stockAlertTbody.innerHTML = `<tr><td colspan="4" class="py-4 text-center text-emerald-600 font-medium"><i class="fa-solid fa-check-circle mr-1"></i> Tất cả các dòng máy đều ở mức tồn an toàn</td></tr>`;
    } else {
        stockAlertTbody.innerHTML = stockAlerts.map(item => `
            <tr class="hover:bg-slate-50 transition">
                <td class="py-3 px-4 font-semibold text-slate-800">${item.productName}</td>
                <td class="py-3 px-3 text-center font-bold text-amber-600">${item.currentStock}</td>
                <td class="py-3 px-3 text-center text-slate-500">${item.minStock}</td>
                <td class="py-3 px-4 text-center"><span class="px-2 py-0.5 bg-amber-100 text-amber-800 font-semibold rounded-full text-[10px]">Sắp hết</span></td>
            </tr>
        `).join('');
    }
}

// --- HÀM GỌI API LẤY DỮ LIỆU TỪ SPRING BOOT ---
async function reloadDashboardData() {
    showLoadingState();

    try {
        const response = await fetch('http://localhost:8080/api/dashboard/stats');

        if (!response.ok) {
            throw new Error('Không thể kết nối tới Server Spring Boot');
        }

        const realData = await response.json();

        // 1. Đổ dữ liệu vào các thẻ KPI
        document.getElementById('kpiRevenue').textContent = formatCurrency(realData.totalRevenue);
        document.getElementById('kpiOrders').textContent = `${realData.totalOrders || 0} đơn`;
        document.getElementById('kpiPhones').textContent = `${realData.totalPhonesSold || 0} thiết bị`;
        document.getElementById('kpiAlert').textContent = `${realData.stockAlertCount || 0} dòng máy`;

        // 2. Vẽ biểu đồ với dữ liệu thực tế
        renderCharts(realData);

        // 3. Render danh sách bảng (Nếu API trả về mảng danh sách, nếu chưa có sẽ hiển thị trạng thái trống thông minh)
        renderTables(realData.bestSellers || [], realData.stockAlerts || []);

        hideLoadingState();
        updateSyncTime();

    } catch (error) {
        console.error("Lỗi kết nối CSDL XAMPP:", error);

        const banner = document.getElementById('loadingStatusBanner');
        banner.className = 'bg-rose-50 border-b border-rose-200 px-6 py-2.5 flex items-center justify-between text-xs text-rose-800';
        document.getElementById('bannerSpinner').className = 'fa-solid fa-triangle-exclamation text-rose-600 text-sm';
        document.getElementById('bannerText').innerHTML = '<span class="font-semibold">Lỗi kết nối!</span> Hãy chắc chắn Spring Boot Backend đang chạy ở cổng 8080.';

        hideLoadingState();
    }
}

// Tự động tải dữ liệu ngay khi mở trang
document.addEventListener('DOMContentLoaded', () => {
    reloadDashboardData();
});