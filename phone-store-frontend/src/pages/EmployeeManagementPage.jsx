import React, { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';

export default function EmployeeManagementPage() {
    const [employees, setEmployees] = useState([]);
    const [isEditing, setIsEditing] = useState(false);
    const [currentId, setCurrentId] = useState(null);

    // Form state phục vụ cả Thêm mới (Create) và Sửa (Update)
    const [form, setForm] = useState({
        employeeCode: '',
        fullName: '',
        email: '',
        phone: '',
        position: 'Nhân viên',
        salary: '',
        username: '',
        password: '',
        status: 'ACTIVE'
    });

    // 1. Lấy danh sách nhân viên từ Backend
    const fetchEmployees = async () => {
        try {
            const response = await axiosClient.get('/employees');
            setEmployees(response.data);
        } catch (error) {
            console.error('Lỗi khi tải danh sách nhân viên:', error);
        }
    };

    useEffect(() => {
        fetchEmployees();
    }, []);

    // 2. Xử lý Submit Form (Thêm mới hoặc Cập nhật)
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (isEditing) {
                // Gọi API Cập nhật (PUT)
                await axiosClient.put(`/employees/${currentId}`, {
                    fullName: form.fullName,
                    email: form.email,
                    phone: form.phone,
                    position: form.position,
                    salary: form.salary ? Number(form.salary) : 0,
                    status: form.status
                });
                alert('Cập nhật nhân viên thành công!');
            } else {
                // Gọi API Thêm mới (POST)
                await axiosClient.post('/employees', {
                    ...form,
                    salary: form.salary ? Number(form.salary) : 0
                });
                alert('Thêm nhân viên thành công!');
            }

            // Reset form và tải lại danh sách
            resetForm();
            fetchEmployees();
        } catch (error) {
            console.error('Lỗi lưu dữ liệu:', error);
            alert('Có lỗi xảy ra, vui lòng kiểm tra lại!');
        }
    };

    // 3. Đưa dữ liệu lên form khi muốn Sửa
    const handleEditClick = (emp) => {
        setIsEditing(true);
        setCurrentId(emp.id);
        setForm({
            employeeCode: emp.employeeCode,
            fullName: emp.fullName,
            email: emp.email || '',
            phone: emp.phone || '',
            position: emp.position || '',
            salary: emp.salary || '',
            status: emp.status
        });
    };

    // 4. Xóa mềm nhân viên (DELETE)
    const handleDelete = async (id) => {
        if (window.confirm('Bạn có chắc chắn muốn vô hiệu hóa nhân viên này?')) {
            try {
                await axiosClient.delete(`/employees/${id}`);
                fetchEmployees();
            } catch (error) {
                console.error('Lỗi khi xóa nhân viên:', error);
            }
        }
    };

    const resetForm = () => {
        setIsEditing(false);
        setCurrentId(null);
        setForm({
            employeeCode: '',
            fullName: '',
            email: '',
            phone: '',
            position: 'Nhân viên',
            salary: '',
            username: '',
            password: '',
            status: 'ACTIVE'
        });
    };

    return (
        <div style={{ padding: '30px', fontFamily: 'Arial, sans-serif', maxWidth: '1200px', margin: '0 auto' }}>
            <h2>Hệ Thống Quản Lý Cửa Hàng Điện Thoại - Quản Lý Nhân Viên</h2>

            {/* Form Thêm/Sửa Nhân Viên */}
            <form onSubmit={handleSubmit} style={{ background: '#f9f9f9', padding: '20px', borderRadius: '8px', marginBottom: '30px', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '15px' }}>
                <h3>{isEditing ? 'Cập Nhật Thông Tin Nhân Viên' : 'Thêm Mới Nhân Viên'}</h3>
                <div style={{ gridColumn: 'span 2' }} />

                <div>
                    <label>Mã nhân viên:</label><br />
                    <input
                        type="text"
                        value={form.employeeCode}
                        onChange={(e) => setForm({ ...form, employeeCode: e.target.value })}
                        disabled={isEditing} // Không cho phép sửa mã nhân viên khi update
                        required
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label>Họ và tên:</label><br />
                    <input
                        type="text"
                        value={form.fullName}
                        onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                        required
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label>Email:</label><br />
                    <input
                        type="email"
                        value={form.email}
                        onChange={(e) => setForm({ ...form, email: e.target.value })}
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label>Số điện thoại:</label><br />
                    <input
                        type="text"
                        value={form.phone}
                        onChange={(e) => setForm({ ...form, phone: e.target.value })}
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label>Chức vụ:</label><br />
                    <input
                        type="text"
                        value={form.position}
                        onChange={(e) => setForm({ ...form, position: e.target.value })}
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                <div>
                    <label>Mức lương (VNĐ):</label><br />
                    <input
                        type="number"
                        value={form.salary}
                        onChange={(e) => setForm({ ...form, salary: e.target.value })}
                        style={{ width: '100%', padding: '8px' }}
                    />
                </div>

                {/* Chỉ hiện trường tạo tài khoản khi thêm mới */}
                {!isEditing && (
                    <>
                        <div>
                            <label>Tên đăng nhập (Username):</label><br />
                            <input
                                type="text"
                                value={form.username}
                                onChange={(e) => setForm({ ...form, username: e.target.value })}
                                style={{ width: '100%', padding: '8px' }}
                            />
                        </div>
                        <div>
                            <label>Mật khẩu (Password):</label><br />
                            <input
                                type="password"
                                value={form.password}
                                onChange={(e) => setForm({ ...form, password: e.target.value })}
                                style={{ width: '100%', padding: '8px' }}
                            />
                        </div>
                    </>
                )}

                {isEditing && (
                    <div>
                        <label>Trạng thái:</label><br />
                        <select
                            value={form.status}
                            onChange={(e) => setForm({ ...form, status: e.target.value })}
                            style={{ width: '100%', padding: '8px' }}
                        >
                            <option value="ACTIVE">ACTIVE (Hoạt động)</option>
                            <option value="INACTIVE">INACTIVE (Đã khóa)</option>
                        </select>
                    </div>
                )}

                <div style={{ gridColumn: 'span 2', display: 'flex', gap: '10px' }}>
                    <button type="submit" style={{ background: '#007bff', color: 'white', border: 'none', padding: '10px 20px', cursor: 'pointer', borderRadius: '4px' }}>
                        {isEditing ? 'Lưu Thay Đổi' : 'Thêm Nhân Viên'}
                    </button>
                    {isEditing && (
                        <button type="button" onClick={resetForm} style={{ background: '#6c757d', color: 'white', border: 'none', padding: '10px 20px', cursor: 'pointer', borderRadius: '4px' }}>
                            Hủy
                        </button>
                    )}
                </div>
            </form>

            {/* Bảng Hiển Thị Danh Sách Nhân Viên */}
            <h3>Danh Sách Nhân Viên Hiện Có</h3>
            <table border="1" cellPadding="10" cellSpacing="0" style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                <tr style={{ background: '#f2f2f2' }}>
                    <th>Mã NV</th>
                    <th>Họ Tên</th>
                    <th>Email</th>
                    <th>Số Điện Thoại</th>
                    <th>Chức Vụ</th>
                    <th>Lương</th>
                    <th>Trạng Thái</th>
                    <th>Thao Tác</th>
                </tr>
                </thead>
                <tbody>
                {employees.length > 0 ? (
                    employees.map((emp) => (
                        <tr key={emp.id}>
                            <td>{emp.employeeCode}</td>
                            <td>{emp.fullName}</td>
                            <td>{emp.email}</td>
                            <td>{emp.phone}</td>
                            <td>{emp.position}</td>
                            <td>{emp.salary ? emp.salary.toLocaleString() + ' đ' : '0 đ'}</td>
                            <td>
                  <span style={{ color: emp.status === 'ACTIVE' ? 'green' : 'red', fontWeight: 'bold' }}>
                    {emp.status}
                  </span>
                            </td>
                            <td>
                                <button onClick={() => handleEditClick(emp)} style={{ marginRight: '5px', padding: '5px 10px', background: '#ffc107', border: 'none', cursor: 'pointer', borderRadius: '3px' }}>
                                    Sửa
                                </button>
                                <button onClick={() => handleDelete(emp.id)} style={{ padding: '5px 10px', background: '#dc3545', color: 'white', border: 'none', cursor: 'pointer', borderRadius: '3px' }}>
                                    Khóa
                                </button>
                            </td>
                        </tr>
                    ))
                ) : (
                    <tr>
                        <td colSpan="8" style={{ textAlign: 'center' }}>Chưa có dữ liệu nhân viên nào.</td>
                    </tr>
                )}
                </tbody>
            </table>
        </div>
    );
}