// =========================================================================
// SCRIPT XỬ LÝ BÁN HÀNG NHANH POS (POS365 / KiotViet / TPOS)
// Đồng bộ với CSDL: quanly_cuahang_dienthoai (bảng san_pham, imei, khuyen_mai)
// =========================================================================

// Dữ liệu sản phẩm từ Database MySQL
const PRODUCTS_DATA = [
    {
        id: 'SP_IP15PM_256TN',
        name: 'iPhone 15 Pro Max 256GB',
        brand: 'Apple',
        color: 'Titan Tự Nhiên',
        rom: '256GB',
        price: 29990000,
        stock: 4,
        availableImeis: ['358941091234562', '358941091234563', '358941091234564', '358941091234565'],
        icon: 'fa-apple',
        imageBg: 'from-slate-700 to-slate-900',
        badge: 'Bán chạy'
    },
    {
        id: 'SP_SS_S24U_256AM',
        name: 'Samsung Galaxy S24 Ultra',
        brand: 'Samsung',
        color: 'Xám Titan',
        rom: '256GB',
        price: 27990000,
        stock: 3,
        availableImeis: ['864219082345672', '864219082345673', '864219082345674'],
        icon: 'fa-android',
        imageBg: 'from-blue-700 to-indigo-900',
        badge: 'AI Phone'
    },
    {
        id: 'SP_MI_14_256TRANG',
        name: 'Xiaomi 14 256GB',
        brand: 'Xiaomi',
        color: 'Trắng Tuyết',
        rom: '256GB',
        price: 20990000,
        stock: 3,
        availableImeis: ['861054073456781', '861054073456782', '861054073456783'],
        icon: 'fa-mobile',
        imageBg: 'from-orange-600 to-red-800',
        badge: 'Leica Cam'
    },
    {
        id: 'SP_IP15PM_512DEN',
        name: 'iPhone 15 Pro Max 512GB',
        brand: 'Apple',
        color: 'Titan Đen',
        rom: '512GB',
        price: 34490000,
        stock: 0,
        availableImeis: [],
        icon: 'fa-apple',
        imageBg: 'from-slate-800 to-black',
        badge: 'Tạm hết'
    },
    {
        id: 'SP_SS_ZFLIP5_256',
        name: 'Samsung Galaxy Z Flip5',
        brand: 'Samsung',
        color: 'Xanh Mint',
        rom: '256GB',
        price: 18490000,
        stock: 0,
        availableImeis: [],
        icon: 'fa-mobile-screen',
        imageBg: 'from-teal-600 to-emerald-800',
        badge: 'Tạm hết'
    },
    {
        id: 'SP_OPPO_RENO11_256',
        name: 'OPPO Reno11 5G 256GB',
        brand: 'OPPO',
        color: 'Xanh Sóng Biển',
        rom: '256GB',
        price: 10490000,
        stock: 0,
        availableImeis: [],
        icon: 'fa-mobile',
        imageBg: 'from-cyan-600 to-blue-800',
        badge: 'Tạm hết'
    }
];

// Giỏ hàng hiện tại
let cart = [];
let currentBrandFilter = 'all';
let currentSearchQuery = '';
let activeVoucher = null;
let paymentMethod = 'cash'; // 'cash' hoặc 'qr'

// Khởi tạo
document.addEventListener('DOMContentLoaded', () => {
    reloadPosData();
    setInterval(updateClock, 1000);
});

function updateClock() {
    const now = new Date();
    document.getElementById('posClock').textContent = now.toLocaleDateString('vi-VN') + ' ' + now.toLocaleTimeString('vi-VN');
}

function reloadPosData() {
    showLoading();
    setTimeout(() => {
        hideLoading();
        renderProductGrid();
        updateCartUI();
    }, 1000);
}

function showLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-emerald-50 border-b border-emerald-200 px-4 py-1.5 flex items-center justify-between text-xs text-emerald-900 flex-shrink-0';
    spinner.className = 'fa-solid fa-circle-notch fa-spin text-emerald-600 text-xs';
    bannerText.textContent = 'Đang đồng bộ danh mục máy và kiểm tra tồn kho từ CSDL MySQL...';

    document.getElementById('productGridSkeleton').classList.remove('hidden');
    document.getElementById('productGrid').classList.add('hidden');
}

