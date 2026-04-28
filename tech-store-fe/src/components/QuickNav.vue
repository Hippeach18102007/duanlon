<template>
  <div class="qnav-wrap">
    <button class="qnav-trigger" :class="{ open: isOpen }" @click="toggleOpen" :title="isOpen ? 'Đóng' : 'Điều hướng nhanh (Ctrl+K)'">
      <span class="qnav-trigger-icon">{{ isOpen ? '✕' : '🧭' }}</span>
      <span class="qnav-trigger-label" v-if="!isOpen">Quick Nav</span>
    </button>
    <transition name="qnav-pop">
      <div v-if="isOpen" class="qnav-panel" @click.stop>
        <div class="qnav-search-wrap">
          <span class="qnav-search-icon">🔍</span>
          <input ref="searchInput" v-model="query" class="qnav-search" placeholder="Tìm chức năng... (Ctrl+K)"
            @keydown.escape="isOpen = false" @keydown.enter="goFirst"
            @keydown.down.prevent="moveDown" @keydown.up.prevent="moveUp" />
          <button v-if="query" class="qnav-clear" @click="query = ''; searchInput?.focus()">✕</button>
        </div>
        <div class="qnav-tabs">
          <button v-for="pg in pages" :key="pg.key" class="qnav-tab" :class="{ active: activePage === pg.key }"
            @click="activePage = pg.key; query = ''; searchInput?.focus()">{{ pg.label }}</button>
        </div>
        <transition name="qnav-toast">
          <div v-if="feedback" class="qnav-feedback" :class="`qnav-feedback-${feedbackType}`">{{ feedback }}</div>
        </transition>
        <div class="qnav-list" ref="listEl">
          <template v-if="filteredItems.length">
            <div v-for="(item, idx) in filteredItems" :key="item.id" class="qnav-item"
              :class="{ focused: focusedIdx === idx, 'qnav-item-group': item.isGroup, 'qnav-item-crosspage': item.crossPage }"
              @click="item.isGroup ? null : navigate(item)" @mouseenter="!item.isGroup && (focusedIdx = idx)">
              <template v-if="item.isGroup">
                <span class="qnav-group-label">{{ item.label }}</span>
              </template>
              <template v-else-if="item.crossPage">
                <span class="qnav-item-icon">{{ item.icon }}</span>
                <div class="qnav-item-body">
                  <span class="qnav-item-name">{{ item.name }}</span>
                  <span class="qnav-item-desc">{{ item.desc }}</span>
                </div>
                <span class="qnav-item-badge">↗ {{ item.crossPage }}</span>
              </template>
              <template v-else>
                <span class="qnav-item-icon">{{ item.icon }}</span>
                <div class="qnav-item-body">
                  <span class="qnav-item-name">{{ item.name }}</span>
                  <span class="qnav-item-desc">{{ item.desc }}</span>
                </div>
                <span class="qnav-item-arrow">→</span>
              </template>
            </div>
          </template>
          <div v-else class="qnav-empty">
            <span>😕</span>
            <span>Không tìm thấy "{{ query }}"</span>
            <button class="qnav-empty-clear" @click="query = ''">Xóa tìm kiếm</button>
          </div>
        </div>
        <div class="qnav-hint">↑↓ di chuyển · Enter chọn · Esc đóng · Ctrl+K mở/đóng</div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from "vue";

const props = defineProps({
  page:            { type: String,   default: "pricing" },
  onSwitchTab:     { type: Function, default: null },
  onTriggerAction: { type: Function, default: null },
});

const isOpen      = ref(false);
const query       = ref("");
const focusedIdx  = ref(0);
const activePage  = ref(props.page);
const searchInput = ref(null);
const listEl      = ref(null);
const feedback     = ref("");
const feedbackType = ref("info");
let feedbackTimer  = null;

const pages = [
  { key: "pricing",   label: "💲 Quản lý giá" },
  { key: "promotion", label: "🏷️ Khuyến mãi" },
];

