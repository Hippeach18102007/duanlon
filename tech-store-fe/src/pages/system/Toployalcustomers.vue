<template>
  <div class="ll-page">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
          <el-icon><Trophy /></el-icon> Loyalty Analytics
        </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Top Khách Hàng Trung Thành</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Đánh giá tổng hợp: điểm tích lũy · hạng VIP · chi tiêu · tần suất hoạt động</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-space align="center" :size="8">
          <el-text type="info" size="small">Hiển thị</el-text>
          <el-select v-model="topN" @change="loadCustomers" style="width:100px">
            <el-option :value="5"  label="Top 5"  />
            <el-option :value="10" label="Top 10" />
            <el-option :value="20" label="Top 20" />
            <el-option :value="50" label="Top 50" />
          </el-select>
        </el-space>
        <el-button plain :loading="loading" @click="loadData" :disabled="loading">
          <template #icon><el-icon><Refresh /></el-icon></template>
          Reload
        </el-button>
      </div>
    </div>

    <!-- ── Stat cards ── -->
    <el-row :gutter="16">
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic :title="`Tổng điểm (Top ${topN})`" :value="totalLoyaltyPoints">
            <template #prefix><el-icon><Trophy /></el-icon></template>
          </el-statistic>
          <el-text type="info" size="small">Σ loyaltyPoints</el-text>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="Khách VIP" :value="vipCount">
            <template #prefix><el-icon><UserFilled /></el-icon></template>
          </el-statistic>
          <el-text type="info" size="small">customerType = VIP</el-text>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="TB điểm / KH" :value="avgPoints">
            <template #prefix><el-icon><DataAnalysis /></el-icon></template>
          </el-statistic>
          <el-text type="info" size="small">avg loyaltyPoints</el-text>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="#1 Trung Thành" :value="sortedCustomers[0]?.fullName || '—'">
            <template #prefix><el-icon><Medal /></el-icon></template>
          </el-statistic>
          <el-text type="info" size="small">
            {{ sortedCustomers[0] ? (sortedCustomers[0].loyaltyPoints || 0).toLocaleString() + ' pts' : '' }}
          </el-text>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Tier Breakdown ── -->
    <el-card v-if="Object.keys(stats.byTier || {}).length" shadow="never">
      <template #header>
        <el-space>
          <el-icon><PieChart /></el-icon>
          <span>Phân bổ chi tiêu theo hạng VIP</span>
        </el-space>
      </template>
      <el-row :gutter="12">
        <el-col
          v-for="(td, tier) in stats.byTier"
          :key="tier"
          :span="4" :xs="12"
        >
          <el-card shadow="never">
            <el-row justify="space-between" align="middle">
              <el-tag :type="getTierTagType(tier)" effect="plain" size="small" round>
                {{ tier }}
              </el-tag>
              <el-text type="info" size="small">{{ td.customerCount }} KH</el-text>
            </el-row>
            <div class="tier-spent">{{ fmtShort(td.totalSpent) }}</div>
            <el-text type="info" size="small">TB: {{ fmtShort(td.averageSpent) }}</el-text>
            <el-progress
              :percentage="stats.totalSpent ? Math.round((td.totalSpent / stats.totalSpent) * 100) : 0"
              :stroke-width="5"
              :show-text="false"
              :color="getTierProgressColor(tier)"
              class="tier-bar"
            />
            <el-text type="info" size="small">
              {{ stats.totalSpent ? Math.round((td.totalSpent / stats.totalSpent) * 100) : 0 }}%
            </el-text>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- ── Controls + Table ── -->
    <el-card shadow="never" :body-style="{ padding: 0 }">

      <!-- Filter + Sort bar -->
      <el-row justify="space-between" align="middle" wrap class="controls-bar">
        <el-radio-group v-model="activeTier" @change="onTierChange">
          <el-radio-button v-for="t in TIER_OPTIONS" :key="t.value" :value="t.value">
            <el-icon><component :is="t.icon" /></el-icon>
            {{ t.label }}
          </el-radio-button>
        </el-radio-group>
        <el-space align="center">
          <el-text type="info" size="small">Sắp xếp:</el-text>
          <el-radio-group v-model="sortKey" size="small">
            <el-radio-button value="loyaltyPoints">
              <el-icon><Coin /></el-icon> Điểm
            </el-radio-button>
            <el-radio-button value="totalSpent">
              <el-icon><Money /></el-icon> Chi tiêu
            </el-radio-button>
            <el-radio-button value="orderCount">
              <el-icon><ShoppingCart /></el-icon> Số lần mua
            </el-radio-button>
          </el-radio-group>
        </el-space>
      </el-row>

      <!-- Table -->
      <el-table :data="sortedCustomers" v-loading="loading" border stripe>

        <!-- Rank -->
        <el-table-column label="Rank" width="70" align="center">
          <template #default="{ $index }">
            <el-icon v-if="$index === 0" color="#f59e0b" size="20"><GoldMedal /></el-icon>
            <el-icon v-else-if="$index === 1" color="#94a3b8" size="20"><Medal /></el-icon>
            <el-icon v-else-if="$index === 2" color="#cd7f32" size="20"><Medal /></el-icon>
            <el-text v-else type="info">{{ $index + 1 }}</el-text>
          </template>
        </el-table-column>

        <!-- Customer -->
        <el-table-column label="Khách hàng" min-width="200">
          <template #default="{ row }">
            <el-space :size="10" align="center">
              <div class="avatar" :style="{ background: getAvatarColor(row.fullName) }">
                {{ getInitials(row.fullName) }}
              </div>
              <div>
                <div>{{ row.fullName }}</div>
                <el-text type="info" size="small">{{ row.email }}</el-text>
              </div>
            </el-space>
          </template>
        </el-table-column>

        <!-- Tier -->
        <el-table-column label="Hạng VIP" width="130" align="center">
          <template #default="{ row }">
            <el-space direction="vertical" :size="4" alignment="center">
              <el-tag v-if="row.raw.vipTier" :type="getTierTagType(row.raw.vipTier)" effect="plain" size="small" round>
                {{ row.raw.vipTier }}
              </el-tag>
              <el-tag v-else type="info" effect="plain" size="small">Member</el-tag>
              <el-text type="info" size="small">{{ row.customerType }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <!-- Points -->
        <el-table-column label="Điểm tích lũy" width="160" align="center">
          <template #default="{ row }">
            <el-space direction="vertical" :size="6" alignment="center">
              <el-tag type="warning" effect="plain">
                {{ (row.loyaltyPoints || 0).toLocaleString() }}
              </el-tag>
              <el-progress
                v-if="row.raw.vipTier"
                :percentage="getTierProgress(row.raw)"
                :stroke-width="4"
                :show-text="false"
                :color="getTierProgressColor(row.raw.vipTier)"
                class="pts-bar"
              />
            </el-space>
          </template>
        </el-table-column>

        <!-- Order count -->
        <el-table-column label="Số lần mua" width="130" align="center">
          <template #default="{ row }">
            <el-space direction="vertical" :size="4" alignment="center">
              <strong>{{ (row.raw.orderCount || 0).toLocaleString() }}</strong>
              <el-text type="info" size="small">đơn hàng</el-text>
              <el-tag :type="getOrderFreqTagType(row.raw.orderCount)" effect="plain" size="small">
                {{ getOrderFreqLabel(row.raw.orderCount) }}
              </el-tag>
            </el-space>
          </template>
        </el-table-column>

        <!-- Next tier -->
        <el-table-column label="Còn thiếu" width="150" align="center">
          <template #default="{ row }">
            <el-space v-if="row.raw.pointsToNextTier > 0" direction="vertical" :size="2" alignment="center">
              <strong>{{ row.raw.pointsToNextTier.toLocaleString() }} pts</strong>
              <el-text type="info" size="small">để lên hạng tiếp</el-text>
            </el-space>
            <el-tag v-else type="success" effect="plain" size="small">✓ Max tier</el-tag>
          </template>
        </el-table-column>

        <!-- Total spent -->
        <el-table-column label="Tổng chi tiêu" width="200" align="right">
          <template #default="{ row }">
            <el-space direction="vertical" :size="5" alignment="flex-end">
              <el-text type="success">{{ formatCurrency(row.raw.totalSpent) }}</el-text>
              <el-progress
                :percentage="maxSpent ? Math.round((row.raw.totalSpent / maxSpent) * 100) : 0"
                :stroke-width="4"
                :show-text="false"
                :color="getTierProgressColor(row.raw.vipTier)"
                class="spent-bar"
              />
            </el-space>
          </template>
        </el-table-column>

        <!-- Discount rate -->
        <el-table-column label="Ưu đãi" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.raw.discountRate" type="success" effect="plain" size="small">
              -{{ (row.raw.discountRate * 100).toFixed(0) }}%
            </el-tag>
            <el-text v-else type="info">—</el-text>
          </template>
        </el-table-column>

        <!-- Activity -->
        <el-table-column label="Hoạt động" width="160" align="center">
          <template #default="{ row }">
            <el-space direction="vertical" :size="5" alignment="flex-start">
              <el-space :size="5">
                <el-icon><Box /></el-icon>
                <el-text :type="getActivityTextType(row.raw.lastOrderAt)" size="small">
                  {{ fmtRelative(row.raw.lastOrderAt) }}
                </el-text>
              </el-space>
              <el-space :size="5">
                <el-icon><Lock /></el-icon>
                <el-text type="info" size="small">{{ fmtRelative(row.raw.lastLoginAt) }}</el-text>
              </el-space>
            </el-space>
          </template>
        </el-table-column>

        <!-- VIP Note -->
        <el-table-column min-width="150">
          <template #header>
            <el-space :size="4"><el-icon><Star /></el-icon> VIP Note</el-space>
          </template>
          <template #default="{ row }">
            <template v-if="row.raw.vipTier">
              <el-tooltip v-if="row.raw.vipNote" :content="row.raw.vipNote" placement="top">
                <el-tag type="warning" effect="plain" class="vip-note-tag">
                  {{ row.raw.vipNote.substring(0, 22) }}{{ row.raw.vipNote.length > 22 ? '…' : '' }}
                </el-tag>
              </el-tooltip>
              <el-text v-else type="info" size="small"><em>Chưa có note</em></el-text>
            </template>
            <el-text v-else type="info">—</el-text>
          </template>
        </el-table-column>
      </el-table>

      <!-- Footer alert -->
      <el-alert type="info" :closable="false" show-icon class="footer-alert">
        <template #default>
          <strong>Khác với "By Spending Range":</strong>
          Trang này đo <em>độ trung thành tổng hợp</em> —
          KH mua 1 lần 200M chưa chắc trung thành hơn KH mua 40 lần tổng 80M.
          Metric chính là <code>loyaltyPoints</code> (tần suất) +
          <code>vipTier</code> (hạng) + <code>lastOrderAt</code> (còn active?).
        </template>
      </el-alert>
    </el-card>

  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  Trophy, UserFilled, DataAnalysis, Medal, GoldMedal,
  Refresh, PieChart, Coin, Money, ShoppingCart,
  Box, Lock, Star,
} from "@element-plus/icons-vue";
import { customersApi } from "../../api/customers.api";
import { toast } from "../../ui/toast";