function hideLoading() {
    const banner = document.getElementById('loadingStatusBanner');
    const spinner = document.getElementById('bannerSpinner');
    const bannerText = document.getElementById('bannerText');

    banner.className = 'bg-emerald-100/70 border-b border-emerald-300 px-4 py-1.5 flex items-center justify-between text-xs text-emerald-950 flex-shrink-0';
    spinner.className = 'fa-solid fa-circle-check text-emerald-700 text-xs';
    bannerText.innerHTML = '<span class="font-bold">CSDL Sẵn sàng:</span> Đã nạp 6 dòng sản phẩm, tồn kho theo thời gian thực.';

    document.getElementById('productGridSkeleton').classList.add('hidden');
    document.getElementById('productGrid').classList.remove('hidden');
}

function filterBrand(brand) {
    currentBrandFilter = brand;

    const tabs = {
        'all': document.getElementById('tabBrandAll'),
        'Apple': document.getElementById('tabBrandApple'),
        'Samsung': document.getElementById('tabBrandSamsung'),
        'Xiaomi': document.getElementById('tabBrandXiaomi'),
        'OPPO': document.getElementById('tabBrandOppo')
    };

    Object.keys(tabs).forEach(k => {
        if (k === brand) {
            tabs[k].className = 'px-3 py-1.5 rounded-lg bg-emerald-600 text-white flex-shrink-0 shadow-sm font-bold';
        } else {
            tabs[k].className = 'px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 flex-shrink-0 transition font-semibold';
        }
    });

    renderProductGrid();
}

function searchProducts(query) {
    currentSearchQuery = query.trim().toLowerCase();
    renderProductGrid();
}

function renderProductGrid() {
    const grid = document.getElementById('productGrid');
    grid.innerHTML = '';

    const filtered = PRODUCTS_DATA.filter(p => {
        const matchBrand = (currentBrandFilter === 'all') || (p.brand === currentBrandFilter);
        const matchSearch = !currentSearchQuery || 
            p.name.toLowerCase().includes(currentSearchQuery) || 
            p.id.toLowerCase().includes(currentSearchQuery) ||
            p.color.toLowerCase().includes(currentSearchQuery);

        return matchBrand && matchSearch;
    });

    filtered.forEach(p => {
        const isOutOfStock = p.stock <= 0;
        const card = document.createElement('div');
        card.className = `bg-white rounded-xl p-3 border transition duration-200 flex flex-col justify-between select-none ${
            isOutOfStock 
            ? 'opacity-60 border-slate-200 cursor-not-allowed bg-slate-50' 
            : 'border-slate-200 hover:border-emerald-500 hover:shadow-md cursor-pointer active:scale-[0.98]'
        }`;

        if (!isOutOfStock) {
            card.onclick = () => addToCart(p);
        }

        card.innerHTML = `
            <div>
                <!-- Ảnh/Icon giả lập máy -->
                <div class="h-20 rounded-lg bg-gradient-to-tr ${p.imageBg} text-white flex items-center justify-center relative overflow-hidden mb-2">
                    <i class="fa-solid ${p.icon} text-3xl opacity-80"></i>
                    <span class="absolute top-1 right-1 px-1.5 py-0.5 rounded text-[10px] font-bold ${isOutOfStock ? 'bg-rose-600' : 'bg-slate-900/80'} text-white">
                        ${p.badge}
                    </span>
                    <span class="absolute bottom-1 left-1.5 text-[10px] text-slate-200 font-mono">
                        ${p.rom}
                    </span>
                </div>

                <h4 class="font-bold text-xs text-slate-900 leading-snug line-clamp-2">${p.name}</h4>
                <p class="text-[11px] text-slate-500 mt-0.5">${p.color}</p>
            </div>

            <div class="mt-2 pt-2 border-t border-slate-100 flex items-center justify-between">
                <div>
                    <span class="font-extrabold text-xs text-emerald-600 block">${formatCurrency(p.price)}</span>
                    <span class="text-[10px] ${isOutOfStock ? 'text-rose-600 font-bold' : 'text-slate-400'}">
                        ${isOutOfStock ? 'Hết hàng (0)' : `Còn tồn: ${p.stock}`}
                    </span>
                </div>
                <button class="w-7 h-7 rounded-lg ${isOutOfStock ? 'bg-slate-200 text-slate-400' : 'bg-emerald-50 text-emerald-600 hover:bg-emerald-600 hover:text-white'} flex items-center justify-center text-xs font-bold transition">
                    <i class="fa-solid fa-plus"></i>
                </button>
            </div>
        `;
        grid.appendChild(card);
    });
}

