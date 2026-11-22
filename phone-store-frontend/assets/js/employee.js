const API_BASE_URL = 'http://localhost:8080/api/employees';
let employeesCache = [];

// Tải danh sách nhân viên khi trang vừa load xong
document.addEventListener('DOMContentLoaded', () => {
    loadEmployees();
});

function isEmployeeActive(emp) {
    return emp.active !== 0 && emp.active !== '0' && emp.active !== false && emp.active !== 'Da_nghi_viec';
}

function updateStats(employees) {
    const list = Array.isArray(employees) ? employees : [];
    const totalEl = document.getElementById('statTotalEmployees');
    const activeEl = document.getElementById('statActiveEmployees');
    const managerEl = document.getElementById('statManagers');

    if (totalEl) totalEl.textContent = `${list.length} người`;
    if (activeEl) {
        const activeCount = list.filter(emp => isEmployeeActive(emp)).length;
        activeEl.textContent = `${activeCount} người`;
    }
    if (managerEl) {
        const managerCount = list.filter(emp =>
            String(emp.role || '').toUpperCase().includes('QUAN_LY') ||
            String(emp.role || '').toLowerCase().includes('quản lý')
        ).length;
        managerEl.textContent = `${managerCount} người`;
    }
}

async function loadEmployees() {
    const banner = document.getElementById('statusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-blue-50 border-b border-blue-200 px-6 py-2 flex items-center justify-between text-xs text-blue-800';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-blue-600 text-sm';
    bannerText.textContent = 'Đang tải danh sách nhân viên từ CSDL XAMPP MySQL...';

    try {
        const response = await fetch(API_BASE_URL);
        if (!response.ok) throw new Error('Không thể kết nối đến Backend Server');

        const employees = await response.json();
        employeesCache = Array.isArray(employees) ? employees : [];
        updateStats(employeesCache);
        renderEmployeeTable(employeesCache);

        const now = new Date();
        document.getElementById('syncTimestamp').textContent = `Vừa cập nhật: ${now.toLocaleTimeString('vi-VN')}`;
        banner.className = 'bg-emerald-50 border-b border-emerald-200 px-6 py-2 flex items-center justify-between text-xs text-emerald-800';
        spinner.className = 'fa-solid fa-check text-emerald-600 text-sm';
        bannerText.textContent = `Đã kết nối thành công với CSDL XAMPP MySQL (Tổng số: ${employeesCache.length} tài khoản)`;

    } catch (error) {
        console.error("Lỗi:", error);
        banner.className = 'bg-rose-50 border-b border-rose-200 px-6 py-2 flex items-center justify-between text-xs text-rose-800';
        spinner.className = 'fa-solid fa-triangle-exclamation text-rose-600 text-sm';
        bannerText.innerHTML = '<span class="font-semibold">Lỗi kết nối!</span> Hãy chắc chắn Spring Boot Backend đang chạy ở cổng 8080.';
    }
}

function renderEmployeeTable(employees) {
    const tbody = document.getElementById('employeeTableBody');
    if (!employees || employees.length === 0) {
        tbody.innerHTML = `<tr><td colspan="3" class="py-6 text-center text-slate-400 font-medium">Chưa có bản ghi nhân viên nào trong CSDL</td></tr>`;
        return;
    }

    tbody.innerHTML = employees.map((emp, index) => `
        <tr class="hover:bg-slate-50 transition">
            <td class="py-3 px-5 text-center text-slate-400 font-mono">${index + 1}</td>
            <td class="py-3 px-5 font-semibold text-slate-800">${escapeHtml(emp.fullName || '--')}</td>
            <td class="py-3 px-5 text-right">
                <div class="inline-flex items-center gap-2">
                    <button onclick="viewEmployeeDetail('${emp.id}')" class="text-blue-600 hover:text-blue-800 font-semibold text-xs bg-blue-50 hover:bg-blue-100 px-2.5 py-1 rounded-md transition border border-blue-200">
                        <i class="fa-solid fa-eye mr-1"></i> Xem
                    </button>
                    <button onclick="openEditModal('${emp.id}')" class="text-amber-700 hover:text-amber-900 font-semibold text-xs bg-amber-50 hover:bg-amber-100 px-2.5 py-1 rounded-md transition border border-amber-200">
                        <i class="fa-solid fa-pen-to-square mr-1"></i> Sửa
                    </button>
                    <button onclick="deleteEmployee('${emp.id}')" class="text-rose-600 hover:text-rose-800 font-semibold text-xs bg-rose-50 hover:bg-rose-100 px-2.5 py-1 rounded-md transition border border-rose-200">
                        <i class="fa-solid fa-trash-can mr-1"></i> Xóa
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

// Xem chi tiết nhân viên khi bấm nút "Xem"
async function viewEmployeeDetail(id) {
    let emp = employeesCache.find(item => String(item.id) === String(id));

    try {
        const res = await fetch(`${API_BASE_URL}/${id}`);
        if (res.ok) {
            emp = await res.json();
        }
    } catch (e) {
        // Fallback sang dữ liệu cache nếu không gọi được endpoint chi tiết
    }

    if (!emp) {
        alert('Không tìm thấy thông tin nhân viên!');
        return;
    }

    const active = isEmployeeActive(emp);
    const roleText = emp.role === 'QUAN_LY' ? 'Quản lý (Manager)' : (emp.role || 'NHAN_VIEN');

    document.getElementById('detailId').textContent = emp.id || '--';
    document.getElementById('detailFullName').textContent = emp.fullName || '--';
    document.getElementById('detailUsername').textContent = emp.username || '--';

    const roleBadge = document.getElementById('detailRole');
    roleBadge.textContent = roleText;
    roleBadge.className = emp.role === 'QUAN_LY'
        ? 'px-2.5 py-0.5 rounded-md font-semibold bg-purple-50 text-purple-700 border border-purple-200'
        : 'px-2.5 py-0.5 rounded-md font-semibold bg-blue-50 text-blue-700 border border-blue-200';

    const statusBadge = document.getElementById('detailStatus');
    statusBadge.textContent = active ? 'Hoạt động' : 'Đã khóa';
    statusBadge.className = active
        ? 'px-2.5 py-0.5 rounded-md font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200'
        : 'px-2.5 py-0.5 rounded-md font-semibold bg-rose-50 text-rose-700 border border-rose-200';

    const editBtn = document.getElementById('detailEditBtn');
    editBtn.onclick = () => {
        closeDetailModal();
        openEditModal(emp.id);
    };

    document.getElementById('detailModal').classList.remove('hidden');
}

function closeDetailModal() {
    document.getElementById('detailModal').classList.add('hidden');
}

// Mở modal Thêm mới
function openCreateModal() {
    document.getElementById('employeeForm').reset();
    document.getElementById('editingEmployeeId').value = '';
    document.getElementById('editCodeWrapper').classList.add('hidden');
    document.getElementById('statusWrapper').classList.add('hidden');

    document.getElementById('modalTitle').innerHTML = '<i class="fa-solid fa-user-plus text-blue-600"></i> Thêm Tài Khoản Nhân Viên Mới';
    document.getElementById('password').required = true;
    document.getElementById('passwordLabel').textContent = 'Mật Khẩu';
    document.getElementById('passwordHint').classList.add('hidden');
    document.getElementById('submitEmployeeBtn').textContent = 'Lưu nhân viên';

    document.getElementById('employeeModal').classList.remove('hidden');
}

// Mở modal Sửa thông tin nhân viên
function openEditModal(id) {
    const emp = employeesCache.find(item => String(item.id) === String(id));
    if (!emp) {
        alert('Không tìm thấy thông tin nhân viên để sửa!');
        return;
    }

    document.getElementById('editingEmployeeId').value = emp.id;
    document.getElementById('employeeCodeDisplay').value = emp.id;
    document.getElementById('editCodeWrapper').classList.remove('hidden');

    document.getElementById('fullName').value = emp.fullName || '';
    document.getElementById('username').value = emp.username || '';
    document.getElementById('password').value = '';
    document.getElementById('password').required = false;
    document.getElementById('passwordLabel').textContent = 'Mật Khẩu Mới (Tùy chọn)';
    document.getElementById('passwordHint').classList.remove('hidden');

    document.getElementById('role').value = emp.role || 'NHAN_VIEN';
    document.getElementById('activeStatus').value = isEmployeeActive(emp) ? '1' : '0';
    document.getElementById('statusWrapper').classList.remove('hidden');

    document.getElementById('modalTitle').innerHTML = '<i class="fa-solid fa-user-pen text-amber-600"></i> Cập Nhật Thông Tin Nhân Viên';
    document.getElementById('submitEmployeeBtn').textContent = 'Cập nhật';

    document.getElementById('employeeModal').classList.remove('hidden');
}

// Xử lý Thêm mới hoặc Cập nhật nhân viên
async function handleSaveEmployee(event) {
    event.preventDefault();

    const editingId = document.getElementById('editingEmployeeId').value.trim();
    const isEdit = Boolean(editingId);
    const passwordValue = document.getElementById('password').value;

    const payload = {
        fullName: document.getElementById('fullName').value.trim(),
        username: document.getElementById('username').value.trim(),
        role: document.getElementById('role').value
    };

    if (isEdit) {
        payload.id = editingId;
        payload.active = Number(document.getElementById('activeStatus').value);
        if (passwordValue.trim() !== '') {
            payload.password = passwordValue;
        }
    } else {
        payload.password = passwordValue;
        payload.active = 1;
    }

    const url = isEdit ? `${API_BASE_URL}/${editingId}` : API_BASE_URL;
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            closeModal();
            document.getElementById('employeeForm').reset();
            loadEmployees();
        } else {
            alert(isEdit
                ? 'Cập nhật thông tin nhân viên thất bại! Vui lòng kiểm tra lại dữ liệu.'
                : 'Thêm nhân viên thất bại! Tên đăng nhập có thể đã bị trùng.');
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

function closeModal() {
    document.getElementById('employeeModal').classList.add('hidden');
}

// Chức năng tìm kiếm nhanh theo tên nhân viên trên bảng
function filterTable() {
    const input = document.getElementById('searchInput').value.toLowerCase();
    const rows = document.querySelectorAll('#employeeTableBody tr');

    rows.forEach(row => {
        const text = row.innerText.toLowerCase();
        row.style.display = text.includes(input) ? '' : 'none';
    });
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}