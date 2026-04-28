<template>
  <el-space direction="vertical" fill :size="24" style="width: 100%">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Odometer /></el-icon> Audit & Security
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Bảng điều khiển <span style="color:var(--el-color-primary);">Audit & Log</span></div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Tổng quan giám sát bảo mật và hoạt động hệ thống theo thời gian thực</div>
      </div>
    </div>

    <!-- ── Summary Cards ── -->
    <el-row :gutter="16">
      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-blue">
          <el-statistic :value="summary.totalLogs">
            <template #title>
              <el-space :size="8">
                <el-icon><Document /></el-icon>
                <span>Tổng Logs</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-orange">
          <el-statistic :value="summary.todayLogs">
            <template #title>
              <el-space :size="8">
                <el-icon><Calendar /></el-icon>
                <span>Logs Hôm Nay</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-red">
          <el-statistic :value="summary.securityViolations">
            <template #title>
              <el-space :size="8">
                <el-icon><Warning /></el-icon>
                <span>Vi Phạm Bảo Mật</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-yellow">
          <el-statistic :value="summary.suspiciousActivities">
            <template #title>
              <el-space :size="8">
                <el-icon><WarningFilled /></el-icon>
                <span>Hoạt Động Đáng Ngờ</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-green">
          <el-statistic :value="summary.sensitiveOperations">
            <template #title>
              <el-space :size="8">
                <el-icon><Lock /></el-icon>
                <span>Thao Tác Nhạy Cảm</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="ad-stat-card ad-stat-purple">
          <el-statistic :value="summary.auditActionsToday">
            <template #title>
              <el-space :size="8">
                <el-icon><List /></el-icon>
                <span>Audit Hôm Nay</span>
              </el-space>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Charts Row ── -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" header="Thống Kê Mức Độ">
          <div class="ad-chart-container">
            <Doughnut v-if="severityChart?.datasets" :data="severityChart" :options="doughnutOptions" />
            <div v-else class="ad-chart-empty">
              <el-icon :size="28"><Loading /></el-icon>
              <span>Đang tải dữ liệu...</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12">
        <el-card shadow="never" header="Audit Theo Loại Hành Động">
          <div class="ad-chart-container">
            <Bar v-if="auditChart?.datasets" :data="auditChart" :options="barOptions" />
            <div v-else class="ad-chart-empty">
              <el-icon :size="28"><Loading /></el-icon>
              <span>Đang tải dữ liệu...</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Weekly Trend ── -->
    <el-card shadow="never" header="Xu Hướng Log & Audit Trong Tuần">
      <div class="ad-chart-container-wide">
        <Line v-if="weeklyTrendChart?.datasets" :data="weeklyTrendChart" :options="lineOptions" />
        <div v-else class="ad-chart-empty">
          <el-icon :size="28"><Loading /></el-icon>
          <span>Đang tải dữ liệu...</span>
        </div>
      </div>
    </el-card>
  </el-space>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { auditApi } from "../../api/audit.api";
import { 
  Operation, Document, Calendar, Warning, WarningFilled, 
  Lock, List, Loading 
} from "@element-plus/icons-vue";

import { Bar, Doughnut, Line } from "vue-chartjs";
import {
  Chart,
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from "chart.js";

Chart.register(
  BarElement,
  CategoryScale,
  LinearScale,
  ArcElement,
  PointElement,
  LineElement,
  Tooltip,
  Legend
);

// ================= STATE =================
const summary = ref({
  todayLogs: 0,
  securityViolations: 0,
  suspiciousActivities: 0,
  sensitiveOperations: 0,
  totalLogs: 0,
  auditActionsToday: 0,
});

const severityChart = ref(null);
const auditChart    = ref(null);
const weeklyTrendChart = ref(null);

// ================= CHART OPTIONS =================
const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: "bottom", labels: { font: { family: "inherit", size: 12 }, padding: 16 } },
    tooltip: { bodyFont: { family: "inherit" }, titleFont: { family: "inherit" } },
  },
};

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: { bodyFont: { family: "inherit" }, titleFont: { family: "inherit" } },
  },
  scales: {
    x: { grid: { display: false } },
    y: { grid: { color: "#f0f2f5" } },
  },
};

const lineOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: "top", labels: { font: { family: "inherit", size: 12 }, padding: 16 } },
    tooltip: { bodyFont: { family: "inherit" }, titleFont: { family: "inherit" } },
  },
  scales: {
    x: { grid: { display: false } },
    y: { grid: { color: "#f0f2f5" } },
  },
};

// ================= LOAD DASHBOARD =================
const loadDashboard = async () => {
  try {
    const res  = await auditApi.dashboard();
    const data = res.data;

    // ===== SUMMARY CARDS =====
    summary.value = {
      todayLogs:            data.todayLogs            ?? 0,
      securityViolations:   data.securityViolations   ?? 0,
      suspiciousActivities: data.suspiciousActivities ?? 0,
      sensitiveOperations:  data.sensitiveOperations  ?? 0,
      totalLogs:            data.totalLogs            ?? 0,
      auditActionsToday:    data.auditActionsToday    ?? 0,
    };

    // ===== SEVERITY CHART (Doughnut) =====
    severityChart.value = {
      labels: ["CAO", "TRUNG BÌNH", "NGHIÊM TRỌNG"],
      datasets: [
        {
          label: "Số lượng",
          backgroundColor: ["#f87171", "#fbbf24", "#ef4444"],
          borderWidth: 0,
          data: [
            data.severityStats?.HIGH     ?? 0,
            data.severityStats?.MEDIUM   ?? 0,
            data.severityStats?.CRITICAL ?? 0,
          ],
        },
      ],
    };

    // ===== AUDIT CHART BY TYPE (Bar) =====
    auditChart.value = {
      labels: Object.keys(data.auditStatsByType ?? {}),
      datasets: [
        {
          label: "Hành động Audit",
          backgroundColor: "#2563eb",
          borderRadius: 6,
          data: Object.values(data.auditStatsByType ?? {}),
        },
      ],
    };

    // ===== WEEKLY TREND (Line) =====
    weeklyTrendChart.value = {
      labels: ["Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"],
      datasets: [
        {
          label: "Security Logs",
          borderColor: "#f87171",
          backgroundColor: "rgba(248,113,113,0.12)",
          fill: true,
          tension: 0.4,
          pointBackgroundColor: "#f87171",
          pointRadius: 4,
          data: [
            data.thisWeekLogs * 0.5,
            data.thisWeekLogs * 0.6,
            data.thisWeekLogs * 0.7,
            data.thisWeekLogs * 1.0,
            data.thisWeekLogs * 0.9,
            data.thisWeekLogs * 1.2,
            data.thisWeekLogs,
          ],
        },
        {
          label: "Hành động Audit",
          borderColor: "#2563eb",
          backgroundColor: "rgba(37,99,235,0.1)",
          fill: true,
          tension: 0.4,
          pointBackgroundColor: "#2563eb",
          pointRadius: 4,
          data: [
            data.auditActionsThisWeek * 0.5,
            data.auditActionsThisWeek * 0.6,
            data.auditActionsThisWeek * 0.7,
            data.auditActionsThisWeek * 1.0,
            data.auditActionsThisWeek * 0.9,
            data.auditActionsThisWeek * 1.2,
            data.auditActionsThisWeek,
          ],
        },
      ],
    };
  } catch (err) {
    console.error("Audit Dashboard error:", err);
  }
};

onMounted(loadDashboard);
</script>

<style scoped>
.ad-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  opacity: 0.6;
  margin-bottom: 8px;
}

.ad-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 4px;
}

.ad-title-accent {
  color: var(--el-color-primary);
}

.ad-subtitle {
  font-size: 13.5px;
  opacity: 0.7;
  margin: 0;
}

.ad-stat-card {
  height: 100%;
}

.ad-stat-blue { border-top: 3px solid var(--el-color-primary); }
.ad-stat-orange { border-top: 3px solid var(--el-color-warning); }
.ad-stat-red { border-top: 3px solid var(--el-color-danger); }
.ad-stat-yellow { border-top: 3px solid #ca8a04; }
.ad-stat-green { border-top: 3px solid var(--el-color-success); }
.ad-stat-purple { border-top: 3px solid #7c3aed; }

.ad-chart-container {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ad-chart-container-wide {
  height: 300px;
}

.ad-chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  opacity: 0.5;
  font-size: 13px;
}
</style>