// THÊM SẢN PHẨM VÀO GIỎ (Use Case 2.7.1.1)
function addToCart(product) {
    if (product.stock <= 0) {
        alert('Cảnh báo tồn kho: Sản phẩm này đã hết hàng trong kho!');
        return;
    }

    const existing = cart.find(i => i.product.id === product.id);
    if (existing) {
        if (existing.quantity >= product.stock) {
            alert(`Tồn kho không đủ! Cửa hàng chỉ còn ${product.stock} máy có sẵn IMEI.`);
            return;
        }
        existing.quantity += 1;
        // Gán thêm 1 IMEI tiếp theo nếu có
        const nextImei = product.availableImeis[existing.quantity - 1] || 'Đang quét thêm...';
        existing.assignedImeis.push(nextImei);
    } else {
        const firstImei = product.availableImeis[0] || 'Chưa gán IMEI';
        cart.push({
            product: product,
            quantity: 1,
            assignedImeis: [firstImei]
        });
    }

    updateCartUI();
}

function updateQuantity(productId, delta) {
    const item = cart.find(i => i.product.id === productId);
    if (!item) return;

    const newQty = item.quantity + delta;
    if (newQty <= 0) {
        removeFromCart(productId);
        return;
    }

    if (newQty > item.product.stock) {
        alert(`Số lượng vượt quá tồn kho hiện có (${item.product.stock} máy)!`);
        return;
    }

    item.quantity = newQty;
    // Cập nhật danh sách IMEI tương ứng
    item.assignedImeis = item.product.availableImeis.slice(0, newQty);
    updateCartUI();
}

function removeFromCart(productId) {
    cart = cart.filter(i => i.product.id !== productId);
    updateCartUI();
}

function clearCart() {
    cart = [];
    activeVoucher = null;
    document.getElementById('voucherInput').value = '';
    document.getElementById('voucherMessage').classList.add('hidden');
    updateCartUI();
}

