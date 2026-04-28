<template>
  <div style="background: #f4f5f9; min-height: 100vh;">

    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><DataAnalysis /></el-icon> Báo cáo & Phân tích
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Tổng quan kinh doanh</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Doanh thu · Hiệu suất · Trạng thái đơn hàng</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-tag effect="plain" size="large" style="border-radius:99px;">{{ todayFormatted }}</el-tag>
      </div>
    </div>

    <!-- KPI Strip -->
    <el-row v-if="channelData.length" :gutter="14" style="margin-bottom:24px;">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" :body-style="{ padding:'16px 18px' }" style="border-left:3px solid var(--el-color-primary); margin-bottom:10px;">
          <div style="display:flex; align-items:center; gap:14px;">
            <el-icon :size="28" style="color:var(--el-color-primary); flex-shrink:0;"><DataAnalysis /></el-icon>
            <div>
              <div style="font-size:12px; color:var(--el-text-color-secondary); margin-bottom:3px;">Tổng doanh thu</div>
              <div style="font-size:16px; font-weight:900; white-space:nowrap;">{{ formatMoney(totalRevenue) }}&thinsp;₫</div>
              <div style="font-size:11px; color:var(--el-color-success); margin-top:3px; font-weight:600;">↑ All channels</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6" v-for="ch in channelData" :key="ch.channel">
        <el-card shadow="never" :body-style="{ padding:'16px 18px' }"
          :style="`border-left:3px solid ${ch.channel==='ONLINE'?'var(--el-color-primary)':'var(--el-color-warning)'}; margin-bottom:10px;`">
          <div style="display:flex; align-items:center; gap:14px;">
            <el-icon :size="28" :style="`color:${ch.channel==='ONLINE'?'var(--el-color-primary)':'var(--el-color-warning)'}; flex-shrink:0;`">
              <component :is="ch.channel==='ONLINE' ? 'ChromeFilled' : 'House'" />
            </el-icon>
            <div>
              <div style="font-size:12px; color:var(--el-text-color-secondary); margin-bottom:3px;">{{ ch.channel }}</div>
              <div style="font-size:16px; font-weight:900; white-space:nowrap;">{{ formatMoney(ch.revenue) }}&thinsp;₫</div>
              <div :style="`font-size:11px; margin-top:3px; font-weight:600; color:${ch.channel==='ONLINE'?'var(--el-color-primary)':'var(--el-color-warning)'};`">
                {{ ((ch.revenue/totalRevenue)*100).toFixed(1) }}% tổng
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" :body-style="{ padding:'16px 18px' }" style="border-left:3px solid var(--el-color-danger); margin-bottom:10px;">
          <div style="display:flex; align-items:center; gap:14px;">
            <el-icon :size="28" style="color:var(--el-color-danger); flex-shrink:0;"><RefreshLeft /></el-icon>
            <div>
              <div style="font-size:12px; color:var(--el-text-color-secondary); margin-bottom:3px;">Tổng tiền đã hoàn lại</div>
              <div style="font-size:16px; font-weight:900; white-space:nowrap;">{{ formatMoney(totalRefundAmount) }}&thinsp;₫</div>
              <div style="font-size:11px; color:var(--el-color-danger); margin-top:3px; font-weight:600;">{{ returnsData.length }} sản phẩm trả</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 1: Channel + Staff -->
    <el-row :gutter="20" style="margin-bottom:20px;">

      <!-- Revenue by Channel -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; gap:10px;">
              <el-avatar :size="32" style="background:var(--el-color-warning-light-8); flex-shrink:0;">
                <el-icon style="color:var(--el-color-warning);"><TrendCharts /></el-icon>
              </el-avatar>
              <div>
                <div style="font-weight:700; font-size:14px;">Doanh thu theo kênh</div>
                <div style="font-size:12px; color:var(--el-text-color-secondary);">Revenue by Channel</div>
              </div>
            </div>
          </template>

          <div v-if="channelData.length && totalRevenue" style="margin-bottom:16px;">
            <div v-for="ch in channelData" :key="ch.channel" style="margin-bottom:14px;">
              <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:6px;">
                <div style="display:flex; align-items:center; gap:8px;">
                  <span :style="`width:8px;height:8px;border-radius:50%;background:${ch.channel==='ONLINE'?'var(--el-color-primary)':'var(--el-color-warning)'};display:inline-block;flex-shrink:0;`"></span>
                  <el-tag :type="ch.channel==='ONLINE'?'primary':'warning'" effect="plain" size="small" round>{{ ch.channel }}</el-tag>
                </div>
                <div style="display:flex; align-items:center; gap:12px;">
                  <span style="font-weight:700; font-family:monospace; font-size:13px;">{{ formatMoney(ch.revenue) }}&thinsp;₫</span>
                  <span style="font-size:12px; color:var(--el-text-color-secondary);">{{ ((ch.revenue/totalRevenue)*100).toFixed(1) }}%</span>
                </div>
              </div>
              <el-progress :percentage="+(((ch.revenue/totalRevenue)*100).toFixed(1))" :stroke-width="8" :show-text="false"
                :color="ch.channel==='ONLINE'?'var(--el-color-primary)':'var(--el-color-warning)'" />
            </div>
          </div>

          <el-table :data="channelData" stripe size="small">
            <el-table-column prop="channel" label="Kênh bán hàng">
              <template #default="{ row }">
                <el-tag :type="row.channel==='ONLINE'?'primary':'warning'" effect="plain" size="small" round>{{ row.channel }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Doanh thu" align="right">
              <template #default="{ row }">
                <span style="font-weight:700; font-family:monospace;">{{ formatMoney(row.revenue) }}&thinsp;₫</span>
              </template>
            </el-table-column>
            <el-table-column label="Tỷ trọng" align="right" width="110">
              <template #default="{ row }">
                <span style="font-weight:700; color:var(--el-color-primary);">{{ ((row.revenue/totalRevenue)*100).toFixed(1) }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Orders by Staff — fixed layout -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <div style="display:flex; align-items:center; gap:10px;">
                <el-avatar :size="32" style="background:var(--el-color-primary-light-8); flex-shrink:0;">
                  <el-icon style="color:var(--el-color-primary);"><User /></el-icon>
                </el-avatar>
                <div>
                  <div style="font-weight:700; font-size:14px;">Hiệu suất nhân viên</div>
                  <div style="font-size:12px; color:var(--el-text-color-secondary);">Orders by Staff</div>
                </div>
              </div>
              <el-tag effect="plain" size="small">{{ staffData.length }} nhân viên</el-tag>
            </div>
          </template>

          <div v-if="!staffData.length" style="padding:32px 0; color:var(--el-text-color-secondary); font-size:13px;">
            Chưa có dữ liệu nhân viên
          </div>
          <div v-else>
            <div
              v-for="(s, idx) in staffData" :key="s.staff"
              style="display:flex; align-items:center; gap:12px; padding:10px 0; border-bottom:1px solid var(--el-border-color-lighter);"
            >
              <!-- Rank badge -->
              <div :style="`
                width:26px; height:26px; border-radius:50%; flex-shrink:0;
                background:${['#f59e0b','#94a3b8','#ea580c'][idx]||'var(--el-fill-color-dark)'};
                color:#fff; font-size:11px; font-weight:800;
                display:flex; align-items:center; justify-content:center;
              `">{{ idx+1 }}</div>

              <!-- Avatar -->
              <el-avatar :size="36" style="flex-shrink:0; background:var(--el-fill-color); font-size:13px; font-weight:700; color:var(--el-text-color-regular);">
                {{ getInitials(s.staff) }}
              </el-avatar>

              <!-- Name + progress bar -->
              <div style="flex:1; min-width:0;">
                <div style="font-size:13px; font-weight:600; margin-bottom:6px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{{ s.staff }}</div>
                <el-progress
                  :percentage="+(((s.orders/staffData[0].orders)*100).toFixed(0))"
                  :stroke-width="6" :show-text="false"
                  color="var(--el-color-primary)"
                />
              </div>

              <!-- Order count -->
              <div style="font-size:13px; font-weight:700; white-space:nowrap; flex-shrink:0; min-width:48px; text-align:right;">
                {{ s.orders }}&thinsp;đơn
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Revenue by Date -->
    <el-card shadow="never" style="margin-bottom:20px;">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px;">
          <div style="display:flex; align-items:center; gap:10px;">
            <el-avatar :size="32" style="background:var(--el-color-primary-light-8); flex-shrink:0;">
              <el-icon style="color:var(--el-color-primary);"><Calendar /></el-icon>
            </el-avatar>
            <div>
              <div style="font-weight:700; font-size:14px;">Doanh thu theo ngày</div>
              <div style="font-size:12px; color:var(--el-text-color-secondary);">Revenue by Date</div>
            </div>
          </div>
          <el-tag effect="plain" size="small">{{ filteredDateData.length }} / {{ dateData.length }} bản ghi</el-tag>
        </div>
      </template>

      <div style="display:flex; align-items:center; flex-wrap:wrap; gap:12px; margin-bottom:16px;">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="→"
          start-placeholder="Từ ngày" end-placeholder="Đến ngày"
          format="DD/MM/YYYY" value-format="YYYY-MM-DD"
          style="width:320px; max-width:100%;" @change="activeQuick = null" />
        <el-radio-group v-model="activeQuick" size="small" @change="applyQuick">
          <el-radio-button v-for="q in quickRanges" :key="q.label" :value="q.label">{{ q.label }}</el-radio-button>
        </el-radio-group>
      </div>

      <el-alert v-if="dateRange && filteredTotal" type="info" :closable="false" style="margin-bottom:12px;">
        Tổng khoảng chọn: <strong>{{ formatMoney(filteredTotal) }}&thinsp;₫</strong>
      </el-alert>

      <el-table :data="filteredDateData" :max-height="440" stripe size="small">
        <el-table-column type="index" label="#" width="52" />
        <el-table-column prop="date" label="Ngày" min-width="180">
          <template #default="{ row }"><span style="font-family:monospace;">{{ formatDate(row.date) }}</span></template>
        </el-table-column>
        <el-table-column label="Doanh thu" align="right">
          <template #default="{ row }">
            <span style="font-weight:700; font-family:monospace;">{{ formatMoney(row.revenue) }}&thinsp;₫</span>
          </template>
        </el-table-column>
        <el-table-column label="vs Trung bình" align="right" width="140">
          <template #default="{ row }">
            <el-tag :type="row.revenue>=avgRevenue?'success':'danger'" size="small" effect="plain" round>
              {{ row.revenue>=avgRevenue?'▲':'▼' }} {{ Math.abs(((row.revenue-avgRevenue)/avgRevenue)*100).toFixed(0) }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Revenue Summary Week/Month -->
    <el-card shadow="never" style="margin-bottom:20px;">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px;">
          <div style="display:flex; align-items:center; gap:10px;">
            <el-avatar :size="32" style="background:var(--el-color-primary-light-8); flex-shrink:0;">
              <el-icon style="color:var(--el-color-primary);"><TrendCharts /></el-icon>
            </el-avatar>
            <div>
              <div style="font-weight:700; font-size:14px;">Tổng hợp doanh thu</div>
              <div style="font-size:12px; color:var(--el-text-color-secondary);">Revenue Summary · Tuần / Tháng</div>
            </div>
          </div>
          <el-radio-group v-model="activePeriod" size="small">
            <el-radio-button value="week">Theo tuần</el-radio-button>
            <el-radio-button value="month">Theo tháng</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <!-- KPI strip -->
      <div v-if="activePeriodData.length" style="display:flex; border-bottom:1px solid var(--el-border-color-lighter); margin-bottom:20px;">
        <div v-for="kpi in periodKpis" :key="kpi.label" style="flex:1; padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
          <div style="font-size:12px; color:var(--el-text-color-secondary); margin-bottom:5px;">{{ kpi.label }}</div>
          <div :style="`font-size:15px; font-weight:800; font-family:monospace; color:${kpi.color||'var(--el-text-color-primary)'};`">{{ kpi.value }}</div>
        </div>
      </div>

      <!-- ── Bar chart — đã fix ── -->
      <div v-if="activePeriodData.length" style="margin:0 16px 24px;">
        <!--
          Cách fix: container cao cố định, mỗi cột dùng
          display:flex; flex-direction:column; justify-content:flex-end; height:100%
          Bar dùng height:X% (%) — works vì parent có height tuyệt đối.
        -->
        <div style="display:flex; align-items:flex-end; height:160px; gap:6px; border-bottom:2px solid var(--el-border-color-light); overflow-x:auto; padding-bottom:0;">
          <div
            v-for="(row, idx) in activePeriodData" :key="idx"
            :title="`${formatPeriodLabel(row)} — ${formatMoney(row.revenue)} ₫`"
            style="flex:1; min-width:32px; max-width:72px; display:flex; flex-direction:column; align-items:stretch; justify-content:flex-end; height:100%;"
          >
            <!-- Value above bar -->
            <div style="font-size:9px; color:var(--el-text-color-secondary); font-family:monospace; white-space:nowrap; margin-bottom:3px; padding-left:2px;">
              {{ formatMoneyShort(row.revenue) }}
            </div>
            <!-- Bar -->
            <div :style="`
              height:${maxPeriodRevenue > 0 ? Math.max(4, (Number(row.revenue)/maxPeriodRevenue)*140) : 4}px;
              background:var(--el-color-primary);
              border-radius:3px 3px 0 0;
              opacity:${0.45 + (Number(row.revenue)/maxPeriodRevenue)*0.55};
              transition:height 0.8s cubic-bezier(.4,0,.2,1);
            `"></div>
          </div>
        </div>
        <!-- Period labels -->
        <div style="display:flex; gap:6px; padding-top:6px; overflow-x:auto;">
          <div
            v-for="(row, idx) in activePeriodData" :key="idx"
            style="flex:1; min-width:32px; max-width:72px;"
          >
            <div style="font-size:9px; color:var(--el-text-color-secondary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis; padding-left:2px;">
              {{ formatPeriodLabel(row) }}
            </div>
          </div>
        </div>
      </div>

      <el-table :data="activePeriodData" :max-height="360" stripe size="small">
        <el-table-column type="index" label="#" width="52"/>
        <el-table-column label="Kỳ" min-width="200">
          <template #default="{ row }"><span style="font-family:monospace;">{{ formatPeriodLabel(row) }}</span></template>
        </el-table-column>
        <el-table-column label="Doanh thu" align="right">
          <template #default="{ row }">
            <span style="font-weight:700; font-family:monospace;">{{ formatMoney(row.revenue) }}&thinsp;₫</span>
          </template>
        </el-table-column>
        <el-table-column label="vs Trung bình" align="right" width="150">
          <template #default="{ row }">
            <el-tag :type="Number(row.revenue)>=periodAvgRevenue?'success':'danger'" size="small" effect="plain" round>
              {{ Number(row.revenue)>=periodAvgRevenue?'▲':'▼' }}
              {{ Math.abs(((Number(row.revenue)-periodAvgRevenue)/periodAvgRevenue)*100).toFixed(0) }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="% Tổng" align="right" width="110">
          <template #default="{ row }">
            <span style="font-weight:700; color:var(--el-color-primary);">{{ ((Number(row.revenue)/periodTotalRevenue)*100).toFixed(1) }}%</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!activePeriodData.length" description="Chưa có dữ liệu" :image-size="60" />
    </el-card>

    <!-- Top Products -->
    <el-card shadow="never" style="margin-bottom:20px;">
      <template #header>
        <div style="display:flex; align-items:center; justify-content:space-between;">
          <div style="display:flex; align-items:center; gap:10px;">
            <el-avatar :size="32" style="background:var(--el-color-warning-light-8); flex-shrink:0;">
              <el-icon style="color:var(--el-color-warning);"><TrendCharts /></el-icon>
            </el-avatar>
            <div>
              <div style="font-weight:700; font-size:14px;">Top sản phẩm bán chạy</div>
              <div style="font-size:12px; color:var(--el-text-color-secondary);">Best-selling Products · DELIVERED</div>
            </div>
          </div>
          <el-tag effect="plain" size="small">Top {{ topProducts.length }}</el-tag>
        </div>
      </template>

      <div v-if="topProducts.length" style="border-bottom:1px solid var(--el-border-color-lighter); margin-bottom:4px;">
        <div v-for="(p, idx) in topProducts" :key="idx"
          style="display:flex; align-items:center; gap:12px; padding:10px 8px;">
          <el-avatar :size="24"
            :style="`background:${['#f59e0b','#94a3b8','#ea580c'][idx]||'var(--el-color-primary-light-5)'};color:#fff;font-size:11px;font-weight:800;flex-shrink:0;`"
          >{{ idx+1 }}</el-avatar>
          <div style="flex:1; min-width:0;">
            <div style="display:flex; align-items:baseline; gap:8px; flex-wrap:wrap; margin-bottom:6px;">
              <span style="font-size:13px; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:280px;">{{ p.productName }}</span>
              <span v-if="p.variantName" style="font-size:11px; color:var(--el-text-color-secondary);">{{ p.variantName }}</span>
              <span v-if="p.sku" style="font-size:10px; color:var(--el-text-color-placeholder); font-family:monospace;">{{ p.sku }}</span>
            </div>
            <el-progress
              :percentage="+(((Number(p.totalQty)/Number(topProducts[0].totalQty))*100).toFixed(0))"
              :stroke-width="5" :show-text="false"
              :color="['#f59e0b','#94a3b8','#ea580c','var(--el-color-primary)'][idx]||'var(--el-color-primary)'"
            />
          </div>
          <div style="text-align:right; flex-shrink:0;">
            <div style="font-family:monospace; font-weight:700; font-size:13px;">{{ p.totalQty }}&thinsp;SP</div>
            <div style="font-family:monospace; font-size:11px; color:var(--el-text-color-secondary);">{{ formatMoney(p.totalRevenue) }}&thinsp;₫</div>
          </div>
        </div>
      </div>

      <el-table v-if="topProducts.length" :data="topProducts" :max-height="420" stripe size="small">
        <el-table-column type="index" label="#" width="52"/>
        <el-table-column label="Sản phẩm" min-width="220">
          <template #default="{ row }">
            <div>
              <div style="font-weight:600;">{{ row.productName }}</div>
              <div v-if="row.variantName" style="font-size:11px; color:var(--el-text-color-secondary);">{{ row.variantName }}</div>
              <div v-if="row.sku" style="font-size:10px; color:var(--el-text-color-placeholder); font-family:monospace;">{{ row.sku }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="SL bán" align="right" width="100">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain" size="small" style="font-family:monospace; font-weight:700;">{{ row.totalQty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Doanh thu" align="right" min-width="160">
          <template #default="{ row }">
            <span style="font-weight:700; font-family:monospace;">{{ formatMoney(row.totalRevenue) }}&thinsp;₫</span>
          </template>
        </el-table-column>
        <el-table-column label="% SL" align="right" width="100">
          <template #default="{ row }">
            <span style="font-weight:700; color:var(--el-color-primary);">{{ totalTopQty ? ((Number(row.totalQty)/totalTopQty)*100).toFixed(1) : 0 }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="% Doanh thu" align="right" width="120">
          <template #default="{ row }">
            <span style="font-weight:700; color:var(--el-color-primary);">{{ totalTopRevenue ? ((Number(row.totalRevenue)/totalTopRevenue)*100).toFixed(1) : 0 }}%</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="Chưa có dữ liệu sản phẩm" :image-size="60" />
    </el-card>

    <!-- Status + Returns -->
    <el-row :gutter="20" style="margin-bottom:20px;">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; gap:10px;">
              <el-avatar :size="32" style="background:var(--el-color-warning-light-8); flex-shrink:0;">
                <el-icon style="color:var(--el-color-warning);"><Clock /></el-icon>
              </el-avatar>
              <div>
                <div style="font-weight:700; font-size:14px;">Trạng thái đơn hàng</div>
                <div style="font-size:12px; color:var(--el-text-color-secondary);">Revenue by Order Status</div>
              </div>
            </div>
          </template>

          <div v-if="statusData.length" style="margin-bottom:16px;">
            <div v-for="s in statusData" :key="s.status" style="margin-bottom:12px;">
              <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:5px; flex-wrap:wrap; gap:4px;">
                <div style="display:flex; align-items:center; gap:8px;">
                  <el-tag :type="statusTagType(s.status)" size="small" effect="plain" round>{{ statusLabel(s.status) }}</el-tag>
                  <span style="font-size:11px; color:var(--el-text-color-secondary);">{{ s.orderCount }} đơn</span>
                </div>
                <span style="font-family:monospace; font-weight:600; font-size:12.5px;">{{ formatMoney(s.totalRevenue) }}&thinsp;₫</span>
              </div>
              <el-progress :percentage="totalStatusRevenue ? +((s.totalRevenue/totalStatusRevenue)*100).toFixed(1) : 0"
                :stroke-width="7" :show-text="false" :color="statusColor(s.status)" />
            </div>
          </div>

          <el-table v-if="statusData.length" :data="statusData" stripe size="small">
            <el-table-column label="Trạng thái" min-width="150">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small" effect="plain" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Số đơn" align="right" width="90">
              <template #default="{ row }"><span style="font-family:monospace;">{{ row.orderCount }}</span></template>
            </el-table-column>
            <el-table-column label="Doanh thu" align="right">
              <template #default="{ row }">
                <span style="font-weight:700; font-family:monospace;">{{ formatMoney(row.totalRevenue) }}&thinsp;₫</span>
              </template>
            </el-table-column>
            <el-table-column label="Tỷ trọng" align="right" width="100">
              <template #default="{ row }">
                <span style="font-weight:700; color:var(--el-color-primary);">{{ totalStatusRevenue ? ((row.totalRevenue/totalStatusRevenue)*100).toFixed(1) : 0 }}%</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="Chưa có dữ liệu" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div style="display:flex; align-items:center; justify-content:space-between;">
              <div style="display:flex; align-items:center; gap:10px;">
                <el-avatar :size="32" style="background:var(--el-color-danger-light-8); flex-shrink:0;">
                  <el-icon style="color:var(--el-color-danger);"><RefreshLeft /></el-icon>
                </el-avatar>
                <div>
                  <div style="font-weight:700; font-size:14px;">Trả hàng theo sản phẩm</div>
                  <div style="font-size:12px; color:var(--el-text-color-secondary);">Returns by Product (excl. Rejected)</div>
                </div>
              </div>
              <el-tag effect="plain" size="small">{{ returnsData.length }} sản phẩm</el-tag>
            </div>
          </template>

          <el-table v-if="returnsData.length" :data="returnsData" :max-height="440" stripe size="small">
            <el-table-column type="index" label="#" width="48"/>
            <el-table-column label="Sản phẩm" min-width="200">
              <template #default="{ row }">
                <div>
                  <div style="font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; max-width:260px;">{{ row.productName }}</div>
                  <div v-if="row.variantName" style="font-size:11px; color:var(--el-text-color-secondary);">{{ row.variantName }}</div>
                  <div v-if="row.sku" style="font-size:10px; color:var(--el-text-color-placeholder); font-family:monospace;">{{ row.sku }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Lần trả" align="center" width="80">
              <template #default="{ row }">
                <el-avatar :size="26" style="background:var(--el-color-danger-light-8); color:var(--el-color-danger); font-size:12px; font-weight:700;">{{ row.returnCount }}</el-avatar>
              </template>
            </el-table-column>
            <el-table-column label="Tổng SL" align="right" width="80">
              <template #default="{ row }"><span style="font-family:monospace;">{{ row.totalQty }}</span></template>
            </el-table-column>
            <el-table-column label="Hoàn tiền" align="right" min-width="140">
              <template #default="{ row }">
                <span style="font-weight:600; font-family:monospace; color:var(--el-color-danger);">{{ formatMoney(row.totalRefund) }}&thinsp;₫</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="Không có dữ liệu trả hàng" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- Revenue by Customer -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex; align-items:flex-start; justify-content:space-between; flex-wrap:wrap; gap:12px;">
          <div>
            <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.07em; font-weight:700; color:var(--el-text-color-secondary); margin-bottom:5px;">Doanh thu theo khách hàng</div>
            <div style="display:flex; align-items:baseline; gap:8px;">
              <span style="font-size:18px; font-weight:800; letter-spacing:-0.02em;">{{ revenueByCustomer.length }}</span>
              <span style="color:var(--el-text-color-secondary);">khách hàng</span>
            </div>
          </div>
          <div>
            <div style="font-size:11px; text-transform:uppercase; letter-spacing:0.07em; font-weight:700; color:var(--el-text-color-secondary); margin-bottom:5px;">Tổng doanh thu</div>
            <div style="font-size:20px; font-weight:800; font-family:monospace; color:var(--el-color-success);">{{ totalRevenueByCustomer.toLocaleString() }} đ</div>
          </div>
        </div>
      </template>

      <el-skeleton v-if="loadingRevenueByCustomer" :rows="6" animated />
      <el-empty v-else-if="!revenueByCustomer.length" description="Không có dữ liệu doanh thu" :image-size="70" />

      <el-table v-else :data="revenueByCustomer" stripe size="small"
        :header-cell-style="{ background:'var(--el-fill-color-light)', color:'var(--el-text-color-secondary)', fontSize:'11px', fontWeight:'700', textTransform:'uppercase', letterSpacing:'0.07em' }">
        <el-table-column type="index" label="#" width="52" align="center">
          <template #default="{ $index }">
            <el-tag :type="$index < 3 ? 'primary' : 'info'" effect="plain" size="small" style="font-family:monospace; min-width:28px;">{{ $index+1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Khách hàng" min-width="200">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; gap:10px;">
              <el-avatar :size="32" :style="`background:${getAvatarColor(row.customerName)};color:#fff;font-size:12px;font-weight:700;flex-shrink:0;border-radius:8px;`">
                {{ (row.customerName||'?').charAt(0).toUpperCase() }}
              </el-avatar>
              <span style="font-weight:600; font-size:13.5px;">{{ row.customerName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Doanh thu" align="right" sortable prop="totalRevenue">
          <template #default="{ row }">
            <span style="font-family:monospace; font-weight:700; white-space:nowrap;">{{ row.totalRevenue?.toLocaleString() }} đ</span>
          </template>
        </el-table-column>
        <el-table-column label="Tỷ trọng" align="right" width="110">
          <template #default="{ row }">
            <span style="font-weight:700; color:var(--el-color-primary); font-family:monospace;">
              {{ totalRevenueByCustomer ? ((row.totalRevenue/totalRevenueByCustomer)*100).toFixed(1) : 0 }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Tỷ lệ" width="200">
          <template #default="{ row }">
            <div style="padding-right:16px;">
              <el-progress :percentage="totalRevenueByCustomer ? +((row.totalRevenue/totalRevenueByCustomer)*100).toFixed(1) : 0"
                :stroke-width="6" :show-text="false" color="var(--el-color-primary)" />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

  </div>
</template>

<script setup>
import {
  Calendar, ChromeFilled, Clock, DataAnalysis,
  House, RefreshLeft, ShoppingCart, TrendCharts, User,
} from "@element-plus/icons-vue";
import { ref, computed, onMounted } from "vue";
import reportsApi from "../../api/report.api";
import { ordersApi } from "../../api/orders.api";

const channelData  = ref([]);
const dateData     = ref([]);
const staffData    = ref([]);
const statusData   = ref([]);
const returnsData  = ref([]);
const weekData     = ref([]);
const monthData    = ref([]);
const topProducts  = ref([]);
const revenueByCustomer        = ref([]);
const loadingRevenueByCustomer = ref(false);

const dateRange   = ref(null);
const activeQuick = ref(null);
const activePeriod = ref("week");

const todayFormatted = new Date().toLocaleDateString("vi-VN", {
  weekday: "long", year: "numeric", month: "long", day: "numeric",
});

const quickRanges = [
  { label: "Tháng này",  getDates: () => { const n=new Date(); return { from:`${n.getFullYear()}-${String(n.getMonth()+1).padStart(2,"0")}-01`, to:toYMD(n) }; } },
  { label: "Tháng trước",getDates: () => { const n=new Date(), m=new Date(n.getFullYear(),n.getMonth()-1,1), l=new Date(n.getFullYear(),n.getMonth(),0); return { from:toYMD(m), to:toYMD(l) }; } },
  { label: "30 ngày",    getDates: () => { const n=new Date(), f=new Date(n); f.setDate(f.getDate()-29); return { from:toYMD(f), to:toYMD(n) }; } },
  { label: "7 ngày",     getDates: () => { const n=new Date(), f=new Date(n); f.setDate(f.getDate()-6);  return { from:toYMD(f), to:toYMD(n) }; } },
];

const totalRevenue = computed(() => channelData.value.reduce((s,r)=>s+Number(r.revenue||0),0));
const totalRefundAmount = computed(() => returnsData.value.reduce((s,r)=>s+Number(r.totalRefund||0),0));
const avgRevenue   = computed(() => dateData.value.length ? dateData.value.reduce((s,r)=>s+Number(r.revenue),0)/dateData.value.length : 0);

const filterFrom = computed(() => dateRange.value?.[0]||null);
const filterTo   = computed(() => dateRange.value?.[1]||null);
const filteredDateData = computed(() => {
  let data = dateData.value;
  if (filterFrom.value) data = data.filter(r=>(r.date?.substring(0,10)??"")>=filterFrom.value);
  if (filterTo.value)   data = data.filter(r=>(r.date?.substring(0,10)??"")<=filterTo.value);
  return data;
});
const filteredTotal      = computed(() => filteredDateData.value.reduce((s,r)=>s+Number(r.revenue||0),0));
const totalStatusRevenue = computed(() => statusData.value.reduce((s,r)=>s+Number(r.totalRevenue||0),0));
const activePeriodData   = computed(() => activePeriod.value==="week" ? weekData.value : monthData.value);
const periodTotalRevenue = computed(() => activePeriodData.value.reduce((s,r)=>s+Number(r.revenue||0),0));
const periodAvgRevenue   = computed(() => activePeriodData.value.length ? periodTotalRevenue.value/activePeriodData.value.length : 0);
const maxPeriodRevenue   = computed(() => activePeriodData.value.length ? Math.max(...activePeriodData.value.map(r=>Number(r.revenue))) : 1);
const totalTopQty        = computed(() => topProducts.value.reduce((s,p)=>s+Number(p.totalQty||0),0));
const totalTopRevenue    = computed(() => topProducts.value.reduce((s,p)=>s+Number(p.totalRevenue||0),0));
const totalRevenueByCustomer = computed(() => revenueByCustomer.value.reduce((sum,i)=>sum+(i.totalRevenue||0),0));
const periodKpis = computed(() => {
  if (!activePeriodData.value.length) return [];
  return [
    { label:"Tổng doanh thu", value:`${formatMoney(periodTotalRevenue.value)} ₫` },
    { label:"Số kỳ",          value:activePeriodData.value.length },
    { label:"TB / kỳ",        value:`${formatMoney(Math.round(periodTotalRevenue.value/activePeriodData.value.length))} ₫` },
    { label:"Cao nhất",       value:`${formatMoney(maxPeriodRevenue.value)} ₫`, color:"var(--el-color-success)" },
  ];
});

function toYMD(d) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,"0")}-${String(d.getDate()).padStart(2,"0")}`; }
function applyQuick(label) {
  const q = quickRanges.find(r=>r.label===label);
  if (!q) { dateRange.value=null; return; }
  const { from, to } = q.getDates();
  dateRange.value = [from, to];
}

const STATUS_LABEL_MAP = { PENDING:"Chờ xử lý",CONFIRMED:"Đã xác nhận",PROCESSING:"Đang xử lý",PAID:"Đã thanh toán",SHIPPING:"Đang giao",DELIVERED:"Đã giao",CANCELLED:"Đã hủy",RETURNED:"Đã trả hàng",PARTIALLY_RETURNED:"Trả một phần" };
function statusLabel(s) { return STATUS_LABEL_MAP[s]||s; }
function statusTagType(s) { return ({DELIVERED:"success",PAID:"primary",PENDING:"warning",PROCESSING:"primary",SHIPPING:"primary",CONFIRMED:"primary",CANCELLED:"danger",RETURNED:"warning",PARTIALLY_RETURNED:"warning"})[s]??"info"; }
function statusColor(s)   { return ({DELIVERED:"#10b981",PAID:"#0ea5e9",PENDING:"#f59e0b",PROCESSING:"#8b5cf6",SHIPPING:"#0891b2",CONFIRMED:"#2563eb",CANCELLED:"#ef4444",RETURNED:"#ea580c",PARTIALLY_RETURNED:"#ea580c"})[s]??"var(--el-color-primary)"; }

function formatMoney(v) { return new Intl.NumberFormat("vi-VN").format(Number(v)||0); }
function formatMoneyShort(v) {
  const n=Number(v)||0;
  if(n>=1_000_000_000) return (n/1_000_000_000).toFixed(1)+"B";
  if(n>=1_000_000)     return (n/1_000_000).toFixed(1)+"M";
  if(n>=1_000)         return (n/1_000).toFixed(0)+"K";
  return String(n);
}
function formatDate(d) {
  if(!d) return d;
  const date=new Date(d);
  if(isNaN(date)) return d;
  return date.toLocaleDateString("vi-VN",{year:"numeric",month:"2-digit",day:"2-digit"});
}
function getInitials(name) {
  if(!name) return "?";
  return name.split(/[@.\s]/).filter(Boolean).slice(0,2).map(p=>p[0].toUpperCase()).join("");
}
function getWeekNumber(d) {
  const date=new Date(Date.UTC(d.getFullYear(),d.getMonth(),d.getDate()));
  date.setUTCDate(date.getUTCDate()+4-(date.getUTCDay()||7));
  const yearStart=new Date(Date.UTC(date.getUTCFullYear(),0,1));
  return Math.ceil((((date-yearStart)/86400000)+1)/7);
}
function formatPeriodLabel(row) {
  const raw=row.weekStart??row.monthStart;
  if(!raw) return "";
  const d=new Date(raw);
  if(isNaN(d)) return String(raw).substring(0,10);
  if(activePeriod.value==="month") return d.toLocaleDateString("vi-VN",{year:"numeric",month:"long"});
  return `T${getWeekNumber(d)} · ${d.toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit"})}`;
}

const AVATAR_COLORS=["#2563eb","#16a34a","#d97706","#7c3aed","#0891b2","#db2777","#dc2626","#0f766e"];
function getAvatarColor(name) { if(!name) return "#6b7280"; return AVATAR_COLORS[name.charCodeAt(0)%AVATAR_COLORS.length]; }

onMounted(async () => {
  loadingRevenueByCustomer.value = true;
  const results = await Promise.allSettled([
    reportsApi.revenueByChannel(), reportsApi.revenueByDate(), reportsApi.ordersByStaff(),
    reportsApi.revenueByStatus(), reportsApi.returnsByProduct(), reportsApi.revenueByWeek(),
    reportsApi.revenueByMonth(), reportsApi.topSellingProducts(), ordersApi.getRevenueByCustomer(),
  ]);
  const [ch,dt,st,sv,rt,wk,mo,tp,rc] = results;
  if(ch.status==="fulfilled") channelData.value  = ch.value.data;
  if(dt.status==="fulfilled") dateData.value     = dt.value.data;
  if(st.status==="fulfilled") staffData.value    = st.value.data;
  if(sv.status==="fulfilled") statusData.value   = sv.value.data;
  if(rt.status==="fulfilled") returnsData.value  = rt.value.data;
  if(wk.status==="fulfilled") weekData.value     = wk.value.data;
  if(mo.status==="fulfilled") monthData.value    = mo.value.data;
  if(tp.status==="fulfilled") topProducts.value  = tp.value.data;
  if(rc.status==="fulfilled") revenueByCustomer.value = rc.value.data||[];
  loadingRevenueByCustomer.value = false;
});
</script>

<style></style>