const allItems = [
  // ══ PRICING ══
  { id: "p-g1",         page: "pricing",   isGroup: true, label: "ĐẶT GIÁ & HIỂN THỊ" },
  { id: "p-set",        page: "pricing",   icon: "💾", name: "Tạo / đặt giá variant",             desc: "Thiết lập giá bán & giá nhập (POST /variants/{id})",  action: { type: "tab", key: "set" } },
  { id: "p-effective",  page: "pricing",   icon: "⚡", name: "Giá hiệu lực (effective price)",     desc: "Xem giá cuối cùng áp dụng cho variant",               action: { type: "tab", key: "set",     scroll: "p-anchor-effective",  scrollDelay: 130 } },
  { id: "p-custprice",  page: "pricing",   icon: "👤", name: "Giá theo khách hàng",                desc: "Giá hiệu lực theo nhóm / VIP tier",                   action: { type: "tab", key: "set",     scroll: "p-anchor-custprice",  scrollDelay: 130 } },
  { id: "p-prodlist",   page: "pricing",   icon: "📦", name: "Giá theo sản phẩm (danh sách)",     desc: "Tất cả variant + giá của 1 sản phẩm",                 action: { type: "tab", key: "history", scroll: "p-anchor-prodprices", scrollDelay: 160 } },
  { id: "p-prodpage",   page: "pricing",   icon: "🛍️", name: "Giá trên danh sách sản phẩm",       desc: "Xem giá & KM trên trang Products",      crossPage: "Products", action: { type: "cross" } },
  { id: "p-currency",   page: "pricing",   icon: "💱", name: "Thiết lập tiền tệ mặc định",         desc: "GET/PUT /api/settings/currency/default", crossPage: "Settings - Currency", action: { type: "cross", route: "/settings/currency" } },

  { id: "p-g2",         page: "pricing",   isGroup: true, label: "LỊCH SỬ & CHỈNH SỬA GIÁ" },
  { id: "p-history",    page: "pricing",   icon: "📋", name: "Lịch sử giá variant",                desc: "Xem lịch sử thay đổi giá theo Variant ID",            action: { type: "tab", key: "history", scroll: "p-anchor-history",   scrollDelay: 160 } },
  { id: "p-editprice",  page: "pricing",   icon: "✏️", name: "Chỉnh sửa giá (sửa history record)", desc: "PUT /prices/history/{id}",                            action: { type: "tab", key: "history", scroll: "p-anchor-history",   scrollDelay: 160, tip: "Tải lịch sử variant → bấm nút Sửa trên dòng cần chỉnh sửa" } },
  { id: "p-delprice",   page: "pricing",   icon: "🗑️", name: "Xóa giá / rollback về giá trước",    desc: "DELETE /prices/history/{id} — tự rollback",           action: { type: "tab", key: "history", scroll: "p-anchor-history",   scrollDelay: 160, tip: "Tải lịch sử variant → bấm nút Xóa trên dòng cần xóa (rollback tự động về bản ghi trước)" } },
  { id: "p-promoend",   page: "pricing",   icon: "🔄", name: "Cập nhật giá sau khuyến mãi",        desc: "Đặt giá mới khi KM kết thúc (PROMO_ENDED)",           action: { type: "tab", key: "warning", scroll: "p-anchor-promoend",  scrollDelay: 160 } },

  { id: "p-g3",         page: "pricing",   isGroup: true, label: "CẢNH BÁO & XUNG ĐỘT" },
  { id: "p-costwarn",   page: "pricing",   icon: "⚠️", name: "Cảnh báo giá dưới giá nhập",        desc: "Kiểm tra variant bán thấp hơn cost",                  action: { type: "tab", key: "warning" } },
  { id: "p-conflicts",  page: "pricing",   icon: "🔀", name: "Xung đột giá toàn hệ thống",         desc: "Quét tất cả variant có giá bán < giá nhập",           action: { type: "tab", key: "conflicts" } },

  { id: "p-g4",         page: "pricing",   isGroup: true, label: "TÍNH GIÁ & DASHBOARD" },
  { id: "p-calculate",  page: "pricing",   icon: "🧮", name: "Tính giá đơn hàng (calculate-order)", desc: "Tính tổng tiền với KM & combo theo giỏ hàng",        action: { type: "tab", key: "calculate" } },
  { id: "p-dashboard",  page: "pricing",   icon: "📊", name: "Dashboard giá & khuyến mãi",         desc: "Tổng quan: KM active, xung đột, sắp hết hạn",        action: { type: "tab", key: "dashboard" } },
  { id: "p-orderapply", page: "pricing",   icon: "🧾", name: "Áp dụng KM tự động khi tạo đơn",    desc: "Dùng promotionCode khi tạo order",       crossPage: "Orders", action: { type: "cross" } },

  // ══ PROMOTION ══
  { id: "pm-g1",        page: "promotion", isGroup: true, label: "QUẢN LÝ KHUYẾN MÃI" },
  { id: "pm-create",    page: "promotion", icon: "➕", name: "Tạo khuyến mãi mới",               desc: "KM % giảm, cố định, combo, nhóm KH",                  action: { type: "action", key: "openCreate" } },
  { id: "pm-list",      page: "promotion", icon: "📋", name: "Danh sách khuyến mãi",             desc: "Xem toàn bộ hoặc lọc chỉ ACTIVE trong bảng",          action: { type: "action", key: "scrollTable" } },
  { id: "pm-edit",      page: "promotion", icon: "✏️", name: "Chỉnh sửa khuyến mãi",             desc: "PUT /promotions/{id}",                                 action: { type: "action", key: "scrollTable", tip: "Tìm KM trong bảng → bấm nút Sửa ở cột Action" } },
  { id: "pm-del",       page: "promotion", icon: "🗑️", name: "Xóa khuyến mãi (soft deactivate)", desc: "DELETE /promotions/{id} — tắt isActive",              action: { type: "action", key: "scrollTable", tip: "Tìm KM trong bảng → bấm nút Xóa ở cột Action" } },
  { id: "pm-validate",  page: "promotion", icon: "🔍", name: "Validate mã khuyến mãi",           desc: "Kiểm tra mã & tính tiền giảm theo tổng đơn",          action: { type: "action", key: "scrollValidate" } },
  { id: "pm-preview",   page: "promotion", icon: "👁️", name: "Preview giá sau khi áp dụng KM",  desc: "Xem giá sản phẩm khi KM được áp dụng",                action: { type: "action", key: "scrollTable", tip: "Tìm KM trong bảng → bấm nút Preview ở cột Action" } },
  { id: "pm-detail",    page: "promotion", icon: "🔎", name: "Hiển thị chi tiết khuyến mãi",     desc: "GET /promotions/{id}",                                 action: { type: "action", key: "scrollTable", tip: "Tìm KM trong bảng → bấm Preview để xem chi tiết" } },

  { id: "pm-g2",        page: "promotion", isGroup: true, label: "COMBO & NHÓM KHÁCH" },
  { id: "pm-combo",     page: "promotion", icon: "🎁", name: "Combo (Mua X tặng Y)",             desc: "Điền buyQty / getQty trong mục Combo khi tạo KM",     action: { type: "action", key: "openCreate", hint: "combo" } },
  { id: "pm-vip",       page: "promotion", icon: "👑", name: "KM theo nhóm khách hàng",          desc: "Giới hạn theo customerTypes / vipTiers",              action: { type: "action", key: "openCreate", hint: "vip" } },
  { id: "pm-priority",  page: "promotion", icon: "🎯", name: "Ưu tiên khuyến mãi (priority)",   desc: "Thiết lập thứ tự ưu tiên khi nhiều KM trùng nhau",   action: { type: "action", key: "openCreate", tip: "Trường Priority trong form tạo / sửa khuyến mãi" } },
  { id: "pm-limit",     page: "promotion", icon: "🔢", name: "Giới hạn số lần áp dụng",          desc: "usageLimit khi tạo / sửa KM",                         action: { type: "action", key: "openCreate", tip: "Trường Giới hạn lượt dùng trong form tạo / sửa" } },
  { id: "pm-orderapply",page: "promotion", icon: "🧾", name: "Áp dụng KM tự động khi tạo đơn",  desc: "Dùng promotionCode khi tạo order",       crossPage: "Orders", action: { type: "cross" } },

  { id: "pm-g3",        page: "promotion", isGroup: true, label: "CẢNH BÁO & BÁO CÁO" },
  { id: "pm-conflicts", page: "promotion", icon: "⚠️", name: "Xung đột khuyến mãi",             desc: "Phát hiện KM chồng chéo phạm vi & thời gian",         action: { type: "action", key: "loadConflicts" } },
  { id: "pm-expiring",  page: "promotion", icon: "⏰", name: "KM sắp hết hạn (3 ngày)",          desc: "Cảnh báo danh sách KM hết hạn sắp tới",              action: { type: "action", key: "loadExpiring" } },
  { id: "pm-active",    page: "promotion", icon: "📋", name: "Báo cáo KM đang áp dụng",         desc: "GET /api/reports/promotions/active",                   action: { type: "action", key: "loadActiveReport" } },
  { id: "pm-report",    page: "promotion", icon: "📊", name: "Báo cáo KM (tổng hợp)",           desc: "Tổng quan + tỉ lệ hoạt động theo tuần / tháng",       action: { type: "action", key: "loadCombinedReport" } },
];