function updateCartUI() {
    const cartItemsList = document.getElementById('cartItemsList');
    const emptyState = document.getElementById('cartEmptyState');
    cartItemsList.innerHTML = '';

    if (cart.length === 0) {
        emptyState.classList.remove('hidden');
        document.getElementById('txtSubTotal').textContent = '0 đ';
        document.getElementById('txtDiscount').textContent = '-0 đ';
        document.getElementById('txtVat').textContent = '0 đ';
        document.getElementById('txtTotal').textContent = '0 đ';
        document.getElementById('customerGivenMoney').value = '';
        document.getElementById('txtChangeMoney').textContent = '0 đ';
        return;
    }

    emptyState.classList.add('hidden');

    let subTotal = 0;

    cart.forEach(item => {
        const lineTotal = item.product.price * item.quantity;
        subTotal += lineTotal;

        const row = document.createElement('div');
        row.className = 'p-2.5 bg-slate-50 border border-slate-200 rounded-xl space-y-1.5';
        row.innerHTML = `
            <div class="flex items-start justify-between">
                <div>
                    <h5 class="font-bold text-xs text-slate-800">${item.product.name}</h5>
                    <span class="text-[10px] text-slate-500">${item.product.color} | Đơn giá: ${formatCurrency(item.product.price)}</span>
                </div>
                <button onclick="removeFromCart('${item.product.id}')" class="text-rose-500 hover:text-rose-700 text-xs">
                    <i class="fa-solid fa-xmark"></i>
                </button>
            </div>

            <!-- Gán IMEI xuất bán (Use Case 2.5.2.1.1 & 2.11) -->
            <div class="bg-white p-1.5 rounded-lg border border-slate-200 text-[11px] font-mono">
                <span class="text-slate-400 block text-[10px] uppercase font-sans">Mã IMEI gán bán:</span>
                <div class="text-amber-700 font-bold">
                    ${item.assignedImeis.map(imei => `<div><i class="fa-solid fa-barcode text-slate-400 mr-1"></i>${imei}</div>`).join('')}
                </div>
            </div>

            <!-- Tăng giảm số lượng -->
            <div class="flex items-center justify-between pt-1">
                <div class="flex items-center gap-1.5 bg-white border border-slate-300 rounded-lg p-0.5">
                    <button onclick="updateQuantity('${item.product.id}', -1)" class="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 font-bold text-slate-700 text-xs">
                        -
                    </button>
                    <span class="w-6 text-center font-bold text-xs">${item.quantity}</span>
                    <button onclick="updateQuantity('${item.product.id}', 1)" class="w-6 h-6 rounded bg-slate-100 hover:bg-slate-200 font-bold text-slate-700 text-xs">
                        +
                    </button>
                </div>
                <span class="font-bold text-xs text-slate-900">${formatCurrency(lineTotal)}</span>
            </div>
        `;
        cartItemsList.appendChild(row);
    });

    // Tính giảm giá Voucher
    let discountAmount = 0;
    if (activeVoucher) {
        if (activeVoucher.type === 'fixed') {
            discountAmount = activeVoucher.val;
        } else if (activeVoucher.type === 'percent') {
            discountAmount = subTotal * (activeVoucher.val / 100);
            if (activeVoucher.max && discountAmount > activeVoucher.max) {
                discountAmount = activeVoucher.max;
            }
        }
    }

    const vat = 0;
    const totalToPay = Math.max(0, subTotal - discountAmount + vat);

    document.getElementById('txtSubTotal').textContent = formatCurrency(subTotal);
    document.getElementById('txtDiscount').textContent = '-' + formatCurrency(discountAmount);
    document.getElementById('txtVat').textContent = formatCurrency(vat);
    document.getElementById('txtTotal').textContent = formatCurrency(totalToPay);

    calculateChangeMoney();
}

// ÁP DỤNG MÃ KHUYẾN MÃI (Use Case 2.14.2)
function applyVoucher() {
    const code = document.getElementById('voucherInput').value.trim().toUpperCase();
    const msg = document.getElementById('voucherMessage');

    if (!code) {
        alert('Vui lòng nhập mã khuyến mãi!');
        return;
    }

    if (cart.length === 0) {
        alert('Giỏ hàng trống! Chưa thể áp dụng khuyến mãi.');
        return;
    }

    let subTotal = cart.reduce((sum, i) => sum + (i.product.price * i.quantity), 0);

    if (code === 'CHAOTHU26') {
        if (subTotal >= 20000000) {
            activeVoucher = { code: 'CHAOTHU26', type: 'fixed', val: 500000, name: 'Chào Thu 2026' };
            msg.className = 'text-[11px] font-semibold text-emerald-600';
            msg.textContent = '✓ Áp dụng thành công Voucher Chào Thu 2026: Giảm 500.000 đ';
            msg.classList.remove('hidden');
        } else {
            alert('Mã CHAOTHU26 chỉ áp dụng cho đơn hàng từ 20.000.000 đ trở lên!');
            return;
        }
    } else if (code === 'APPLEVip5') {
        const hasApple = cart.some(i => i.product.brand === 'Apple');
        if (hasApple && subTotal >= 25000000) {
            activeVoucher = { code: 'APPLEVip5', type: 'percent', val: 5, max: 1500000, name: 'Tri ân tín đồ Apple' };
            msg.className = 'text-[11px] font-semibold text-emerald-600';
            msg.textContent = '✓ Áp dụng thành công Voucher Apple VIP: Giảm 5% (tối đa 1.500.000 đ)';
            msg.classList.remove('hidden');
        } else {
            alert('Mã APPLEVip5 chỉ áp dụng cho dòng iPhone và đơn từ 25 triệu trở lên!');
            return;
        }
    } else {
        alert('Mã khuyến mãi không tồn tại hoặc đã hết hạn!');
        return;
    }

    updateCartUI();
}

