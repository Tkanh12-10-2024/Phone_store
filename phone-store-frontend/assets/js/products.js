(() => {
    const BACKEND_ORIGIN = API_BASE_URL.replace(/\/api\/?$/, '');
    const FALLBACK_SVG =
        'data:image/svg+xml;utf8,' +
        encodeURIComponent(
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">' +
                '<rect width="120" height="120" rx="16" fill="#f1f5f9"/>' +
                '<rect x="38" y="20" width="44" height="80" rx="8" fill="#cbd5e1"/>' +
                '<rect x="43" y="27" width="34" height="62" rx="4" fill="#e2e8f0"/>' +
                '<circle cx="60" cy="94" r="3" fill="#94a3b8"/>' +
            '</svg>'
        );

    const state = {
        activeTab: 'products',
        viewMode: 'table',
        products: [],
        categories: [],
        brands: [],
        filterOptions: null,
        editingProductId: null,
        editingCategoryId: null,
        editingBrandId: null,
        galleryUrls: [],
        primaryImageUrl: '',
        currentDetailProduct: null,
        pendingDelete: null
    };

    const $ = (id) => document.getElementById(id);

    function escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatVnd(value) {
        return Number(value || 0).toLocaleString('vi-VN') + ' đ';
    }

    function resolveImageUrl(path) {
        if (!path || !String(path).trim()) return FALLBACK_SVG;
        const clean = String(path).trim();
        if (/^(https?:\/\/|data:)/i.test(clean)) return clean;
        if (clean.startsWith('/')) return `${BACKEND_ORIGIN}${clean}`;
        return `${BACKEND_ORIGIN}/${clean}`;
    }

    async function apiRequest(path, options = {}) {
        const isFormData = options.body instanceof FormData;
        const headers = isFormData
            ? { ...(options.headers || {}) }
            : { 'Content-Type': 'application/json', ...(options.headers || {}) };

        const response = await fetch(`${API_BASE_URL}${path}`, {
            ...options,
            headers
        });
        const text = await response.text();
        let data = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = null;
        }
        if (!response.ok) {
            throw new Error(data?.message || `Yêu cầu thất bại (HTTP ${response.status})`);
        }
        return data;
    }

    function showLiveStatus(message, isError = false) {
        const el = $('productLiveStatus');
        el.textContent = message;
        el.className = isError
            ? 'rounded-xl px-4 py-3 text-sm font-medium border bg-red-50 border-red-200 text-red-700'
            : 'rounded-xl px-4 py-3 text-sm font-medium border bg-emerald-50 border-emerald-200 text-emerald-700';
        el.classList.remove('hidden');
        clearTimeout(showLiveStatus._timer);
        showLiveStatus._timer = setTimeout(() => el.classList.add('hidden'), 6000);
    }

    function showBoxError(elementId, message) {
        const el = $(elementId);
        if (!message) {
            el.textContent = '';
            el.classList.add('hidden');
            return;
        }
        el.textContent = message;
        el.classList.remove('hidden');
    }

    function switchTab(tabName) {
        state.activeTab = tabName;
        document.querySelectorAll('.tab-btn').forEach((btn) => {
            const active = btn.dataset.tab === tabName;
            btn.className = active
                ? 'tab-btn px-4 py-2.5 text-sm font-bold rounded-t-xl border-b-2 border-blue-600 text-blue-600 bg-white flex items-center gap-2'
                : 'tab-btn px-4 py-2.5 text-sm font-semibold rounded-t-xl border-b-2 border-transparent text-slate-600 hover:text-slate-900 flex items-center gap-2';
        });
        $('tabProducts').classList.toggle('hidden', tabName !== 'products');
        $('tabCategories').classList.toggle('hidden', tabName !== 'categories');
        $('tabBrands').classList.toggle('hidden', tabName !== 'brands');
    }

    function setViewMode(mode) {
        state.viewMode = mode;
        const isTable = mode === 'table';
        $('productTableWrapper').classList.toggle('hidden', !isTable);
        $('productGridWrapper').classList.toggle('hidden', isTable);
        $('btnViewTable').className = isTable
            ? 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 text-white transition flex items-center gap-1.5'
            : 'px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 transition flex items-center gap-1.5';
        $('btnViewGrid').className = !isTable
            ? 'px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-600 text-white transition flex items-center gap-1.5'
            : 'px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 hover:text-slate-900 transition flex items-center gap-1.5';
    }

    async function loadAllData() {
        await Promise.all([loadFilterOptions(), loadCategories(), loadBrands()]);
        await loadProducts();
    }

    async function loadFilterOptions() {
        try {
            const opts = await apiRequest('/products/filters');
            state.filterOptions = opts;

            const currentBrand = $('filterBrand').value;
            $('filterBrand').innerHTML = '<option value="">Tất cả hãng</option>' +
                (opts.brands || []).map((b) => `<option value="${b.maThuongHieu}">${escapeHtml(b.tenThuongHieu)}</option>`).join('');
            $('filterBrand').value = currentBrand;

            const currentCat = $('filterCategory').value;
            $('filterCategory').innerHTML = '<option value="">Tất cả danh mục</option>' +
                (opts.categories || []).map((c) => `<option value="${c.maDanhMuc}">${escapeHtml(c.tenDanhMuc)}</option>`).join('');
            $('filterCategory').value = currentCat;

            const currentRam = $('filterRam').value;
            $('filterRam').innerHTML = '<option value="">Tất cả RAM</option>' +
                (opts.rams || []).map((r) => `<option value="${escapeHtml(r)}">${escapeHtml(r)}</option>`).join('');
            $('filterRam').value = currentRam;

            const currentRom = $('filterRom').value;
            $('filterRom').innerHTML = '<option value="">Tất cả ROM</option>' +
                (opts.roms || []).map((r) => `<option value="${escapeHtml(r)}">${escapeHtml(r)}</option>`).join('');
            $('filterRom').value = currentRom;

            $('filterColorList').innerHTML = (opts.colors || [])
                .map((c) => `<option value="${escapeHtml(c)}"></option>`)
                .join('');

            $('productLinesDatalist').innerHTML = (opts.productLines || [])
                .map((line) => `<option value="${escapeHtml(line)}"></option>`)
                .join('');

            $('inputThuongHieu').innerHTML = (opts.brands || [])
                .map((b) => `<option value="${b.maThuongHieu}">${escapeHtml(b.tenThuongHieu)}</option>`)
                .join('');
            $('inputDanhMuc').innerHTML = (opts.categories || [])
                .map((c) => `<option value="${c.maDanhMuc}">${escapeHtml(c.tenDanhMuc)}</option>`)
                .join('');
        } catch (err) {
            showLiveStatus(`Không thể tải tùy chọn lọc: ${err.message}`, true);
        }
    }

    function buildProductFilterQuery() {
        const params = new URLSearchParams();
        const mappings = [
            ['keyword', $('filterKeyword').value.trim()],
            ['maThuongHieu', $('filterBrand').value],
            ['maDanhMuc', $('filterCategory').value],
            ['trangThai', $('filterStatus').value],
            ['minPrice', $('filterMinPrice').value.trim()],
            ['maxPrice', $('filterMaxPrice').value.trim()],
            ['ram', $('filterRam').value],
            ['rom', $('filterRom').value],
            ['mauSac', $('filterColor').value.trim()],
            ['sort', $('filterSort').value]
        ];
        for (const [k, v] of mappings) {
            if (v !== '') params.set(k, v);
        }
        const qs = params.toString();
        return qs ? `?${qs}` : '';
    }

    async function loadProducts() {
        try {
            const list = await apiRequest(`/products${buildProductFilterQuery()}`);
            state.products = Array.isArray(list) ? list : [];
            renderProducts();
            updateKpiCards();
        } catch (err) {
            showLiveStatus(`Lỗi tải danh sách sản phẩm: ${err.message}`, true);
            $('productsTableBody').innerHTML =
                `<tr><td colspan="7" class="py-8 text-center text-red-500">${escapeHtml(err.message)}</td></tr>`;
        }
    }

    async function loadCategories() {
        try {
            const kw = $('catSearchInput').value.trim();
            const qs = kw ? `?keyword=${encodeURIComponent(kw)}` : '';
            const list = await apiRequest(`/categories${qs}`);
            state.categories = Array.isArray(list) ? list : [];
            renderCategories();
            updateKpiCards();
        } catch (err) {
            showLiveStatus(`Lỗi tải danh mục: ${err.message}`, true);
        }
    }

    async function loadBrands() {
        try {
            const kw = $('brandSearchInput').value.trim();
            const qs = kw ? `?keyword=${encodeURIComponent(kw)}` : '';
            const list = await apiRequest(`/brands${qs}`);
            state.brands = Array.isArray(list) ? list : [];
            renderBrands();
            updateKpiCards();
        } catch (err) {
            showLiveStatus(`Lỗi tải thương hiệu: ${err.message}`, true);
        }
    }

    function updateKpiCards() {
        const total = state.products.length;
        const active = state.products.filter((p) => p.trangThai === 'Dang_kinh_doanh').length;
        const stopped = total - active;
        $('statTotalProducts').textContent = total;
        $('statActiveProducts').textContent = active;
        $('statStoppedProducts').textContent = stopped;
        $('statCatBrandCount').textContent = `${state.categories.length} / ${state.brands.length}`;
        $('resultCountLabel').textContent = `Hiển thị: ${total} sản phẩm`;
    }

    function renderProducts() {
        const tbody = $('productsTableBody');
        const grid = $('productGridWrapper');

        if (!state.products.length) {
            tbody.innerHTML =
                '<tr><td colspan="7" class="py-12 text-center text-slate-400">Không tìm thấy sản phẩm nào phù hợp điều kiện lọc.</td></tr>';
            grid.innerHTML =
                '<div class="col-span-full py-12 text-center text-slate-400 border border-dashed border-slate-300 rounded-2xl">Không tìm thấy sản phẩm nào phù hợp điều kiện lọc.</div>';
            return;
        }

        tbody.innerHTML = state.products.map((p) => {
            const isActive = p.trangThai === 'Dang_kinh_doanh';
            const statusBadge = isActive
                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                : 'bg-slate-100 text-slate-600 border-slate-300';
            const statusText = isActive ? 'Đang kinh doanh' : 'Ngừng kinh doanh';
            const stockClass = Number(p.soLuongTon) <= Number(p.dinhMucTonToiThieu)
                ? 'text-red-600 font-bold'
                : 'text-slate-800 font-bold';
            const imgSrc = resolveImageUrl(p.hinhAnh);

            return `
                <tr class="hover:bg-slate-50/80 transition">
                    <td class="py-3 px-4">
                        <div class="flex items-center gap-3">
                            <img src="${escapeHtml(imgSrc)}" alt="${escapeHtml(p.tenSanPham)}"
                                 onerror="this.src='${FALLBACK_SVG}'"
                                 class="w-12 h-12 rounded-xl object-contain bg-slate-100 border border-slate-200 p-1 flex-shrink-0">
                            <div class="min-w-0">
                                <p class="font-bold text-slate-900 truncate max-w-xs">${escapeHtml(p.tenSanPham)}</p>
                                <p class="text-xs text-slate-500 font-mono">${escapeHtml(p.maSanPham)} · ${escapeHtml(p.dongMay)}</p>
                            </div>
                        </div>
                    </td>
                    <td class="py-3 px-3">
                        <span class="inline-block px-2 py-0.5 rounded-md bg-blue-50 text-blue-700 text-xs font-bold">${escapeHtml(p.tenThuongHieu)}</span>
                        <p class="text-xs text-slate-500 mt-1">${escapeHtml(p.tenDanhMuc)}</p>
                    </td>
                    <td class="py-3 px-3 text-xs">
                        <div class="flex flex-wrap gap-1">
                            <span class="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 font-semibold">${escapeHtml(p.mauSac)}</span>
                            <span class="px-2 py-0.5 rounded-md bg-indigo-50 text-indigo-700 font-semibold">${escapeHtml(p.ram)} / ${escapeHtml(p.rom)}</span>
                        </div>
                        ${p.chipXuLy ? `<p class="text-[11px] text-slate-400 mt-1 truncate max-w-[180px]">${escapeHtml(p.chipXuLy)}</p>` : ''}
                    </td>
                    <td class="py-3 px-3 text-right">
                        <p class="font-bold text-blue-600">${formatVnd(p.giaBan)}</p>
                        <p class="text-xs text-slate-400">Nhập: ${formatVnd(p.giaNhapThamKhao)}</p>
                    </td>
                    <td class="py-3 px-3 text-center">
                        <span class="${stockClass}">${p.soLuongTon}</span>
                        <p class="text-[11px] text-slate-400">Min: ${p.dinhMucTonToiThieu}</p>
                    </td>
                    <td class="py-3 px-3 text-center">
                        <button type="button" data-action="toggle-status" data-id="${escapeHtml(p.maSanPham)}" data-status="${escapeHtml(p.trangThai)}"
                                title="Bấm để đổi trạng thái kinh doanh"
                                class="px-2.5 py-1 rounded-full text-xs font-bold border ${statusBadge} hover:opacity-80 transition">
                            ${statusText}
                        </button>
                    </td>
                    <td class="py-3 px-4 text-center">
                        <div class="inline-flex items-center gap-1">
                            <button type="button" data-action="detail" data-id="${escapeHtml(p.maSanPham)}" title="Xem chi tiết"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-blue-100 text-slate-600 hover:text-blue-700 transition">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                            <button type="button" data-action="edit" data-id="${escapeHtml(p.maSanPham)}" title="Chỉnh sửa"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-600 hover:text-amber-700 transition">
                                <i class="fa-solid fa-pen"></i>
                            </button>
                            <button type="button" data-action="delete" data-id="${escapeHtml(p.maSanPham)}" title="Xóa / Ngừng kinh doanh"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-red-100 text-slate-600 hover:text-red-700 transition">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        grid.innerHTML = state.products.map((p) => {
            const isActive = p.trangThai === 'Dang_kinh_doanh';
            const imgSrc = resolveImageUrl(p.hinhAnh);
            return `
                <article class="bg-white border border-slate-200 rounded-2xl p-4 shadow-sm hover:shadow-md transition flex flex-col justify-between gap-3">
                    <div>
                        <div class="relative aspect-[4/3] bg-slate-50 rounded-xl border border-slate-100 flex items-center justify-center p-3 overflow-hidden mb-3">
                            <img src="${escapeHtml(imgSrc)}" alt="${escapeHtml(p.tenSanPham)}"
                                 onerror="this.src='${FALLBACK_SVG}'"
                                 class="max-h-full max-w-full object-contain">
                            <span class="absolute top-2 left-2 px-2 py-0.5 rounded-md text-[11px] font-bold bg-blue-600 text-white">${escapeHtml(p.tenThuongHieu)}</span>
                            <span class="absolute top-2 right-2 px-2 py-0.5 rounded-full text-[11px] font-bold ${isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-700'}">
                                ${isActive ? 'Đang bán' : 'Ngừng bán'}
                            </span>
                        </div>
                        <p class="text-[11px] font-mono text-slate-400">${escapeHtml(p.maSanPham)}</p>
                        <h3 class="font-bold text-sm text-slate-900 line-clamp-2 mt-0.5">${escapeHtml(p.tenSanPham)}</h3>
                        <div class="flex flex-wrap gap-1 mt-2 text-[11px]">
                            <span class="px-2 py-0.5 bg-slate-100 text-slate-700 rounded-md font-semibold">${escapeHtml(p.mauSac)}</span>
                            <span class="px-2 py-0.5 bg-indigo-50 text-indigo-700 rounded-md font-semibold">${escapeHtml(p.ram)}/${escapeHtml(p.rom)}</span>
                            <span class="px-2 py-0.5 bg-amber-50 text-amber-700 rounded-md font-semibold">Tồn: ${p.soLuongTon}</span>
                        </div>
                    </div>
                    <div class="pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                        <p class="text-base font-extrabold text-blue-600">${formatVnd(p.giaBan)}</p>
                        <div class="flex items-center gap-1">
                            <button type="button" data-action="detail" data-id="${escapeHtml(p.maSanPham)}" title="Chi tiết"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-blue-100 text-slate-600 hover:text-blue-700">
                                <i class="fa-solid fa-eye"></i>
                            </button>
                            <button type="button" data-action="edit" data-id="${escapeHtml(p.maSanPham)}" title="Sửa"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-amber-100 text-slate-600 hover:text-amber-700">
                                <i class="fa-solid fa-pen"></i>
                            </button>
                            <button type="button" data-action="delete" data-id="${escapeHtml(p.maSanPham)}" title="Xóa"
                                    class="w-8 h-8 rounded-lg bg-slate-100 hover:bg-red-100 text-slate-600 hover:text-red-700">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </article>
            `;
        }).join('');
    }

    function renderCategories() {
        const tbody = $('categoriesTableBody');
        if (!state.categories.length) {
            tbody.innerHTML = '<tr><td colspan="5" class="py-10 text-center text-slate-400">Chưa có danh mục nào.</td></tr>';
            return;
        }
        tbody.innerHTML = state.categories.map((c) => `
            <tr class="hover:bg-slate-50/80 transition">
                <td class="py-3 px-4 font-mono text-xs text-slate-500">#${c.maDanhMuc}</td>
                <td class="py-3 px-4 font-bold text-slate-900">${escapeHtml(c.tenDanhMuc)}</td>
                <td class="py-3 px-4 text-slate-600">${escapeHtml(c.moTa || '-')}</td>
                <td class="py-3 px-4 text-center">
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-50 text-blue-700">${c.soLuongSanPham || 0} SP</span>
                </td>
                <td class="py-3 px-4 text-center">
                    <div class="inline-flex items-center gap-1.5">
                        <button type="button" data-cat-edit="${c.maDanhMuc}" class="px-2.5 py-1 rounded-lg bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-semibold">Sửa</button>
                        <button type="button" data-cat-del="${c.maDanhMuc}" class="px-2.5 py-1 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 text-xs font-semibold">Xóa</button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    function renderBrands() {
        const tbody = $('brandsTableBody');
        if (!state.brands.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="py-10 text-center text-slate-400">Chưa có thương hiệu nào.</td></tr>';
            return;
        }
        tbody.innerHTML = state.brands.map((b) => `
            <tr class="hover:bg-slate-50/80 transition">
                <td class="py-3 px-4 font-mono text-xs text-slate-500">#${b.maThuongHieu}</td>
                <td class="py-3 px-4 font-bold text-slate-900">${escapeHtml(b.tenThuongHieu)}</td>
                <td class="py-3 px-4 text-slate-600">${escapeHtml(b.quocGia || '-')}</td>
                <td class="py-3 px-4 text-slate-600">${escapeHtml(b.moTa || '-')}</td>
                <td class="py-3 px-4 text-center">
                    <span class="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700">${b.soLuongSanPham || 0} SP</span>
                </td>
                <td class="py-3 px-4 text-center">
                    <div class="inline-flex items-center gap-1.5">
                        <button type="button" data-brand-edit="${b.maThuongHieu}" class="px-2.5 py-1 rounded-lg bg-amber-50 hover:bg-amber-100 text-amber-700 text-xs font-semibold">Sửa</button>
                        <button type="button" data-brand-del="${b.maThuongHieu}" class="px-2.5 py-1 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 text-xs font-semibold">Xóa</button>
                    </div>
                </td>
            </tr>
        `).join('');
    }

    function addVariantRow(preset = {}) {
        const tr = document.createElement('tr');
        tr.className = 'variant-row';
        tr.innerHTML = `
            <td class="py-1.5 px-1">
                <input type="text" class="v-sku w-32 px-2 py-1.5 border border-slate-300 rounded-lg font-mono uppercase"
                       maxlength="20" placeholder="Tự động" value="${escapeHtml(preset.maSanPham || '')}">
            </td>
            <td class="py-1.5 px-1">
                <input type="text" class="v-color w-28 px-2 py-1.5 border border-slate-300 rounded-lg"
                       placeholder="Titan Đen" value="${escapeHtml(preset.mauSac || '')}" required>
            </td>
            <td class="py-1.5 px-1">
                <input type="text" class="v-ram w-20 px-2 py-1.5 border border-slate-300 rounded-lg"
                       placeholder="8GB" value="${escapeHtml(preset.ram || '8GB')}" required>
            </td>
            <td class="py-1.5 px-1">
                <input type="text" class="v-rom w-24 px-2 py-1.5 border border-slate-300 rounded-lg"
                       placeholder="256GB" value="${escapeHtml(preset.rom || '256GB')}" required>
            </td>
            <td class="py-1.5 px-1">
                <input type="number" min="0" step="10000" class="v-cost w-28 px-2 py-1.5 border border-slate-300 rounded-lg"
                       placeholder="20000000" value="${preset.giaNhapThamKhao ?? ''}">
            </td>
            <td class="py-1.5 px-1">
                <input type="number" min="1" step="10000" class="v-price w-28 px-2 py-1.5 border border-slate-300 rounded-lg"
                       placeholder="25990000" value="${preset.giaBan ?? ''}" required>
            </td>
            <td class="py-1.5 px-1">
                <input type="number" min="0" class="v-minstock w-16 px-2 py-1.5 border border-slate-300 rounded-lg"
                       value="${preset.dinhMucTonToiThieu ?? 2}">
            </td>
            <td class="py-1.5 px-1 text-center">
                <button type="button" class="btn-remove-variant w-7 h-7 rounded-lg bg-red-50 hover:bg-red-100 text-red-600" title="Xóa dòng">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            </td>
        `;
        tr.querySelector('.btn-remove-variant').addEventListener('click', () => {
            if ($('variantRowsBody').querySelectorAll('.variant-row').length <= 1) {
                showBoxError('productFormError', 'Sản phẩm mới phải có ít nhất 1 biến thể.');
                return;
            }
            tr.remove();
        });
        $('variantRowsBody').appendChild(tr);
    }

    function renderGalleryPreviews() {
        const container = $('galleryPreviewList');
        if (!state.galleryUrls.length) {
            container.innerHTML =
                '<div class="col-span-full text-xs text-slate-400 italic py-2">Chưa có hình ảnh nào. Hãy chọn ảnh tải lên từ máy tính.</div>';
            return;
        }
        container.innerHTML = state.galleryUrls.map((url, idx) => {
            const isPrimary = url === state.primaryImageUrl;
            return `
                <div class="relative bg-white rounded-xl border ${isPrimary ? 'border-blue-600 ring-2 ring-blue-200' : 'border-slate-200'} p-2 flex flex-col gap-1.5">
                    <div class="aspect-square bg-slate-50 rounded-lg flex items-center justify-center overflow-hidden">
                        <img src="${escapeHtml(resolveImageUrl(url))}" onerror="this.src='${FALLBACK_SVG}'" class="max-h-full max-w-full object-contain">
                    </div>
                    <div class="flex items-center justify-between gap-1 text-[11px]">
                        <button type="button" data-gallery-primary="${idx}"
                                class="px-1.5 py-0.5 rounded ${isPrimary ? 'bg-blue-600 text-white font-bold' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}">
                            ${isPrimary ? 'Ảnh chính' : 'Đặt chính'}
                        </button>
                        <button type="button" data-gallery-remove="${idx}" class="text-red-500 hover:text-red-700 px-1" title="Xóa ảnh">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }

    function openCreateProductModal() {
        state.editingProductId = null;
        state.galleryUrls = [];
        state.primaryImageUrl = '';
        showBoxError('productFormError', '');
        $('uploadImageStatus').classList.add('hidden');
        $('productForm').reset();
        $('inputBaoHanh').value = '12';
        $('inputTrangThai').value = 'Dang_kinh_doanh';

        $('productModalTitle').textContent = 'Thêm sản phẩm mới kèm biến thể';
        $('productModalSubtitle').textContent = 'Nhập thông số chung, tải ảnh lên thư mục cục bộ và khai báo danh sách biến thể';
        $('createVariantsSection').classList.remove('hidden');
        $('editVariantSection').classList.add('hidden');
        toggleVariantInputsRequired(true);

        $('variantRowsBody').innerHTML = '';
        addVariantRow({ mauSac: 'Titan Tự Nhiên', ram: '8GB', rom: '256GB', dinhMucTonToiThieu: 2 });
        renderGalleryPreviews();
        $('productModal').showModal();
    }

    async function openEditProductModal(maSanPham) {
        try {
            const p = await apiRequest(`/products/${encodeURIComponent(maSanPham)}`);
            state.editingProductId = p.maSanPham;
            state.galleryUrls = (p.galleries || []).map((g) => g.duongDanAnh);
            if (!state.galleryUrls.length && p.hinhAnh) {
                state.galleryUrls = [p.hinhAnh];
            }
            state.primaryImageUrl = p.hinhAnh || state.galleryUrls[0] || '';

            showBoxError('productFormError', '');
            $('uploadImageStatus').classList.add('hidden');
            $('productModalTitle').textContent = `Chỉnh sửa sản phẩm: ${p.maSanPham}`;
            $('productModalSubtitle').textContent = 'Cập nhật thông số kỹ thuật, giá bán, trạng thái và bộ ảnh sản phẩm';

            $('inputTenSanPham').value = p.tenSanPham || '';
            $('inputDongMay').value = p.dongMay || '';
            $('inputThuongHieu').value = p.maThuongHieu || '';
            $('inputDanhMuc').value = p.maDanhMuc || '';
            $('inputBaoHanh').value = p.thoiGianBaoHanhThang ?? 12;
            $('inputTrangThai').value = p.trangThai || 'Dang_kinh_doanh';
            $('inputManHinh').value = p.manHinh || '';
            $('inputChipXuLy').value = p.chipXuLy || '';
            $('inputPin').value = p.pin || '';
            $('inputCamera').value = p.camera || '';
            $('inputHeDieuHanh').value = p.heDieuHanh || '';

            $('editMaSanPham').value = p.maSanPham;
            $('editMauSac').value = p.mauSac || '';
            $('editRam').value = p.ram || '';
            $('editRom').value = p.rom || '';
            $('editGiaNhap').value = p.giaNhapThamKhao ?? 0;
            $('editGiaBan').value = p.giaBan ?? 0;
            $('editDinhMuc').value = p.dinhMucTonToiThieu ?? 2;

            $('createVariantsSection').classList.add('hidden');
            $('editVariantSection').classList.remove('hidden');
            toggleVariantInputsRequired(false);

            renderGalleryPreviews();
            $('productModal').showModal();
        } catch (err) {
            showLiveStatus(`Không thể tải thông tin sản phẩm: ${err.message}`, true);
        }
    }

    function toggleVariantInputsRequired(isCreateMode) {
        $('variantRowsBody').querySelectorAll('input').forEach((inp) => {
            if (inp.classList.contains('v-color') || inp.classList.contains('v-ram') ||
                inp.classList.contains('v-rom') || inp.classList.contains('v-price')) {
                inp.required = isCreateMode;
            }
        });
        ['editMauSac', 'editRam', 'editRom', 'editGiaBan'].forEach((id) => {
            $(id).required = !isCreateMode;
        });
    }

    async function handleUploadSelectedFiles(fileList) {
        if (!fileList || !fileList.length) return;
        const formData = new FormData();
        Array.from(fileList).forEach((f) => formData.append('files', f));

        const statusEl = $('uploadImageStatus');
        statusEl.textContent = `Đang tải lên ${fileList.length} hình ảnh...`;
        statusEl.className = 'text-xs font-semibold text-blue-700';
        statusEl.classList.remove('hidden');

        try {
            const res = await apiRequest('/products/upload', {
                method: 'POST',
                body: formData
            });
            const uploaded = res?.urls || [];
            uploaded.forEach((url) => {
                if (!state.galleryUrls.includes(url)) {
                    state.galleryUrls.push(url);
                }
            });
            if (!state.primaryImageUrl && state.galleryUrls.length) {
                state.primaryImageUrl = state.galleryUrls[0];
            }
            renderGalleryPreviews();
            statusEl.textContent = `Đã tải lên thành công ${uploaded.length} ảnh vào thư mục tĩnh cục bộ.`;
            statusEl.className = 'text-xs font-semibold text-emerald-700';
        } catch (err) {
            statusEl.textContent = `Lỗi tải ảnh: ${err.message}`;
            statusEl.className = 'text-xs font-semibold text-red-700';
        } finally {
            $('productImageFiles').value = '';
        }
    }

    async function handleProductSubmit(e) {
        e.preventDefault();
        showBoxError('productFormError', '');

        const basePayload = {
            tenSanPham: $('inputTenSanPham').value.trim(),
            maThuongHieu: Number($('inputThuongHieu').value),
            maDanhMuc: Number($('inputDanhMuc').value),
            dongMay: $('inputDongMay').value.trim(),
            manHinh: $('inputManHinh').value.trim(),
            chipXuLy: $('inputChipXuLy').value.trim(),
            pin: $('inputPin').value.trim(),
            camera: $('inputCamera').value.trim(),
            heDieuHanh: $('inputHeDieuHanh').value.trim(),
            thoiGianBaoHanhThang: Number($('inputBaoHanh').value || 12),
            trangThai: $('inputTrangThai').value,
            hinhAnh: state.primaryImageUrl || state.galleryUrls[0] || null,
            galleryUrls: state.galleryUrls
        };

        try {
            if (!state.editingProductId) {
                const variants = [];
                $('variantRowsBody').querySelectorAll('.variant-row').forEach((row) => {
                    variants.push({
                        maSanPham: row.querySelector('.v-sku').value.trim() || null,
                        mauSac: row.querySelector('.v-color').value.trim(),
                        ram: row.querySelector('.v-ram').value.trim(),
                        rom: row.querySelector('.v-rom').value.trim(),
                        giaNhapThamKhao: Number(row.querySelector('.v-cost').value || 0),
                        giaBan: Number(row.querySelector('.v-price').value || 0),
                        dinhMucTonToiThieu: Number(row.querySelector('.v-minstock').value || 2)
                    });
                });
                const created = await apiRequest('/products/with-variants', {
                    method: 'POST',
                    body: JSON.stringify({ ...basePayload, variants })
                });
                $('productModal').close();
                showLiveStatus(`Đã tạo thành công ${created.length} biến thể sản phẩm!`);
            } else {
                const updatePayload = {
                    ...basePayload,
                    mauSac: $('editMauSac').value.trim(),
                    ram: $('editRam').value.trim(),
                    rom: $('editRom').value.trim(),
                    giaNhapThamKhao: Number($('editGiaNhap').value || 0),
                    giaBan: Number($('editGiaBan').value || 0),
                    dinhMucTonToiThieu: Number($('editDinhMuc').value || 2)
                };
                await apiRequest(`/products/${encodeURIComponent(state.editingProductId)}`, {
                    method: 'PUT',
                    body: JSON.stringify(updatePayload)
                });
                $('productModal').close();
                showLiveStatus(`Đã cập nhật sản phẩm ${state.editingProductId}.`);
            }
            await loadAllData();
        } catch (err) {
            showBoxError('productFormError', err.message);
        }
    }

    async function openProductDetailModal(maSanPham) {
        try {
            const p = await apiRequest(`/products/${encodeURIComponent(maSanPham)}`);
            state.currentDetailProduct = p;

            const isActive = p.trangThai === 'Dang_kinh_doanh';
            $('detailTitle').textContent = `Chi tiết sản phẩm: ${p.maSanPham}`;
            $('detailStatusBadge').textContent = isActive ? 'Đang kinh doanh' : 'Ngừng kinh doanh';
            $('detailStatusBadge').className = isActive
                ? 'px-2.5 py-0.5 rounded-full text-xs font-bold bg-emerald-100 text-emerald-700'
                : 'px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-200 text-slate-700';

            $('detailBrandBadge').textContent = p.tenThuongHieu || '';
            $('detailCategoryBadge').textContent = p.tenDanhMuc || '';
            $('detailSkuText').textContent = `SKU: ${p.maSanPham}`;
            $('detailProductName').textContent = p.tenSanPham || '';
            $('detailDongMay').textContent = `Dòng máy: ${p.dongMay || '-'}`;
            $('detailGiaBan').textContent = formatVnd(p.giaBan);
            $('detailGiaNhap').textContent = `Giá nhập tham khảo: ${formatVnd(p.giaNhapThamKhao)}`;
            $('detailStock').textContent = `${p.soLuongTon} máy (Min: ${p.dinhMucTonToiThieu})`;
            $('detailWarranty').textContent = `Bảo hành chính hãng ${p.thoiGianBaoHanhThang} tháng`;

            const thumbs = (p.galleries && p.galleries.length)
                ? p.galleries.map((g) => g.duongDanAnh)
                : (p.hinhAnh ? [p.hinhAnh] : []);
            const mainImg = p.hinhAnh || thumbs[0] || '';
            $('detailMainImage').src = resolveImageUrl(mainImg);
            $('detailMainImage').onerror = function () { this.src = FALLBACK_SVG; };

            $('detailGalleryThumbs').innerHTML = thumbs.map((url) => `
                <button type="button" data-detail-thumb="${escapeHtml(url)}"
                        class="aspect-square rounded-xl border border-slate-200 bg-slate-50 p-1 hover:border-blue-500 transition overflow-hidden">
                    <img src="${escapeHtml(resolveImageUrl(url))}" onerror="this.src='${FALLBACK_SVG}'" class="w-full h-full object-contain">
                </button>
            `).join('');

            const siblings = p.siblingVariants || [];
            $('detailSiblingVariants').innerHTML = siblings.length
                ? siblings.map((v) => {
                    const current = v.maSanPham === p.maSanPham;
                    return `
                        <button type="button" data-switch-variant="${escapeHtml(v.maSanPham)}"
                                class="px-3 py-1.5 rounded-xl border text-xs font-semibold transition ${
                                    current
                                        ? 'border-blue-600 bg-blue-50 text-blue-700 ring-1 ring-blue-300'
                                        : 'border-slate-200 bg-white text-slate-700 hover:border-blue-400'
                                }">
                            ${escapeHtml(v.mauSac)} · ${escapeHtml(v.ram)}/${escapeHtml(v.rom)}
                            <span class="block text-[11px] font-bold ${current ? 'text-blue-600' : 'text-slate-500'}">${formatVnd(v.giaBan)}</span>
                        </button>
                    `;
                }).join('')
                : '<span class="text-xs text-slate-400">Không có biến thể khác.</span>';

            const specRows = [
                ['Màu sắc', p.mauSac],
                ['RAM', p.ram],
                ['Bộ nhớ trong (ROM)', p.rom],
                ['Màn hình', p.manHinh || 'Chưa cập nhật'],
                ['Chip xử lý', p.chipXuLy || 'Chưa cập nhật'],
                ['Dung lượng Pin', p.pin || 'Chưa cập nhật'],
                ['Camera', p.camera || 'Chưa cập nhật'],
                ['Hệ điều hành', p.heDieuHanh || 'Chưa cập nhật']
            ];
            $('detailSpecsGrid').innerHTML = specRows.map(([k, v]) => `
                <div class="px-4 py-2.5 grid grid-cols-3 gap-2 bg-white">
                    <dt class="font-semibold text-slate-500">${escapeHtml(k)}</dt>
                    <dd class="col-span-2 font-semibold text-slate-800">${escapeHtml(v)}</dd>
                </div>
            `).join('');

            $('btnDetailToggleStatus').textContent = isActive ? 'Chuyển Ngừng kinh doanh' : 'Kích hoạt Đang kinh doanh';
            if (!$('detailModal').open) {
                $('detailModal').showModal();
            }
        } catch (err) {
            showLiveStatus(`Không thể tải chi tiết sản phẩm: ${err.message}`, true);
        }
    }

    async function toggleProductStatus(maSanPham, currentStatus) {
        const nextStatus = currentStatus === 'Dang_kinh_doanh' ? 'Ngung_kinh_doanh' : 'Dang_kinh_doanh';
        try {
            await apiRequest(`/products/${encodeURIComponent(maSanPham)}/status`, {
                method: 'PUT',
                body: JSON.stringify({ trangThai: nextStatus })
            });
            showLiveStatus(`Đã chuyển trạng thái ${maSanPham} sang "${nextStatus === 'Dang_kinh_doanh' ? 'Đang kinh doanh' : 'Ngừng kinh doanh'}".`);
            await loadProducts();
            if ($('detailModal').open && state.currentDetailProduct?.maSanPham === maSanPham) {
                await openProductDetailModal(maSanPham);
            }
        } catch (err) {
            showLiveStatus(`Lỗi đổi trạng thái: ${err.message}`, true);
        }
    }

    function openDeleteConfirm(type, id, name) {
        state.pendingDelete = { type, id };
        if (type === 'product') {
            const prod = state.products.find((x) => x.maSanPham === id);
            $('confirmDeleteTitle').textContent = 'Xác nhận xóa / ngừng kinh doanh';
            $('confirmDeleteMessage').textContent =
                `Bạn muốn xóa sản phẩm "${name}" (${id})? Nếu sản phẩm đã phát sinh dữ liệu nhập kho, bán hàng hoặc bảo hành, hệ thống sẽ tự động chuyển sang "Ngừng kinh doanh" để bảo toàn lịch sử.` +
                (prod && prod.soLuongTon > 0 ? ` (Hiện còn ${prod.soLuongTon} máy tồn kho)` : '');
        } else if (type === 'category') {
            $('confirmDeleteTitle').textContent = 'Xác nhận xóa danh mục';
            $('confirmDeleteMessage').textContent = `Bạn có chắc muốn xóa danh mục "${name}"?`;
        } else {
            $('confirmDeleteTitle').textContent = 'Xác nhận xóa thương hiệu';
            $('confirmDeleteMessage').textContent = `Bạn có chắc muốn xóa thương hiệu "${name}"?`;
        }
        $('confirmDeleteModal').showModal();
    }

    async function executeConfirmedDelete() {
        if (!state.pendingDelete) return;
        const { type, id } = state.pendingDelete;
        try {
            let resp;
            if (type === 'product') {
                resp = await apiRequest(`/products/${encodeURIComponent(id)}`, { method: 'DELETE' });
            } else if (type === 'category') {
                resp = await apiRequest(`/categories/${id}`, { method: 'DELETE' });
            } else {
                resp = await apiRequest(`/brands/${id}`, { method: 'DELETE' });
            }
            $('confirmDeleteModal').close();
            showLiveStatus(resp?.message || 'Thao tác xóa thành công.');
            await loadAllData();
        } catch (err) {
            $('confirmDeleteModal').close();
            showLiveStatus(err.message, true);
        } finally {
            state.pendingDelete = null;
        }
    }

    function openCategoryModal(cat = null) {
        state.editingCategoryId = cat ? cat.maDanhMuc : null;
        showBoxError('categoryFormError', '');
        $('categoryModalTitle').textContent = cat ? `Sửa danh mục #${cat.maDanhMuc}` : 'Thêm danh mục sản phẩm';
        $('catTenDanhMuc').value = cat?.tenDanhMuc || '';
        $('catMoTa').value = cat?.moTa || '';
        $('categoryModal').showModal();
    }

    async function handleCategorySubmit(e) {
        e.preventDefault();
        showBoxError('categoryFormError', '');
        const payload = {
            tenDanhMuc: $('catTenDanhMuc').value.trim(),
            moTa: $('catMoTa').value.trim()
        };
        try {
            if (state.editingCategoryId) {
                await apiRequest(`/categories/${state.editingCategoryId}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showLiveStatus('Đã cập nhật danh mục thành công.');
            } else {
                await apiRequest('/categories', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showLiveStatus('Đã thêm danh mục mới thành công.');
            }
            $('categoryModal').close();
            await loadAllData();
        } catch (err) {
            showBoxError('categoryFormError', err.message);
        }
    }

    function openBrandModal(brand = null) {
        state.editingBrandId = brand ? brand.maThuongHieu : null;
        showBoxError('brandFormError', '');
        $('brandModalTitle').textContent = brand ? `Sửa thương hiệu #${brand.maThuongHieu}` : 'Thêm thương hiệu mới';
        $('brandTenThuongHieu').value = brand?.tenThuongHieu || '';
        $('brandQuocGia').value = brand?.quocGia || '';
        $('brandMoTa').value = brand?.moTa || '';
        $('brandModal').showModal();
    }

    async function handleBrandSubmit(e) {
        e.preventDefault();
        showBoxError('brandFormError', '');
        const payload = {
            tenThuongHieu: $('brandTenThuongHieu').value.trim(),
            quocGia: $('brandQuocGia').value.trim(),
            moTa: $('brandMoTa').value.trim()
        };
        try {
            if (state.editingBrandId) {
                await apiRequest(`/brands/${state.editingBrandId}`, {
                    method: 'PUT',
                    body: JSON.stringify(payload)
                });
                showLiveStatus('Đã cập nhật thương hiệu thành công.');
            } else {
                await apiRequest('/brands', {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                showLiveStatus('Đã thêm thương hiệu mới thành công.');
            }
            $('brandModal').close();
            await loadAllData();
        } catch (err) {
            showBoxError('brandFormError', err.message);
        }
    }

    function bindEvents() {
        document.querySelectorAll('.tab-btn').forEach((btn) => {
            btn.addEventListener('click', () => switchTab(btn.dataset.tab));
        });
        $('btnViewTable').addEventListener('click', () => setViewMode('table'));
        $('btnViewGrid').addEventListener('click', () => setViewMode('grid'));
        $('btnRefreshAll').addEventListener('click', loadAllData);
        $('btnSidebarReload').addEventListener('click', loadAllData);
        $('btnOpenCreateProduct').addEventListener('click', openCreateProductModal);

        $('productFilterForm').addEventListener('submit', (e) => {
            e.preventDefault();
            loadProducts();
        });
        ['filterBrand', 'filterCategory', 'filterStatus', 'filterRam', 'filterRom', 'filterSort'].forEach((id) => {
            $(id).addEventListener('change', loadProducts);
        });
        $('btnResetFilters').addEventListener('click', () => {
            $('productFilterForm').reset();
            loadProducts();
        });

        let searchTimer;
        $('filterKeyword').addEventListener('input', () => {
            clearTimeout(searchTimer);
            searchTimer = setTimeout(loadProducts, 280);
        });

        let catTimer;
        $('catSearchInput').addEventListener('input', () => {
            clearTimeout(catTimer);
            catTimer = setTimeout(loadCategories, 250);
        });
        let brandTimer;
        $('brandSearchInput').addEventListener('input', () => {
            clearTimeout(brandTimer);
            brandTimer = setTimeout(loadBrands, 250);
        });

        // Product action delegation (table & card grid)
        ['productsTableBody', 'productGridWrapper'].forEach((id) => {
            $(id).addEventListener('click', (e) => {
                const btn = e.target.closest('button[data-action]');
                if (!btn) return;
                const maSp = btn.dataset.id;
                const action = btn.dataset.action;
                if (action === 'detail') openProductDetailModal(maSp);
                else if (action === 'edit') openEditProductModal(maSp);
                else if (action === 'toggle-status') toggleProductStatus(maSp, btn.dataset.status);
                else if (action === 'delete') {
                    const p = state.products.find((item) => item.maSanPham === maSp);
                    openDeleteConfirm('product', maSp, p ? p.tenSanPham : maSp);
                }
            });
        });

        // Category & Brand table actions
        $('categoriesTableBody').addEventListener('click', (e) => {
            const editBtn = e.target.closest('button[data-cat-edit]');
            if (editBtn) {
                const cat = state.categories.find((c) => c.maDanhMuc === Number(editBtn.dataset.catEdit));
                if (cat) openCategoryModal(cat);
                return;
            }
            const delBtn = e.target.closest('button[data-cat-del]');
            if (delBtn) {
                const cat = state.categories.find((c) => c.maDanhMuc === Number(delBtn.dataset.catDel));
                if (cat) openDeleteConfirm('category', cat.maDanhMuc, cat.tenDanhMuc);
            }
        });
        $('brandsTableBody').addEventListener('click', (e) => {
            const editBtn = e.target.closest('button[data-brand-edit]');
            if (editBtn) {
                const b = state.brands.find((x) => x.maThuongHieu === Number(editBtn.dataset.brandEdit));
                if (b) openBrandModal(b);
                return;
            }
            const delBtn = e.target.closest('button[data-brand-del]');
            if (delBtn) {
                const b = state.brands.find((x) => x.maThuongHieu === Number(delBtn.dataset.brandDel));
                if (b) openDeleteConfirm('brand', b.maThuongHieu, b.tenThuongHieu);
            }
        });

        // Product modal controls
        $('btnCloseProductModal').addEventListener('click', () => $('productModal').close());
        $('btnCancelProductModal').addEventListener('click', () => $('productModal').close());
        $('btnAddVariantRow').addEventListener('click', () => addVariantRow({ dinhMucTonToiThieu: 2 }));
        $('productImageFiles').addEventListener('change', (e) => handleUploadSelectedFiles(e.target.files));
        $('btnAddManualImage').addEventListener('click', () => {
            const url = $('inputManualImageUrl').value.trim();
            if (!url) return;
            if (!state.galleryUrls.includes(url)) state.galleryUrls.push(url);
            if (!state.primaryImageUrl) state.primaryImageUrl = url;
            $('inputManualImageUrl').value = '';
            renderGalleryPreviews();
        });
        $('galleryPreviewList').addEventListener('click', (e) => {
            const priBtn = e.target.closest('button[data-gallery-primary]');
            if (priBtn) {
                state.primaryImageUrl = state.galleryUrls[Number(priBtn.dataset.galleryPrimary)] || '';
                renderGalleryPreviews();
                return;
            }
            const remBtn = e.target.closest('button[data-gallery-remove]');
            if (remBtn) {
                const idx = Number(remBtn.dataset.galleryRemove);
                const removed = state.galleryUrls.splice(idx, 1)[0];
                if (removed === state.primaryImageUrl) {
                    state.primaryImageUrl = state.galleryUrls[0] || '';
                }
                renderGalleryPreviews();
            }
        });
        $('productForm').addEventListener('submit', handleProductSubmit);

        // Detail modal controls
        $('btnCloseDetailModal').addEventListener('click', () => $('detailModal').close());
        $('btnDetailDone').addEventListener('click', () => $('detailModal').close());
        $('detailGalleryThumbs').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-detail-thumb]');
            if (btn) $('detailMainImage').src = resolveImageUrl(btn.dataset.detailThumb);
        });
        $('detailSiblingVariants').addEventListener('click', (e) => {
            const btn = e.target.closest('button[data-switch-variant]');
            if (btn) openProductDetailModal(btn.dataset.switchVariant);
        });
        $('btnDetailEdit').addEventListener('click', () => {
            const ma = state.currentDetailProduct?.maSanPham;
            $('detailModal').close();
            if (ma) openEditProductModal(ma);
        });
        $('btnDetailToggleStatus').addEventListener('click', () => {
            if (state.currentDetailProduct) {
                toggleProductStatus(state.currentDetailProduct.maSanPham, state.currentDetailProduct.trangThai);
            }
        });

        // Category & Brand modals
        $('btnOpenCreateCategory').addEventListener('click', () => openCategoryModal(null));
        $('btnCloseCategoryModal').addEventListener('click', () => $('categoryModal').close());
        $('btnCancelCategoryModal').addEventListener('click', () => $('categoryModal').close());
        $('categoryForm').addEventListener('submit', handleCategorySubmit);

        $('btnOpenCreateBrand').addEventListener('click', () => openBrandModal(null));
        $('btnCloseBrandModal').addEventListener('click', () => $('brandModal').close());
        $('btnCancelBrandModal').addEventListener('click', () => $('brandModal').close());
        $('brandForm').addEventListener('submit', handleBrandSubmit);

        // Delete confirm modal
        $('btnCancelDelete').addEventListener('click', () => $('confirmDeleteModal').close());
        $('btnConfirmDelete').addEventListener('click', executeConfirmedDelete);
    }

    document.addEventListener('DOMContentLoaded', () => {
        bindEvents();
        loadAllData();
    });
})();
