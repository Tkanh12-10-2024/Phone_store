// Cấu hình tập trung đường dẫn Backend Spring Boot
const API_BASE_URL = 'http://localhost:8080/api';

// Hàm helper gọi API chung bằng Fetch
async function fetchAPI(endpoint, options = {}) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            ...options,
        });
        if (!response.ok) throw new Error(`HTTP Error: ${response.status}`);
        return await response.json();
    } catch (error) {
        console.error(`[API Error] ${endpoint}:`, error);
        return null;
    }
}