<template>
  <div class="order-dashboard">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><DataAnalysis /></el-icon> Đơn hàng
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Order Dashboard</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Tổng quan tình trạng đơn hàng theo thời gian thực</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-select v-model="period" @change="fetchAll" style="width:148px">
          <template #prefix><el-icon><Calendar /></el-icon></template>
          <el-option label="7 ngày qua"  value="7"  />
          <el-option label="30 ngày qua" value="30" />
          <el-option label="90 ngày qua" value="90" />
        </el-select>
        <el-button plain :loading="loading" @click="fetchAll" :disabled="loading">
          <template #icon><el-icon><Refresh /></el-icon></template>
          Refresh
        </el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon class="error-alert" />

    <!-- ══ KPI CARDS ══ -->
    <el-row :gutter="14" v-loading="loading" class="kpi-row">
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="Tổng đơn" :value="kpi.total">
            <template #prefix><el-icon><Box /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="Hoàn thành" :value="kpi.completed">
            <template #prefix><el-icon><CircleCheck /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="Huỷ" :value="kpi.cancelled">
            <template #prefix><el-icon><CircleClose /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6" :xs="12">
        <el-card shadow="never">
          <el-statistic title="Doanh thu" :value="kpi.revenue" :formatter="formatVnd">
            <template #prefix><el-icon><Money /></el-icon></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- ══ TRẠNG THÁI + KÊNH BÁN ══ -->
    <el-row :gutter="14" v-loading="loading" class="section-row">

      <!-- Trạng thái đơn -->
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span>Theo trạng thái</span>
              <el-text type="info" size="small">{{ kpi.total }} đơn</el-text>
            </el-row>
          </template>
          <div class="status-grid">
            <div
              v-for="s in statusCards"
              :key="s.key"
              class="status-card"
              :style="{'--sc': s.color, '--sc-bg': s.bg}"
            >
              <div class="sc-dot"></div>
              <div class="sc-body">
                <span class="sc-label">{{ s.label }}</span>
                <span class="sc-count">{{ s.count }}</span>
              </div>
              <div class="sc-pct">{{ s.pct }}%</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Kênh bán -->
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span>Theo kênh bán</span>
              <el-text type="info" size="small">{{ kpi.total }} đơn</el-text>
            </el-row>
          </template>
          <el-row :gutter="10" class="channel-grid">
            <el-col v-for="c in channelCards" :key="c.key" :span="12">
              <el-card shadow="never" :body-style="{ padding: '12px 14px' }">
                <el-space align="center">
                  <el-icon><component :is="c.key === 'ONLINE' ? Monitor : OfficeBuilding" /></el-icon>
                  <el-text>{{ c.label }}</el-text>
                </el-space>
                <div class="cc-count">{{ c.count }}</div>
                <el-text type="info" size="small">{{ formatVnd(c.revenue) }}</el-text>
                <el-progress :percentage="c.pct" :show-text="false" :stroke-width="4" class="cc-bar" />
                <el-text type="primary" size="small">{{ c.pct }}% tổng đơn</el-text>
              </el-card>
            </el-col>
          </el-row>

          <el-divider class="divider-sm" />

          <el-table :data="channelRates" size="small" :show-header="true">
            <el-table-column label="Kênh" prop="channel" width="80" />
            <el-table-column label="Hoàn thành" prop="done" align="center" />
            <el-table-column label="Huỷ" prop="cancel" align="center" />
            <el-table-column label="Tỉ lệ HT" align="center">
              <template #default="{ row }">
                <el-text :type="row.rate >= 50 ? 'success' : 'warning'">{{ row.rate }}%</el-text>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ══ CHARTS ROW 1 ══ -->
    <el-row :gutter="14" v-loading="loading" class="charts-row">
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span>Đơn hàng theo ngày</span>
              <el-space :size="10">
                <el-text type="primary" size="small">● Mới</el-text>
                <el-text type="success" size="small">● Hoàn thành</el-text>
                <el-text type="danger"  size="small">● Huỷ</el-text>
              </el-space>
            </el-row>
          </template>
          <div class="canvas-wrap"><canvas ref="lineCanvas"></canvas></div>
        </el-card>
      </el-col>
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span>Online vs Offline theo ngày</span>
              <el-space :size="10">
                <el-text type="primary" size="small">● Online</el-text>
                <el-text size="small" style="color:#6366f1">● Offline</el-text>
              </el-space>
            </el-row>
          </template>
          <div class="canvas-wrap"><canvas ref="channelCanvas"></canvas></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ══ CHARTS ROW 2 ══ -->
    <el-row :gutter="14" v-loading="loading" class="charts-row">
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>Doanh thu theo ngày</template>
          <div class="canvas-wrap"><canvas ref="barCanvas"></canvas></div>
        </el-card>
      </el-col>
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>Giá trị trung bình đơn (AOV)</template>
          <div class="canvas-wrap"><canvas ref="aovCanvas"></canvas></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ══ BOTTOM ══ -->
    <el-row :gutter="14" v-loading="loading" class="charts-row">
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>Top 5 khách hàng theo doanh thu</template>
          <div class="canvas-wrap"><canvas ref="topCustCanvas"></canvas></div>
        </el-card>
      </el-col>
      <el-col :span="12" :xs="24">
        <el-card shadow="never">
          <template #header>
            <el-row justify="space-between" align="middle">
              <span>Đơn gần đây</span>
              <router-link to="/system/orders/filter">
                <el-button link type="primary" size="small">Xem tất cả</el-button>
              </router-link>
            </el-row>
          </template>
          <el-table :data="recentOrders" size="small">
            <el-table-column prop="id" label="Mã" width="95" />
            <el-table-column label="Khách hàng" min-width="100">
              <template #default="{row}">
                {{ row.customerName || row.customer?.name || row.customer?.fullName || "—" }}
              </template>
            </el-table-column>
            <el-table-column label="Kênh" width="80">
              <template #default="{row}">
                <el-tag :type="row.channel === 'ONLINE' ? 'primary' : ''" effect="plain" size="small">
                  {{ row.channel || "—" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Tổng" width="105">
              <template #default="{row}">{{ formatVnd(row.totalAmount ?? row.total ?? 0) }}</template>
            </el-table-column>
            <el-table-column label="TT" width="105">
              <template #default="{row}">
                <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue";
import { Calendar, Refresh, Box, CircleCheck, CircleClose, Money, Monitor, OfficeBuilding } from "@element-plus/icons-vue";
import { ordersApi } from "../../api/orders.api";

const period  = ref("30");
const loading = ref(false);
const error   = ref("");

const kpi          = ref({ total:0, completed:0, cancelled:0, revenue:0 });
const statusCards  = ref([]);
const channelCards = ref([]);
const channelRates = ref([]);
const recentOrders = ref([]);

const lineCanvas    = ref(null);
const channelCanvas = ref(null);
const barCanvas     = ref(null);
const aovCanvas     = ref(null);
const topCustCanvas = ref(null);
let charts = {};

const STATUS_META = [
  { key:"NEW",        label:"Mới",           color:"#f59e0b", bg:"rgba(245,158,11,.1)"  },
  { key:"PENDING",    label:"Chờ xử lý",     color:"#f97316", bg:"rgba(249,115,22,.1)"  },
  { key:"PROCESSING", label:"Đang xử lý",    color:"#3b82f6", bg:"rgba(59,130,246,.1)"  },
  { key:"SHIPPING",   label:"Đang giao",     color:"#8b5cf6", bg:"rgba(139,92,246,.1)"  },
  { key:"SHIPPED",    label:"Đã giao hàng",  color:"#06b6d4", bg:"rgba(6,182,212,.1)"   },
  { key:"DELIVERED",  label:"Hoàn thành",    color:"#10b981", bg:"rgba(16,185,129,.1)"  },
  { key:"COMPLETED",  label:"Đã hoàn tất",   color:"#059669", bg:"rgba(5,150,105,.1)"   },
  { key:"CANCELLED",  label:"Đã huỷ",        color:"#ef4444", bg:"rgba(239,68,68,.1)"   },
];

function isDone(s)      { return ["DELIVERED","COMPLETED"].includes((s||"").toUpperCase()); }
function isCancelled(s) { return (s||"").toUpperCase() === "CANCELLED"; }

function toISO(d) { return d.toISOString().slice(0,10); }
function buildDateRange(days) {
  const to = new Date(), from = new Date();
  from.setDate(from.getDate() - (days-1));
  return { from: toISO(from), to: toISO(to) };
}
function fmtDay(s) { const d=new Date(s); return `${d.getDate()}/${d.getMonth()+1}`; }

function processOrders(orders, from, to) {
  const total     = orders.length;
  const completed = orders.filter(o => isDone(o.status)).length;
  const cancelled = orders.filter(o => isCancelled(o.status)).length;
  const revenue   = orders.filter(o => !isCancelled(o.status))
                          .reduce((s,o) => s+(o.totalAmount??o.total??0), 0);
  kpi.value = { total, completed, cancelled, revenue };

  const statusMap = {};
  for (const o of orders) {
    const k = (o.status||"UNKNOWN").toUpperCase();
    statusMap[k] = (statusMap[k]||0)+1;
  }
  statusCards.value = STATUS_META
    .filter(m => statusMap[m.key])
    .map(m => ({ ...m, count: statusMap[m.key]||0, pct: total ? Math.round(((statusMap[m.key]||0)/total)*100) : 0 }));

  const ch = { ONLINE:{count:0,revenue:0}, OFFLINE:{count:0,revenue:0} };
  for (const o of orders) {
    const c = (o.channel||"ONLINE").toUpperCase();
    if (!ch[c]) ch[c] = {count:0,revenue:0};
    ch[c].count++;
    if (!isCancelled(o.status)) ch[c].revenue += (o.totalAmount??o.total??0);
  }
  channelCards.value = [
    { key:"ONLINE",  label:"Online",  color:"#3b82f6", count:ch.ONLINE.count,  revenue:ch.ONLINE.revenue,  pct: total ? Math.round((ch.ONLINE.count/total)*100)  : 0 },
    { key:"OFFLINE", label:"Offline", color:"#6366f1", count:ch.OFFLINE.count, revenue:ch.OFFLINE.revenue, pct: total ? Math.round((ch.OFFLINE.count/total)*100) : 0 },
  ];

  channelRates.value = ["ONLINE","OFFLINE"].map(c => {
    const sub = orders.filter(o => (o.channel||"ONLINE").toUpperCase()===c);
    const done   = sub.filter(o => isDone(o.status)).length;
    const cancel = sub.filter(o => isCancelled(o.status)).length;
    return { channel:c, done, cancel, rate: sub.length ? Math.round((done/sub.length)*100) : 0 };
  });

  recentOrders.value = [...orders]
    .sort((a,b) => new Date(b.createdAt||b.orderDate||0)-new Date(a.createdAt||a.orderDate||0))
    .slice(0,8);

  const days=[], cur=new Date(from), end=new Date(to);
  while (cur<=end) { days.push(toISO(new Date(cur))); cur.setDate(cur.getDate()+1); }

  const byDay={};
  for (const d of days) byDay[d]={new:0,completed:0,cancelled:0,revenue:0,online:0,offline:0,aovRevenue:0,aovCount:0};
  for (const o of orders) {
    const d=(o.createdAt||o.orderDate||"").slice(0,10);
    if (!byDay[d]) continue;
    byDay[d].new++;
    const ch2=(o.channel||"ONLINE").toUpperCase();
    if (ch2==="ONLINE") byDay[d].online++; else byDay[d].offline++;
    if (isDone(o.status))      { byDay[d].completed++; byDay[d].aovRevenue+=(o.totalAmount??o.total??0); byDay[d].aovCount++; }
    if (isCancelled(o.status)) byDay[d].cancelled++;
    if (!isCancelled(o.status)) byDay[d].revenue+=(o.totalAmount??o.total??0);
  }

  const maxT=18, step=days.length>maxT?Math.ceil(days.length/maxT):1;
  const sparse=days.filter((_,i)=>i%step===0||i===days.length-1);

  const custRev={};
  for (const o of orders) {
    if (isCancelled(o.status)) continue;
    const name=o.customerName||o.customer?.name||o.customer?.fullName||"Unknown";
    custRev[name]=(custRev[name]||0)+(o.totalAmount??o.total??0);
  }
  const top5=Object.entries(custRev).sort((a,b)=>b[1]-a[1]).slice(0,5);

  return {
    labels:    sparse.map(fmtDay),
    lineNew:       sparse.map(d=>byDay[d].new),
    lineCompleted: sparse.map(d=>byDay[d].completed),
    lineCancelled: sparse.map(d=>byDay[d].cancelled),
    barRevenue:    sparse.map(d=>byDay[d].revenue),
    chOnline:      sparse.map(d=>byDay[d].online),
    chOffline:     sparse.map(d=>byDay[d].offline),
    aov:           sparse.map(d=>byDay[d].aovCount?Math.round(byDay[d].aovRevenue/byDay[d].aovCount):0),
    top5Labels:    top5.map(x=>x[0]),
    top5Values:    top5.map(x=>x[1]),
  };
}

async function buildCharts(cd) {
  Object.values(charts).forEach(c=>c?.destroy());
  charts={};

  let Chart, registerables;
  try   { ({Chart,registerables}=await import("chart.js")); }
  catch { ({Chart,registerables}=await import("https://cdn.jsdelivr.net/npm/chart.js@4/+esm")); }
  Chart.register(...registerables);

  const grid="#00000010", tick={size:11};
  const opts=(extra={})=>({ responsive:true, maintainAspectRatio:false, animation:false, plugins:{legend:{display:false}}, ...extra });

  charts.line = new Chart(lineCanvas.value, {
    type:"line",
    data:{ labels:cd.labels, datasets:[
      {label:"Mới",       data:cd.lineNew,       borderColor:"#3b82f6",backgroundColor:"rgba(59,130,246,.08)", tension:.35,fill:true,pointRadius:0,pointHitRadius:10,borderWidth:2},
      {label:"Hoàn thành",data:cd.lineCompleted, borderColor:"#10b981",backgroundColor:"rgba(16,185,129,.07)", tension:.35,fill:true,pointRadius:0,pointHitRadius:10,borderWidth:2},
      {label:"Huỷ",       data:cd.lineCancelled, borderColor:"#ef4444",backgroundColor:"rgba(239,68,68,.06)",  tension:.35,fill:true,pointRadius:0,pointHitRadius:10,borderWidth:2},
    ]},
    options:opts({scales:{x:{grid:{color:grid},ticks:{font:tick,maxRotation:0}},y:{grid:{color:grid},ticks:{font:tick},beginAtZero:true}}}),
  });

  charts.channel = new Chart(channelCanvas.value, {
    type:"bar",
    data:{ labels:cd.labels, datasets:[
      {label:"Online",  data:cd.chOnline,  backgroundColor:"#3b82f6", borderRadius:3, borderSkipped:false},
      {label:"Offline", data:cd.chOffline, backgroundColor:"#6366f1", borderRadius:3, borderSkipped:false},
    ]},
    options:opts({scales:{x:{grid:{display:false},ticks:{font:tick,maxRotation:0}},y:{grid:{color:grid},ticks:{font:tick},beginAtZero:true}}}),
  });

  charts.bar = new Chart(barCanvas.value, {
    type:"bar",
    data:{ labels:cd.labels, datasets:[{label:"Doanh thu",data:cd.barRevenue,backgroundColor:"#3b82f6",hoverBackgroundColor:"#2563eb",borderRadius:4,borderSkipped:false}]},
    options:opts({scales:{x:{grid:{display:false},ticks:{font:tick,maxRotation:0}},y:{grid:{color:grid},ticks:{font:tick,callback:v=>formatM(v)},beginAtZero:true}}}),
  });

  charts.aov = new Chart(aovCanvas.value, {
    type:"line",
    data:{ labels:cd.labels, datasets:[{label:"AOV",data:cd.aov,borderColor:"#f59e0b",backgroundColor:"rgba(245,158,11,.08)",tension:.35,fill:true,pointRadius:0,pointHitRadius:10,borderWidth:2}]},
    options:opts({scales:{x:{grid:{color:grid},ticks:{font:tick,maxRotation:0}},y:{grid:{color:grid},ticks:{font:tick,callback:v=>formatM(v)},beginAtZero:true}}}),
  });

  charts.topCust = new Chart(topCustCanvas.value, {
    type:"bar",
    data:{ labels:cd.top5Labels, datasets:[{label:"Doanh thu",data:cd.top5Values, backgroundColor:["#3b82f6","#6366f1","#8b5cf6","#ec4899","#f59e0b"], borderRadius:5,borderSkipped:false}]},
    options:opts({ indexAxis:"y", scales:{ x:{grid:{color:grid},ticks:{font:tick,callback:v=>formatM(v)},beginAtZero:true}, y:{grid:{display:false},ticks:{font:{size:12}}} }}),
  });
}

async function fetchAll() {
  loading.value = true;
  error.value   = "";
  try {
    const {from,to} = buildDateRange(Number(period.value));
    const res    = await ordersApi.filter(undefined, from+"T00:00:00Z", to+"T23:59:59Z", undefined, undefined);
    const orders = Array.isArray(res) ? res : (res?.data ?? res?.orders ?? res?.content ?? []);
    const cd     = processOrders(orders, from, to);
    await new Promise(r=>setTimeout(r,0));
    await buildCharts(cd);
  } catch(e) {
    error.value = e?.response?.data?.message || e?.message || "Không thể tải dữ liệu.";
  } finally {
    loading.value = false;
  }
}

onMounted(fetchAll);
onBeforeUnmount(()=>Object.values(charts).forEach(c=>c?.destroy()));

function formatVnd(n) {
  return new Intl.NumberFormat("vi-VN",{style:"currency",currency:"VND",maximumFractionDigits:0}).format(n||0);
}
function formatM(n) {
  if (n>=1_000_000_000) return (n/1_000_000_000).toFixed(1)+"B";
  if (n>=1_000_000)     return (n/1_000_000).toFixed(0)+"M";
  return n.toLocaleString("vi-VN");
}
function statusType(s) {
  return {DELIVERED:"success",COMPLETED:"success",CANCELLED:"danger",PENDING:"warning",NEW:"warning",SHIPPING:"info",SHIPPED:"info",PROCESSING:""}[(s||"").toUpperCase()]??"";
}
</script>

<style scoped>
.order-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 8px 48px;
}

.page-header { margin-bottom: 28px; }
.page-title  { font-size: 22px; font-weight: 700; margin: 0 0 4px; }

.error-alert { margin-bottom: 16px; }
.kpi-row     { margin-bottom: 16px; }
.section-row { margin-bottom: 16px; }
.charts-row  { margin-bottom: 14px; }
.divider-sm  { margin: 12px 0; }

/* Status grid */
.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 11px;
  background: var(--sc-bg);
  border: 1px solid color-mix(in srgb, var(--sc) 20%, transparent);
  border-radius: 9px;
}

.sc-dot   { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; background: var(--sc); }
.sc-body  { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.sc-label { font-size: 11px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sc-count { font-size: 16px; font-weight: 700; line-height: 1.2; }
.sc-pct   { font-size: 11px; font-weight: 600; color: var(--sc); flex-shrink: 0; }

/* Channel grid */
.channel-grid { margin-bottom: 0; }
.cc-count { font-size: 22px; font-weight: 700; line-height: 1.1; margin: 4px 0; }
.cc-bar   { margin-top: 6px; }

/* Canvas */
.canvas-wrap          { position: relative; height: 200px; width: 100%; }
.canvas-wrap canvas   { position: absolute; inset: 0; width: 100% !important; height: 100% !important; }
</style>