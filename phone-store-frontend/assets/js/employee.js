const API_BASE_URL = 'http://localhost:8080/api/employees';

// Tải danh sách nhân viên khi trang vừa load xong
document.addEventListener('DOMContentLoaded', () => {
    loadEmployees();
});

async function loadEmployees() {
    const banner = document.getElementById('statusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-blue-50 border-b border-blue-200 px-8 py-2.5 flex items-center justify-between text-xs text-blue-800';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-blue-600 text-sm';
    bannerText.textContent = 'Đang tải danh sách nhân viên từ CSDL XAMPP MySQL...';

    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error('Không thể kết nối đến Backend Server');

        const employees = await response.json();
        renderEmployeeTable(employees);

        const now = new Date();
        document.getElementById('syncTimestamp').textContent = `Vừa cập nhật: ${now.toLocaleTimeString('vi-VN')}`;
        banner.className = 'bg-emerald-50 border-b border-emerald-200 px-8 py-2.5 flex items-center justify-between text-xs text-emerald-800';
        spinner.className = 'fa-solid fa-check text-emerald-600 text-sm';
        bannerText.textContent = `Đã kết nối thành công với CSDL XAMPP MySQL (Tổng số: ${employees.length} tài khoản)`;

    } catch (error) {
        console.error("Lỗi:", error);
        banner.className = 'bg-rose-50 border-b border-rose-200 px-8 py-2.5 flex items-center justify-between text-xs text-rose-800';
        spinner.className = 'fa-solid fa-triangle-exclamation text-rose-600 text-sm';
        bannerText.innerHTML = '<span class="font-semibold">Lỗi kết nối!</span> Hãy chắc chắn Spring Boot Backend đang chạy ở cổng 8080.';
    }
}

function renderEmployeeTable(employees) {
    const tbody = document.getElementById('employeeTableBody');
    if (!employees || employees.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="py-6 text-center text-slate-400 font-medium">Chưa có bản ghi nhân viên nào trong CSDL</td></tr>`;
        return;
    }

    tbody.innerHTML = employees.map(emp => `
        <tr class="hover:bg-slate-50 transition">
            <td class="py-3 px-6 font-semibold text-slate-800">${emp.fullName || '--'}</td>
            <td class="py-3 px-4 text-slate-600 font-mono">${emp.username || '--'}</td>
            <td class="py-3 px-4 text-center">
                <span class="px-2.5 py-0.5 ${emp.role === 'QUAN_LY' ? 'bg-purple-50 text-purple-700 border border-purple-200' : 'bg-blue-50 text-blue-700 border border-blue-200'} rounded-md font-semibold text-[11px]">
                    ${emp.role || 'NHAN_VIEN'}
                </span>
            </td>
            <td class="py-3 px-4 text-center">
                <span class="px-2.5 py-0.5 ${emp.active !== 0 ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-rose-50 text-rose-700 border border-rose-200'} rounded-md font-semibold text-[11px]">
                    ${emp.active !== 0 ? 'Hoạt động' : 'Đã khóa'}
                </span>
            </td>
            <td class="py-3 px-6 text-center">
                <button onclick="deleteEmployee('${emp.id}')" class="text-rose-600 hover:text-rose-800 font-semibold text-xs bg-rose-50 hover:bg-rose-100 px-2.5 py-1 rounded-md transition border border-rose-200">
                    <i class="fa-solid fa-trash-can mr-1"></i> Xóa
                </button>
            </td>
        </tr>
    `).join('');
}

// Xử lý Thêm mới nhân viên
async function handleCreateEmployee(event) {
    event.preventDefault();

    const newEmployee = {
        fullName: document.getElementById('fullName').value,
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        role: document.getElementById('role').value,
        active: 1
    };

    try {
        const response = await fetch(API_BASE_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newEmployee)
        });

        if (response.ok) {
            closeModal();
            document.getElementById('employeeForm').reset();
            loadEmployees();
        } else {
            alert('Thêm nhân viên thất bại! Tên đăng nhập có thể đã bị trùng.');
        }
    } catch (error) {
        console.error("Lỗi:", error);
        alert('Không thể kết nối đến máy chủ.');
    }
}

// Xử lý Xóa nhân viên
async function deleteEmployee(id) {
    if (!confirm(`Bạn có chắc chắn muốn xóa tài khoản nhân viên [${id}] này khỏi CSDL không?`)) return;

    try {
        const response = await fetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            loadEmployees();
        } else {
            alert('Xóa nhân viên thất bại!');
        }
    } catch (error) {
        console.error("Lỗi:", error);
    }
}

// Mở/Đóng Modal
function openModal() {
    document.getElementById('employeeModal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('employeeModal').classList.add('hidden');
}

// Chức năng tìm kiếm nhanh trên bảng
function filterTable() {
    const input = document.getElementById('searchInput').value.toLowerCase();
    const rows = document.querySelectorAll('#employeeTableBody tr');

    rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(input) ? '' : 'none';
    });
}