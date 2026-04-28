<template>
  <div style="background:#f6f7f9; min-height:100vh; padding:32px 40px 60px;">
    <div style="max-width:1440px; margin:0 auto; display:flex; flex-direction:column; gap:20px;">

      <!-- Header -->
      <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
        <div>
          <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><User /></el-icon> Quản lý Quan hệ Khách hàng
          </div>
          <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý <span style="color:var(--el-color-primary);">Khách hàng</span></div>
          <div style="font-size:13px; color:var(--el-text-color-secondary);">{{ rows.length }} tài khoản đã đăng ký</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
          <el-button plain :loading="loading" @click="reloadCurrentTab">
            <el-icon><Refresh /></el-icon> Tải lại
          </el-button>
          <el-button v-if="mainTab === 'customers'" type="primary" @click="openCreate">
            <el-icon><Plus /></el-icon> Thêm khách hàng
          </el-button>
          <el-button v-if="mainTab === 'customers'" plain @click="openSummaryDialog">
            <el-icon><TrendCharts /></el-icon> Tổng điểm
          </el-button>
        </div>
      </div>

      <!-- Tab bar -->
      <el-tabs v-model="mainTab" class="crm2-main-tabs" @tab-click="onTabClick">
        <el-tab-pane name="customers">
          <template #label><el-space :size="6"><el-icon><User /></el-icon>Khách hàng &amp; Thành viên</el-space></template>
        </el-tab-pane>
        <el-tab-pane name="birthdays">
          <template #label><el-space :size="6"><el-icon><Present /></el-icon>Sinh nhật</el-space></template>
        </el-tab-pane>
        <el-tab-pane name="inactive">
          <template #label><el-space :size="6"><el-icon><Clock /></el-icon>Không hoạt động</el-space></template>
        </el-tab-pane>
        <el-tab-pane name="zeroorder">
          <template #label><el-space :size="6"><el-icon><ShoppingCart /></el-icon>Chưa mua hàng</el-space></template>
        </el-tab-pane>
      </el-tabs>

      <!-- ══ TAB 1: CUSTOMERS ══ -->
      <template v-if="mainTab === 'customers'">
        <!-- Stats -->
        <el-row :gutter="14">
          <el-col :xs="12" :sm="6" v-for="st in custStats" :key="st.label">
            <el-card shadow="never" :body-style="{ padding:'16px' }" :style="`border-top:3px solid ${st.color};`">
              <div :style="`font-size:24px; font-weight:800; letter-spacing:-0.03em; color:${st.color}; margin-bottom:4px;`">{{ st.value }}</div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ st.label }}</div>
            </el-card>
          </el-col>
          <el-col :xs="12" :sm="6" v-if="hasActiveFilters">
            <el-card shadow="never" :body-style="{ padding:'16px' }" style="border-top:3px solid #7c3aed;">
              <div style="font-size:24px; font-weight:800; letter-spacing:-0.03em; color:#7c3aed; margin-bottom:4px;">{{ filtered.length }}</div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">Đã lọc</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Filter panel -->
        <el-card shadow="never" :body-style="{ padding:0 }">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:10px 16px; border-bottom:1px solid var(--el-border-color-lighter);">
            <el-space :size="8">
              <el-icon><Filter /></el-icon>
              <el-text style="font-size:13px; font-weight:500;">Bộ lọc</el-text>
              <el-tag v-if="hasActiveFilters" type="primary" size="small" effect="plain">
                {{ [typeFilter,vipTierFilter,activityFilter,isPointsFilterApplied].filter(Boolean).length }} đang áp dụng
              </el-tag>
            </el-space>
            <el-button link type="danger" @click="clearAllFilters" :disabled="!hasActiveFilters">Xóa tất cả</el-button>
          </div>
          <div style="display:grid; grid-template-columns:repeat(4,1fr); border-bottom:1px solid var(--el-border-color-lighter);">
            <div v-for="ff in filterFields" :key="ff.label" style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">{{ ff.label }}</div>
              <el-input v-if="ff.type==='text'" v-model="q" placeholder="Tên / Email / SĐT…" size="small" clearable />
              <el-select v-else v-model="ff.model.value" size="small" style="width:100%;" @change="handleFilterChange">
                <el-option v-for="o in ff.options" :key="o.value" :value="o.value" :label="o.label" />
              </el-select>
            </div>
          </div>
          <!-- Points filter -->
          <div style="display:grid; grid-template-columns:1fr 1fr auto; border-bottom:1px solid var(--el-border-color-lighter);">
            <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Điểm tối thiểu</div>
              <el-input-number v-model="pointsMin" :min="0" size="small" style="width:100%;" placeholder="0" />
            </div>
            <div style="padding:12px 16px; border-right:1px solid var(--el-border-color-lighter);">
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-bottom:6px;">Điểm tối đa</div>
              <el-input-number v-model="pointsMax" :min="0" size="small" style="width:100%;" placeholder="999999" />
            </div>
            <div style="padding:12px 16px; display:flex; align-items:flex-end; gap:8px;">
              <el-button type="primary" plain size="small" @click="applyPointsFilter" :disabled="!canApplyPointsFilter">Áp dụng</el-button>
              <el-button plain size="small" @click="clearPointsFilter" :disabled="!isPointsFilterApplied">Reset</el-button>
            </div>
          </div>
          <!-- Active chips -->
          <div v-if="hasActiveFilters" style="display:flex; align-items:center; gap:8px; flex-wrap:wrap; padding:10px 16px;">
            <el-text size="small" type="info" style="text-transform:uppercase; letter-spacing:0.07em; font-weight:700; font-size:10px;">Đang hoạt động:</el-text>
            <el-tag v-if="typeFilter" size="small" closable @close="typeFilter=''; handleFilterChange()">Loại: {{ typeFilter }}</el-tag>
            <el-tag v-if="vipTierFilter" size="small" closable @close="vipTierFilter=''; handleFilterChange()">Cấp: {{ vipTierFilter }}</el-tag>
            <el-tag v-if="activityFilter === 'ACTIVE_30'" size="small" closable @close="activityFilter=''; handleFilterChange()">HD 30 ngày</el-tag>
            <el-tag v-if="isPointsFilterApplied" size="small" closable @close="clearPointsFilter">Điểm: {{ appliedPointsMin }}–{{ appliedPointsMax === 999999 ? '∞' : appliedPointsMax }}</el-tag>
          </div>
        </el-card>

        <!-- Table -->
        <el-card shadow="never" :body-style="{ padding:0 }">
          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-bottom:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light);">
            <div style="display:flex; align-items:baseline; gap:4px;">
              <span style="font-size:18px; font-weight:800;">{{ filtered.length }}</span>
              <span style="font-size:12px; color:var(--el-text-color-placeholder); font-weight:600;">/ {{ rows.length }} khách</span>
            </div>
          </div>

          <el-table :data="paged" v-loading="loading" stripe size="small">
            <el-table-column label="STT" width="70" align="center">
              <template #default="{ row }">
                <el-tag type="primary" effect="plain" size="small" style="font-family:monospace;">{{ row.id }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Khách hàng" min-width="180">
              <template #default="{ row }">
                <div style="display:flex; align-items:center; gap:10px;">
                  <el-avatar :size="32" :style="`background:${getAvatarColor(row.fullName)};color:#fff;font-size:11px;font-weight:700;border-radius:8px;`">{{ getInitials(row.fullName) }}</el-avatar>
                  <div>
                    <div style="font-weight:600; font-size:13px;">{{ row.fullName }}</div>
                    <div style="font-size:11px; color:var(--el-text-color-secondary); font-family:monospace;" v-if="row.birthDate">{{ row.birthDate }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Liên hệ" min-width="160">
              <template #default="{ row }">
                <div style="font-size:12.5px; color:var(--el-text-color-secondary);">{{ row.email }}</div>
                <div style="font-size:11.5px; color:var(--el-text-color-secondary); font-family:monospace;" v-if="row.phone">{{ row.phone }}</div>
              </template>
            </el-table-column>
            <el-table-column label="Loại" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.customerType==='VIP'?'warning':'info'" effect="plain" size="small">{{ row.customerType||'REGULAR' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Cấp" width="110" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.raw.vipTier" size="small" effect="plain">{{ row.raw.vipTier }}</el-tag>
                <span v-else style="color:var(--el-text-color-placeholder);">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Điểm" width="130">
              <template #default="{ row }">
                <div style="font-family:monospace; font-size:12.5px; font-weight:700; color:var(--el-color-primary); margin-bottom:4px;">{{ (row.loyaltyPoints||0).toLocaleString() }}</div>
                <el-progress v-if="row.raw.vipTier" :percentage="getPointsProgress(row.raw)" :stroke-width="3" :show-text="false" :color="getTierColor(row.raw.vipTier)" />
              </template>
            </el-table-column>
            <el-table-column label="Chi tiêu" width="110">
              <template #default="{ row }">
                <el-text type="info" size="small" style="font-family:monospace;">{{ formatCurrencyShort(row.raw.totalSpent) }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="Ghi chú VIP" min-width="160">
              <template #default="{ row }">
                <template v-if="row.raw.vipTier">
                  <div v-if="row.raw.vipNote" @click="openVipNoteEdit(row)"
                    style="cursor:pointer; padding:4px 8px; border-radius:6px; background:#fffbeb; border:1px solid #fde68a; max-width:180px;">
                    <el-text size="small" style="color:#d97706; line-height:1.4; display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
                      {{ row.raw.vipNote.substring(0,28) }}{{ row.raw.vipNote.length > 28 ? '…' : '' }}
                    </el-text>
                  </div>
                  <el-button v-else link type="warning" size="small" @click="openVipNoteEdit(row)">+ Thêm ghi chú</el-button>
                </template>
                <span v-else style="color:var(--el-text-color-placeholder);">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Ghi chú" min-width="140">
              <template #default="{ row }">
                <el-text size="small" type="info" v-if="row.notes" :title="row.notes" style="display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; max-width:140px;">
                  {{ row.notes.substring(0,22) }}{{ row.notes.length > 22 ? '…' : '' }}
                </el-text>
                <span v-else style="color:var(--el-text-color-placeholder);">—</span>
              </template>
            </el-table-column>
            <el-table-column label="Thao tác" width="180" align="center">
              <template #default="{ row }">
                <el-space :size="4">
                  <el-button size="small" plain type="primary" @click="viewDetails(row)">Xem</el-button>
                  <el-button size="small" plain @click="openEdit(row)">Sửa</el-button>
                  <el-button v-if="row.raw.vipTier" size="small" plain type="warning" @click="openVipNoteEdit(row)">VIP</el-button>
                  <el-button size="small" plain type="danger" @click="remove(row)">Xóa</el-button>
                </el-space>
              </template>
            </el-table-column>
            <template #empty><el-empty description="Không tìm thấy khách hàng" :image-size="70" /></template>
          </el-table>

          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); flex-wrap:wrap; gap:10px;">
            <el-text size="small" type="info">Hiển thị {{ Math.min((page-1)*pageSize+1, filtered.length) }}–{{ Math.min(page*pageSize, filtered.length) }} của {{ filtered.length }}</el-text>
            <el-pagination v-model:current-page="page" :page-size="pageSize" :total="filtered.length" layout="prev, pager, next" background small />
          </div>
        </el-card>
      </template>

      <!-- ══ TAB 2: BIRTHDAYS ══ -->
      <template v-if="mainTab === 'birthdays'">
        <el-tabs v-model="birthdayTab" @tab-click="(t) => { if(t.paneName==='monthly') loadMonthlyBirthdays(); if(t.paneName==='history') loadNotificationHistory(); }">
          <el-tab-pane label="Hôm nay" name="today" />
          <el-tab-pane label="Tháng này" name="monthly" />
          <el-tab-pane label="Thống kê" name="statistics" />
          <el-tab-pane label="Sắp tới" name="upcoming" />
          <el-tab-pane label="Lịch sử" name="history" />
        </el-tabs>

        <el-card shadow="never" :body-style="{ padding:0 }" v-loading="loading">
          <!-- Today -->
          <template v-if="!loading && birthdayTab==='today'">
            <el-alert :type="todayBirthdays.length>0?'success':'info'" :closable="false" style="border-radius:0;">
              <el-icon><Calendar /></el-icon>
              {{ todayBirthdays.length>0 ? `${todayBirthdays.length} sinh nhật hôm nay` : 'Không có sinh nhật nào hôm nay' }}
            </el-alert>
            <el-table :data="todayBirthdays" stripe size="small">
              <el-table-column label="Khách hàng" min-width="160"><template #default="{ row }"><strong>{{ row.name }}</strong></template></el-table-column>
              <el-table-column label="Email"><template #default="{ row }"><el-text type="info" size="small">{{ row.email }}</el-text></template></el-table-column>
              <el-table-column label="SĐT" width="130"><template #default="{ row }"><el-text type="info" size="small" style="font-family:monospace;">{{ row.phone }}</el-text></template></el-table-column>
              <el-table-column label="Tuổi" width="70" align="center"><template #default="{ row }"><span style="font-family:monospace;">{{ row.age }}</span></template></el-table-column>
              <el-table-column label="Loại" width="90" align="center"><template #default="{ row }"><el-tag :type="row.customerType==='VIP'?'warning':'info'" size="small" effect="plain">{{ row.customerType }}</el-tag></template></el-table-column>
              <el-table-column label="Cấp" width="90" align="center"><template #default="{ row }"><el-tag v-if="row.vipTier" size="small" effect="plain">{{ row.vipTier }}</el-tag><span v-else style="color:var(--el-text-color-placeholder);">—</span></template></el-table-column>
              <el-table-column label="Thao tác" width="130" align="center">
                <template #default="{ row }"><el-button type="primary" plain size="small" @click="sendGreeting(row)">Gửi lời chúc</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="Không có sinh nhật nào hôm nay" :image-size="60" /></template>
            </el-table>
          </template>

          <!-- Monthly -->
          <template v-if="!loading && birthdayTab==='monthly'">
            <div style="display:flex; align-items:center; gap:12px; padding:12px 16px; border-bottom:1px solid var(--el-border-color-lighter);">
              <el-select v-model="selectedMonth" @change="loadMonthlyBirthdays" style="width:160px;" size="small">
                <el-option v-for="m in monthOptions" :key="m.value" :value="m.value" :label="m.label" />
              </el-select>
              <el-tag v-if="monthlyBirthdays.length>0" effect="plain">{{ monthlyBirthdays.length }} khách hàng tháng này</el-tag>
            </div>
            <el-table :data="monthlyBirthdays" stripe size="small">
              <el-table-column label="Khách hàng" min-width="160"><template #default="{ row }"><strong>{{ row.name }}</strong></template></el-table-column>
              <el-table-column label="Email"><template #default="{ row }"><el-text type="info" size="small">{{ row.email }}</el-text></template></el-table-column>
              <el-table-column label="SĐT" width="130"><template #default="{ row }"><el-text type="info" size="small" style="font-family:monospace;">{{ row.phone }}</el-text></template></el-table-column>
              <el-table-column label="Ngày" width="120"><template #default="{ row }">{{ formatBirthdayDate(row) }}</template></el-table-column>
              <el-table-column label="Tuổi" width="70" align="center"><template #default="{ row }"><span style="font-family:monospace;">{{ row.age }}</span></template></el-table-column>
              <el-table-column label="Loại" width="90" align="center"><template #default="{ row }"><el-tag :type="row.customerType==='VIP'?'warning':'info'" size="small" effect="plain">{{ row.customerType }}</el-tag></template></el-table-column>
              <el-table-column label="Thao tác" width="100" align="center">
                <template #default="{ row }"><el-button type="primary" plain size="small" @click="sendGreeting(row)">Send</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="Không có sinh nhật nào trong tháng" :image-size="60" /></template>
            </el-table>
          </template>

          <!-- Statistics -->
          <template v-if="!loading && birthdayTab==='statistics'">
            <el-table :data="statsTableData" stripe size="small">
              <el-table-column label="Tháng" min-width="120"><template #default="{ row }"><strong>{{ row.monthLabel }}</strong></template></el-table-column>
              <el-table-column label="Số lượng" width="100" align="center"><template #default="{ row }"><el-tag :type="row.count>0?'primary':'info'" size="small" effect="plain">{{ row.count }}</el-tag></template></el-table-column>
              <el-table-column label="Phân bổ" min-width="200">
                <template #default="{ row }"><el-progress :percentage="getBdBarPct(row.count)" :stroke-width="8" :show-text="false" /></template>
              </el-table-column>
              <el-table-column label="Thao tác" width="120" align="center">
                <template #default="{ row }"><el-button v-if="row.count>0" link size="small" @click="viewMonthDetail(row.month)">Chi tiết</el-button></template>
              </el-table-column>
            </el-table>
          </template>

          <!-- Upcoming -->
          <template v-if="!loading && birthdayTab==='upcoming'">
            <el-alert :type="upcomingBirthdays.length>0?'warning':'info'" :closable="false" style="border-radius:0;">
              {{ upcomingBirthdays.length>0 ? `${upcomingBirthdays.length} sắp tới trong 7 ngày` : 'Không có sinh nhật sắp tới' }}
            </el-alert>
            <el-table :data="upcomingBirthdays" stripe size="small">
              <el-table-column label="Khách hàng" min-width="160"><template #default="{ row }"><strong>{{ row.name }}</strong></template></el-table-column>
              <el-table-column label="Email"><template #default="{ row }"><el-text type="info" size="small">{{ row.email }}</el-text></template></el-table-column>
              <el-table-column label="SĐT" width="130"><template #default="{ row }"><el-text type="info" size="small" style="font-family:monospace;">{{ row.phone }}</el-text></template></el-table-column>
              <el-table-column label="Ngày" width="120"><template #default="{ row }">{{ formatBirthdayDate(row) }}</template></el-table-column>
              <el-table-column label="Còn lại" width="100" align="center"><template #default="{ row }"><el-tag :type="row.daysUntilBirthday<=3?'danger':'warning'" size="small" effect="plain">{{ row.daysUntilBirthday }} ngày</el-tag></template></el-table-column>
              <el-table-column label="Loại" width="90" align="center"><template #default="{ row }"><el-tag :type="row.customerType==='VIP'?'warning':'info'" size="small" effect="plain">{{ row.customerType }}</el-tag></template></el-table-column>
              <el-table-column label="Thao tác" width="100" align="center">
                <template #default="{ row }"><el-button type="primary" plain size="small" @click="sendGreeting(row)">Send</el-button></template>
              </el-table-column>
              <template #empty><el-empty description="Không có sinh nhật sắp tới" :image-size="60" /></template>
            </el-table>
          </template>

          <!-- History -->
          <template v-if="birthdayTab==='history'">
            <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-bottom:1px solid var(--el-border-color-lighter);">
              <el-tag v-if="notificationHistory.length>0" effect="plain">{{ notificationHistory.length }} thông báo đã gửi</el-tag>
              <el-button plain size="small" :loading="historyLoading" @click="loadNotificationHistory">Tải lại</el-button>
            </div>
            <el-skeleton v-if="historyLoading" :rows="4" animated style="padding:16px;" />
            <el-table v-else :data="notificationHistory" stripe size="small">
              <el-table-column label="Khách hàng" min-width="140"><template #default="{ row }"><strong>{{ row.customerName }}</strong></template></el-table-column>
              <el-table-column label="Email"><template #default="{ row }"><el-text type="info" size="small">{{ row.customerEmail }}</el-text></template></el-table-column>
              <el-table-column label="Tiêu đề" min-width="160"><template #default="{ row }">{{ row.title }}</template></el-table-column>
              <el-table-column label="Nội dung" min-width="200" show-overflow-tooltip><template #default="{ row }">{{ row.message }}</template></el-table-column>
              <el-table-column label="Trạng thái" width="100" align="center"><template #default="{ row }"><el-tag :type="row.isRead?'success':'warning'" size="small" effect="plain">{{ row.isRead ? 'Đã đọc' : 'Chưa đọc' }}</el-tag></template></el-table-column>
              <el-table-column label="Gửi lúc" width="140"><template #default="{ row }"><el-text size="small" type="info" style="font-family:monospace;">{{ formatDateTime(row.createdAt) }}</el-text></template></el-table-column>
              <el-table-column label="Đọc lúc" width="140"><template #default="{ row }"><el-text size="small" type="info" style="font-family:monospace;">{{ row.readAt ? formatDateTime(row.readAt) : '—' }}</el-text></template></el-table-column>
              <template #empty><el-empty description="Chưa có thông báo nào" :image-size="60" /></template>
            </el-table>
          </template>
        </el-card>
      </template>

      <!-- ══ TAB 3: INACTIVE ══ -->
      <template v-if="mainTab === 'inactive'">
        <!-- Segment tabs -->
        <el-card shadow="never" :body-style="{ padding:'12px 16px' }">
          <el-space :size="8">
            <el-button
              v-for="tab in inactiveTabs" :key="tab.days"
              :type="activeInactiveTab===tab.days ? 'primary' : ''"
              :plain="activeInactiveTab!==tab.days"
              size="small"
              @click="switchInactiveTab(tab.days)"
            >
              {{ tab.label }}
              <el-tag v-if="inactiveCounts[tab.days]!==null" size="small" :effect="activeInactiveTab===tab.days?'dark':'plain'" style="margin-left:4px;">{{ inactiveCounts[tab.days] }}</el-tag>
            </el-button>
          </el-space>
        </el-card>

        <!-- Stats -->
        <el-row v-if="!loading && currentInactive.length>0" :gutter="14">
          <el-col :xs="12" :sm="6" v-for="ist in inactiveStats" :key="ist.label">
            <el-card shadow="never" :body-style="{ padding:'16px' }" :style="`border-top:3px solid ${ist.color};`">
              <div :style="`font-size:22px; font-weight:800; color:${ist.color}; margin-bottom:3px;`">{{ ist.value }}</div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ ist.label }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Filter bar -->
        <el-card shadow="never" v-if="!loading">
          <el-row :gutter="12" align="middle">
            <el-col :xs="24" :sm="10">
              <el-input v-model="inactiveQ" placeholder="Tìm tên, email, SĐT…" clearable size="small">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </el-col>
            <el-col :xs="12" :sm="7">
              <el-select v-model="inactiveTierFilter" clearable placeholder="Tất cả cấp" size="small" style="width:100%;">
                <el-option v-for="t in tiers" :key="t" :value="t" :label="t" />
              </el-select>
            </el-col>
            <el-col :xs="12" :sm="7">
              <el-select v-model="inactiveSortBy" size="small" style="width:100%;">
                <el-option value="spent" label="Chi tiêu nhiều nhất" />
                <el-option value="points" label="Điểm nhiều nhất" />
                <el-option value="name" label="Tên A→Z" />
              </el-select>
            </el-col>
          </el-row>
        </el-card>

        <el-skeleton v-if="loading" :rows="4" animated />

        <el-empty v-else-if="inactiveFiltered.length===0" :image-size="80">
          <template #description><div style="font-size:16px; font-weight:700;">Tốt cả! Tất cả khách hàng đều hoạt động trong {{ activeInactiveTab }} ngày qua.</div></template>
        </el-empty>

        <!-- Cards grid -->
        <div v-if="!loading && inactivePaged.length>0" style="display:grid; grid-template-columns:repeat(auto-fill, minmax(300px, 1fr)); gap:14px;">
          <el-card v-for="row in inactivePaged" :key="row.id" shadow="never" :body-style="{ padding:0 }">
            <div :style="`height:4px; background:${getTierColor(row.raw?.vipTier||'none')};`"></div>
            <div style="padding:16px;">
              <div style="display:flex; align-items:center; gap:12px; margin-bottom:12px;">
                <el-avatar :size="36" :style="`background:${getAvatarColor(row.fullName)};color:#fff;font-size:13px;font-weight:700;`">{{ getInitials(row.fullName) }}</el-avatar>
                <div style="flex:1; min-width:0;">
                  <div style="font-weight:700; font-size:14px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{{ row.fullName }}</div>
                  <div style="font-size:12px; color:var(--el-text-color-secondary); white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">{{ row.email }}</div>
                </div>
                <el-tag v-if="row.raw?.vipTier" size="small" effect="plain">{{ row.raw.vipTier }}</el-tag>
              </div>
              <el-divider style="margin:10px 0;" />
              <div style="display:flex; justify-content:space-around; margin-bottom:12px;">
                <div style="text-align:center;">
                  <div style="font-family:monospace; font-weight:800; font-size:15px;">{{ (row.loyaltyPoints||0).toLocaleString() }}</div>
                  <div style="font-size:10px; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-top:2px;">Điểm</div>
                </div>
                <el-divider direction="vertical" style="height:auto;" />
                <div style="text-align:center;">
                  <div style="font-family:monospace; font-weight:800; font-size:15px;">{{ formatMoneyShort(row.raw?.totalSpent) }}</div>
                  <div style="font-size:10px; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-top:2px;">Chi tiêu</div>
                </div>
                <el-divider direction="vertical" style="height:auto;" />
                <div style="text-align:center;">
                  <div style="font-family:monospace; font-weight:800; font-size:15px;">{{ row.phone || '—' }}</div>
                  <div style="font-size:10px; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-top:2px;">SĐT</div>
                </div>
              </div>
              <div style="display:flex; align-items:center; justify-content:space-between;">
                <el-text size="small" :type="row.raw?.lastOrderAt ? 'warning' : 'danger'">
                  {{ row.raw?.lastOrderAt ? 'Đơn cuối: '+relativeDate(row.raw.lastOrderAt) : 'Chưa có đơn hàng' }}
                </el-text>
                <el-button type="primary" plain size="small" @click="openInactiveDetail(row)">Xem</el-button>
              </div>
            </div>
          </el-card>
        </div>

        <div v-if="!loading && inactiveFiltered.length>inactivePageSize" style="display:flex; align-items:center; justify-content:space-between; padding:12px 0; flex-wrap:wrap; gap:10px;">
          <el-text size="small" type="info">{{ (inactivePage-1)*inactivePageSize+1 }}–{{ Math.min(inactivePage*inactivePageSize, inactiveFiltered.length) }} / {{ inactiveFiltered.length }}</el-text>
          <el-pagination v-model:current-page="inactivePage" :page-size="inactivePageSize" :total="inactiveFiltered.length" layout="prev, pager, next" background small />
        </div>
      </template>

      <!-- ══ TAB 4: ZERO ORDER ══ -->
      <template v-if="mainTab === 'zeroorder'">
        <!-- Stats -->
        <el-row :gutter="14">
          <el-col :xs="12" :sm="6" v-for="zs in zeroStats" :key="zs.label">
            <el-card shadow="never" :body-style="{ padding:'16px' }" :style="`border-top:3px solid ${zs.color};`">
              <div :style="`font-size:22px; font-weight:800; color:${zs.color}; margin-bottom:3px;`">{{ zs.value }}</div>
              <div style="font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ zs.label }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- Filter + actions -->
        <el-card shadow="never">
          <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:12px;">
            <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
              <el-text size="small" type="info">Đăng ký tối thiểu</el-text>
              <el-button v-for="opt in zoDayOptions" :key="opt.value" size="small"
                :type="zoSelectedDays===opt.value?'primary':''"
                :plain="zoSelectedDays!==opt.value"
                @click="zoSelectDays(opt.value)">{{ opt.label }}</el-button>
            </div>
            <el-space :size="8">
              <el-button plain size="small" @click="zoSelectAll">{{ zoIsAllSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả' }}</el-button>
              <el-button type="primary" :disabled="zoSelected.length===0||zoSending" @click="openZoSendDialog">
                <el-icon><Promotion /></el-icon>
                Gửi thông báo
                <el-badge v-if="zoSelected.length>0" :value="zoSelected.length" style="margin-left:4px;" />
              </el-button>
            </el-space>
          </div>
          <el-input v-model="zoSearch" placeholder="Tên / Email / SĐT…" clearable size="small" style="max-width:400px;" @input="zoPage=1">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-card>

        <el-card shadow="never" :body-style="{ padding:0 }">
          <el-skeleton v-if="loading" :rows="5" animated style="padding:16px;" />
          <el-table v-else :data="zoPaged" stripe size="small">
            <el-table-column width="44" align="center">
              <template #header><el-checkbox :model-value="zoIsAllSelected" @change="zoSelectAll" /></template>
              <template #default="{ row }"><el-checkbox :value="row.id" v-model="zoSelected" /></template>
            </el-table-column>
            <el-table-column label="Khách hàng" min-width="200">
              <template #default="{ row }">
                <div style="display:flex; align-items:center; gap:10px;">
                  <el-avatar :size="30" :style="`background:${getAvatarColor(row.name)};color:#fff;font-size:11px;font-weight:700;`">{{ getInitials(row.name) }}</el-avatar>
                  <div>
                    <div style="font-weight:600; font-size:13px;">{{ row.name }}</div>
                    <div style="font-size:11px; color:var(--el-text-color-secondary);">{{ row.email }}</div>
                    <div v-if="row.phone" style="font-size:11px; color:var(--el-text-color-secondary); font-family:monospace;">{{ row.phone }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="Loại" width="90" align="center">
              <template #default="{ row }"><el-tag :type="row.customerType==='VIP'?'warning':'info'" size="small" effect="plain">{{ row.customerType||'REGULAR' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="Đăng ký lúc" width="160">
              <template #default="{ row }">
                <div style="font-size:12px; font-family:monospace;">{{ formatDate(row.createdAt) }}</div>
                <div style="font-size:11px; color:var(--el-text-color-secondary);">{{ relativeDate(row.createdAt) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="Ngày chờ" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="zoDaysSince(row.createdAt)>=30?'danger':zoDaysSince(row.createdAt)>=7?'warning':'info'" size="small" effect="plain">{{ zoDaysSince(row.createdAt) }} ngày</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Trạng thái" min-width="130">
              <template #default="{ row }"><el-text size="small" type="info">{{ zoStatusLabel(row) }}</el-text></template>
            </el-table-column>
            <el-table-column label="Thao tác" width="90" align="center">
              <template #default="{ row }">
                <el-button size="small" plain type="primary" :loading="zoSendingIds.has(row.id)" @click="zoSendSingle(row)">
                  {{ zoSendingIds.has(row.id) ? '…' : '📨 Gửi' }}
                </el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="🎉 Tất cả khách đều đã mua hàng trong khoảng này!" :image-size="60" /></template>
          </el-table>

          <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 20px; border-top:1px solid var(--el-border-color-lighter); background:var(--el-fill-color-light); flex-wrap:wrap; gap:10px;">
            <el-text size="small" type="info">
              {{ zoFiltered.length > 0 ? `${Math.min((zoPage-1)*pageSize+1, zoFiltered.length)}–${Math.min(zoPage*pageSize, zoFiltered.length)} của ${zoFiltered.length}` : '0 khách' }}
            </el-text>
            <el-pagination v-if="zoPageCount>1" v-model:current-page="zoPage" :page-size="pageSize" :total="zoFiltered.length" layout="prev, pager, next" background small />
          </div>
        </el-card>
      </template>

    </div><!-- /inner -->

    <!-- ══ DIALOG: Create / Edit ══ -->
    <el-dialog v-model="dlg.open" :title="dlg.mode==='create' ? 'Thêm khách hàng' : 'Sửa khách hàng'" width="640px" :close-on-click-modal="false">
      <el-alert v-if="dlg.alert" type="error" :title="dlg.alert" show-icon :closable="false" style="margin-bottom:16px;" />
      <el-form label-position="top" style="display:flex; flex-direction:column; gap:0;">
        <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:var(--el-text-color-secondary); margin-bottom:10px;">Thông tin cơ bản</div>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="Họ và tên *">
              <el-input v-model="dlg.form.fullName" placeholder="Nguyễn Văn A" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Loại khách hàng">
              <el-select v-model="dlg.form.customerType" style="width:100%;">
                <el-option value="REGULAR" label="Thường" />
                <el-option value="VIP" label="VIP" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Email *">
              <el-input v-model="dlg.form.email" type="email" placeholder="email@example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SĐT *">
              <el-input v-model="dlg.form.phone" placeholder="0912345678" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Ngày sinh">
              <el-date-picker v-model="dlg.form.birthDate" type="date" placeholder="Chọn ngày" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Địa chỉ">
              <el-input v-model="dlg.form.address" placeholder="123 Đường ABC, Quận 1" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="Ghi chú">
              <el-input v-model="dlg.form.notes" type="textarea" :rows="2" placeholder="Ghi chú chung…" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="dlg.mode==='edit' && dlg.form.vipTier">
          <el-divider />
          <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:10px;">
            <div style="font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:0.06em; color:var(--el-text-color-secondary);">Ghi chú VIP</div>
            <div style="display:flex; align-items:center; gap:6px;">
              <el-tag size="small" effect="plain">{{ tierIcon(dlg.form.vipTier) }} {{ dlg.form.vipTier }}</el-tag>
              <el-tag size="small" type="danger" effect="plain">Nội bộ · Chỉ admin</el-tag>
            </div>
          </div>
          <el-input v-model="dlg.form.vipNote" type="textarea" :rows="3" placeholder="vd: Thích laptop gaming, không lấy hàng trưng bày…" :maxlength="500" show-word-limit />
          <el-text size="small" type="info" style="margin-top:4px;">Khách hàng không thể xem ghi chú này.</el-text>
        </template>
        <el-alert v-if="dlg.mode==='create'" type="info" title="Ghi chú VIP có sẵn khi khách hàng đạt cấp Đồng trở lên." :closable="false" style="margin-top:8px;" />
      </el-form>
      <template #footer>
        <el-button @click="dlg.open=false">Hủy bỏ</el-button>
        <el-button :type="dlg.mode==='create'?'primary':'warning'" :loading="dlg.loading" @click="save">
          {{ dlg.mode==='create' ? 'Tạo mới' : 'Lưu thay đổi' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: Details ══ -->
    <el-dialog v-model="detailsDialog.open" width="640px" :close-on-click-modal="false" title="">
      <template v-if="detailsDialog.customer">
        <div style="display:flex; align-items:center; gap:14px; margin-bottom:20px;">
          <el-avatar :size="52" :style="`background:${getAvatarColor(detailsDialog.customer.fullName)};color:#fff;font-size:18px;font-weight:700;`">{{ getInitials(detailsDialog.customer.fullName) }}</el-avatar>
          <div>
            <div style="font-size:20px; font-weight:800; letter-spacing:-0.02em;">{{ detailsDialog.customer.fullName }}</div>
            <div style="color:var(--el-text-color-secondary); font-size:13px;">#{{ detailsDialog.customer.id }} · {{ detailsDialog.customer.email }}</div>
          </div>
        </div>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-card shadow="never">
              <template #header><el-text tag="b" size="small">Thông tin cá nhân</el-text></template>
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="Mã"><span style="font-family:monospace; color:var(--el-color-primary);">#{{ detailsDialog.customer.id }}</span></el-descriptions-item>
                <el-descriptions-item label="Tên"><strong>{{ detailsDialog.customer.fullName }}</strong></el-descriptions-item>
                <el-descriptions-item label="Email">{{ detailsDialog.customer.email }}</el-descriptions-item>
                <el-descriptions-item label="SĐT"><span style="font-family:monospace;">{{ detailsDialog.customer.phone||'—' }}</span></el-descriptions-item>
                <el-descriptions-item label="Ngày sinh">{{ detailsDialog.customer.birthDate||'—' }}</el-descriptions-item>
                <el-descriptions-item label="Địa chỉ">{{ detailsDialog.customer.address||'—' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-card shadow="never">
              <template #header><el-text tag="b" size="small">Loyalty &amp; VIP</el-text></template>
              <el-descriptions :column="1" size="small" border>
                <el-descriptions-item label="Loại"><el-tag :type="detailsDialog.customer.customerType==='VIP'?'warning':'info'" size="small" effect="plain">{{ detailsDialog.customer.customerType }}</el-tag></el-descriptions-item>
                <el-descriptions-item label="Cấp">
                  <el-tag v-if="detailsDialog.customer.raw.vipTier" size="small" effect="plain">{{ detailsDialog.customer.raw.vipTier }}</el-tag>
                  <span v-else style="color:var(--el-text-color-placeholder);">Chưa có cấp</span>
                </el-descriptions-item>
                <el-descriptions-item label="Điểm"><strong style="color:var(--el-color-primary);">{{ (detailsDialog.customer.loyaltyPoints||0).toLocaleString() }} điểm</strong></el-descriptions-item>
                <el-descriptions-item label="Tổng chi tiêu"><strong style="color:var(--el-color-success);">{{ formatCurrency(detailsDialog.customer.raw.totalSpent) }}</strong></el-descriptions-item>
              </el-descriptions>
            </el-card>
            <el-card shadow="never" style="margin-top:12px;" v-if="detailsDialog.customer.raw.vipTier">
              <template #header>
                <div style="display:flex; align-items:center; justify-content:space-between;">
                  <el-text tag="b" size="small">Ghi chú VIP</el-text>
                  <el-button link size="small" @click="openVipNoteEdit(detailsDialog.customer)">Sửa</el-button>
                </div>
              </template>
              <el-text v-if="detailsDialog.customer.raw.vipNote" type="warning" size="small">{{ detailsDialog.customer.raw.vipNote }}</el-text>
              <el-text v-else type="info" size="small" tag="i">Chưa có ghi chú VIP.</el-text>
            </el-card>
          </el-col>
        </el-row>
      </template>
      <template #footer>
        <el-button @click="detailsDialog.open=false">Đóng</el-button>
        <el-button type="primary" @click="openEdit(detailsDialog.customer)">Sửa khách hàng</el-button>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: VIP Note ══ -->
    <el-dialog v-model="vipNoteDialog.open" title="Ghi chú VIP" width="480px" :close-on-click-modal="false">
      <el-alert type="warning" title="Chỉ nội bộ — Khách hàng không thể xem. Chỉ Admin và Staff." :closable="false" style="margin-bottom:16px;" />
      <div v-if="vipNoteDialog.customerName" style="display:flex; align-items:center; gap:8px; margin-bottom:12px;">
        <el-tag effect="plain">♦ {{ vipNoteDialog.vipTier }}</el-tag>
        <el-text style="font-weight:600;">{{ vipNoteDialog.customerName }}</el-text>
      </div>
      <div v-if="!vipNoteDialog.note" style="margin-bottom:12px;">
        <el-text size="small" type="info" style="display:block; margin-bottom:6px;">Gợi ý nhanh</el-text>
        <el-space wrap :size="6">
          <el-button v-for="s in vipSuggestions" :key="s" size="small" plain @click="vipNoteDialog.note = s">{{ s }}</el-button>
        </el-space>
      </div>
      <el-input v-model="vipNoteDialog.note" type="textarea" :rows="5" placeholder="Ghi chú nội bộ về khách hàng VIP này…" :maxlength="500" show-word-limit ref="vipNoteTextarea" />
      <template #footer>
        <el-button @click="vipNoteDialog.open=false">Hủy bỏ</el-button>
        <el-button type="warning" :loading="vipNoteDialog.loading" @click="saveVipNote">Lưu ghi chú VIP</el-button>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: Loyalty Summary ══ -->
    <el-dialog v-model="summaryDlg.open" title="Tổng điểm" width="780px" :close-on-click-modal="false">
      <!-- Controls -->
      <div style="display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; margin-bottom:16px;">
        <el-space :size="12" wrap>
          <el-space :size="6">
            <el-text size="small" type="info">Kỳ</el-text>
            <el-radio-group v-model="summaryDlg.mode" size="small" @change="loadAdminSummary">
              <el-radio-button value="monthly">Tháng</el-radio-button>
              <el-radio-button value="weekly">Tuần</el-radio-button>
            </el-radio-group>
          </el-space>
          <el-space :size="6">
            <el-text size="small" type="info">{{ summaryDlg.mode==='weekly' ? 'Tuần' : 'Tháng' }}</el-text>
            <el-select v-model="summaryDlg.range" style="width:80px;" size="small" @change="loadAdminSummary">
              <template v-if="summaryDlg.mode==='weekly'">
                <el-option :value="4" label="4w" /><el-option :value="8" label="8w" /><el-option :value="12" label="12w" /><el-option :value="26" label="26w" />
              </template>
              <template v-else>
                <el-option :value="3" label="3m" /><el-option :value="6" label="6m" /><el-option :value="12" label="12m" /><el-option :value="24" label="24m" />
              </template>
            </el-select>
          </el-space>
          <el-radio-group v-model="summaryDlg.view" size="small">
            <el-radio-button value="overview">Tổng quan</el-radio-button>
            <el-radio-button value="detail">Theo KH</el-radio-button>
          </el-radio-group>
        </el-space>
        <el-button plain size="small" :loading="summaryDlg.loading" @click="loadAdminSummary">Tải lại</el-button>
      </div>

      <el-skeleton v-if="summaryDlg.loading" :rows="6" animated />
      <el-empty v-else-if="!summaryDlg.data.length" description="Không có dữ liệu trong kỳ này" :image-size="60" />
      <template v-else>
        <!-- KPI -->
        <el-row :gutter="10" style="margin-bottom:16px;">
          <el-col :span="6" v-for="sk in [
            {val:'+'+adminSummaryTotals.earned.toLocaleString(), label:'Điểm kiếm được', type:'success'},
            {val:'-'+adminSummaryTotals.deducted.toLocaleString(), label:'Điểm đã dùng', type:'warning'},
            {val:(adminSummaryTotals.net>=0?'+':'')+adminSummaryTotals.net.toLocaleString(), label:'Lý thuyết', type:adminSummaryTotals.net>=0?'primary':'danger'},
            {val:adminSummaryTotals.transactions.toLocaleString(), label:'Giao dịch', type:'info'},
          ]" :key="sk.label">
            <el-card shadow="never" :body-style="{ padding:'12px 14px' }">
              <div :style="`font-size:16px; font-weight:800; font-family:monospace; color:var(--el-color-${sk.type}); margin-bottom:3px;`">{{ sk.val }}</div>
              <div style="font-size:10px; font-weight:700; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary);">{{ sk.label }}</div>
            </el-card>
          </el-col>
        </el-row>
        <!-- Overview -->
        <template v-if="summaryDlg.view==='overview'">
          <div v-for="period in summaryDlg.data" :key="period.period" style="padding:12px 0; border-bottom:1px solid var(--el-border-color-lighter);">
            <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; flex-wrap:wrap; gap:8px;">
              <div>
                <div style="font-weight:700; font-size:13px;">{{ period.periodLabel }}</div>
                <div style="font-size:11px; font-family:monospace; color:var(--el-text-color-secondary);">{{ period.periodStart }} – {{ period.periodEnd }}</div>
              </div>
              <el-tag :type="period.netPoints>=0?'primary':'danger'" effect="plain" size="small" style="font-family:monospace;">{{ period.netPoints>=0?'+':'' }}{{ period.netPoints.toLocaleString() }}</el-tag>
            </div>
            <el-row :gutter="8">
              <el-col :span="12">
                <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:4px;">
                  <el-text size="small" style="color:#16a34a;">+{{ period.totalPointsEarned.toLocaleString() }}</el-text>
                </div>
                <el-progress :percentage="getAdminBarWidth(period.totalPointsEarned,'earn')" :stroke-width="6" :show-text="false" color="#3b82f6" />
              </el-col>
              <el-col :span="12">
                <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:4px;">
                  <el-text size="small" style="color:#dc2626;">-{{ period.totalPointsDeducted.toLocaleString() }}</el-text>
                  <el-text size="small" type="info">{{ period.totalTransactions }} gd</el-text>
                </div>
                <el-progress :percentage="getAdminBarWidth(period.totalPointsDeducted,'deduct')" :stroke-width="6" :show-text="false" color="#ef4444" />
              </el-col>
            </el-row>
          </div>
        </template>
        <!-- Detail by customer -->
        <template v-else>
          <div v-for="period in summaryDlg.data" :key="period.period" style="margin-bottom:16px;">
            <div style="display:flex; align-items:center; gap:12px; padding:8px 0; border-bottom:1px solid var(--el-border-color-lighter); margin-bottom:8px;">
              <el-text style="font-weight:700; font-family:monospace;">{{ period.periodLabel }}</el-text>
              <el-text size="small" style="color:#16a34a; font-family:monospace;">+{{ period.totalPointsEarned.toLocaleString() }}</el-text>
              <el-text size="small" style="color:#dc2626; font-family:monospace;">-{{ period.totalPointsDeducted.toLocaleString() }}</el-text>
              <el-text size="small" type="info" style="font-family:monospace;">{{ period.totalTransactions }} txn</el-text>
            </div>
            <el-table v-if="period.customerBreakdown?.length" :data="period.customerBreakdown" size="small" stripe>
              <el-table-column label="Khách hàng" min-width="160">
                <template #default="{ row }">
                  <div style="font-size:12px; font-weight:600;">{{ row.customerName }}</div>
                  <div style="font-size:10px; color:var(--el-text-color-secondary);">{{ row.customerEmail }}</div>
                </template>
              </el-table-column>
              <el-table-column label="Cấp" width="80" align="center"><template #default="{ row }"><el-tag v-if="row.vipTier&&row.vipTier!=='Member'" size="small" effect="plain">{{ row.vipTier }}</el-tag><span v-else style="color:var(--el-text-color-placeholder);">—</span></template></el-table-column>
              <el-table-column label="Kiếm được" align="right" width="90"><template #default="{ row }"><span style="color:#16a34a; font-family:monospace; font-size:12px;">+{{ row.pointsEarned.toLocaleString() }}</span></template></el-table-column>
              <el-table-column label="Đã dùng" align="right" width="90"><template #default="{ row }"><span style="color:#dc2626; font-family:monospace; font-size:12px;">{{ row.pointsDeducted>0?'-':'' }}{{ row.pointsDeducted.toLocaleString() }}</span></template></el-table-column>
              <el-table-column label="Lý thuyết" align="right" width="90"><template #default="{ row }"><span :style="`font-family:monospace; font-size:12px; font-weight:700; color:${row.netPoints>=0?'#2563eb':'#dc2626'};`">{{ row.netPoints>=0?'+':'' }}{{ row.netPoints.toLocaleString() }}</span></template></el-table-column>
              <el-table-column label="Gd" align="center" width="50"><template #default="{ row }"><el-text size="small" type="info" style="font-family:monospace;">{{ row.transactionCount }}</el-text></template></el-table-column>
            </el-table>
            <el-empty v-else description="Không có giao dịch" :image-size="40" />
          </div>
        </template>
      </template>
      <template #footer>
        <el-button @click="summaryDlg.open=false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: Greeting ══ -->
    <el-dialog v-model="greetingDialog.open" title="Gửi lời chúc" width="460px" :close-on-click-modal="false">
      <el-alert v-if="greetingDialog.customer" type="info" :closable="false" style="margin-bottom:16px;">
        Gửi tới: <strong>{{ greetingDialog.customer.name }}</strong> ({{ greetingDialog.customer.email }})
      </el-alert>
      <el-form-item label="Tin nhắn">
        <el-input v-model="greetingDialog.form.message" type="textarea" :rows="8" placeholder="Viết lời chúc sinh nhật…" />
      </el-form-item>
      <template #footer>
        <el-button @click="greetingDialog.open=false">Hủy bỏ</el-button>
        <el-button type="success" :loading="greetingDialog.sending" @click="confirmSendGreeting">
          <el-icon><Promotion /></el-icon> Gửi lời chúc
        </el-button>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: Inactive Detail ══ -->
    <el-dialog v-model="inactiveDetail.open" width="400px" :close-on-click-modal="false" title="" align-center>
      <template v-if="inactiveDetail.customer">
        <div style="text-align:center; padding:12px 0;">
          <el-avatar :size="64" :style="`background:${getAvatarColor(inactiveDetail.customer.fullName)};color:#fff;font-size:22px;font-weight:700;margin-bottom:12px;`">{{ getInitials(inactiveDetail.customer.fullName) }}</el-avatar>
          <div style="font-size:20px; font-weight:800; letter-spacing:-0.02em; margin-bottom:4px;">{{ inactiveDetail.customer.fullName }}</div>
          <div style="font-size:13px; color:var(--el-text-color-secondary); margin-bottom:12px;">{{ inactiveDetail.customer.email }}</div>
          <el-tag v-if="inactiveDetail.customer.raw?.vipTier" size="large" effect="plain">{{ inactiveDetail.customer.raw.vipTier }}</el-tag>
        </div>
        <el-row :gutter="0" style="margin:16px 0; border:1px solid var(--el-border-color-light); border-radius:8px; overflow:hidden;">
          <el-col :span="12" style="padding:12px; text-align:center; border-right:1px solid var(--el-border-color-light);">
            <div style="font-family:monospace; font-weight:800; font-size:16px; color:var(--el-color-warning);">{{ (inactiveDetail.customer.loyaltyPoints||0).toLocaleString() }}</div>
            <div style="font-size:10px; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-top:3px;">Điểm</div>
          </el-col>
          <el-col :span="12" style="padding:12px; text-align:center;">
            <div style="font-family:monospace; font-weight:800; font-size:16px; color:var(--el-color-success);">{{ formatCurrency(inactiveDetail.customer.raw?.totalSpent) }}</div>
            <div style="font-size:10px; text-transform:uppercase; letter-spacing:0.05em; color:var(--el-text-color-secondary); margin-top:3px;">Chi tiêu</div>
          </el-col>
        </el-row>
        <el-descriptions :column="1" size="small" border style="margin-bottom:12px;">
          <el-descriptions-item label="SĐT"><span style="font-family:monospace;">{{ inactiveDetail.customer.phone||'—' }}</span></el-descriptions-item>
          <el-descriptions-item label="Ngày sinh">{{ inactiveDetail.customer.birthDate||'—' }}</el-descriptions-item>
          <el-descriptions-item label="Địa chỉ">{{ inactiveDetail.customer.raw?.address||'—' }}</el-descriptions-item>
          <el-descriptions-item label="Đơn hàng cuối"><span style="color:var(--el-color-warning);">{{ relativeDate(inactiveDetail.customer.raw?.lastOrderAt) }}</span></el-descriptions-item>
          <el-descriptions-item v-if="inactiveDetail.customer.notes" label="Ghi chú">{{ inactiveDetail.customer.notes }}</el-descriptions-item>
        </el-descriptions>
        <el-alert type="warning" :closable="false">
          Không có giao dịch trong <strong>{{ activeInactiveTab }}+ ngày</strong>. Cân nhắc liên hệ lại.
        </el-alert>
      </template>
    </el-dialog>

    <!-- ══ DIALOG: Zero Order Send ══ -->
    <el-dialog v-model="zoDlg.open" title="Gửi thông báo chào mừng" width="620px" :close-on-click-modal="false">
      <el-descriptions :column="1" size="small" border style="margin-bottom:16px;">
        <el-descriptions-item label="Số lượng">
          <el-tag type="primary" effect="plain">{{ zoSelected.length }} khách</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-form-item label="Mẫu thông báo">
        <div style="display:grid; grid-template-columns:repeat(2,1fr); gap:8px;">
          <div v-for="tpl in zoTemplates" :key="tpl.id"
            @click="zoSelectTemplate(tpl)"
            :style="`padding:10px 12px; border-radius:8px; border:2px solid ${zoDlg.form.templateId===tpl.id?'var(--el-color-primary)':'var(--el-border-color)'}; cursor:pointer; background:${zoDlg.form.templateId===tpl.id?'var(--el-color-primary-light-9)':'transparent'}; transition:all .15s;`">
            <div style="font-size:18px; margin-bottom:4px;">{{ tpl.icon }}</div>
            <div style="font-size:12px; font-weight:700;">{{ tpl.name }}</div>
            <div style="font-size:11px; color:var(--el-text-color-secondary);">{{ tpl.desc }}</div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="Tiêu đề thông báo">
        <el-input v-model="zoDlg.form.title" placeholder="Nhập tiêu đề…" />
      </el-form-item>
      <el-form-item label="Nội dung">
        <el-input v-model="zoDlg.form.message" type="textarea" :rows="5" placeholder="Nhập nội dung…" />
        <el-text size="small" type="info" style="margin-top:4px;">Dùng <code style="background:var(--el-fill-color); padding:1px 6px; border-radius:4px; font-family:monospace;">{name}</code> để chèn tên khách</el-text>
      </el-form-item>

      <!-- Preview -->
      <el-card shadow="never" :body-style="{ padding:'12px 14px', background:'var(--el-fill-color-light)' }">
        <div style="display:flex; gap:12px; align-items:flex-start;">
          <span style="font-size:22px; flex-shrink:0;">{{ zoDlg.currentTpl?.icon || '📢' }}</span>
          <div>
            <div style="font-size:13px; font-weight:700; margin-bottom:4px;">{{ zoDlg.form.title || '(Chưa có tiêu đề)' }}</div>
            <div style="font-size:12px; color:var(--el-text-color-secondary); white-space:pre-line; line-height:1.5;">{{ (zoDlg.form.message || '(Chưa có nội dung)').replace(/\{name\}/g, 'Nguyễn Văn A') }}</div>
          </div>
        </div>
      </el-card>

      <el-progress v-if="zoSending" :percentage="Math.round((zoSentCount/zoSelected.length)*100)" style="margin-top:12px;" />

      <template #footer>
        <el-button @click="zoDlg.open=false" :disabled="zoSending">Hủy bỏ</el-button>
        <el-button type="primary" :loading="zoSending" :disabled="!zoDlg.form.title||!zoDlg.form.message||zoSending" @click="zoSendBulk">
          <el-icon><Promotion /></el-icon>
          {{ zoSending ? `Đang gửi (${zoSentCount}/${zoSelected.length})…` : `Gửi cho ${zoSelected.length} khách` }}
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { Calendar, Check, Clock, Close, Filter, Plus, Present, Promotion, Refresh, Search, ShoppingCart, TrendCharts, User } from "@element-plus/icons-vue";
import { customersApi } from "../../api/customers.api";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";
import http from "../../api/http";

const mainTab = ref("customers");

function onTabClick({ paneName }) {
  if (paneName === 'birthdays')  { loadBirthdayData(); }
  if (paneName === 'inactive')   { loadInactiveAll(); }
  if (paneName === 'zeroorder')  { loadZeroOrderData(); loadZeroOrderStats(); }
}

function reloadCurrentTab() {
  if (mainTab.value === 'customers') load();
  else if (mainTab.value === 'birthdays') loadBirthdayData();
  else if (mainTab.value === 'inactive') loadInactiveAll();
  else if (mainTab.value === 'zeroorder') loadZeroOrderData();
}

const loading = ref(false);
const rows = ref([]);
const q = ref("");
const typeFilter = ref("");
const vipTierFilter = ref("");
const activityFilter = ref("");
const pointsMin = ref(null);
const pointsMax = ref(null);
const appliedPointsMin = ref(null);
const appliedPointsMax = ref(null);
const page = ref(1);
const pageSize = 10;
const vipNoteTextarea = ref(null);

const summaryDlg = reactive({ open: false, loading: false, mode: 'monthly', range: 6, view: 'overview', data: [] });
const adminSummaryTotals = computed(() => {
  const earned = summaryDlg.data.reduce((s, r) => s + (r.totalPointsEarned || 0), 0);
  const deducted = summaryDlg.data.reduce((s, r) => s + (r.totalPointsDeducted || 0), 0);
  const transactions = summaryDlg.data.reduce((s, r) => s + (r.totalTransactions || 0), 0);
  return { earned, deducted, net: earned - deducted, transactions };
});
function openSummaryDialog() { summaryDlg.open = true; if (!summaryDlg.data.length) loadAdminSummary(); }
async function loadAdminSummary() {
  summaryDlg.loading = true;
  try {
    const res = summaryDlg.mode === 'weekly' ? await customersApi.getLoyaltyWeeklySummaryAdmin(summaryDlg.range) : await customersApi.getLoyaltyMonthlySummaryAdmin(summaryDlg.range);
    const raw = res?.data;
    summaryDlg.data = Array.isArray(raw) ? raw : Array.isArray(raw?.data) ? raw.data : [];
  } catch { toast('Lỗi tải tổng quan', 'error'); summaryDlg.data = []; }
  finally { summaryDlg.loading = false; }
}
function getAdminBarWidth(value, type) {
  if (!summaryDlg.data.length) return 0;
  const maxVal = Math.max(...summaryDlg.data.map(r => type === 'earn' ? r.totalPointsEarned : r.totalPointsDeducted));
  return maxVal === 0 ? 0 : Math.round((value / maxVal) * 100);
}
function formatAdminDate(d) { if (!d) return '—'; return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }); }

const dlg = reactive({ open: false, mode: "create", loading: false, alert: "", id: null, form: { fullName: "", email: "", phone: "", birthDate: "", customerType: "REGULAR", address: "", notes: "", vipNote: "", vipTier: "" } });
const detailsDialog = reactive({ open: false, customer: null });
const vipNoteDialog = reactive({ open: false, loading: false, customerId: null, customerName: "", vipTier: "", note: "" });
const vipSuggestions = ["Prefers gaming high-end", "No display units", "Morning delivery preferred", "Corporate, needs VAT", "Slow to respond", "Top spender – prioritize"];

const hasActiveFilters = computed(() => !!(typeFilter.value || vipTierFilter.value || activityFilter.value || isPointsFilterApplied.value));
const isPointsFilterApplied = computed(() => appliedPointsMin.value !== null || appliedPointsMax.value !== null);
const canApplyPointsFilter = computed(() => pointsMin.value !== null || pointsMax.value !== null);
const vipCount = computed(() => rows.value.filter(r => r.customerType === 'VIP').length);
const totalPoints = computed(() => rows.value.reduce((s, r) => s + (r.loyaltyPoints || 0), 0));
const avgPoints = computed(() => !rows.value.length ? 0 : Math.round(totalPoints.value / rows.value.length));
// Fix: filtered phải áp dụng tất cả filter client-side (keyword + type + tier + activity + points)
// Các filter type/tier/activity đã gọi API riêng qua handleFilterChange → rows đã là kết quả đúng
// filtered chỉ cần filter keyword và points range trên rows hiện tại
const filtered = computed(() => {
  let data = rows.value;
  const kw = (q.value || "").trim().toLowerCase();
  if (kw) data = data.filter(r => `${r.fullName} ${r.email} ${r.phone}`.toLowerCase().includes(kw));
  if (isPointsFilterApplied.value) {
    if (appliedPointsMin.value !== null) data = data.filter(r => (r.loyaltyPoints || 0) >= appliedPointsMin.value);
    if (appliedPointsMax.value !== null) data = data.filter(r => (r.loyaltyPoints || 0) <= appliedPointsMax.value);
  }
  return data;
});
const pageCount = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize));

const birthdayTab = ref("today");
const selectedMonth = ref(new Date().getMonth() + 1);
const todayBirthdays = ref([]);
const monthlyBirthdays = ref([]);
const upcomingBirthdays = ref([]);
const monthlyStats = ref({});
const notificationHistory = ref([]);
const historyLoading = ref(false);
const greetingDialog = reactive({ open: false, customer: null, sending: false, form: { message: "" } });
const monthOptions = [
  {value:1,label:"January"},{value:2,label:"February"},{value:3,label:"March"},{value:4,label:"April"},
  {value:5,label:"May"},{value:6,label:"June"},{value:7,label:"July"},{value:8,label:"August"},
  {value:9,label:"September"},{value:10,label:"October"},{value:11,label:"November"},{value:12,label:"December"},
];
const statsTableData = computed(() => monthOptions.map(m => ({ month: m.value, monthLabel: m.label, count: monthlyStats.value[m.value] || 0 })));
const maxBdCount = computed(() => Math.max(...Object.values(monthlyStats.value || {}), 1));
function getBdBarPct(count) { return (count / maxBdCount.value) * 100; }

const inactiveData = reactive({ 30: [], 60: [], 90: [] });
const inactiveCounts = reactive({ 30: null, 60: null, 90: null });
const activeInactiveTab = ref(30);
const inactiveQ = ref("");
const inactiveTierFilter = ref("");
const inactiveSortBy = ref("spent");
const inactivePage = ref(1);
const inactivePageSize = 9;
const inactiveDetail = reactive({ open: false, customer: null });
const inactiveTabs = [{ days: 30, label: "30 days" }, { days: 60, label: "60 days" }, { days: 90, label: "90 days" }];
const tiers = ["BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND"];
const currentInactive = computed(() => inactiveData[activeInactiveTab.value] || []);
const inactiveFiltered = computed(() => {
  let list = currentInactive.value;
  if (inactiveTierFilter.value) list = list.filter(r => r.raw?.vipTier === inactiveTierFilter.value);
  const kw = inactiveQ.value.trim().toLowerCase();
  if (kw) list = list.filter(r => `${r.fullName} ${r.email} ${r.phone}`.toLowerCase().includes(kw));
  return [...list].sort((a, b) => {
    if (inactiveSortBy.value === "spent") return (b.raw?.totalSpent || 0) - (a.raw?.totalSpent || 0);
    if (inactiveSortBy.value === "points") return (b.loyaltyPoints || 0) - (a.loyaltyPoints || 0);
    return a.fullName.localeCompare(b.fullName);
  });
});
const inactivePaged = computed(() => { const s = (inactivePage.value - 1) * inactivePageSize; return inactiveFiltered.value.slice(s, s + inactivePageSize); });
const inactiveTotalPages = computed(() => Math.ceil(inactiveFiltered.value.length / inactivePageSize));
const inactiveTotalPoints = computed(() => currentInactive.value.reduce((s, r) => s + (r.loyaltyPoints || 0), 0).toLocaleString());
const inactiveTotalSpent = computed(() => currentInactive.value.reduce((s, r) => s + parseFloat(r.raw?.totalSpent || 0), 0));
const inactiveTopTier = computed(() => { const order = ["DIAMOND","PLATINUM","GOLD","SILVER","BRONZE"]; for (const t of order) { if (currentInactive.value.some(r => r.raw?.vipTier === t)) return t; } return "—"; });

function extractList(payload) {
  if (!payload) return [];
  if (Array.isArray(payload)) return payload;
  const root = payload?.data ?? payload;
  if (Array.isArray(root)) return root;
  for (const k of ["content","items","results","rows","list"]) { if (Array.isArray(root?.[k])) return root[k]; if (Array.isArray(root?.data?.[k])) return root.data[k]; }
  return [];
}
function normalize(list) {
  return (list || []).map(c => ({ id: c?.id ?? c?.customerId, fullName: c?.fullName ?? c?.name ?? "", email: c?.email ?? "", phone: c?.phone ?? "", birthDate: c?.birthDate ?? c?.dateOfBirth ?? "", customerType: (c?.customerType ?? "REGULAR").toString().toUpperCase(), loyaltyPoints: c?.loyaltyPoints ?? 0, address: c?.address ?? "", notes: c?.notes ?? "", vipNote: c?.vipNote ?? "", raw: c }));
}
function tierIcon(tier) { return { BRONZE: 'B', SILVER: 'S', GOLD: 'G', PLATINUM: 'P', DIAMOND: 'D' }[tier] || ''; }
function getTierColor(tier) { return { BRONZE: '#cd7f32', SILVER: '#94a3b8', GOLD: '#f0b429', PLATINUM: '#818cf8', DIAMOND: '#34d399' }[tier] || '#4b5563'; }
function getPointsProgress(customer) {
  const map = { BRONZE: [0,500], SILVER: [500,1500], GOLD: [1500,3000], PLATINUM: [3000,6000], DIAMOND: [6000,10000] };
  const r = map[customer.vipTier]; if (!r) return 0;
  return Math.min(100, Math.max(0, ((customer.loyaltyPoints - r[0]) / (r[1] - r[0])) * 100));
}
function getInitials(name) {
  if (!name) return '?';
  const p = name.trim().split(' ');
  return p.length === 1 ? p[0].substring(0, 2).toUpperCase() : (p[0][0] + p[p.length - 1][0]).toUpperCase();
}
function getAvatarColor(name) {
  const colors = ['#1e40af','#065f46','#7c2d12','#581c87','#0e7490','#a21caf','#b45309','#0f766e','#1d4ed8','#be123c'];
  const hash = (name || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  return colors[hash % colors.length];
}
function formatCurrency(v) { if (!v) return '0 ₫'; return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v); }
function formatCurrencyShort(v) {
  if (!v || v === 0) return '—';
  if (v >= 1_000_000_000) return (v / 1_000_000_000).toFixed(1) + 'B';
  if (v >= 1_000_000) return (v / 1_000_000).toFixed(1) + 'M';
  if (v >= 1_000) return (v / 1_000).toFixed(0) + 'K';
  return v + '₫';
}
const zoCustomers   = ref([]);
const zoStats       = ref({});
const zeroOrderStats = zoStats;
const zoSelected    = ref([]);
const zoSendingIds  = ref(new Set());
const zoSending     = ref(false);
const zoSentCount   = ref(0);
const zoSearch      = ref("");
const zoSelectedDays = ref(3);
const zoPage        = ref(1);

const zoDayOptions = [
  { value: 1,  label: "≥ 1 ngày" },
  { value: 3,  label: "≥ 3 ngày" },
  { value: 7,  label: "≥ 7 ngày" },
  { value: 14, label: "≥ 14 ngày" },
  { value: 30, label: "≥ 30 ngày" },
];

const zoTemplates = [
  { id: "welcome", icon: "🎉", name: "Chào mừng", desc: "Mời mua lần đầu với ưu đãi", title: "🎉 Chào mừng bạn đến với cửa hàng!", message: `Xin chào {name}! 👋\n\nCảm ơn bạn đã đăng ký tài khoản.\n\n🛍️ Đặt đơn hàng đầu tiên ngay để nhận ưu đãi đặc biệt dành cho khách mới!` },
  { id: "offer", icon: "💰", name: "Ưu đãi hấp dẫn", desc: "Voucher giảm giá kèm deadline", title: "💰 Ưu đãi đặc biệt chỉ dành riêng cho bạn!", message: `Xin chào {name}!\n\nBạn chưa thực hiện đơn hàng nào.\n\n🎁 Chúng tôi tặng bạn ưu đãi GIẢM GIÁ ĐẶC BIỆT cho lần mua đầu tiên!\n\n⏰ Ưu đãi có hạn — Đặt hàng ngay!` },
  { id: "nudge", icon: "🤝", name: "Hỗ trợ tư vấn", desc: "Mời chat với tư vấn viên", title: "🤝 Chúng tôi sẵn sàng hỗ trợ bạn!", message: `Xin chào {name}!\n\nBạn đang phân vân chọn sản phẩm?\n\n💬 Đội ngũ tư vấn luôn sẵn sàng giúp bạn.\n✅ Tư vấn miễn phí · Bảo hành chính hãng · Giao hàng nhanh` },
  { id: "custom", icon: "✏️", name: "Tùy chỉnh", desc: "Soạn nội dung riêng", title: "", message: "" },
];

const zoDlg = reactive({
  open: false,
  form: { templateId: "welcome", title: "", message: "" },
  get currentTpl() { return zoTemplates.find(t => t.id === this.form.templateId); },
});

const zoFiltered = computed(() => {
  const kw = zoSearch.value.trim().toLowerCase();
  if (!kw) return zoCustomers.value;
  return zoCustomers.value.filter(c => `${c.name} ${c.email} ${c.phone ?? ""}`.toLowerCase().includes(kw));
});
const zoPageCount  = computed(() => Math.max(1, Math.ceil(zoFiltered.value.length / pageSize)));
const zoPaged      = computed(() => zoFiltered.value.slice((zoPage.value - 1) * pageSize, zoPage.value * pageSize));
const zoIsAllSelected = computed(() => zoFiltered.value.length > 0 && zoFiltered.value.every(c => zoSelected.value.includes(c.id)));

function zoDaysSince(dateStr) { if (!dateStr) return 0; return Math.floor((Date.now() - new Date(dateStr).getTime()) / 86400000); }
function formatDate(d) { if (!d) return "—"; return new Date(d).toLocaleDateString("vi-VN"); }
function zoStatusLabel(c) {
  const d = zoDaysSince(c.createdAt);
  if (d >= 30) return "Nguy cơ cao 🚨";
  if (d >= 7)  return "Cần tư vấn 📞";
  return "Mới đăng ký 🌱";
}

function zoSelectDays(d) { zoSelectedDays.value = d; zoPage.value = 1; zoSelected.value = []; loadZeroOrderData(); }
function zoSelectAll() { if (zoIsAllSelected.value) { zoSelected.value = []; } else { zoSelected.value = zoFiltered.value.map(c => c.id); } }
function zoSelectTemplate(tpl) { zoDlg.form.templateId = tpl.id; if (tpl.id !== "custom") { zoDlg.form.title = tpl.title; zoDlg.form.message = tpl.message; } }
function openZoSendDialog() { zoSelectTemplate(zoTemplates[0]); zoDlg.open = true; }

async function loadZeroOrderData() {
  loading.value = true; zoSelected.value = [];
  try { const res = await customersApi.listZeroOrder(zoSelectedDays.value); zoCustomers.value = Array.isArray(res.data) ? res.data : (res.data?.data ?? []); }
  catch { toast("Không thể tải danh sách chưa mua", "error"); zoCustomers.value = []; }
  finally { loading.value = false; }
}
async function loadZeroOrderStats() {
  try { const res = await customersApi.getZeroOrderStats(); zoStats.value = res.data ?? {}; } catch { /* silent */ }
}

async function zoSendSingle(customer) {
  const s = new Set(zoSendingIds.value); s.add(customer.id); zoSendingIds.value = s;
  try {
    await http.post("/api/auth/notifications/send", { customerIds: [customer.id], title: `🎉 Xin chào ${customer.name}!`, message: zoTemplates[0].message.replace(/\{name\}/g, customer.name), type: "WELCOME" });
    toast(`Đã gửi tới ${customer.name}`, "success");
  } catch (e) { toast(e?.response?.data?.message || "Gửi thất bại", "error"); }
  finally { const s2 = new Set(zoSendingIds.value); s2.delete(customer.id); zoSendingIds.value = s2; }
}

async function zoSendBulk() {
  if (!zoDlg.form.title || !zoDlg.form.message) return;
  zoSending.value = true; zoSentCount.value = 0;
  const ids = [...zoSelected.value]; let success = 0, fail = 0;
  for (let i = 0; i < ids.length; i += 10) {
    const chunk = ids.slice(i, i + 10);
    const chunkCustomers = zoCustomers.value.filter(c => chunk.includes(c.id));
    await Promise.all(chunkCustomers.map(async c => {
      try { await http.post("/api/auth/notifications/send", { customerIds: [c.id], title: zoDlg.form.title, message: zoDlg.form.message.replace(/\{name\}/g, c.name), type: "WELCOME" }); success++; }
      catch { fail++; }
      finally { zoSentCount.value++; }
    }));
  }
  zoSending.value = false; zoDlg.open = false; zoSelected.value = [];
  toast(fail === 0 ? `✅ Đã gửi thành công ${success} thông báo!` : `⚠️ Thành công: ${success} | Thất bại: ${fail}`, fail === 0 ? "success" : "error");
  await loadZeroOrderData();
}
function formatMoneyShort(val) {
  if (!val) return "0"; const n = parseFloat(val);
  if (n >= 1e9) return (n / 1e9).toFixed(1) + "B"; if (n >= 1e6) return (n / 1e6).toFixed(1) + "M"; if (n >= 1e3) return (n / 1e3).toFixed(0) + "K";
  return n.toLocaleString();
}
function relativeDate(iso) {
  if (!iso) return "Never";
  const diff = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  if (diff === 0) return "Today"; if (diff === 1) return "Yesterday"; if (diff < 30) return `${diff}d ago`;
  if (diff < 365) return `${Math.floor(diff / 30)}mo ago`; return `${Math.floor(diff / 365)}yr ago`;
}
function formatBirthdayDate(customer) {
  if (customer.isBirthdayToday) return "Today";
  return `${customer.birthDay} ${monthOptions[customer.birthMonth - 1]?.label} (${customer.daysUntilBirthday}d)`;
}
function formatDateTime(dateString) { if (!dateString) return '—'; return new Date(dateString).toLocaleString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }); }

async function load() {
  loading.value = true;
  try {
    let res;
    if (isPointsFilterApplied.value && vipTierFilter.value) res = await customersApi.listByVipTierAndPoints(vipTierFilter.value, appliedPointsMin.value || 0, appliedPointsMax.value || 999999);
    else if (isPointsFilterApplied.value) res = await customersApi.listByPointsRange(appliedPointsMin.value || 0, appliedPointsMax.value || 999999);
    else if (vipTierFilter.value) res = await customersApi.listByVipTier(vipTierFilter.value);
    else if (activityFilter.value === 'ACTIVE_30') res = await customersApi.listActiveLast30Days();
    else if (typeFilter.value) res = await customersApi.listByType(typeFilter.value);
    else res = await customersApi.listAll();
    rows.value = normalize(extractList(res?.data)); page.value = 1;
  } catch (e) { rows.value = []; toast("Lỗi tải danh sách khách hàng", "error"); }
  finally { loading.value = false; }
}
function handleFilterChange() {
  if (activityFilter.value || typeFilter.value || vipTierFilter.value) { appliedPointsMin.value = null; appliedPointsMax.value = null; pointsMin.value = null; pointsMax.value = null; }
  load();
}
function applyPointsFilter() {
  if (!canApplyPointsFilter.value) { toast("Enter at least one points value", "warning"); return; }
  if (pointsMin.value !== null && pointsMax.value !== null && pointsMin.value > pointsMax.value) { toast("Min cannot exceed max", "error"); return; }
  appliedPointsMin.value = pointsMin.value; appliedPointsMax.value = pointsMax.value; activityFilter.value = ""; typeFilter.value = ""; load();
}
function clearPointsFilter() { pointsMin.value = null; pointsMax.value = null; appliedPointsMin.value = null; appliedPointsMax.value = null; load(); }
function clearAllFilters() { typeFilter.value = ""; vipTierFilter.value = ""; activityFilter.value = ""; pointsMin.value = null; pointsMax.value = null; appliedPointsMin.value = null; appliedPointsMax.value = null; load(); }
function openCreate() { dlg.open = true; dlg.mode = "create"; dlg.alert = ""; dlg.id = null; dlg.form = { fullName: "", email: "", phone: "", birthDate: "", customerType: "REGULAR", address: "", notes: "", vipNote: "", vipTier: "" }; }
function openEdit(row) { dlg.open = true; dlg.mode = "edit"; dlg.alert = ""; dlg.id = row?.id; dlg.form = { fullName: row?.fullName||"", email: row?.email||"", phone: row?.phone||"", birthDate: row?.birthDate||"", customerType: row?.customerType||"REGULAR", address: row?.address||"", notes: row?.notes||"", vipNote: row?.raw?.vipNote||"", vipTier: row?.raw?.vipTier||"" }; detailsDialog.open = false; }
async function save() {
  dlg.alert = "";
  if (!dlg.form.fullName || !dlg.form.email || !dlg.form.phone) { dlg.alert = "Name, email and phone are required."; return; }
  dlg.loading = true;
  try {
    const payload = { ...dlg.form }; delete payload.vipTier;
    if (dlg.mode === "create") { await customersApi.create(payload); toast("Đã tạo khách hàng!", "success"); }
    else { await customersApi.update(dlg.id, payload); toast("Đã cập nhật!", "success"); }
    dlg.open = false; await load();
  } catch (e) { const msg = e?.response?.data?.message || e?.message || "Save failed"; dlg.alert = typeof msg === "string" ? msg : JSON.stringify(msg); }
  finally { dlg.loading = false; }
}
async function remove(row) {
  const ok = await confirmModal(`Xác nhận xóa "${row?.fullName}"?`, "Xác nhận xóa", "Xóa", true);
  if (!ok) return;
  try { await customersApi.remove(row.id); toast("Đã xóa!", "success"); await load(); }
  catch { toast("Xóa thất bại.", "error"); }
}
function viewDetails(row) { detailsDialog.customer = row; detailsDialog.open = true; }
function openVipNoteEdit(row) {
  if (!row.raw?.vipTier) return;
  vipNoteDialog.customerId = row.id; vipNoteDialog.customerName = row.fullName; vipNoteDialog.vipTier = row.raw.vipTier; vipNoteDialog.note = row.raw?.vipNote || ""; vipNoteDialog.open = true; nextTick(() => vipNoteTextarea.value?.focus());
}
async function saveVipNote() {
  vipNoteDialog.loading = true;
  try {
    await customersApi.updateVipNote(vipNoteDialog.customerId, vipNoteDialog.note); toast("Đã lưu ghi chú VIP.", "success"); vipNoteDialog.open = false; await load();
    if (detailsDialog.open && detailsDialog.customer?.id === vipNoteDialog.customerId) { detailsDialog.customer.raw.vipNote = vipNoteDialog.note; }
  } catch { toast("Không thể lưu ghi chú VIP", "error"); }
  finally { vipNoteDialog.loading = false; }
}
async function loadBirthdayData() {
  loading.value = true;
  try {
    await Promise.all([
      http.get("/api/auth/admin/birthdays/today").then(r => { todayBirthdays.value = r.data || []; }),
      http.get("/api/auth/admin/birthdays/statistics").then(r => { monthlyStats.value = r.data?.monthlyCount || {}; }),
      http.get("/api/auth/admin/birthdays/upcoming?days=7").then(r => { upcomingBirthdays.value = r.data || []; }),
    ]);
  } catch { toast("Lỗi tải dữ liệu sinh nhật", "error"); }
  finally { loading.value = false; }
}
async function loadMonthlyBirthdays() {
  loading.value = true;
  try { const res = await http.get(`/api/auth/admin/birthdays/month/${selectedMonth.value}`); monthlyBirthdays.value = res.data || []; }
  catch { toast("Lỗi tải dữ liệu theo tháng", "error"); }
  finally { loading.value = false; }
}
async function loadNotificationHistory() {
  historyLoading.value = true;
  try { const res = await http.get("/api/auth/admin/birthdays/notification-history"); notificationHistory.value = res.data || []; }
  catch { toast("Lỗi tải lịch sử", "error"); }
  finally { historyLoading.value = false; }
}
function viewMonthDetail(month) { selectedMonth.value = parseInt(month); birthdayTab.value = "monthly"; loadMonthlyBirthdays(); }
function sendGreeting(customer) {
  greetingDialog.open = true; greetingDialog.customer = customer;
  greetingDialog.form.message = `Happy Birthday, ${customer.name}!\n\nWishing you a wonderful year ahead. Thank you for your continued trust and loyalty.\n\nAs a special gift, enjoy an exclusive birthday voucher with great offers!`;
}
async function confirmSendGreeting() {
  if (!greetingDialog.form.message.trim()) { toast("Please enter a message", "warning"); return; }
  greetingDialog.sending = true;
  try {
    const response = await http.post(`/api/auth/admin/birthdays/send-greeting/${greetingDialog.customer.id}`, { message: greetingDialog.form.message });
    if (response.data.status === 'success') { toast(`Đã gửi tới ${greetingDialog.customer.name}`, "success"); greetingDialog.open = false; if (birthdayTab.value === 'history') loadNotificationHistory(); }
    else { toast(response.data.message || "Không thể gửi", "error"); }
  } catch (e) { toast(e.response?.data?.message || "Không thể gửi lời chúc", "error"); }
  finally { greetingDialog.sending = false; }
}
function normalizeInactive(list) {
  return (list || []).map(c => ({ id: c.id, fullName: c.name ?? c.fullName ?? "", email: c.email ?? "", phone: c.phone ?? "", birthDate: c.dateOfBirth ?? c.birthDate ?? "", customerType: c.customerType ?? "REGULAR", loyaltyPoints: c.loyaltyPoints ?? 0, notes: c.notes ?? "", raw: c }));
}
async function fetchInactiveFor(days) {
  try {
    const res = await customersApi.listInactiveTransaction(days);
    const arr = Array.isArray(res?.data) ? res.data : (res?.data?.content ?? []);
    inactiveData[days] = normalizeInactive(arr); inactiveCounts[days] = inactiveData[days].length;
  } catch { inactiveData[days] = []; inactiveCounts[days] = 0; }
}
async function loadInactiveAll() { loading.value = true; await Promise.all([fetchInactiveFor(30), fetchInactiveFor(60), fetchInactiveFor(90)]); loading.value = false; }
function switchInactiveTab(days) { activeInactiveTab.value = days; inactivePage.value = 1; inactiveQ.value = ""; inactiveTierFilter.value = ""; }
function openInactiveDetail(row) { inactiveDetail.customer = row; inactiveDetail.open = true; }

// ── Extra computed for EP template ────────────────────────────────
const custStats = computed(() => [
  { label: 'Tổng khách hàng', value: rows.value.length, color: '#2563eb' },
  { label: 'Thành viên VIP', value: vipCount.value, color: '#d97706' },
  { label: 'Tổng điểm', value: totalPoints.value.toLocaleString(), color: '#16a34a' },
  { label: 'Điểm TB', value: avgPoints.value.toLocaleString(), color: '#6b7280' },
]);
const inactiveStats = computed(() => [
  { label: 'Khách hàng', value: currentInactive.value.length, color: '#2563eb' },
  { label: 'Tổng điểm', value: inactiveTotalPoints.value, color: '#d97706' },
  { label: 'Tổng chi tiêu', value: formatMoneyShort(inactiveTotalSpent.value), color: '#16a34a' },
  { label: 'Cấp cao nhất', value: inactiveTopTier.value, color: '#7c3aed' },
]);
const zeroStats = computed(() => [
  { label: 'Đăng ký ≥ 3 ngày', value: zeroOrderStats.registeredOver3Days ?? '—', color: '#6b7280' },
  { label: 'Đăng ký ≥ 7 ngày', value: zeroOrderStats.registeredOver7Days ?? '—', color: '#d97706' },
  { label: 'Đăng ký ≥ 30 ngày', value: zeroOrderStats.registeredOver30Days ?? '—', color: '#dc2626' },
  { label: 'Đang chọn', value: zoSelected.value.length, color: '#2563eb' },
]);
const filterFields = computed(() => [
  { label: 'Tìm kiếm', type: 'text' },
  { label: 'Loại', type: 'select', model: typeFilter,
    options: [{value:'',label:'Tất cả'},{value:'REGULAR',label:'Thường'},{value:'VIP',label:'VIP'}] },
  { label: 'Cấp VIP', type: 'select', model: vipTierFilter,
    options: [{value:'',label:'Tất cả cấp'},{value:'BRONZE',label:'Đồng'},{value:'SILVER',label:'Bạc'},{value:'GOLD',label:'Vàng'},{value:'PLATINUM',label:'Bạch kim'},{value:'DIAMOND',label:'Kim cương'}] },
  { label: 'Hoạt động', type: 'select', model: activityFilter,
    options: [{value:'',label:'Tất cả'},{value:'ACTIVE_30',label:'Hoạt động (30 ngày)'}] },
]);

onMounted(load);
</script>

<style></style>