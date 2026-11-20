@echo off
chcp 65001 >nul
title PhoneStore Pro - Khoi dong he thong
cd /d "%~dp0"

echo ========================================================
echo       HE THONG QUAN LY CUA HANG DIEN THOAI (PHONESTORE)
echo ========================================================
echo.
echo 1. Dang khoi dong Backend Spring Boot (Port 8080)...
start "PhoneStore Backend" cmd /c "cd /d "%~dp0phone_store_backend" && java "-Dspring.profiles.active=h2" -jar target\phong_store_backend-0.0.1-SNAPSHOT.jar"

echo 2. Cho Backend san sang trong 6 giay...
timeout /t 6 /nobreak >nul

echo 3. Dang mo giao dien Frontend tren trinh duyet...
start "" "%~dp0phone-store-frontend\login.html"

echo.
echo ========================================================
echo   HE THONG DA DUOC KHOI DONG THANH CONG!
echo   - Tai khoan dang nhap: admin
echo   - Mat khau:           123456
echo ========================================================
echo.
pause
