// Hiển thị / Ẩn mật khẩu
function togglePassword() {
    const passwordInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        eyeIcon.className = 'fa-solid fa-eye-slash';
    } else {
        passwordInput.type = 'password';
        eyeIcon.className = 'fa-solid fa-eye';
    }
}

// Xử lý sự kiện Submit Form đăng nhập
async function handleLogin(event) {
    event.preventDefault(); // Chặn reload trang mặc định

    const usernameInput = document.getElementById('username').value.trim();
    const passwordInput = document.getElementById('password').value.trim();
    const errorAlert = document.getElementById('errorAlert');
    const errorMessage = document.getElementById('errorMessage');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = document.getElementById('btnText');

    // Hiệu ứng Loading khi đang gửi request
    submitBtn.disabled = true;
    btnText.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin mr-2"></i> Đang xác thực...';
    errorAlert.classList.add('hidden');

    try {
        // Gửi request POST lên API Spring Boot (Ví dụ: /api/auth/login)
        const response = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                username: usernameInput,
                password: passwordInput
            })
        });

        if (!response.ok) {
            throw new Error('Tài khoản hoặc mật khẩu không chính xác');
        }

        const data = await response.json();

        // Lưu thông tin token hoặc session vào localStorage nếu cần
        localStorage.setItem('userToken', data.token || 'authenticated_session');
        localStorage.setItem('userName', data.fullName || usernameInput);

        // Đăng nhập thành công -> Chuyển hướng sang trang Dashboard
        window.location.href = 'index.html'; // Hoặc đường dẫn tới trang chủ của bạn

    } catch (error) {
        console.error("Lỗi đăng nhập:", error);
        errorAlert.classList.remove('hidden');
        errorMessage.textContent = error.message || 'Không thể kết nối đến máy chủ xác thực!';

        // Khôi phục nút bấm
        submitBtn.disabled = false;
        btnText.textContent = 'Đăng Nhập Hệ Thống';
    }
}