const filteredItems = computed(() => {
  const q   = query.value.trim().toLowerCase();
  const src = allItems.filter(i => i.page === activePage.value);
  if (!q) return src;
  return src.filter(i => !i.isGroup && (i.name.toLowerCase().includes(q) || i.desc.toLowerCase().includes(q)));
});

watch(filteredItems, () => { focusedIdx.value = firstNavIdx(); });
watch(isOpen, async (val) => {
  if (val) {
    activePage.value = props.page;
    query.value      = "";
    feedback.value   = "";
    focusedIdx.value = firstNavIdx();
    await nextTick();
    searchInput.value?.focus();
  }
});

function onKeydown(e) {
  if ((e.ctrlKey || e.metaKey) && e.key === "k") { e.preventDefault(); isOpen.value = !isOpen.value; }
  if (e.key === "Escape" && isOpen.value) isOpen.value = false;
}
onMounted(()  => window.addEventListener("keydown", onKeydown));
onUnmounted(() => window.removeEventListener("keydown", onKeydown));

function firstNavIdx() { return Math.max(0, filteredItems.value.findIndex(i => !i.isGroup)); }

function showFeedback(msg, type = "info", ms = 3000) {
  clearTimeout(feedbackTimer);
  feedback.value = msg; feedbackType.value = type;
  feedbackTimer = setTimeout(() => { feedback.value = ""; }, ms);
}