// ══════════════════════════════════════
// CONSTANTS
// ══════════════════════════════════════
const TIER_MAP = {
  DIAMOND:  { color: "#22c55e", minPts: 15000, maxPts: 20000 },
  PLATINUM: { color: "#409EFF", minPts: 10000, maxPts: 15000 },
  GOLD:     { color: "#E6A23C", minPts: 5000,  maxPts: 10000 },
  SILVER:   { color: "#909399", minPts: 2000,  maxPts: 5000  },
  BRONZE:   { color: "#cd7f32", minPts: 500,   maxPts: 2000  },
};

// Map tier → EP icon component
const TIER_ICON_MAP = {
  DIAMOND:  Star,
  PLATINUM: Medal,
  GOLD:     Trophy,
  SILVER:   GoldMedal,
  BRONZE:   Coin,
};

const TIER_OPTIONS = [
  { value: "ALL",      icon: Trophy,    label: "Tất cả"  },
  { value: "DIAMOND",  icon: Star,      label: "Diamond" },
  { value: "PLATINUM", icon: Medal,     label: "Platinum"},
  { value: "GOLD",     icon: Trophy,    label: "Gold"    },
  { value: "SILVER",   icon: GoldMedal, label: "Silver"  },
  { value: "BRONZE",   icon: Coin,      label: "Bronze"  },
];