function setPaymentMethod(method) {
    paymentMethod = method;
    const btnCash = document.getElementById('btnPayCash');
    const btnQr = document.getElementById('btnPayQr');
    const cashBox = document.getElementById('cashCalculationBox');

    if (method === 'cash') {
        btnCash.className = 'py-1.5 px-2 rounded-lg bg-emerald-600 text-white font-bold flex items-center justify-center gap-1.5 shadow-sm';
        btnQr.className = 'py-1.5 px-2 rounded-lg bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold flex items-center justify-center gap-1.5 transition';
        cashBox.classList.remove('hidden');
    } else {
        btnQr.className = 'py-1.5 px-2 rounded-lg bg-emerald-600 text-white font-bold flex items-center justify-center gap-1.5 shadow-sm';
        btnCash.className = 'py-1.5 px-2 rounded-lg bg-slate-200 hover:bg-slate-300 text-slate-700 font-semibold flex items-center justify-center gap-1.5 transition';
        cashBox.classList.add('hidden');
    }
}

function calculateChangeMoney() {
    const rawTotal = document.getElementById('txtTotal').textContent.replace(/[^\d]/g, '');
    const totalToPay = parseInt(rawTotal) || 0;
    const given = parseInt(document.getElementById('customerGivenMoney').value) || 0;

    const change = Math.max(0, given - totalToPay);
    document.getElementById('txtChangeMoney').textContent = formatCurrency(change);
}

// XỬ LÝ THANH TOÁN & IN HÓA ĐƠN (Use Case 2.10 & 2.11)
function handleCheckout() {
    if (cart.length === 0) {
        alert('Giỏ hàng trống! Vui lòng chọn sản phẩm trước khi thanh toán.');
        return;
    }

    const rawTotal = document.getElementById('txtTotal').textContent.replace(/[^\d]/g, '');
    const totalToPay = parseInt(rawTotal) || 0;

    const customerSelect = document.getElementById('customerSelect');
    const customerText = customerSelect.options[customerSelect.selectedIndex].text;

    // Build nội dung hóa đơn
    const invCode = 'HD' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '_' + Math.floor(100 + Math.random() * 900);
    document.getElementById('invCode').textContent = `Mã HĐ: ${invCode}`;

    let detailsHtml = `
        <div class="flex justify-between pb-1 border-b border-slate-100">
            <span>Khách hàng: <strong>${customerText}</strong></span>
            <span>Thu ngân: <strong>Đặng Hoàng Nam</strong></span>
        </div>
        <div class="space-y-2 py-2">
    `;

    cart.forEach(item => {
        detailsHtml += `
            <div class="border-b border-slate-100 pb-1.5">
                <div class="flex justify-between font-bold">
                    <span>${item.product.name} (x${item.quantity})</span>
                    <span>${formatCurrency(item.product.price * item.quantity)}</span>
                </div>
                <div class="text-[10px] text-slate-500 font-mono">
                    IMEI: ${item.assignedImeis.join(', ')}
                </div>
            </div>
        `;
    });

    detailsHtml += `
        </div>
        <div class="pt-1 space-y-1 font-medium">
            <div class="flex justify-between"><span>Tổng tiền hàng:</span><span>${document.getElementById('txtSubTotal').textContent}</span></div>
            <div class="flex justify-between text-pink-600"><span>Giảm giá:</span><span>${document.getElementById('txtDiscount').textContent}</span></div>
            <div class="flex justify-between font-bold text-slate-900 text-sm pt-1 border-t border-slate-200">
                <span>TỔNG THANH TOÁN:</span><span class="text-emerald-700">${document.getElementById('txtTotal').textContent}</span>
            </div>
            <div class="flex justify-between text-[11px] text-slate-500">
                <span>Phương thức:</span>
                <span>${paymentMethod === 'cash' ? 'Tiền mặt' : 'Chuyển khoản VietQR'}</span>
            </div>
        </div>
    `;

    document.getElementById('invoiceDetails').innerHTML = detailsHtml;
    document.getElementById('invoiceModal').classList.remove('hidden');
}

function closeInvoiceModal() {
    document.getElementById('invoiceModal').classList.add('hidden');
    // Hỏi làm mới giỏ hàng sau khi xuất hoá đơn
    if (confirm('Thanh toán thành công! Bạn có muốn làm mới giỏ hàng để tiếp tục đơn tiếp theo?')) {
        clearCart();
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}