function moveDown() {
  const items = filteredItems.value; let idx = focusedIdx.value;
  do { idx = (idx + 1) % items.length; } while (items[idx]?.isGroup);
  focusedIdx.value = idx; scrollFocused();
}
function moveUp() {
  const items = filteredItems.value; let idx = focusedIdx.value;
  do { idx = (idx - 1 + items.length) % items.length; } while (items[idx]?.isGroup);
  focusedIdx.value = idx; scrollFocused();
}
function scrollFocused() {
  nextTick(() => {
    const rows = listEl.value?.querySelectorAll(".qnav-item:not(.qnav-item-group)");
    const navIdx = filteredItems.value.filter(i => !i.isGroup).indexOf(filteredItems.value[focusedIdx.value]);
    rows?.[navIdx]?.scrollIntoView({ block: "nearest" });
  });
}
function goFirst() {
  const item = filteredItems.value[focusedIdx.value];
  if (item && !item.isGroup) navigate(item);
}

function navigate(item) {
  if (item.action.type === "cross") {
    const routeHint = item.action.route ? ` (${item.action.route})` : "";
    showFeedback(`↗ Chức năng này nằm trên trang ${item.crossPage}${routeHint}`, "info", 5000);
    return;
  }
  isOpen.value = false; query.value = "";
  if (item.action.type === "tab")    doTabNav(item);
  if (item.action.type === "action") doActionNav(item);
}

