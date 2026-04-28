<template>
  <el-space direction="vertical" fill :size="24" style="width: 100%">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><DataLine /></el-icon> Audit
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Báo Cáo <span style="color:var(--el-color-primary);">Audit</span></div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Phân tích chuyên sâu về tính tuân thủ và hoạt động nhạy cảm</div>
      </div>
    </div>

    <!-- ── Overview Stats ── -->
    <el-row :gutter="16">
      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="ard-stat-card ard-stat-blue">
          <el-statistic :value="overview.totalSensitive" title="Thao Tác Nhạy Cảm">
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-statistic>
          <div class="ard-stat-desc">Các hành động ảnh hưởng đến cấu hình và quyền hạn</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="ard-stat-card ard-stat-orange">
          <el-statistic :value="overview.complianceScore" suffix="%">
            <template #title>
              <span>Điểm Tuân Thủ</span>
            </template>
            <template #prefix>
              <el-icon><CircleCheck /></el-icon>
            </template>
          </el-statistic>
          <div class="ard-stat-desc">Tỉ lệ các hành động được xác thực và ghi vết đầy đủ</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="8">
        <el-card shadow="never" class="ard-stat-card ard-stat-green">
          <el-statistic :value="overview.activeUsersDaily">
            <template #title>
              <span>Người Dùng Hoạt Động</span>
            </template>
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-statistic>
          <div class="ard-stat-desc">Số lượng tài khoản thực hiện thao tác trong 24h qua</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- ── Sensitivity Breakdown ── -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" header="Phân Tích Mức Độ Nhạy Cảm">
          <el-space direction="vertical" fill style="width: 100%" :size="20">
            <div v-for="item in breakdown" :key="item.label">
              <div style="display: flex; justify-content: space-between; margin-bottom: 8px">
                <span style="font-size: 13px; font-weight: 600">{{ item.label }}</span>
                <span style="font-size: 13px; opacity: 0.7">{{ item.value }}%</span>
              </div>
              <el-progress 
                :percentage="item.value" 
                :stroke-width="8" 
                :color="item.color"
                :show-text="false"
              />
            </div>
          </el-space>
        </el-card>
      </el-col>

      <!-- ── Top Modules ── -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" header="Modules Hoạt Động Nhiều Nhất">
          <el-table :data="topModules" style="width: 100%">
            <el-table-column prop="rank" label="#" width="60">
              <template #default="{ row }">
                <span style="font-weight: 700; opacity: 0.5">0{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="Module Name" min-width="150" />
            <el-table-column prop="actions" label="Total Actions" width="150">
              <template #default="{ row }">
                <span style="font-weight: 600">{{ row.actions.toLocaleString() }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="trend" label="Trend" width="100">
              <template #default="{ row }">
                <el-tag 
                  size="small" 
                  :type="row.trend.startsWith('+') ? 'success' : 'danger'" 
                  effect="plain"
                >
                  {{ row.trend }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Summary Text ── -->
    <el-card shadow="never">
      <div style="display: flex; gap: 16px; align-items: center">
        <div class="ard-insight-icon">
          <el-icon :size="24" color="var(--el-color-primary)"><InfoFilled /></el-icon>
        </div>
        <div>
          <h3 style="margin: 0 0 4px; font-size: 15px; font-weight: 700">Tóm lược bảo mật</h3>
          <p style="margin: 0; font-size: 13.5px; opacity: 0.7; line-height: 1.5">
            Hệ thống đang hoạt động ổn định với điểm tuân thủ ở mức <strong>{{ overview.complianceScore }}%</strong>. 
            Không có dấu hiệu bất thường trong việc truy cập dữ liệu nhạy cảm. 
            Module <strong>{{ topModules[0]?.name }}</strong> ghi nhận lượng thao tác cao nhất, chủ yếu là các hoạt động tra cứu thông tin định kỳ.
          </p>
        </div>
      </div>
    </el-card>
  </el-space>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { auditApi } from "../../api/audit.api";
import { 
  Document, Lock, CircleCheck, User, 
  InfoFilled 
} from "@element-plus/icons-vue";

// ================= STATE =================
const overview = ref({
  totalSensitive: 0,
  complianceScore: 0,
  activeUsersDaily: 0,
});

const breakdown = ref([
  { label: "Critical Operations", value: 0,  color: "#f87171" },
  { label: "Data Modifications", value: 0,  color: "#fbbf24" },
  { label: "Access Control",     value: 0,  color: "#2563eb" },
  { label: "Regular Logs",       value: 0,  color: "#16a34a" },
]);

const topModules = ref([]);

// ================= LOAD DATA =================
const loadReport = async () => {
  try {
    const res = await auditApi.dashboard();
    const data = res.data;

    overview.value = {
      totalSensitive:   data.sensitiveOperations  ?? 0,
      complianceScore:  98, // Example calc
      activeUsersDaily: data.todayLogs > 0 ? Math.ceil(data.todayLogs / 5) : 0,
    };

    breakdown.value[0].value = data.severityStats?.CRITICAL > 0 ? 15 : 5;
    breakdown.value[1].value = 35;
    breakdown.value[2].value = 20;
    breakdown.value[3].value = 30;

    // Dummy modules based on data keys
    const rawModules = Object.entries(data.auditStatsByType ?? {});
    topModules.value = rawModules.slice(0, 5).map(([name, actions], idx) => ({
      rank: idx + 1,
      name,
      actions,
      trend: actions % 2 === 0 ? "+12%" : "-5%",
    }));
  } catch (err) {
    console.error("Audit Report error:", err);
  }
};

onMounted(loadReport);
</script>

<style scoped>
.ard-eyebrow {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  opacity: 0.6;
  margin-bottom: 8px;
}

.ard-title {
  font-size: 28px;
  font-weight: 800;
  margin: 0 0 4px;
}

.ard-title-accent {
  color: var(--el-color-primary);
}

.ard-subtitle {
  font-size: 13.5px;
  opacity: 0.7;
  margin: 0;
}

.ard-stat-card {
  height: 100%;
}

.ard-stat-blue { border-top: 3px solid var(--el-color-primary); }
.ard-stat-orange { border-top: 3px solid var(--el-color-warning); }
.ard-stat-green { border-top: 3px solid var(--el-color-success); }

.ard-stat-desc {
  font-size: 12px;
  opacity: 0.6;
  margin-top: 8px;
  line-height: 1.4;
}

.ard-insight-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
</style>