// ══════════════════════════════════════
// STATE
// ══════════════════════════════════════
const loading    = ref(false);
const customers  = ref([]);
const activeTier = ref("ALL");
const topN       = ref(10);
const sortKey    = ref("loyaltyPoints");

const stats = ref({
  totalSpent: 0,
  totalCustomers: 0,
  averageSpent: 0,
  byTier: {},
  spendingRanges: {},
});

// ══════════════════════════════════════
// COMPUTED
// ══════════════════════════════════════
const sortedCustomers = computed(() =>
  [...customers.value].sort((a, b) => {
    let va, vb;
    if (sortKey.value === "loyaltyPoints") {
      va = a.loyaltyPoints || 0;
      vb = b.loyaltyPoints || 0;
    } else if (sortKey.value === "totalSpent") {
      va = a.raw.totalSpent || 0;
      vb = b.raw.totalSpent || 0;
    } else {
      va = a.raw.orderCount || 0;
      vb = b.raw.orderCount || 0;
    }
    return vb - va;
  })
);

const maxSpent = computed(() =>
  Math.max(...customers.value.map(c => c.raw.totalSpent || 0), 1)
);

const totalLoyaltyPoints = computed(() =>
  customers.value.reduce((s, c) => s + (c.loyaltyPoints || 0), 0)
);

