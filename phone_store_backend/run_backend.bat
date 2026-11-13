@echo off
chcp 65001 >nul
title PhoneStore Pro - Backend Spring Boot
echo ========================================================
echo    DANG KHOI DONG BACKEND SPRING BOOT (PORT 8080)
echo ========================================================
cd /d "%~dp0"
java -jar target\phong_store_backend-0.0.1-SNAPSHOT.jar
pause
