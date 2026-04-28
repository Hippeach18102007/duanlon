<template>
  <div class="sa-root">

    <!-- ── Header ─────────────────────────────────────────────── -->
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
          Analytics
        </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Customer Spending Analytics</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Analyze customer spending patterns and top performers</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <button class="sa-btn-reload" @click="loadAll" :disabled="loading">
          <svg :class="{ spin: loading }" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M23 4v6h-6M1 20v-6h6"/>
            <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"/>
          </svg>
          Reload
        </button>
      </div>
    </div>

    <!-- ── KPI Cards ───────────────────────────────────────────── -->
    <div class="sa-kpi-row">
      <div class="sa-kpi" style="--c:#3b82f6;--bg:#eff6ff">
        <div class="sa-kpi-icon" style="background:#eff6ff;color:#3b82f6">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
        </div>
        <div class="sa-kpi-body">
          <div class="sa-kpi-label">Total Spent</div>
          <div class="sa-kpi-value">{{ formatCurrency(stats.totalSpent) }}</div>
        </div>
      </div>
      <div class="sa-kpi" style="--c:#10b981;--bg:#ecfdf5">
        <div class="sa-kpi-icon" style="background:#ecfdf5;color:#10b981">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/></svg>
        </div>
        <div class="sa-kpi-body">
          <div class="sa-kpi-label">Total Customers</div>
          <div class="sa-kpi-value">{{ (stats.totalCustomers || 0).toLocaleString() }}</div>
        </div>
      </div>
      <div class="sa-kpi" style="--c:#f59e0b;--bg:#fffbeb">
        <div class="sa-kpi-icon" style="background:#fffbeb;color:#f59e0b">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>
        </div>
        <div class="sa-kpi-body">
          <div class="sa-kpi-label">Average Spent</div>
          <div class="sa-kpi-value">{{ formatCurrency(stats.averageSpent) }}</div>
        </div>
      </div>
      <div class="sa-kpi" style="--c:#8b5cf6;--bg:#f5f3ff">
        <div class="sa-kpi-icon" style="background:#f5f3ff;color:#8b5cf6">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9H4.5a2.5 2.5 0 010-5H6"/><path d="M18 9h1.5a2.5 2.5 0 000-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0012 0V2z"/></svg>
        </div>
        <div class="sa-kpi-body">
          <div class="sa-kpi-label">Top Spender</div>
          <div class="sa-kpi-value sm">{{ topSpenders[0]?.fullName || 'N/A' }}</div>
        </div>
      </div>
    </div>

    <!-- ── Tabs ────────────────────────────────────────────────── -->
    <div class="sa-tabs">
      <button v-for="tab in tabList" :key="tab.id"
        class="sa-tab" :class="{ active: activeTab === tab.id }"
        @click="switchTab(tab.id)">
        <span v-html="tab.icon"></span>
        {{ tab.label }}
      </button>
    </div>

    <!-- ── Tab: Top Spenders ───────────────────────────────────── -->
    <div v-if="activeTab === 'topSpenders'" class="sa-panel">
      <div class="sa-filter-bar">
        <div class="sa-filter-group">
          <label class="sa-label">Limit</label>
          <div class="sa-number-input">
            <button @click="topSpendersLimit = Math.max(5, topSpendersLimit - 5)">−</button>
            <span>{{ topSpendersLimit }}</span>
            <button @click="topSpendersLimit = Math.min(100, topSpendersLimit + 5)">+</button>
          </div>
        </div>
        <div class="sa-filter-group">
          <label class="sa-label">VIP Tier</label>
          <select v-model="topSpendersTier" class="sa-select">
            <option value="">All Tiers</option>
            <option v-for="t in tiers" :key="t" :value="t">{{ t }}</option>
          </select>
        </div>
        <button class="sa-btn-primary" @click="loadTopSpenders" :disabled="loadingTopSpenders">
          <span v-if="loadingTopSpenders" class="spinner-sm"></span>
          <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          Load
        </button>
      </div>

      <div class="sa-table-wrap">
        <div v-if="loadingTopSpenders" class="sa-table-skeleton">
          <div v-for="i in 5" :key="i" class="sk-row">
            <div class="sk sk-rank"></div>
            <div class="sk-user"><div class="sk sk-av"></div><div class="sk-lines"><div class="sk sk-l w70"></div><div class="sk sk-l w50"></div></div></div>
            <div class="sk sk-pill"></div>
            <div class="sk sk-money"></div>
            <div class="sk sk-pill"></div>
          </div>
        </div>
        <table v-else-if="topSpenders.length" class="sa-table">
          <thead>
            <tr>
              <th style="width:70px;text-align:center">Rank</th>
              <th>Customer</th>
              <th style="width:110px;text-align:center">VIP Tier</th>
              <th style="width:180px;text-align:right">Total Spent</th>
              <th style="width:130px;text-align:center">Points</th>
              <th style="width:110px;text-align:center">Type</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in topSpenders" :key="row.id">
              <td style="text-align:center">
                <div class="rank-badge" :class="['rank-' + getRankKey(idx)]">
                  <svg v-if="idx < 3" width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M6 9H4.5a2.5 2.5 0 010-5H6m12 0h1.5a2.5 2.5 0 010 5H18M4 22h16M18 2H6v7a6 6 0 0012 0V2z"/></svg>
                  <span v-else>{{ idx + 1 }}</span>
                </div>
              </td>
              <td>
                <div class="cust-cell">
                  <div class="cust-av" :style="`background:${avatarColor(row.fullName)}`">{{ initials(row.fullName) }}</div>
                  <div>
                    <div class="cust-name">{{ row.fullName }}</div>
                    <div class="cust-email">{{ row.email }}</div>
                  </div>
                </div>
              </td>
              <td style="text-align:center">
                <span v-if="row.raw.vipTier" class="tier-chip" :class="'tier-' + row.raw.vipTier.toLowerCase()">{{ row.raw.vipTier }}</span>
                <span v-else class="muted-dash">—</span>
              </td>
              <td style="text-align:right">
                <span class="spend-amount">{{ formatCurrency(row.raw.totalSpent) }}</span>
              </td>
              <td style="text-align:center">
                <span class="points-badge">⭐ {{ row.loyaltyPoints }}</span>
              </td>
              <td style="text-align:center">
                <span class="type-chip" :class="row.customerType === 'VIP' ? 'type-vip' : 'type-reg'">{{ row.customerType }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="sa-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
          <p>Chưa có dữ liệu</p>
        </div>
      </div>
    </div>

    <!-- ── Tab: Spending Range ─────────────────────────────────── -->
    <div v-else-if="activeTab === 'spendingRange'" class="sa-panel">
      <div class="sa-filter-bar wrap">
        <div class="sa-filter-group">
          <label class="sa-label">Min Spending (₫)</label>
          <input type="number" v-model.number="spendingMin" class="sa-input" placeholder="0" min="0" step="1000000"/>
        </div>
        <div class="sa-filter-group">
          <label class="sa-label">Max Spending (₫)</label>
          <input type="number" v-model.number="spendingMax" class="sa-input" placeholder="999,999,999" min="0" step="1000000"/>
        </div>
        <div class="sa-filter-actions">
          <button class="sa-btn-primary" @click="loadBySpendingRange" :disabled="loadingSpendingRange">
            <span v-if="loadingSpendingRange" class="spinner-sm"></span>
            <svg v-else width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="22 3 2 9 11 13 15 22 22 3"/></svg>
            Apply
          </button>
          <button class="sa-btn-ghost" @click="clearSpendingRange">Clear</button>
        </div>
      </div>

      <!-- Quick Ranges -->
      <div class="sa-quick-ranges">
        <span class="sa-label">Quick:</span>
        <button v-for="q in quickRanges" :key="q.label" class="sa-quick-btn" @click="setQuickRange(q.min, q.max)">
          {{ q.label }}
        </button>
      </div>

      <!-- Active Range Alert -->
      <div class="sa-range-alert" v-if="appliedSpendingRange.min !== null || appliedSpendingRange.max !== null">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <strong>Active Range:</strong>
        {{ formatCurrency(appliedSpendingRange.min || 0) }} – {{ formatCurrency(appliedSpendingRange.max || 999999999) }}
        <span class="range-count-badge">{{ customersBySpending.length }} customers</span>
      </div>

      <div class="sa-table-wrap">
        <div v-if="loadingSpendingRange" class="sa-table-skeleton">
          <div v-for="i in 5" :key="i" class="sk-row">
            <div class="sk sk-id"></div>
            <div class="sk-user"><div class="sk sk-av"></div><div class="sk-lines"><div class="sk sk-l w70"></div><div class="sk sk-l w50"></div></div></div>
            <div class="sk sk-pill"></div>
            <div class="sk sk-money"></div>
          </div>
        </div>
        <table v-else-if="pagedSpending.length" class="sa-table">
          <thead>
            <tr>
              <th style="width:60px">ID</th>
              <th>Customer</th>
              <th style="width:130px">Phone</th>
              <th style="width:110px;text-align:center">VIP Tier</th>
              <th style="width:180px;text-align:right">Total Spent</th>
              <th style="width:120px;text-align:center">Points</th>
              <th style="width:100px;text-align:center">Type</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedSpending" :key="row.id">
              <td class="muted-text">{{ row.id }}</td>
              <td>
                <div class="cust-cell">
                  <div class="cust-av sm" :style="`background:${avatarColor(row.fullName)}`">{{ initials(row.fullName) }}</div>
                  <div>
                    <div class="cust-name">{{ row.fullName }}</div>
                    <div class="cust-email">{{ row.email }}</div>
                  </div>
                </div>
              </td>
              <td class="muted-text">{{ row.phone || '—' }}</td>
              <td style="text-align:center">
                <span v-if="row.raw.vipTier" class="tier-chip" :class="'tier-' + row.raw.vipTier.toLowerCase()">{{ row.raw.vipTier }}</span>
                <span v-else class="muted-dash">—</span>
              </td>
              <td style="text-align:right"><span class="spend-amount">{{ formatCurrency(row.raw.totalSpent) }}</span></td>
              <td style="text-align:center"><span class="points-badge">⭐ {{ row.loyaltyPoints }}</span></td>
              <td style="text-align:center">
                <span class="type-chip" :class="row.customerType === 'VIP' ? 'type-vip' : 'type-reg'">{{ row.customerType }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="sa-empty">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9 9h6M9 12h6M9 15h4"/></svg>
          <p>Chưa có dữ liệu. Hãy nhập khoảng chi tiêu và nhấn Apply.</p>
        </div>
      </div>

      <!-- Pagination -->
      <div class="sa-pagination" v-if="customersBySpending.length > pageSize">
        <button class="page-btn" :disabled="spendingPage === 1" @click="spendingPage--">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <button v-for="p in totalSpendingPages" :key="p"
          class="page-btn" :class="{ active: p === spendingPage }"
          @click="spendingPage = p">{{ p }}</button>
        <button class="page-btn" :disabled="spendingPage === totalSpendingPages" @click="spendingPage++">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
        <span class="page-info">{{ (spendingPage-1)*pageSize+1 }}–{{ Math.min(spendingPage*pageSize, customersBySpending.length) }} / {{ customersBySpending.length }}</span>
      </div>
    </div>

    <!-- ── Tab: Statistics ─────────────────────────────────────── -->
    <div v-else-if="activeTab === 'statistics'" class="sa-panel">

      <!-- Distribution chart -->
      <div class="sa-card mb-14">
        <div class="sa-card-header">
          <div class="sa-card-title">Distribution by Spending Range</div>
          <div class="sa-card-sub">Số khách hàng theo mức chi tiêu</div>
        </div>
        <div class="sa-ranges">
          <div v-for="(count, range) in stats.spendingRanges" :key="range" class="sa-range-row">
            <div class="sa-range-label">{{ formatRangeLabel(range) }}</div>
            <div class="sa-range-bar-wrap">
              <div class="sa-range-bar" :style="`width:${getPercentage(count, stats.totalCustomers)}%;background:${getRangeColor(range)}`"></div>
            </div>
            <div class="sa-range-pct">{{ getPercentage(count, stats.totalCustomers) }}%</div>
            <div class="sa-range-count">{{ count }} khách</div>
          </div>
        </div>
      </div>

      <!-- Tier stats grid -->
      <div class="sa-tier-grid">
        <div v-for="(tierData, tier) in stats.byTier" :key="tier" class="sa-tier-card">
          <div class="sa-tier-card-top">
            <span class="tier-chip" :class="'tier-' + tier.toLowerCase()">{{ tier }}</span>
            <span class="sa-tier-count">{{ tierData.customerCount }} khách</span>
          </div>
          <div class="sa-tier-divider"/>
          <div class="sa-tier-row">
            <div class="sa-tier-lbl">Total Spent</div>
            <div class="sa-tier-val">{{ formatCurrency(tierData.totalSpent) }}</div>
          </div>
          <div class="sa-tier-divider"/>
          <div class="sa-tier-row">
            <div class="sa-tier-lbl">Avg Spent</div>
            <div class="sa-tier-val blue">{{ formatCurrency(tierData.averageSpent) }}</div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { customersApi } from "../../api/customers.api";
import { toast } from "../../ui/toast";

// ── State ──────────────────────────────────────────────────────────
const loading               = ref(false);
const loadingTopSpenders    = ref(false);
const loadingSpendingRange  = ref(false);
const activeTab             = ref("topSpenders");

const topSpenders       = ref([]);
const topSpendersLimit  = ref(10);
const topSpendersTier   = ref("");

const customersBySpending = ref([]);
const spendingMin         = ref(null);
const spendingMax         = ref(null);
const appliedSpendingRange = reactive({ min: null, max: null });
const spendingPage        = ref(1);
const pageSize            = 10;

const stats = ref({ totalSpent: 0, totalCustomers: 0, averageSpent: 0, byTier: {}, spendingRanges: {} });

const tiers = ["BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"];

const tabList = [
  { id: 'topSpenders',   label: 'Top Spenders',    icon: `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9H4.5a2.5 2.5 0 010-5H6m12 0h1.5a2.5 2.5 0 010 5H18M18 2H6v7a6 6 0 0012 0V2z"/></svg>` },
  { id: 'spendingRange', label: 'By Spending Range', icon: `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="22 3 2 9 11 13 15 22 22 3"/></svg>` },
  { id: 'statistics',    label: 'Statistics',       icon: `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>` },
];

const quickRanges = [
  { label: 'Under 1M',   min: 0,        max: 1000000   },
  { label: '1M – 5M',    min: 1000000,  max: 5000000   },
  { label: '5M – 10M',   min: 5000000,  max: 10000000  },
  { label: '10M – 50M',  min: 10000000, max: 50000000  },
  { label: 'Over 50M',   min: 50000000, max: 999999999 },
];

// ── Computed ───────────────────────────────────────────────────────
const pagedSpending = computed(() => {
  const s = (spendingPage.value - 1) * pageSize;
  return customersBySpending.value.slice(s, s + pageSize);
});
const totalSpendingPages = computed(() => Math.ceil(customersBySpending.value.length / pageSize));

// ── Helpers ────────────────────────────────────────────────────────
function extractList(payload) {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  const root = payload?.data ?? payload;
  if (Array.isArray(root)) return root;
  for (const k of ["content", "items", "results", "rows", "list"]) {
    if (Array.isArray(root?.[k])) return root[k];
    if (Array.isArray(root?.data?.[k])) return root.data[k];
  }
  return [];
}

function normalize(list) {
  return (list || []).map(c => ({
    id: c?.id ?? c?.customerId,
    fullName: c?.fullName ?? c?.name ?? "",
    email: c?.email ?? "",
    phone: c?.phone ?? "",
    customerType: (c?.customerType ?? "REGULAR").toString().toUpperCase(),
    loyaltyPoints: c?.loyaltyPoints ?? 0,
    raw: c,
  }));
}

function formatCurrency(value) {
  if (!value) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value);
}

function avatarColor(name) {
  const p = ['#3b82f6','#8b5cf6','#ec4899','#f59e0b','#10b981','#ef4444','#06b6d4','#f97316'];
  const h = (name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return p[h % p.length];
}
function initials(name) {
  if (!name) return '?';
  const p = name.trim().split(' ');
  return p.length === 1 ? p[0].slice(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase();
}
function getRankKey(i) { return ['gold','silver','bronze'][i] || 'other'; }
function formatRangeLabel(r) {
  return { under1M: 'Under 1M', '1M-5M': '1M – 5M', '5M-10M': '5M – 10M', '10M-50M': '10M – 50M', over50M: 'Over 50M' }[r] || r;
}
function getPercentage(count, total) { return total ? Math.round(count / total * 100) : 0; }
function getRangeColor(r) {
  return { under1M: '#9ca3af', '1M-5M': '#3b82f6', '5M-10M': '#10b981', '10M-50M': '#f59e0b', over50M: '#ef4444' }[r] || '#3b82f6';
}

// ── API ────────────────────────────────────────────────────────────
async function loadTopSpenders() {
  loadingTopSpenders.value = true;
  try {
    const res = topSpendersTier.value
      ? await customersApi.listTopSpendersByVipTier(topSpendersTier.value, topSpendersLimit.value)
      : await customersApi.listTopSpenders(topSpendersLimit.value);
    topSpenders.value = normalize(extractList(res?.data));
  } catch { toast("Failed to load top spenders.", "error"); }
  finally { loadingTopSpenders.value = false; }
}

async function loadBySpendingRange() {
  if (spendingMin.value === null && spendingMax.value === null) { toast("Enter at least one value", "warning"); return; }
  if (spendingMin.value != null && spendingMax.value != null && spendingMin.value > spendingMax.value) { toast("Min cannot exceed Max", "error"); return; }
  loadingSpendingRange.value = true;
  try {
    const min = spendingMin.value || 0;
    const max = spendingMax.value || 999999999;
    const res = await customersApi.listBySpendingRange(min, max);
    customersBySpending.value = normalize(extractList(res?.data));
    appliedSpendingRange.min = min;
    appliedSpendingRange.max = max;
    spendingPage.value = 1;
    toast(`Found ${customersBySpending.value.length} customers`, "success");
  } catch { toast("Failed to load customers.", "error"); }
  finally { loadingSpendingRange.value = false; }
}

async function loadStatistics() {
  loading.value = true;
  try { const res = await customersApi.getSpendingStatistics(); stats.value = res?.data || {}; }
  catch { toast("Failed to load statistics.", "error"); }
  finally { loading.value = false; }
}

function clearSpendingRange() {
  spendingMin.value = null; spendingMax.value = null;
  appliedSpendingRange.min = null; appliedSpendingRange.max = null;
  customersBySpending.value = [];
}
function setQuickRange(min, max) { spendingMin.value = min; spendingMax.value = max; loadBySpendingRange(); }
async function loadAll() { await Promise.all([loadTopSpenders(), loadStatistics()]); }
function switchTab(id) {
  activeTab.value = id;
  if (id === 'topSpenders' && !topSpenders.value.length) loadTopSpenders();
  if (id === 'statistics' && !Object.keys(stats.value.byTier || {}).length) loadStatistics();
}

onMounted(loadAll);
</script>

<style scoped>
/* ── Base ──────────────────────────────────────────────────────── */
.sa-root {
  font-family: 'Be Vietnam Pro', 'Segoe UI', sans-serif;
  padding: 32px 36px 80px;
  background: #f5f5f5;
  min-height: 100vh;
  color: #111827;
  box-sizing: border-box;
}
*, *::before, *::after { box-sizing: border-box; }

/* ── Header ────────────────────────────────────────────────────── */
.sa-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 14px;
}
.sa-kicker {
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1.2px;
  color: #3b82f6;
  margin-bottom: 5px;
}
.sa-title {
  font-size: 26px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 4px;
  letter-spacing: -.4px;
}
.sa-sub { font-size: 13px; color: #9ca3af; margin: 0; }

.sa-btn-reload {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 9px 18px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  background: white;
  font-size: 13px; font-weight: 600; color: #374151;
  cursor: pointer; transition: all .15s; font-family: inherit;
}
.sa-btn-reload:hover { border-color: #3b82f6; color: #3b82f6; background: #eff6ff; }
.sa-btn-reload:disabled { opacity: .5; cursor: not-allowed; }
@keyframes spin-r { to { transform: rotate(360deg); } }
.spin { animation: spin-r .7s linear infinite; }

/* ── KPI ───────────────────────────────────────────────────────── */
.sa-kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.sa-kpi {
  background: white;
  border: 1.5px solid #f0f0f0;
  border-radius: 14px;
  padding: 18px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all .15s;
}
.sa-kpi:hover { box-shadow: 0 4px 16px rgba(0,0,0,.06); transform: translateY(-2px); }
.sa-kpi-icon {
  width: 44px; height: 44px;
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sa-kpi-label { font-size: 11px; color: #9ca3af; font-weight: 600; text-transform: uppercase; letter-spacing: .05em; margin-bottom: 4px; }
.sa-kpi-value { font-size: 20px; font-weight: 800; color: #111827; letter-spacing: -.3px; }
.sa-kpi-value.sm { font-size: 15px; }

/* ── Tabs ──────────────────────────────────────────────────────── */
.sa-tabs {
  display: flex;
  gap: 4px;
  background: white;
  border: 1.5px solid #f0f0f0;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 16px;
  width: fit-content;
}
.sa-tab {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 8px 18px;
  border-radius: 8px;
  border: none; background: transparent;
  font-family: inherit; font-size: 13px; font-weight: 600;
  color: #6b7280; cursor: pointer; transition: all .15s;
}
.sa-tab:hover { color: #111827; background: #f9fafb; }
.sa-tab.active { background: #111827; color: white; }

/* ── Panel ─────────────────────────────────────────────────────── */
.sa-panel {
  background: white;
  border: 1.5px solid #f0f0f0;
  border-radius: 16px;
  overflow: hidden;
}

/* ── Filter Bar ────────────────────────────────────────────────── */
.sa-filter-bar {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  padding: 18px 20px;
  border-bottom: 1px solid #f3f4f6;
  flex-wrap: wrap;
}
.sa-filter-bar.wrap { flex-wrap: wrap; }
.sa-filter-group { display: flex; flex-direction: column; gap: 5px; }
.sa-filter-actions { display: flex; gap: 8px; align-items: flex-end; }
.sa-label { font-size: 11px; font-weight: 700; color: #9ca3af; text-transform: uppercase; letter-spacing: .05em; }

.sa-select {
  padding: 8px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-family: inherit; font-size: 13px; color: #374151;
  background: white; outline: none; cursor: pointer; min-width: 140px;
}
.sa-select:focus { border-color: #3b82f6; }

.sa-input {
  padding: 8px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  font-family: inherit; font-size: 13px; color: #374151;
  background: white; outline: none; width: 180px;
}
.sa-input:focus { border-color: #3b82f6; }

.sa-number-input {
  display: flex; align-items: center;
  border: 1.5px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}
.sa-number-input button {
  padding: 7px 12px;
  border: none; background: transparent;
  font-size: 16px; cursor: pointer; color: #6b7280; transition: background .1s;
}
.sa-number-input button:hover { background: #f3f4f6; }
.sa-number-input span { padding: 0 12px; font-size: 13px; font-weight: 700; color: #111827; min-width: 40px; text-align: center; }

.sa-btn-primary {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 9px 18px;
  background: #111827; border: 1.5px solid #111827;
  border-radius: 8px; color: white;
  font-family: inherit; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all .15s;
}
.sa-btn-primary:hover:not(:disabled) { background: #1f2937; border-color: #1f2937; }
.sa-btn-primary:disabled { opacity: .5; cursor: not-allowed; }

.sa-btn-ghost {
  padding: 9px 18px;
  background: white; border: 1.5px solid #e5e7eb;
  border-radius: 8px; color: #6b7280;
  font-family: inherit; font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all .15s;
}
.sa-btn-ghost:hover { border-color: #d1d5db; color: #374151; }

.spinner-sm {
  width: 13px; height: 13px;
  border: 2px solid rgba(255,255,255,.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin-r .6s linear infinite;
}

/* Quick ranges */
.sa-quick-ranges {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px 14px;
  border-bottom: 1px solid #f3f4f6;
  flex-wrap: wrap;
}
.sa-quick-btn {
  padding: 5px 12px;
  border: 1.5px solid #e5e7eb;
  border-radius: 20px;
  background: white; color: #374151;
  font-family: inherit; font-size: 12px; font-weight: 600;
  cursor: pointer; transition: all .15s;
}
.sa-quick-btn:hover { border-color: #3b82f6; color: #3b82f6; background: #eff6ff; }

/* Range alert */
.sa-range-alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 20px;
  background: #eff6ff; border-bottom: 1px solid #bfdbfe;
  font-size: 13px; color: #1d4ed8;
}
.range-count-badge {
  margin-left: 4px; padding: 2px 10px;
  background: #dbeafe; border-radius: 20px;
  font-size: 11px; font-weight: 700; color: #1d4ed8;
}

/* ── Table ─────────────────────────────────────────────────────── */
.sa-table-wrap { overflow-x: auto; }
.sa-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.sa-table th {
  padding: 10px 16px;
  text-align: left;
  font-size: 10px; font-weight: 700;
  color: #9ca3af; text-transform: uppercase; letter-spacing: .08em;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  white-space: nowrap;
}
.sa-table td {
  padding: 13px 16px;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}
.sa-table tr:last-child td { border-bottom: none; }
.sa-table tbody tr:hover td { background: #fafafa; }

/* Rank badge */
.rank-badge {
  width: 34px; height: 34px;
  border-radius: 10px;
  display: inline-flex; align-items: center; justify-content: center;
  font-weight: 800; font-size: 13px;
  margin: 0 auto;
}
.rank-gold   { background: #fef9c3; color: #b45309; }
.rank-silver { background: #f1f5f9; color: #475569; }
.rank-bronze { background: #fef3c7; color: #92400e; }
.rank-other  { background: #f3f4f6; color: #6b7280; font-size: 12px; }

/* Customer cell */
.cust-cell { display: flex; align-items: center; gap: 10px; }
.cust-av {
  width: 36px; height: 36px;
  border-radius: 10px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 800; color: white;
}
.cust-av.sm { width: 30px; height: 30px; border-radius: 8px; font-size: 10px; }
.cust-name  { font-size: 13px; font-weight: 700; color: #111827; }
.cust-email { font-size: 11px; color: #9ca3af; margin-top: 1px; }

/* Chips */
.tier-chip {
  display: inline-block; padding: 3px 9px;
  border-radius: 6px; font-size: 10px; font-weight: 700;
  text-transform: uppercase; letter-spacing: .04em;
}
.tier-bronze   { background: #fef3c7; color: #92400e; }
.tier-silver   { background: #f1f5f9; color: #475569; }
.tier-gold     { background: #fef9c3; color: #b45309; }
.tier-platinum { background: #ede9fe; color: #6d28d9; }
.tier-diamond  { background: #dbeafe; color: #1d4ed8; }

.type-chip {
  display: inline-block; padding: 3px 9px;
  border-radius: 6px; font-size: 11px; font-weight: 600;
}
.type-vip { background: #fef3c7; color: #b45309; }
.type-reg { background: #f3f4f6; color: #6b7280; }

.points-badge {
  display: inline-block; padding: 3px 10px;
  background: #fffbeb; border: 1px solid #fde68a;
  border-radius: 20px; font-size: 12px; font-weight: 700; color: #b45309;
}

.spend-amount { font-size: 14px; font-weight: 800; color: #059669; }
.muted-text { color: #9ca3af; font-size: 12px; }
.muted-dash { color: #d1d5db; }

/* Skeleton */
.sa-table-skeleton { padding: 8px 0; }
.sk-row {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 20px; border-bottom: 1px solid #f3f4f6;
}
.sk-user { display: flex; align-items: center; gap: 10px; flex: 1; }
.sk-lines { display: flex; flex-direction: column; gap: 6px; flex: 1; }
.sk {
  border-radius: 6px;
  background: linear-gradient(90deg, #f3f4f6 25%, #e9eaec 50%, #f3f4f6 75%);
  background-size: 200%; animation: shimmer 1.4s ease infinite;
}
.sk-rank  { width: 34px; height: 34px; border-radius: 10px; }
.sk-av    { width: 36px; height: 36px; border-radius: 10px; flex-shrink: 0; }
.sk-l     { height: 12px; }
.sk-pill  { width: 64px; height: 24px; border-radius: 6px; }
.sk-money { width: 100px; height: 18px; }
.sk-id    { width: 40px; height: 16px; }
.w70 { width: 70%; } .w50 { width: 50%; }
@keyframes shimmer { from { background-position: 200% 0; } to { background-position: -200% 0; } }

/* Empty */
.sa-empty {
  padding: 60px 20px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  color: #9ca3af; font-size: 13px;
}

/* ── Pagination ────────────────────────────────────────────────── */
.sa-pagination {
  display: flex; align-items: center; justify-content: center;
  gap: 6px; padding: 16px;
  border-top: 1px solid #f0f0f0;
}
.page-btn {
  min-width: 32px; height: 32px;
  border: 1.5px solid #e5e7eb; border-radius: 8px;
  background: white; font-family: inherit; font-size: 13px; font-weight: 600;
  color: #374151; cursor: pointer; display: flex; align-items: center; justify-content: center;
  padding: 0 8px; transition: all .15s;
}
.page-btn:hover:not(:disabled) { border-color: #3b82f6; color: #3b82f6; }
.page-btn:disabled { opacity: .4; cursor: not-allowed; }
.page-btn.active { background: #111827; border-color: #111827; color: white; }
.page-info { font-size: 12px; color: #9ca3af; margin-left: 6px; }

/* ── Statistics Tab ────────────────────────────────────────────── */
.sa-card {
  background: white; border: 1.5px solid #f0f0f0; border-radius: 14px; overflow: hidden;
}
.mb-14 { margin-bottom: 14px; }
.sa-card-header {
  padding: 16px 20px 14px;
  border-bottom: 1px solid #f3f4f6;
}
.sa-card-title { font-size: 13px; font-weight: 700; color: #111827; }
.sa-card-sub   { font-size: 11px; color: #9ca3af; margin-top: 2px; }

.sa-ranges { display: flex; flex-direction: column; gap: 14px; padding: 18px 20px; }
.sa-range-row {
  display: grid;
  grid-template-columns: 130px 1fr 50px 100px;
  align-items: center;
  gap: 14px;
}
.sa-range-label { font-size: 13px; font-weight: 600; color: #374151; }
.sa-range-bar-wrap { background: #f3f4f6; border-radius: 20px; height: 8px; overflow: hidden; }
.sa-range-bar { height: 100%; border-radius: 20px; transition: width .6s ease; min-width: 2px; }
.sa-range-pct { font-size: 12px; font-weight: 700; color: #374151; text-align: right; }
.sa-range-count { font-size: 12px; color: #9ca3af; text-align: right; }

.sa-tier-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 4px;
}
.sa-tier-card {
  background: white; border: 1.5px solid #f0f0f0; border-radius: 14px; padding: 16px;
  transition: all .15s;
}
.sa-tier-card:hover { box-shadow: 0 4px 14px rgba(0,0,0,.06); transform: translateY(-2px); }
.sa-tier-card-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.sa-tier-count { font-size: 12px; color: #9ca3af; font-weight: 500; }
.sa-tier-divider { height: 1px; background: #f3f4f6; margin: 10px 0; }
.sa-tier-row { display: flex; align-items: center; justify-content: space-between; }
.sa-tier-lbl { font-size: 11px; color: #9ca3af; font-weight: 600; text-transform: uppercase; letter-spacing: .04em; }
.sa-tier-val { font-size: 15px; font-weight: 800; color: #111827; }
.sa-tier-val.blue { color: #2563eb; }

/* ── Responsive ────────────────────────────────────────────────── */
@media (max-width: 1100px) {
  .sa-kpi-row { grid-template-columns: repeat(2, 1fr); }
  .sa-tier-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .sa-root { padding: 16px; }
  .sa-kpi-row { grid-template-columns: 1fr 1fr; }
  .sa-tier-grid { grid-template-columns: 1fr; }
  .sa-range-row { grid-template-columns: 1fr 1fr; }
  .sa-tabs { flex-wrap: wrap; }
}
</style>