const vipCount = computed(() =>
  customers.value.filter(c => c.customerType === "VIP").length
);

const avgPoints = computed(() => {
  if (!customers.value.length) return 0;
  return Math.round(totalLoyaltyPoints.value / customers.value.length);
});

// ══════════════════════════════════════
// API
// ══════════════════════════════════════
async function loadCustomers() {
  loading.value = true;
  try {
    const res = activeTier.value === "ALL"
      ? await customersApi.listTopSpenders(topN.value)
      : await customersApi.listTopSpendersByVipTier(activeTier.value, topN.value);
    customers.value = normalize(extractList(res?.data));
  } catch (e) {
    console.error(e);
    toast("Không thể tải danh sách loyal customers", "error");
  } finally {
    loading.value = false;
  }
}

async function loadStats() {
  try {
    const res = await customersApi.getSpendingStatistics();
    stats.value = res?.data || {};
  } catch (e) {
    console.error(e);
  }
}

async function loadData() {
  await Promise.all([loadCustomers(), loadStats()]);
}

function onTierChange(tier) {
  activeTier.value = tier;
  loadCustomers();
}

// ══════════════════════════════════════
// HELPERS
// ══════════════════════════════════════
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
    id:            c?.id ?? c?.customerId,
    fullName:      c?.fullName ?? c?.name ?? "",
    email:         c?.email ?? "",
    phone:         c?.phone ?? "",
    customerType:  (c?.customerType ?? "REGULAR").toString().toUpperCase(),
    loyaltyPoints: c?.loyaltyPoints ?? 0,
    raw: c,
  }));
}

function formatCurrency(v) {
  if (!v) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(v);
}