function doTabNav(item) {
  if (!props.onSwitchTab) return;
  props.onSwitchTab(item.action.key);
  if (item.action.tip) setTimeout(() => showFeedback("💡 " + item.action.tip, "info", 4500), 200);
  if (!item.action.scroll) return;
  const delay = item.action.scrollDelay ?? 180;
  setTimeout(() => {
    const el = document.getElementById(item.action.scroll);
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "start" });
      el.classList.add("qnav-highlight");
      setTimeout(() => el.classList.remove("qnav-highlight"), 2000);
    } else {
      showFeedback("✅ Đã chuyển tab — kéo xuống để tìm mục", "ok", 2500);
    }
  }, delay);
}

function doActionNav(item) {
  if (!props.onTriggerAction) return;
  props.onTriggerAction(item.action.key, item.action.hint);
  if (item.action.tip) setTimeout(() => showFeedback("💡 " + item.action.tip, "info", 4500), 150);
}

function toggleOpen() { isOpen.value = !isOpen.value; }
</script>

<style scoped>
.qnav-wrap {
  position: fixed; bottom: 28px; right: 28px; z-index: 9999;
  display: flex; flex-direction: column; align-items: flex-end; gap: 10px;
  font-family: 'DM Sans', sans-serif;
}
.qnav-trigger {
  display: flex; align-items: center; gap: 7px;
  padding: 0 18px 0 14px; height: 44px; border-radius: 22px;
  border: 1.5px solid rgba(79,110,247,.28);
  background: rgba(255,255,255,.88);
  backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px);
  box-shadow: 0 4px 24px rgba(79,110,247,.18), 0 1px 6px rgba(79,110,247,.1);
  cursor: pointer; color: #4f6ef7; font-size: 13.5px; font-weight: 600;
  transition: all .18s ease; white-space: nowrap; user-select: none;
}
.qnav-trigger:hover { background: rgba(79,110,247,.1); box-shadow: 0 8px 32px rgba(79,110,247,.26); transform: translateY(-1px); }
.qnav-trigger.open  { background: rgba(79,110,247,.12); border-color: rgba(79,110,247,.5); padding: 0 14px; }
.qnav-trigger-icon  { font-size: 17px; line-height: 1; }

