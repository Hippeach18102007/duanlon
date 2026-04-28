<template>
  <div style="max-width: 1600px; margin: 0 auto; padding: 32px 24px 80px; display: flex; flex-direction: column; gap: 20px;">

    <!-- ── Header ─────────────────────────────────────────── -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><Grid /></el-icon> Phân tích › Loyalty › {{ mode === 'monthly' ? 'Hàng tháng' : 'Hàng tuần' }}
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">LoyaltyOS</div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">{{ currentTab?.label }}</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <!-- Status pill -->
          <el-tag :type="loading ? 'warning' : 'success'" effect="plain" size="small" round>
            <span :style="`width:6px;height:6px;border-radius:50%;background:currentColor;display:inline-block;vertical-align:middle;margin-right:4px;`"></span>
            {{ loading ? 'Đang đồng bộ…' : 'Trực tiếp' }}
          </el-tag>

          <!-- Mode toggle -->
          <el-radio-group v-model="mode" size="small" @change="range = 12">
            <el-radio-button v-for="m in modes" :key="m.value" :value="m.value">{{ m.label }}</el-radio-button>
          </el-radio-group>

          <!-- Range select -->
          <el-select v-model="range" size="small" style="width:90px;">
            <el-option v-for="n in rangeOptions" :key="n" :value="n"
              :label="`${n} ${mode === 'monthly' ? 'tháng' : 'tuần'}`" />
          </el-select>

          <el-button plain size="small" :loading="loading" @click="load">
            <el-icon><Refresh /></el-icon> {{ loading ? 'Đang tải…' : 'Làm mới' }}
          </el-button>
        </div>
      </div>

      <!-- Row 2: Tab Nav -->
      <el-tabs v-model="activeTab" class="ls-main-tabs">
        <el-tab-pane v-for="tab in tabs" :key="tab.id" :name="tab.id">
          <template #label>
            <el-space :size="6">
              <el-icon><component :is="tab.icon" /></el-icon>
              {{ tab.label }}
            </el-space>
          </template>
        </el-tab-pane>
      </el-tabs>

    <!-- ── Main ───────────────────────────────────────────── -->
    <div style="flex:1;">

      <!-- Error -->
      <el-alert v-if="error" type="error" :title="error" show-icon :closable="false" style="margin-bottom:16px;" />

      <!-- Skeleton -->
      <div v-if="loading">
        <el-row :gutter="14" style="margin-bottom:14px;">
          <el-col :span="6" v-for="i in 4" :key="i">
            <el-skeleton style="height:100px; border-radius:12px;" animated />
          </el-col>
        </el-row>
        <el-row :gutter="14">
          <el-col :span="16"><el-skeleton style="height:280px; border-radius:12px;" animated /></el-col>
          <el-col :span="8"><el-skeleton style="height:280px; border-radius:12px;" animated /></el-col>
        </el-row>
      </div>

      <template v-else-if="data.length">
        <!-- KPI Row -->
        <el-row :gutter="14" style="margin-bottom:20px;">
          <el-col :xs="12" :sm="6" v-for="k in kpis" :key="k.key">
            <el-card shadow="never" :body-style="{ padding:'16px 18px' }"
              :style="`border-top:3px solid ${k.color};`">
              <div style="display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:8px;">
                <div :style="`width:32px;height:32px;border-radius:8px;background:${k.color}18;color:${k.color};display:flex;align-items:center;justify-content:center;`">
                  <el-icon><component :is="k.icon" /></el-icon>
                </div>
                <el-tag :type="k.deltaClass === 'up' ? 'success' : 'danger'" size="small" effect="plain" round>{{ k.delta }}</el-tag>
              </div>
              <div :style="`font-size:20px; font-weight:800; font-family:monospace; color:${k.color}; margin-bottom:3px; letter-spacing:-0.02em;`">{{ k.value }}</div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ k.label }}</div>
              <!-- Sparkline -->
              <svg viewBox="0 0 80 32" preserveAspectRatio="none" style="width:100%; height:28px; margin-top:8px;">
                <polygon :points="sparkArea(k.key)" :fill="k.color" opacity=".08"/>
                <polyline :points="sparkPoints(k.key)" fill="none" :stroke="k.color" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </el-card>
          </el-col>
        </el-row>

        <!-- ── OVERVIEW ── -->
        <div v-if="activeTab === 'overview'">
          <el-row :gutter="14" style="margin-bottom:14px;">
            <el-col :xs="24" :lg="16">
              <el-card shadow="never">
                <template #header>
                  <div style="display:flex; align-items:center; justify-content:space-between;">
                    <div>
                      <div style="font-weight:700; font-size:14px;">Tích lũy vs Khấu trừ</div>
                      <div style="font-size:12px; color:var(--el-text-color-secondary);">Điểm tích lũy &amp; khấu trừ theo kỳ</div>
                    </div>
                    <el-space :size="12">
                      <el-space :size="4"><span style="width:10px;height:10px;background:#3b82f6;border-radius:2px;display:inline-block;"></span><el-text size="small">Tích lũy</el-text></el-space>
                      <el-space :size="4"><span style="width:10px;height:10px;background:#ef4444;border-radius:2px;display:inline-block;"></span><el-text size="small">Khấu trừ</el-text></el-space>
                      <el-space :size="4"><span style="width:10px;height:10px;background:#f59e0b;border-radius:50%;display:inline-block;"></span><el-text size="small">Thuần</el-text></el-space>
                    </el-space>
                  </div>
                </template>
                <div style="height:240px;"><canvas ref="comboChartRef"></canvas></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :lg="8">
              <el-card shadow="never">
                <template #header>
                  <div>
                    <div style="font-weight:700; font-size:14px;">Phân phối Hạng</div>
                    <div style="font-size:12px; color:var(--el-text-color-secondary);">Điểm cộng theo hạng VIP</div>
                  </div>
                </template>
                <div style="height:160px;"><canvas ref="donutChartRef"></canvas></div>
                <div style="margin-top:12px; display:flex; flex-direction:column; gap:6px;">
                  <div v-for="(t, i) in tierDonutData" :key="t.tier" style="display:flex; align-items:center; gap:8px;">
                    <span :style="`width:10px;height:10px;border-radius:50%;background:${TIER_COLORS[i % TIER_COLORS.length]};flex-shrink:0;`"></span>
                    <el-text size="small" style="flex:1;">{{ t.tier }}</el-text>
                    <el-text size="small" style="font-family:monospace; font-weight:700;">{{ fmt(t.value) }}</el-text>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <el-card shadow="never">
            <template #header>
              <div>
                <div style="font-weight:700; font-size:14px;">Xu hướng Điểm Thuần</div>
                <div style="font-size:12px; color:var(--el-text-color-secondary);">Điểm thuần qua từng kỳ</div>
              </div>
            </template>
            <div style="height:120px;"><canvas ref="netChartRef"></canvas></div>
          </el-card>
        </div>

        <!-- ── TABLE ── -->
        <div v-else-if="activeTab === 'table'">
          <el-row :gutter="14">
            <el-col :xs="24" :lg="selected ? 12 : 24">
              <el-card shadow="never">
                <template #header>
                  <div style="display:flex; align-items:center; justify-content:space-between;">
                    <el-text tag="b">Danh sách kỳ</el-text>
                    <el-tag effect="plain" size="small">{{ data.length }} kỳ</el-tag>
                  </div>
                </template>
                <!-- Period header -->
                <div style="display:flex; align-items:center; padding:8px 0; border-bottom:1px solid var(--el-border-color-lighter); font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:var(--el-text-color-secondary); gap:8px;">
                  <span style="flex:1.5;">Kỳ</span>
                  <span style="width:80px; text-align:right;">Tích lũy</span>
                  <span style="width:80px; text-align:right;">Khấu trừ</span>
                  <span style="width:70px; text-align:right;">Thuần</span>
                  <span style="width:50px; text-align:right;">Gd</span>
                </div>
                <div
                  v-for="item in data" :key="item.period"
                  @click="togglePeriod(item.period)"
                  :style="`display:flex; align-items:center; gap:8px; padding:10px 0; border-bottom:1px solid var(--el-border-color-lighter); cursor:pointer; border-radius:6px; background:${selectedPeriod===item.period?'var(--el-color-primary-light-9)':'transparent'}; transition:background .12s;`"
                >
                  <div style="flex:1.5; min-width:0;">
                    <div style="font-size:13px; font-weight:600;">{{ item.periodLabel }}</div>
                    <div style="font-size:11px; color:var(--el-text-color-secondary); font-family:monospace;">{{ item.periodStart }} – {{ item.periodEnd }}</div>
                  </div>
                  <span style="width:80px; text-align:right; font-family:monospace; font-size:12px; color:#16a34a; font-weight:700;">+{{ fmt(item.totalPointsEarned) }}</span>
                  <span style="width:80px; text-align:right; font-family:monospace; font-size:12px; color:#dc2626; font-weight:700;">-{{ fmt(item.totalPointsDeducted) }}</span>
                  <span :style="`width:70px; text-align:right; font-family:monospace; font-size:12px; font-weight:700; color:${item.netPoints>=0?'#2563eb':'#dc2626'};`">
                    {{ item.netPoints >= 0 ? '+' : '' }}{{ fmt(item.netPoints) }}
                  </span>
                  <span style="width:50px; text-align:right; font-size:12px; color:var(--el-text-color-secondary); font-family:monospace;">{{ item.totalTransactions }}</span>
                </div>
              </el-card>
            </el-col>

            <transition name="el-fade-in">
              <el-col v-if="selected" :xs="24" :lg="12">
                <el-card shadow="never">
                  <template #header>
                    <div style="display:flex; align-items:center; justify-content:space-between;">
                      <div>
                        <div style="font-weight:700;">{{ selected.periodLabel }}</div>
                        <div style="font-size:12px; color:var(--el-text-color-secondary);">{{ selected.customerBreakdown?.length ?? 0 }} khách hàng</div>
                      </div>
                      <el-button circle plain size="small" @click="selectedPeriod = null">
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </div>
                  </template>
                  <el-row :gutter="0" style="margin-bottom:16px; border-bottom:1px solid var(--el-border-color-lighter);">
                    <el-col :span="6" v-for="ms in [
                      { label:'Tích lũy', val: '+'+fmt(selected.totalPointsEarned), color:'#16a34a' },
                      { label:'Khấu trừ', val: '-'+fmt(selected.totalPointsDeducted), color:'#dc2626' },
                      { label:'Thuần', val: (selected.netPoints>=0?'+':'')+fmt(selected.netPoints), color: selected.netPoints>=0?'#2563eb':'#dc2626' },
                      { label:'Giao dịch', val: selected.totalTransactions, color:'#6b7280' },
                    ]" :key="ms.label">
                      <div style="padding:10px 12px;">
                        <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:4px;">{{ ms.label }}</div>
                        <div :style="`font-size:14px; font-weight:800; font-family:monospace; color:${ms.color};`">{{ ms.val }}</div>
                      </div>
                    </el-col>
                  </el-row>
                  <el-table v-if="selected.customerBreakdown?.length" :data="selected.customerBreakdown" size="small" :max-height="360">
                    <el-table-column label="Khách hàng" min-width="160">
                      <template #default="{ row }">
                        <div style="display:flex; align-items:center; gap:8px;">
                          <el-avatar :size="28" :style="`background:${avatarColor(row.customerName)}; font-size:11px; font-weight:700;`">{{ initials(row.customerName) }}</el-avatar>
                          <div>
                            <div style="font-size:12px; font-weight:600;">{{ row.customerName }}</div>
                            <div style="font-size:10px; color:var(--el-text-color-secondary);">{{ row.customerEmail }}</div>
                          </div>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column label="Hạng" width="80" align="center">
                      <template #default="{ row }">
                        <el-tag v-if="row.vipTier && row.vipTier !== 'Member'" size="small" effect="plain">{{ row.vipTier }}</el-tag>
                        <span v-else style="color:var(--el-text-color-placeholder);">—</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="Tích lũy" align="right" width="80">
                      <template #default="{ row }"><span style="color:#16a34a; font-family:monospace; font-size:12px;">+{{ fmt(row.pointsEarned) }}</span></template>
                    </el-table-column>
                    <el-table-column label="Khấu trừ" align="right" width="80">
                      <template #default="{ row }"><span style="color:#dc2626; font-family:monospace; font-size:12px;">{{ row.pointsDeducted > 0 ? '-' : '' }}{{ fmt(row.pointsDeducted) }}</span></template>
                    </el-table-column>
                    <el-table-column label="Thuần" align="right" width="80">
                      <template #default="{ row }">
                        <span :style="`font-family:monospace; font-size:12px; font-weight:700; color:${row.netPoints>=0?'#2563eb':'#dc2626'};`">{{ row.netPoints >= 0 ? '+' : '' }}{{ fmt(row.netPoints) }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else description="Không có dữ liệu" :image-size="48" />
                </el-card>
              </el-col>
            </transition>
          </el-row>
        </div>

        <!-- ── RANKING ── -->
        <div v-else-if="activeTab === 'ranking'">
          <el-row :gutter="14">
            <el-col :xs="24" :lg="14">
              <el-card shadow="never">
                <template #header>
                  <div>
                    <div style="font-weight:700; font-size:14px;">Top Tích điểm</div>
                    <div style="font-size:12px; color:var(--el-text-color-secondary);">Khách hàng tích điểm nhiều nhất</div>
                  </div>
                </template>
                <div style="height:280px;"><canvas ref="topEarnerChartRef"></canvas></div>
              </el-card>
            </el-col>
            <el-col :xs="24" :lg="10">
              <el-card shadow="never">
                <template #header>
                  <div>
                    <div style="font-weight:700; font-size:14px;">Tỷ lệ Tích lũy %</div>
                    <div style="font-size:12px; color:var(--el-text-color-secondary);">Tỷ lệ Tích lũy / (Tích lũy + Khấu trừ)</div>
                  </div>
                </template>
                <div style="height:280px;"><canvas ref="rateChartRef"></canvas></div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- ── SPENDING ANALYSIS ── -->
        <div v-else-if="activeTab === 'spending'">
          <div style="display:flex; align-items:flex-start; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:20px;">
            <div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.07em; color:var(--el-text-color-secondary); margin-bottom:4px;">Phân tích</div>
              <div style="font-size:22px; font-weight:800; letter-spacing:-0.02em;">Phân tích Chi tiêu Khách hàng</div>
              <div style="font-size:13px; color:var(--el-text-color-secondary);">Phân tích xu hướng chi tiêu và khách hàng hàng đầu</div>
            </div>
            <el-button plain :loading="loadingSpending" @click="loadAllSpending">
              <el-icon><Refresh /></el-icon> Tải lại
            </el-button>
          </div>

          <!-- Spending KPIs -->
          <el-row :gutter="14" style="margin-bottom:20px;">
            <el-col :xs="12" :sm="6" v-for="sk in spendingKpis" :key="sk.label">
              <el-card shadow="never" :body-style="{ padding:'16px 18px' }">
                <div style="display:flex; align-items:center; gap:12px;">
                  <el-avatar :size="36" :style="`background:${sk.bg}; color:${sk.color};`">
                    <el-icon><component :is="sk.icon" /></el-icon>
                  </el-avatar>
                  <div>
                    <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:3px;">{{ sk.label }}</div>
                    <div style="font-size:15px; font-weight:800; font-family:monospace;">{{ sk.value }}</div>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>

          <!-- Spending sub-tabs -->
          <el-tabs v-model="spendingActiveTab" @tab-click="(t) => switchSpendingTab(t.paneName)">
            <el-tab-pane v-for="tab in spendingTabList" :key="tab.id" :name="tab.id">
              <template #label>
                <el-space :size="6"><el-icon><component :is="tab.icon" /></el-icon>{{ tab.label }}</el-space>
              </template>
            </el-tab-pane>
          </el-tabs>

          <!-- Top Spenders -->
          <div v-if="spendingActiveTab === 'topSpenders'">
            <el-row :gutter="12" style="margin-bottom:12px; align-items:flex-end;" align="bottom">
              <el-col flex="none">
                <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:4px;">Giới hạn</div>
                <el-input-number v-model="topSpendersLimit" :min="5" :max="100" :step="5" size="small" />
              </el-col>
              <el-col flex="none">
                <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:4px;">Hạng VIP</div>
                <el-select v-model="topSpendersTier" size="small" clearable placeholder="Tất cả hạng" style="width:140px;">
                  <el-option v-for="t in ['BRONZE','SILVER','GOLD','PLATINUM','DIAMOND']" :key="t" :value="t" :label="t" />
                </el-select>
              </el-col>
              <el-col flex="none">
                <el-button type="primary" plain size="small" :loading="loadingTopSpenders" @click="loadTopSpenders">
                  <el-icon><Search /></el-icon> Tải
                </el-button>
              </el-col>
            </el-row>

            <el-skeleton v-if="loadingTopSpenders" :rows="5" animated />
            <el-table v-else-if="topSpenders.length" :data="topSpenders" stripe size="small">
              <el-table-column label="Hạng" width="70" align="center">
                <template #default="{ $index }">
                  <el-avatar :size="28"
                    :style="`background:${['#f59e0b','#94a3b8','#ea580c'][Math.min($index,2)]||'var(--el-color-primary-light-5)'};color:#fff;font-size:12px;font-weight:800;`">
                    <el-icon v-if="$index < 3"><Trophy /></el-icon>
                    <span v-else>{{ $index + 1 }}</span>
                  </el-avatar>
                </template>
              </el-table-column>
              <el-table-column label="Khách hàng" min-width="180">
                <template #default="{ row }">
                  <div style="display:flex; align-items:center; gap:10px;">
                    <el-avatar :size="32" :style="`background:${avatarColor(row.fullName)};color:#fff;font-size:11px;font-weight:700;`">{{ initials(row.fullName) }}</el-avatar>
                    <div>
                      <div style="font-size:13px; font-weight:600;">{{ row.fullName }}</div>
                      <div style="font-size:11px; color:var(--el-text-color-secondary);">{{ row.email }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="Hạng VIP" width="110" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.raw.vipTier" size="small" effect="plain">{{ row.raw.vipTier }}</el-tag>
                  <span v-else style="color:var(--el-text-color-placeholder);">—</span>
                </template>
              </el-table-column>
              <el-table-column label="Tổng Chi tiêu" align="right" min-width="140">
                <template #default="{ row }">
                  <span style="font-family:monospace; font-weight:700; color:var(--el-color-success);">{{ formatCurrency(row.raw.totalSpent) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Điểm" align="center" width="100">
                <template #default="{ row }">
                  <el-tag type="warning" effect="plain" size="small">⭐ {{ row.loyaltyPoints }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="Loại" width="90" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.customerType==='VIP'?'warning':'info'" effect="plain" size="small">{{ row.customerType }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="Chưa có dữ liệu" :image-size="60" />
          </div>

          <!-- Spending Range -->
          <div v-else-if="spendingActiveTab === 'spendingRange'">
            <el-row :gutter="12" style="margin-bottom:12px; align-items:flex-end;" align="bottom">
              <el-col :xs="24" :sm="8">
                <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:4px;">Chi tiêu Tối thiểu (₫)</div>
                <el-input-number v-model="spendingMin" :min="0" :step="1000000" placeholder="0" style="width:100%;" />
              </el-col>
              <el-col :xs="24" :sm="8">
                <div style="font-size:11px; color:var(--el-text-color-secondary); margin-bottom:4px;">Chi tiêu Tối đa (₫)</div>
                <el-input-number v-model="spendingMax" :min="0" :step="1000000" placeholder="999,999,999" style="width:100%;" />
              </el-col>
              <el-col flex="none">
                <el-button type="primary" plain :loading="loadingSpendingRange" @click="loadBySpendingRange">
                  <el-icon><Aim /></el-icon> Áp dụng
                </el-button>
                <el-button plain @click="clearSpendingRange">Xóa</el-button>
              </el-col>
            </el-row>
            <el-space wrap :size="6" style="margin-bottom:16px;">
              <el-text size="small" type="info">Nhanh:</el-text>
              <el-button v-for="q in quickRanges" :key="q.label" size="small" plain @click="setQuickRange(q.min, q.max)">{{ q.label }}</el-button>
            </el-space>

            <el-alert v-if="appliedSpendingRange.min !== null || appliedSpendingRange.max !== null"
              type="info" :closable="false" style="margin-bottom:12px;"
              :title="`${formatCurrency(appliedSpendingRange.min||0)} – ${formatCurrency(appliedSpendingRange.max||999999999)}`" />

            <el-skeleton v-if="loadingSpendingRange" :rows="4" animated />
            <el-table v-else-if="pagedSpending.length" :data="pagedSpending" stripe size="small">
              <el-table-column type="index" label="#" width="52" align="center" />
              <el-table-column label="Khách hàng" min-width="180">
                <template #default="{ row }">
                  <div style="display:flex; align-items:center; gap:10px;">
                    <el-avatar :size="30" :style="`background:${avatarColor(row.fullName)};color:#fff;font-size:11px;font-weight:700;`">{{ initials(row.fullName) }}</el-avatar>
                    <div>
                      <div style="font-size:13px; font-weight:600;">{{ row.fullName }}</div>
                      <div style="font-size:11px; color:var(--el-text-color-secondary);">{{ row.email }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="Hạng VIP" width="110" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.raw.vipTier" size="small" effect="plain">{{ row.raw.vipTier }}</el-tag>
                  <span v-else style="color:var(--el-text-color-placeholder);">—</span>
                </template>
              </el-table-column>
              <el-table-column label="Tổng Chi tiêu" align="right">
                <template #default="{ row }">
                  <span style="font-family:monospace; font-weight:700; color:var(--el-color-success);">{{ formatCurrency(row.raw.totalSpent) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Điểm" align="center" width="100">
                <template #default="{ row }">
                  <el-tag type="warning" effect="plain" size="small">⭐ {{ row.loyaltyPoints }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else-if="!loadingSpendingRange" description="Chưa có dữ liệu" :image-size="60" />

            <div v-if="totalSpendingPages > 1" style="display:flex; justify-content:center; margin-top:12px;">
              <el-pagination v-model:current-page="spendingPage" :page-size="10" :total="customersBySpending.length" layout="prev, pager, next" background small />
            </div>
          </div>

          <!-- Statistics -->
          <div v-else-if="spendingActiveTab === 'statistics'">
            <el-skeleton v-if="loadingSpending" :rows="6" animated />
            <template v-else>
              <el-row :gutter="14" style="margin-bottom:16px;">
                <!-- By tier -->
                <el-col :xs="24" :lg="12">
                  <el-card shadow="never">
                    <template #header><el-text tag="b">Theo Hạng VIP</el-text></template>
                    <el-table :data="Object.entries(spendingStats.byTier||{}).map(([tier,val])=>({tier,val}))" size="small" stripe>
                      <el-table-column label="Hạng" prop="tier">
                        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.tier }}</el-tag></template>
                      </el-table-column>
                      <el-table-column label="Tổng Chi tiêu" align="right">
                        <template #default="{ row }">
                          <span style="font-family:monospace; font-weight:700; color:var(--el-color-success);">{{ formatCurrency(row.val) }}</span>
                        </template>
                      </el-table-column>
                    </el-table>
                  </el-card>
                </el-col>
                <!-- By range -->
                <el-col :xs="24" :lg="12">
                  <el-card shadow="never">
                    <template #header><el-text tag="b">Theo Khoảng</el-text></template>
                    <el-space direction="vertical" fill :size="10" style="width:100%;">
                      <div v-for="(count, range) in (spendingStats.spendingRanges||{})" :key="range">
                        <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:4px;">
                          <el-text size="small">{{ formatRangeLabel(range) }}</el-text>
                          <el-text size="small" style="font-family:monospace; font-weight:700;">{{ count }}</el-text>
                        </div>
                        <el-progress :percentage="getPercentage(count, spendingStats.totalCustomers)" :stroke-width="6" :show-text="false" :color="getRangeColor(range)" />
                      </div>
                    </el-space>
                  </el-card>
                </el-col>
              </el-row>
            </template>
          </div>
        </div>

      </template>

      <!-- Empty -->
      <el-empty v-else description="Không có dữ liệu trong khoảng thời gian này." :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onBeforeUnmount, reactive } from 'vue'
import { Aim, BottomRight, Close, CreditCard, DataAnalysis, Grid, InfoFilled, Money, Refresh, Search, TopRight, TrendCharts, Trophy, UserFilled } from '@element-plus/icons-vue'
import { customersApi } from '../../api/customers.api'
import { toast } from '../../ui/toast'
import Chart from 'chart.js/auto'


const getChart = () => Chart

// ── Constants ──────────────────────────────────────────────────────
const TIER_COLORS = ['#3b82f6','#f59e0b','#ef4444','#8b5cf6','#10b981','#f97316']
const TIER_META = {
  BRONZE: '#cd7f32', SILVER: '#9ca3af', GOLD: '#f59e0b',
  PLATINUM: '#8b5cf6', DIAMOND: '#3b82f6', Member: '#6b7280',
}

// EP icon component names — dùng <component :is="..."> thay v-html SVG string
const ICON_OVERVIEW = 'Grid'
const ICON_TABLE    = 'Grid'
const ICON_TREND    = 'TrendCharts'
const ICON_EARN     = 'TopRight'
const ICON_DEDUCT   = 'BottomRight'
const ICON_NET      = 'DataAnalysis'
const ICON_TX       = 'CreditCard'
const ICON_SPENDING = 'Money'

const tabs = [
  { id: 'overview', label: 'Tổng quan',  icon: 'Grid'        },
  { id: 'table',    label: 'Chi tiết',   icon: 'Grid'        },
  { id: 'ranking',  label: 'Xếp hạng',   icon: 'TrendCharts' },
  { id: 'spending', label: 'Phân tích Chi tiêu', icon: 'Money' },
]
const modes = [
  { value: 'monthly', label: 'Hàng tháng' },
  { value: 'weekly',  label: 'Hàng tuần'  },
]

// ── Spending Analysis State ───────────────────────────────────────
const loadingSpending         = ref(false);
const loadingTopSpenders      = ref(false);
const loadingSpendingRange    = ref(false);
const spendingActiveTab       = ref("topSpenders");

const topSpenders            = ref([]);
const topSpendersLimit       = ref(10);
const topSpendersTier        = ref("");

const customersBySpending    = ref([]);
const spendingMin            = ref(null);
const spendingMax            = ref(null);
const appliedSpendingRange   = reactive({ min: null, max: null });
const spendingPage           = ref(1);
const pageSize               = 10;

const spendingStats          = ref({ totalSpent: 0, totalCustomers: 0, averageSpent: 0, byTier: {}, spendingRanges: {} });

const spendingKpis = computed(() => [
  { label: 'Tổng Chi tiêu',       value: formatCurrency(spendingStats.value.totalSpent),              color: '#3b82f6', bg: '#eff6ff', icon: 'InfoFilled' },
  { label: 'Tổng Khách hàng',     value: (spendingStats.value.totalCustomers||0).toLocaleString(),     color: '#10b981', bg: '#ecfdf5', icon: 'UserFilled' },
  { label: 'Chi tiêu TB',         value: formatCurrency(spendingStats.value.averageSpent),             color: '#f59e0b', bg: '#fffbeb', icon: 'TrendCharts'  },
  { label: 'Chi tiêu Nhiều nhất', value: topSpenders.value[0]?.fullName || 'N/A',                     color: '#8b5cf6', bg: '#f5f3ff', icon: 'Money'       },
])


const spendingTabList = [
  { id: 'topSpenders',   label: 'Top Chi tiêu',         icon: 'Money'        },
  { id: 'spendingRange', label: 'Theo Khoảng Chi tiêu',  icon: 'Aim'          },
  { id: 'statistics',    label: 'Thống kê',              icon: 'DataAnalysis' },
];

const quickRanges = [
  { label: 'Dưới 1M',   min: 0,        max: 1000000   },
  { label: '1M – 5M',    min: 1000000,  max: 5000000   },
  { label: '5M – 10M',   min: 5000000,  max: 10000000  },
  { label: '10M – 50M',  min: 10000000, max: 50000000  },
  { label: 'Trên 50M',   min: 50000000, max: 999999999 },
];

// ── Spending Analysis Computed ───────────────────────────────────
const pagedSpending = computed(() => {
  const s = (spendingPage.value - 1) * pageSize;
  return customersBySpending.value.slice(s, s + pageSize);
});
const totalSpendingPages = computed(() => Math.ceil(customersBySpending.value.length / pageSize));

// ── Spending Analysis Functions ───────────────────────────────────
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

function getRankKey(i) { return ['gold','silver','bronze'][i] || 'other'; }
function formatRangeLabel(r) {
  return { under1M: 'Dưới 1M', '1M-5M': '1M – 5M', '5M-10M': '5M – 10M', '10M-50M': '10M – 50M', over50M: 'Trên 50M' }[r] || r;
}
function getPercentage(count, total) { return total ? Math.round(count / total * 100) : 0; }
function getRangeColor(r) {
  return { under1M: '#9ca3af', '1M-5M': '#3b82f6', '5M-10M': '#10b981', '10M-50M': '#f59e0b', over50M: '#ef4444' }[r] || '#3b82f6';
}

async function loadTopSpenders() {
  loadingTopSpenders.value = true;
  try {
    const res = topSpendersTier.value
      ? await customersApi.listTopSpendersByVipTier(topSpendersTier.value, topSpendersLimit.value)
      : await customersApi.listTopSpenders(topSpendersLimit.value);
    topSpenders.value = normalize(extractList(res?.data));
  } catch { toast("Không thể tải danh sách chi tiêu hàng đầu.", "error"); }
  finally { loadingTopSpenders.value = false; }
}

async function loadBySpendingRange() {
  if (spendingMin.value === null && spendingMax.value === null) { toast("Nhập ít nhất một giá trị", "warning"); return; }
  if (spendingMin.value != null && spendingMax.value != null && spendingMin.value > spendingMax.value) { toast("Giá trị tối thiểu không thể vượt quá tối đa", "error"); return; }
  loadingSpendingRange.value = true;
  try {
    const min = spendingMin.value || 0;
    const max = spendingMax.value || 999999999;
    const res = await customersApi.listBySpendingRange(min, max);
    customersBySpending.value = normalize(extractList(res?.data));
    appliedSpendingRange.min = min;
    appliedSpendingRange.max = max;
    spendingPage.value = 1;
    toast(`Tìm thấy ${customersBySpending.value.length} khách hàng`, "success");
  } catch { toast("Không thể tải khách hàng.", "error"); }
  finally { loadingSpendingRange.value = false; }
}

async function loadStatistics() {
  loadingSpending.value = true;
  try { const res = await customersApi.getSpendingStatistics(); spendingStats.value = res?.data || {}; }
  catch { toast("Không thể tải thống kê.", "error"); }
  finally { loadingSpending.value = false; }
}

function clearSpendingRange() {
  spendingMin.value = null; spendingMax.value = null;
  appliedSpendingRange.min = null; appliedSpendingRange.max = null;
  customersBySpending.value = [];
}
function setQuickRange(min, max) { spendingMin.value = min; spendingMax.value = max; loadBySpendingRange(); }
async function loadAllSpending() { await Promise.all([loadTopSpenders(), loadStatistics()]); }
function switchSpendingTab(id) {
  spendingActiveTab.value = id;
  if (id === 'topSpenders' && !topSpenders.value.length) loadTopSpenders();
  if (id === 'statistics' && !Object.keys(spendingStats.value.byTier || {}).length) loadStatistics();
}

// ── State ──────────────────────────────────────────────────────────
const mode           = ref('monthly')
const range          = ref(12)
const data           = ref([])
const loading        = ref(false)
const error          = ref(null)
const selectedPeriod = ref(null)
const activeTab      = ref('overview')

const comboChartRef     = ref(null)
const donutChartRef     = ref(null)
const netChartRef       = ref(null)
const topEarnerChartRef = ref(null)
const rateChartRef      = ref(null)
let charts = {}

// ── Computed ───────────────────────────────────────────────────────
const rangeOptions = computed(() =>
  mode.value === 'monthly' ? [3, 6, 12, 24] : [4, 8, 12, 24, 52]
)
const currentTab = computed(() => tabs.find(t => t.id === activeTab.value))
const selected   = computed(() => data.value.find(d => d.period === selectedPeriod.value))

const totalEarned   = computed(() => data.value.reduce((s, d) => s + (d.totalPointsEarned   || 0), 0))
const totalDeducted = computed(() => data.value.reduce((s, d) => s + (d.totalPointsDeducted || 0), 0))
const totalTx       = computed(() => data.value.reduce((s, d) => s + (d.totalTransactions   || 0), 0))
const netTotal      = computed(() => totalEarned.value - totalDeducted.value)

const tierDonutData = computed(() => {
  const map = {}
  data.value.forEach(p => {
    ;(p.customerBreakdown || []).forEach(c => {
      const t = c.vipTier || 'Member'
      map[t] = (map[t] || 0) + (c.pointsEarned || 0)
    })
  })
  return Object.entries(map)
    .map(([tier, value]) => ({ tier, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 6)
})

const topEarners = computed(() => {
  const map = {}
  data.value.forEach(p => {
    ;(p.customerBreakdown || []).forEach(c => {
      if (!map[c.customerName]) map[c.customerName] = { earn: 0, deduct: 0 }
      map[c.customerName].earn   += c.pointsEarned   || 0
      map[c.customerName].deduct += c.pointsDeducted || 0
    })
  })
  return Object.entries(map)
    .map(([name, v]) => ({ name, ...v }))
    .sort((a, b) => b.earn - a.earn)
    .slice(0, 8)
})

function sparkPoints(key) {
  const vals = [...data.value].reverse().map(d => d[key] || 0)
  const max = Math.max(...vals, 1)
  return vals.map((v, i) => {
    const x = (i / Math.max(vals.length - 1, 1)) * 80
    const y = 32 - (v / max) * 28
    return `${x},${y}`
  }).join(' ')
}
function sparkArea(key) {
  const vals = [...data.value].reverse().map(d => d[key] || 0)
  const max = Math.max(...vals, 1)
  const pts = vals.map((v, i) => {
    const x = (i / Math.max(vals.length - 1, 1)) * 80
    const y = 32 - (v / max) * 28
    return `${x},${y}`
  })
  return `0,32 ${pts.join(' ')} 80,32`
}

function calcDelta(key) {
  if (data.value.length < 2) return null
  const cur  = data.value[0][key] || 0
  const prev = data.value[1][key] || 1
  const pct  = ((cur - prev) / Math.abs(prev) * 100).toFixed(1)
  return { val: pct, pos: cur >= prev }
}

const kpis = computed(() => {
  const dE = calcDelta('totalPointsEarned')
  const dD = calcDelta('totalPointsDeducted')
  const dT = calcDelta('totalTransactions')
  return [
    { key: 'totalPointsEarned',   label: 'Tổng Tích lũy',   color: '#3b82f6', icon: ICON_EARN,   value: '+' + fmt(totalEarned.value),   delta: dE ? (dE.pos ? '↑' : '↓') + Math.abs(dE.val) + '%' : '—', deltaClass: dE?.pos ? 'up' : 'dn' },
    { key: 'totalPointsDeducted', label: 'Tổng Khấu trừ', color: '#ef4444', icon: ICON_DEDUCT, value: '-' + fmt(totalDeducted.value), delta: dD ? (!dD.pos ? '↑' : '↓') + Math.abs(dD.val) + '%' : '—', deltaClass: !dD?.pos ? 'up' : 'dn' },
    { key: 'netPoints',           label: 'Điểm Thuần',     color: '#f59e0b', icon: ICON_NET,    value: (netTotal.value >= 0 ? '+' : '') + fmt(netTotal.value), delta: '—', deltaClass: '' },
    { key: 'totalTransactions',   label: 'Giao dịch',   color: '#8b5cf6', icon: ICON_TX,     value: fmt(totalTx.value), delta: dT ? (dT.pos ? '↑' : '↓') + Math.abs(dT.val) + '%' : '—', deltaClass: dT?.pos ? 'up' : 'dn' },
  ]
})

// ── Helpers ────────────────────────────────────────────────────────
const fmt = (n) => (n ?? 0).toLocaleString('vi-VN')
const setMode = (m) => { mode.value = m; range.value = 12 }
const togglePeriod = (p) => { selectedPeriod.value = selectedPeriod.value === p ? null : p }

function avatarColor(name) {
  const palette = ['#3b82f6','#8b5cf6','#ec4899','#f59e0b','#10b981','#ef4444','#06b6d4','#f97316']
  const h = (name || '?').split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return palette[h % palette.length]
}
function initials(name) {
  if (!name) return '?'
  const p = name.trim().split(' ')
  return p.length === 1 ? p[0].slice(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase()
}

// ── Load ───────────────────────────────────────────────────────────
async function load() {
  loading.value = true
  error.value = null
  selectedPeriod.value = null
  try {
    const res = mode.value === 'weekly'
      ? await customersApi.getLoyaltyWeeklySummaryAdmin(range.value)
      : await customersApi.getLoyaltyMonthlySummaryAdmin(range.value)
    data.value = (res.data ?? []).reverse()
console.log('keys:', Object.keys(data.value[0] || {}))
console.log('breakdown:', data.value[0]?.customerBreakdown)
setTimeout(() => buildCharts(), 50)
  } catch {
    error.value = 'Không thể tải dữ liệu. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

// ── Charts ─────────────────────────────────────────────────────────
function destroyAll() {
  Object.values(charts).forEach(c => c?.destroy())
  charts = {}
}

const FONT = "'Plus Jakarta Sans', 'Segoe UI', sans-serif"
const GRID = 'rgba(0,0,0,0.05)'
const TICK = '#9ca3af'
const TT = {
  backgroundColor: '#0f1117',
  borderColor: '#1f2937',
  borderWidth: 1,
  titleColor: '#9ca3af',
  bodyColor: '#f9fafb',
  titleFont: { family: FONT, size: 11 },
  bodyFont:  { family: FONT, size: 12 },
  padding: 12,
  cornerRadius: 8,
}

function buildCharts() {
  destroyAll()
  const C = getChart()
  if (!C) return

  const reversed = [...data.value].reverse()
  const labels   = reversed.map(d => d.periodLabel)
  const earned   = reversed.map(d => d.totalPointsEarned   || 0)
  const deducted = reversed.map(d => d.totalPointsDeducted || 0)
  const net      = reversed.map(d => d.netPoints            || 0)

  const xScale = {
    grid: { color: GRID },
    ticks: { color: TICK, font: { family: FONT, size: 11 }, maxRotation: 40 },
    border: { display: false },
  }
  const yScale = {
    grid: { color: GRID },
    ticks: { color: TICK, font: { family: FONT, size: 11 }, callback: v => fmt(v) },
    border: { display: false },
  }

  // 1. Combo
  if (comboChartRef.value) {
    charts.combo = new C(comboChartRef.value, {
      type: 'bar',
      data: {
        labels,
        datasets: [
          { label: 'Tích lũy',   data: earned,   backgroundColor: 'rgba(59,130,246,0.75)', borderRadius: 4, borderSkipped: false, order: 2 },
          { label: 'Khấu trừ', data: deducted, backgroundColor: 'rgba(239,68,68,0.65)',  borderRadius: 4, borderSkipped: false, order: 2 },
          { label: 'Thuần', data: net, type: 'line', borderColor: '#f59e0b', backgroundColor: 'rgba(245,158,11,0.05)', borderWidth: 2, pointRadius: 3, pointBackgroundColor: '#f59e0b', tension: 0.4, fill: false, order: 1 },
        ],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { display: false }, tooltip: { ...TT, callbacks: { label: ctx => ` ${ctx.dataset.label}: ${fmt(ctx.raw)}` } } },
        scales: { x: xScale, y: yScale },
      },
    })
  }

  // 2. Donut
  if (donutChartRef.value && tierDonutData.value.length) {
    charts.donut = new C(donutChartRef.value, {
      type: 'doughnut',
      data: {
        labels: tierDonutData.value.map(t => t.tier),
        datasets: [{ data: tierDonutData.value.map(t => t.value), backgroundColor: TIER_COLORS, borderWidth: 3, borderColor: '#ffffff' }],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        cutout: '72%',
        plugins: { legend: { display: false }, tooltip: { ...TT, callbacks: { label: ctx => ` ${ctx.label}: ${fmt(ctx.raw)}` } } },
      },
    })
  }

  // 3. Net trend
  if (netChartRef.value) {
    charts.net = new C(netChartRef.value, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Thuần', data: net,
          borderColor: '#3b82f6',
          backgroundColor: ctx => {
            const g = ctx.chart.ctx.createLinearGradient(0, 0, 0, 120)
            g.addColorStop(0, 'rgba(59,130,246,0.15)')
            g.addColorStop(1, 'rgba(59,130,246,0)')
            return g
          },
          borderWidth: 2, fill: true, tension: 0.4,
          pointRadius: 3, pointBackgroundColor: '#3b82f6', pointBorderColor: '#fff', pointBorderWidth: 2,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { display: false }, tooltip: { ...TT, callbacks: { label: ctx => ` ${ctx.dataset.label}: ${fmt(ctx.raw)}` } } },
        scales: {
          x: { display: false },
          y: { grid: { color: GRID }, ticks: { color: TICK, font: { family: FONT, size: 10 }, callback: v => fmt(v) }, border: { display: false } },
        },
      },
    })
  }

  // 4. Top earners
  if (topEarnerChartRef.value && topEarners.value.length) {
    charts.topEarner = new C(topEarnerChartRef.value, {
      type: 'bar',
      data: {
        labels: topEarners.value.map(e => e.name.length > 16 ? e.name.slice(0, 16) + '…' : e.name),
        datasets: [
          { label: 'Tích lũy',   data: topEarners.value.map(e => e.earn),   backgroundColor: 'rgba(59,130,246,0.75)',  borderRadius: 4, borderSkipped: false },
          { label: 'Khấu trừ', data: topEarners.value.map(e => e.deduct), backgroundColor: 'rgba(239,68,68,0.60)',  borderRadius: 4, borderSkipped: false },
        ],
      },
      options: {
        indexAxis: 'y', responsive: true, maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: { legend: { display: false }, tooltip: { ...TT, callbacks: { label: ctx => ` ${ctx.dataset.label}: ${fmt(ctx.raw)}` } } },
        scales: {
          x: { grid: { color: GRID }, ticks: { color: TICK, font: { family: FONT, size: 11 }, callback: v => fmt(v) }, border: { display: false } },
          y: { grid: { display: false }, ticks: { color: '#374151', font: { family: FONT, size: 12 } } },
        },
      },
    })
  }

  // 5. Earn rate
  if (rateChartRef.value) {
    const rate = reversed.map(d => {
      const total = (d.totalPointsEarned || 0) + (d.totalPointsDeducted || 0)
      return total > 0 ? +((d.totalPointsEarned / total) * 100).toFixed(1) : 0
    })
    charts.rate = new C(rateChartRef.value, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Tỷ lệ Tích lũy %', data: rate,
          borderColor: '#8b5cf6',
          backgroundColor: ctx => {
            const g = ctx.chart.ctx.createLinearGradient(0, 0, 0, 280)
            g.addColorStop(0, 'rgba(139,92,246,0.12)')
            g.addColorStop(1, 'rgba(139,92,246,0)')
            return g
          },
          borderWidth: 2, fill: true, tension: 0.4,
          pointRadius: 4, pointBackgroundColor: '#8b5cf6', pointBorderColor: '#fff', pointBorderWidth: 2,
        }],
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { ...TT, callbacks: { label: ctx => ` Tỷ lệ: ${ctx.raw}%` } } },
        scales: {
          x: xScale,
          y: { min: 0, max: 100, grid: { color: GRID }, ticks: { color: TICK, font: { family: FONT, size: 11 }, callback: v => v + '%' }, border: { display: false } },
        },
      },
    })
  }
}
watch(activeTab, (newTab) => {
  if (!data.value.length) return
  const delay = newTab === 'ranking' ? 150 : 50
  setTimeout(() => buildCharts(), delay)
})
watch([mode, range], load, { immediate: true })
onBeforeUnmount(destroyAll)
</script>

<style></style>