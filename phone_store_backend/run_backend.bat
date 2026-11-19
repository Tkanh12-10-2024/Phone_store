@echo off
chcp 65001 >nul
title PhoneStore Pro - Backend Spring Boot
cd /d "%~dp0"

echo ========================================================
echo       PHONESTORE PRO - BACKEND SPRING BOOT
echo ========================================================
echo.
echo [1] Chay Demo voi CSDL H2 In-Memory (Khuyen dung - Khong can MySQL)
echo [2] Chay voi CSDL MySQL / XAMPP (Port 3306)
echo.
set /p opt="Chon che do chay (mac dinh nhan Enter la [1]): "

if "%opt%"=="2" (
    echo.
    echo --- Dang khoi dong voi MySQL tren port 3306... ---
    java -jar target\phong_store_backend-0.0.1-SNAPSHOT.jar
) else (
    echo.
    echo --- Dang khoi dong voi H2 In-Memory (Day du du lieu demo)... ---
    java "-Dspring.profiles.active=h2" -jar target\phong_store_backend-0.0.1-SNAPSHOT.jar
)

pause