.qnav-panel {
  width: 380px; max-height: 570px; display: flex; flex-direction: column;
  border-radius: 16px; border: 1.5px solid rgba(255,255,255,.75);
  background: rgba(255,255,255,.92); backdrop-filter: blur(28px); -webkit-backdrop-filter: blur(28px);
  box-shadow: 0 24px 64px rgba(79,110,247,.2), 0 4px 16px rgba(79,110,247,.1), 0 1px 0 rgba(255,255,255,.9) inset;
  overflow: hidden;
}
.qnav-search-wrap {
  display: flex; align-items: center; gap: 8px;
  padding: 12px 14px 10px; border-bottom: 1px solid rgba(79,110,247,.08); flex-shrink: 0;
}
.qnav-search-icon { font-size: 14px; opacity: .45; flex-shrink: 0; }
.qnav-search      { flex: 1; border: none; outline: none; background: transparent; font-size: 13.5px; font-family: inherit; color: #1a1f36; }
.qnav-search::placeholder { color: #9ba3c8; }
.qnav-clear { border: none; background: none; cursor: pointer; color: #9ba3c8; font-size: 12px; padding: 2px 4px; border-radius: 4px; line-height: 1; transition: all .12s; }
.qnav-clear:hover { color: #ff4d6a; background: rgba(255,77,106,.08); }

.qnav-tabs { display: flex; gap: 4px; padding: 8px 10px 6px; border-bottom: 1px solid rgba(79,110,247,.07); flex-shrink: 0; }
.qnav-tab  { flex: 1; padding: 5px 6px; border-radius: 8px; border: 1.5px solid transparent; background: transparent; font-size: 12px; font-family: inherit; font-weight: 500; color: #9ba3c8; cursor: pointer; transition: all .15s; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qnav-tab:hover  { background: rgba(79,110,247,.07); color: #4f6ef7; }
.qnav-tab.active { background: rgba(79,110,247,.1); border-color: rgba(79,110,247,.25); color: #4f6ef7; font-weight: 600; }

.qnav-feedback      { margin: 6px 10px 0; padding: 7px 12px; border-radius: 8px; font-size: 12px; line-height: 1.4; flex-shrink: 0; }
.qnav-feedback-info { background: rgba(79,110,247,.09); color: #3a56d4; border: 1px solid rgba(79,110,247,.18); }
.qnav-feedback-ok   { background: rgba(0,200,150,.09);  color: #007a5a; border: 1px solid rgba(0,200,150,.22); }
.qnav-feedback-warn { background: rgba(255,140,66,.09); color: #b85a0c; border: 1px solid rgba(255,140,66,.22); }

.qnav-list { flex: 1; overflow-y: auto; padding: 6px 0; scrollbar-width: thin; scrollbar-color: rgba(79,110,247,.18) transparent; min-height: 0; }
.qnav-list::-webkit-scrollbar       { width: 4px; }
.qnav-list::-webkit-scrollbar-thumb { background: rgba(79,110,247,.18); border-radius: 4px; }

.qnav-item-group  { padding: 10px 16px 3px; cursor: default; }
.qnav-group-label { font-size: 10px; font-weight: 700; letter-spacing: .09em; color: #9ba3c8; text-transform: uppercase; }

.qnav-item { display: flex; align-items: center; gap: 10px; padding: 7px 14px; cursor: pointer; transition: background .1s; }
.qnav-item:not(.qnav-item-group):hover,
.qnav-item:not(.qnav-item-group).focused { background: rgba(79,110,247,.08); }
.qnav-item-icon  { font-size: 15px; width: 22px; text-align: center; flex-shrink: 0; line-height: 1; }
.qnav-item-body  { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.qnav-item-name  { font-size: 12.5px; font-weight: 600; color: #1a1f36; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qnav-item-desc  { font-size: 11px; color: #9ba3c8; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.qnav-item-arrow { font-size: 12px; color: #c4cadf; flex-shrink: 0; transition: all .13s; }
.qnav-item:not(.qnav-item-group):hover .qnav-item-arrow,
.qnav-item:not(.qnav-item-group).focused .qnav-item-arrow { color: #4f6ef7; transform: translateX(3px); }

.qnav-item-crosspage { opacity: .72; }
.qnav-item-crosspage:hover,
.qnav-item-crosspage.focused { opacity: 1; background: rgba(155,109,255,.07) !important; }
.qnav-item-badge { font-size: 10.5px; font-weight: 600; color: #9b6dff; background: rgba(155,109,255,.1); border: 1px solid rgba(155,109,255,.2); border-radius: 5px; padding: 2px 6px; white-space: nowrap; flex-shrink: 0; }

.qnav-empty       { padding: 28px 16px; display: flex; flex-direction: column; align-items: center; gap: 6px; font-size: 13px; color: #9ba3c8; }
.qnav-empty-clear { margin-top: 4px; border: 1px solid rgba(79,110,247,.2); background: rgba(79,110,247,.07); color: #4f6ef7; padding: 4px 12px; border-radius: 6px; font-size: 12px; cursor: pointer; font-family: inherit; transition: all .13s; }
.qnav-empty-clear:hover { background: rgba(79,110,247,.14); }

.qnav-hint { padding: 7px 14px; border-top: 1px solid rgba(79,110,247,.07); font-size: 10.5px; color: #c4cadf; text-align: center; letter-spacing: .02em; flex-shrink: 0; }

:global(.qnav-highlight) { outline: 2.5px solid rgba(79,110,247,.55) !important; outline-offset: 3px; border-radius: 10px; animation: qnav-ring 2s ease forwards; }
@keyframes qnav-ring { 0% { outline-color: rgba(79,110,247,.7); } 70% { outline-color: rgba(79,110,247,.35); } 100% { outline-color: transparent; } }

.qnav-pop-enter-active  { animation: qnavIn .2s cubic-bezier(.22,1,.36,1); }
.qnav-pop-leave-active  { animation: qnavIn .14s cubic-bezier(.22,1,.36,1) reverse; }
@keyframes qnavIn { from { opacity: 0; transform: scale(.93) translateY(12px); transform-origin: bottom right; } to { opacity: 1; transform: scale(1) translateY(0); } }
.qnav-toast-enter-active { animation: qnavToast .18s ease; }
.qnav-toast-leave-active { animation: qnavToast .18s ease reverse; }
@keyframes qnavToast { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
</style>