function fmtShort(v) {
  if (!v || v === 0) return "—";
  if (v >= 1_000_000_000) return (v / 1_000_000_000).toFixed(1) + " tỷ";
  if (v >= 1_000_000)     return (v / 1_000_000).toFixed(1) + "M";
  if (v >= 1_000)         return (v / 1_000).toFixed(0) + "K";
  return v + " ₫";
}

function fmtRelative(s) {
  if (!s) return "—";
  const diff = (Date.now() - new Date(s)) / 86_400_000;
  if (diff < 1)  return "Hôm nay";
  if (diff < 2)  return "Hôm qua";
  if (diff < 7)  return `${Math.floor(diff)} ngày trước`;
  if (diff < 30) return `${Math.floor(diff / 7)} tuần trước`;
  return `${Math.floor(diff / 30)} tháng trước`;
}

function getActivityTextType(lastOrderAt) {
  if (!lastOrderAt) return "danger";
  const diff = (Date.now() - new Date(lastOrderAt)) / 86_400_000;
  if (diff < 7)  return "success";
  if (diff < 30) return "warning";
  if (diff < 90) return "info";
  return "danger";
}

function getOrderFreqTagType(count) {
  if (!count || count === 0) return "info";
  if (count >= 20) return "danger";
  if (count >= 10) return "warning";
  if (count >= 5)  return "success";
  return "primary";
}

function getOrderFreqLabel(count) {
  if (!count || count === 0) return "Chưa mua";
  if (count >= 20) return "Rất thường xuyên";
  if (count >= 10) return "Thường xuyên";
  if (count >= 5)  return "Khá đều";
  return "Thỉnh thoảng";
}

function getTierProgress(customer) {
  const tier = TIER_MAP[customer.vipTier];
  if (!tier) return 0;
  const range = tier.maxPts - tier.minPts;
  const current = (customer.loyaltyPoints || 0) - tier.minPts;
  return Math.min(100, Math.max(0, Math.round((current / range) * 100)));
}

function getTierTagType(tier) {
  const map = { DIAMOND: "success", PLATINUM: "primary", GOLD: "warning", SILVER: "info", BRONZE: "" };
  return map[tier] ?? "info";
}

function getTierProgressColor(tier) {
  return TIER_MAP[tier]?.color ?? "#409EFF";
}

function getInitials(name) {
  if (!name) return "?";
  const p = name.trim().split(" ");
  return p.length === 1 ? p[0].substring(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase();
}

function getAvatarColor(name) {
  const colors = ["#1e40af","#065f46","#7c2d12","#581c87","#0e7490","#a21caf","#b45309","#0f766e","#1d4ed8","#be123c"];
  const h = (name || "").split("").reduce((a, c) => a + c.charCodeAt(0), 0);
  return colors[h % colors.length];
}

onMounted(loadData);
</script>

<style scoped>
.ll-page {
  max-width: 1600px;
  margin: 0 auto;
  padding: 32px 40px 60px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── Header ───────────────────────────────── */
.label-upper {
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.page-title {
  font-size: 28px;
  margin: 4px 0;
}

.header-right {
  padding-top: 6px;
}

/* ── Tier breakdown ───────────────────────── */
.tier-spent {
  font-size: 18px;
  font-weight: 800;
  margin: 8px 0 2px;
}

.tier-bar {
  margin: 8px 0 4px;
}

/* ── Controls bar ─────────────────────────── */
.controls-bar {
  padding: 14px 20px;
  gap: 12px;
}

/* ── Table cells ──────────────────────────── */
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 700;
  font-size: 12px;
  flex-shrink: 0;
}

.pts-bar {
  width: 100px;
}

.spent-bar {
  width: 130px;
}

.vip-note-tag {
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ── Footer alert ─────────────────────────── */
.footer-alert {
  border-radius: 0;
}

/* ── Responsive ───────────────────────────── */
@media (max-width: 700px) {
  .ll-page { padding: 20px 16px 40px; }
}
</style>