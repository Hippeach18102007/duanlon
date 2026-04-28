<template>
  <div style="min-height: 100vh; padding: 24px 0 40px;">
    <div style="max-width: 1280px; margin: 0 auto; padding: 0 24px;">
      <el-skeleton v-if="loading" :rows="8" animated />

      <template v-else-if="customer">

        <!-- ── Profile header card ── -->
        <el-card shadow="never" style="margin-bottom: 16px;">
          <el-row align="middle" justify="space-between" style="flex-wrap: wrap; gap: 12px;">
            <el-space :size="16" align="center">
              <el-avatar :size="72" style="background: linear-gradient(135deg, #3b82f6, #2563eb); font-size: 28px; font-weight: 800; flex-shrink: 0;">
                {{ customer.name?.charAt(0) || 'U' }}
              </el-avatar>
              <el-space direction="vertical" :size="6">
                <el-text tag="div" style="font-size: 22px; font-weight: 800; color: #0f172a;">{{ customer.name }}</el-text>
                <el-space :size="8" style="flex-wrap: wrap;">
                  <el-tag :type="getTierTagType(customer.vipTier)" effect="dark">{{ customer.vipTierDisplay }}</el-tag>
                  <el-tag type="info">{{ customer.customerTypeDisplay }}</el-tag>
                </el-space>
              </el-space>
            </el-space>

            <el-space v-if="!editing" :size="8">
              <el-button type="primary" plain @click="startEdit">Chỉnh sửa</el-button>
            </el-space>
            <el-space v-else :size="8">
              <el-button type="success" :loading="saving" @click="handleSave">{{ saving ? 'Đang lưu...' : 'Lưu' }}</el-button>
              <el-button :disabled="saving" @click="handleCancel">Hủy</el-button>
            </el-space>
          </el-row>
        </el-card>

        <!-- ── Tabs ── -->
        <el-tabs v-model="activeTab" style="margin-bottom: 12px;">

          <!-- TAB: DASHBOARD -->
          <el-tab-pane name="dashboard">
            <template #label><el-space :size="4"><el-icon><DataAnalysis /></el-icon>Dashboard</el-space></template>

            <el-skeleton v-if="dashboardLoading" :rows="10" animated />

            <template v-else-if="myDashboard">
              <!-- KPI row -->
              <el-row :gutter="16" style="margin-bottom: 20px;">
                <el-col :xs="12" :sm="6" v-for="kpi in kpiCards" :key="kpi.label">
                  <el-card shadow="never" :body-style="{ padding: '18px 20px' }" :style="`background: ${kpi.bg}; border-color: ${kpi.border}; margin-bottom: 10px;`">
                    <el-space :size="14" align="center">
                      <el-icon :size="28" :style="`color: ${kpi.color};`"><component :is="kpi.icon" /></el-icon>
                      <el-space direction="vertical" :size="2">
                        <el-text tag="div" :style="`font-size: 22px; font-weight: 900; color: ${kpi.color}; line-height: 1.1;`">{{ kpi.value }}</el-text>
                        <el-text size="small" type="info">{{ kpi.label }}</el-text>
                      </el-space>
                    </el-space>
                  </el-card>
                </el-col>
              </el-row>

              <!-- VIP Journey + Spin -->
              <el-row :gutter="16" style="margin-bottom: 20px;">
                <el-col :xs="24" :md="14">
                  <el-card shadow="never">
                    <template #header>
                      <el-space :size="8">
                        <el-icon :size="20" style="color: #f59e0b;"><Trophy /></el-icon>
                        <el-text tag="b">Hành trình VIP</el-text>
                      </el-space>
                    </template>

                    <!-- Current tier -->
                    <el-row align="middle" :gutter="16" style="margin-bottom: 20px; padding: 14px; background: var(--el-fill-color-lighter); border-radius: 8px;">
                      <el-col flex="none">
                        <el-tag :type="getTierTagType(myDashboard.vipJourney?.currentTier)" effect="dark" size="large">
                          {{ myDashboard.vipJourney?.currentTierDisplay || 'Member' }}
                        </el-tag>
                      </el-col>
                      <el-col flex="1">
                        <el-text style="font-size: 28px; font-weight: 900; line-height: 1;">
                          {{ (myDashboard.vipJourney?.currentPoints || 0).toLocaleString() }}
                        </el-text>
                        <el-text size="small" type="info"> điểm</el-text>
                      </el-col>
                    </el-row>

                    <!-- Progress -->
                    <div v-if="myDashboard.vipJourney?.nextTier" style="margin-bottom: 16px;">
                      <el-row justify="space-between" style="margin-bottom: 6px;">
                        <el-text size="small" type="info">
                          Cần thêm <strong>{{ (myDashboard.vipJourney?.pointsToNext || 0).toLocaleString() }} điểm</strong> để lên <strong>{{ myDashboard.vipJourney?.nextTierDisplay }}</strong>
                        </el-text>
                        <el-text size="small" type="primary" style="font-weight: 700;">{{ myDashboard.vipJourney?.progressPercent || 0 }}%</el-text>
                      </el-row>
                      <el-progress :percentage="myDashboard.vipJourney?.progressPercent || 0" :show-text="false" :stroke-width="8" status="success" />
                    </div>
                    <el-alert v-else type="warning" :closable="false" style="margin-bottom: 16px;">
                      <el-icon><Star /></el-icon> Bạn đang ở hạng cao nhất!
                    </el-alert>

                    <!-- Milestones -->
                    <el-space direction="vertical" fill :size="8" style="width: 100%;">
                      <el-card
                        v-for="m in myDashboard.vipJourney?.milestones"
                        :key="m.tier"
                        shadow="never"
                        :body-style="{ padding: '10px 14px' }"
                        :style="m.achieved ? 'background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5);' : 'background: var(--el-fill-color-lighter);'"
                      >
                        <el-row align="middle" justify="space-between">
                          <el-space :size="10" align="center">
                            <el-icon v-if="m.achieved" style="color: var(--el-color-success);"><CircleCheck /></el-icon>
                            <el-icon v-else style="color: var(--el-text-color-placeholder);"><Clock /></el-icon>
                            <el-space direction="vertical" :size="2">
                              <el-text tag="b" size="small">{{ m.tierDisplay }}</el-text>
                              <el-text size="small" type="info">{{ m.requiredPoints.toLocaleString() }} điểm</el-text>
                            </el-space>
                          </el-space>
                          <el-text v-if="m.achieved && m.achievedAt" size="small" type="success">{{ m.achievedAt }}</el-text>
                        </el-row>
                      </el-card>
                    </el-space>
                  </el-card>
                </el-col>

                <el-col :xs="24" :md="10">
                  <el-card shadow="never">
                    <template #header>
                      <el-space :size="8">
                        <el-icon :size="20" style="color: #67c23a;"><Trophy /></el-icon>
                        <el-text tag="b">Vòng quay may mắn</el-text>
                      </el-space>
                    </template>

                    <el-alert
                      v-if="myDashboard.spinWheel?.hasBonus"
                      type="success"
                      :closable="false"
                      style="margin-bottom: 14px;"
                    >
                      🎁 Bonus <strong>-{{ myDashboard.spinWheel?.currentBonus }}%</strong> đang chờ áp dụng
                    </el-alert>

                    <el-row :gutter="12" style="margin-bottom: 14px;">
                      <el-col :span="12">
                        <el-card shadow="never" :body-style="{ padding: '10px', textAlign: 'center' }" style="background: var(--el-fill-color-lighter);">
                          <el-text tag="div" :type="myDashboard.spinWheel?.canSpin ? 'success' : 'info'" style="font-weight: 700;">
                            {{ myDashboard.spinWheel?.canSpin ? '✓ Sẵn sàng' : '✗ Chưa thể' }}
                          </el-text>
                          <el-text size="small" type="info">Trạng thái</el-text>
                        </el-card>
                      </el-col>
                      <el-col :span="12">
                        <el-card shadow="never" :body-style="{ padding: '10px', textAlign: 'center' }" style="background: var(--el-fill-color-lighter);">
                          <el-text tag="div" style="font-weight: 700; font-size: 18px;">{{ myDashboard.spinWheel?.spinsRemaining || 0 }}</el-text>
                          <el-text size="small" type="info">Lượt còn lại</el-text>
                        </el-card>
                      </el-col>
                    </el-row>

                    <el-text v-if="myDashboard.spinWheel?.nextSpinAt" size="small" type="info" style="display: flex; align-items: center; gap: 6px; margin-bottom: 14px;">
                      <el-icon><Clock /></el-icon> Quay lại lúc: {{ myDashboard.spinWheel?.nextSpinAt }}
                    </el-text>

                    <el-divider style="margin: 12px 0;" />

                    <el-space direction="vertical" fill :size="8" style="width: 100%;">
                      <el-row justify="space-between">
                        <el-text size="small" type="info">Trung bình mỗi đơn</el-text>
                        <el-text size="small" style="font-weight: 700;">{{ formatCurrency(myDashboard.summary?.avgOrderValue) }}</el-text>
                      </el-row>
                      <el-row v-if="myDashboard.summary?.lastOrderAt" justify="space-between">
                        <el-text size="small" type="info">Đơn hàng gần nhất</el-text>
                        <el-text size="small">{{ formatDateTime(myDashboard.summary?.lastOrderAt) }}</el-text>
                      </el-row>
                    </el-space>

                    <el-alert
                      v-if="!myDashboard.spinWheel?.hasBonus && !myDashboard.spinWheel?.canSpin"
                      type="info" :closable="false" show-icon style="margin-top: 12px;"
                      title="Hiện chưa có bonus spin"
                    />
                  </el-card>
                </el-col>
              </el-row>

              <!-- Recent Payments + Loyalty -->
              <el-row :gutter="16" style="margin-bottom: 20px;">
                <!-- Recent Payments -->
                <el-col :xs="24" :md="12">
                  <el-card shadow="never">
                    <template #header>
                      <el-row align="middle" justify="space-between">
                        <el-space :size="8"><el-icon style="color: #409eff;"><CreditCard /></el-icon><el-text tag="b">Giao dịch gần đây</el-text></el-space>
                        <el-button size="small" text type="primary" @click="activeTab = 'payments'">Xem tất cả</el-button>
                      </el-row>
                    </template>
                    <el-empty v-if="!myDashboard.recentPayments?.length" description="Chưa có giao dịch" :image-size="60" />
                    <el-space v-else direction="vertical" fill :size="10" style="width: 100%;">
                      <el-row v-for="p in myDashboard.recentPayments" :key="p.paymentId" align="middle" justify="space-between">
                        <el-space :size="10">
                          <el-avatar :size="32" style="background: var(--el-color-primary-light-8); flex-shrink: 0;">
                            <el-icon style="color: var(--el-color-primary);"><CreditCard /></el-icon>
                          </el-avatar>
                          <el-space direction="vertical" :size="2">
                            <el-text size="small" style="font-weight: 600;">Đơn #{{ p.orderId }}</el-text>
                            <el-text size="small" type="info">{{ p.method }} · {{ p.paidAt }}</el-text>
                          </el-space>
                        </el-space>
                        <el-space direction="vertical" :size="4" style="align-items: flex-end;">
                          <el-text type="primary" style="font-weight: 700;">{{ formatCurrency(p.amount) }}</el-text>
                          <el-tag :type="getPaymentStatusType(p.status)" size="small">{{ p.status }}</el-tag>
                        </el-space>
                      </el-row>
                    </el-space>
                  </el-card>
                </el-col>

                <!-- Recent Loyalty -->
                <el-col :xs="24" :md="12">
                  <el-card shadow="never">
                    <template #header>
                      <el-row align="middle" justify="space-between">
                        <el-space :size="8"><el-icon style="color: #f59e0b;"><Star /></el-icon><el-text tag="b">Điểm gần đây</el-text></el-space>
                        <el-button size="small" text type="primary" @click="activeTab = 'loyalty'">Xem tất cả</el-button>
                      </el-row>
                    </template>
                    <el-empty v-if="!myDashboard.recentLoyaltyHistory?.length" description="Chưa có lịch sử điểm" :image-size="60" />
                    <el-space v-else direction="vertical" fill :size="10" style="width: 100%;">
                      <el-row v-for="h in myDashboard.recentLoyaltyHistory" :key="h.id" align="middle" justify="space-between">
                        <el-space :size="10">
                          <el-avatar :size="32" :style="`background: ${h.pointsDelta > 0 ? 'var(--el-color-success-light-8)' : 'var(--el-color-warning-light-8)'}; flex-shrink: 0;`">
                            <el-icon :style="`color: ${h.pointsDelta > 0 ? 'var(--el-color-success)' : 'var(--el-color-warning)'};`">
                              <component :is="h.pointsDelta > 0 ? 'CirclePlus' : 'Remove'" />
                            </el-icon>
                          </el-avatar>
                          <el-space direction="vertical" :size="2">
                            <el-text size="small" style="font-weight: 600;">{{ h.transactionTypeDisplay || h.transactionType }}</el-text>
                            <el-text size="small" type="info">{{ formatDateTime(h.createdAt) }}</el-text>
                          </el-space>
                        </el-space>
                        <el-tag :type="h.pointsDelta > 0 ? 'success' : 'warning'" effect="dark" size="small">
                          {{ h.pointsDelta > 0 ? '+' : '' }}{{ h.pointsDelta }}
                        </el-tag>
                      </el-row>
                    </el-space>
                  </el-card>
                </el-col>
              </el-row>

              <!-- Recent Promotions -->
              <el-row v-if="myDashboard.recentPromotions?.length" :gutter="16">
                <el-col :xs="24">
                  <el-card shadow="never">
                    <template #header>
                      <el-row align="middle" justify="space-between">
                        <el-space :size="8"><el-icon style="color: #9333ea;"><Ticket /></el-icon><el-text tag="b">Ưu đãi gần đây</el-text></el-space>
                        <el-button size="small" text type="primary" @click="activeTab = 'promotions'">Xem tất cả</el-button>
                      </el-row>
                    </template>
                    <el-row :gutter="12">
                      <el-col v-for="p in myDashboard.recentPromotions" :key="`dash-promo-${p.orderId}-${p.promotionCode}`" :xs="24" :sm="12" :md="8">
                        <el-card shadow="never" :body-style="{ padding: '12px' }" style="background: var(--el-fill-color-lighter); margin-bottom: 10px;">
                          <el-row align="middle" justify="space-between">
                            <el-space :size="10">
                              <el-icon :style="`color: ${p.type === 'SPIN_WHEEL' ? '#67c23a' : '#9333ea'};`">
                                <component :is="p.type === 'SPIN_WHEEL' ? 'Trophy' : 'Ticket'" />
                              </el-icon>
                              <el-space direction="vertical" :size="4">
                                <el-text size="small" style="font-weight: 600;">{{ p.promotionName }}</el-text>
                                <el-space :size="4">
                                  <el-tag size="small" :type="p.type === 'SPIN_WHEEL' ? 'success' : 'primary'">{{ p.type === 'SPIN_WHEEL' ? 'Spin' : p.promotionCode }}</el-tag>
                                  <el-tag size="small" :type="getPromoStatusTagType(p.status)">{{ p.status }}</el-tag>
                                </el-space>
                              </el-space>
                            </el-space>
                            <el-text :type="p.type === 'SPIN_WHEEL' ? 'success' : 'primary'" style="font-size: 18px; font-weight: 900;">
                              {{ p.discountType === 'PERCENT' || p.discountType === 'percent' ? `${p.discountValue}%` : formatCurrencyShort(p.discountValue) }}
                            </el-text>
                          </el-row>
                        </el-card>
                      </el-col>
                    </el-row>
                  </el-card>
                </el-col>
              </el-row>
            </template>

            <el-empty v-else description="Không tải được dữ liệu dashboard">
              <el-button type="primary" @click="loadMyDashboard">Thử lại</el-button>
            </el-empty>
          </el-tab-pane>

          <!-- TAB: THÔNG TIN -->
          <el-tab-pane label="Thông tin" name="info">
            <el-row :gutter="16">
              <el-col :xs="24" :lg="8">
                <!-- Loyalty points -->
                <el-card shadow="never" style="margin-bottom: 14px;">
                  <template #header>
                    <el-space :size="8"><el-icon style="color: #f59e0b;"><Star /></el-icon><el-text tag="b">Điểm tích lũy</el-text></el-space>
                  </template>
                  <el-statistic :value="customer.loyaltyPoints || 0" suffix=" điểm" style="text-align: center; padding: 12px 0;" />
                  <el-divider style="margin: 10px 0;" />
                  <el-row justify="space-between" align="middle" style="margin-bottom: 8px;">
                    <el-text size="small" type="info">Lên hạng tiếp:</el-text>
                    <el-text size="small" type="primary" style="font-weight: 700;">
                      {{ customer.pointsToNextTier > 0 ? `${customer.pointsToNextTier} điểm` : 'Hạng cao nhất' }}
                    </el-text>
                  </el-row>
                  <el-progress v-if="customer.pointsToNextTier > 0" :percentage="getProgressPercent()" :stroke-width="8" :show-text="false" />
                </el-card>

                <!-- Discount -->
                <el-card shadow="never" style="margin-bottom: 14px; background: linear-gradient(135deg, #16a34a, #15803d); border: none;">
                  <template #header>
                    <el-space :size="8"><el-icon style="color: #fff;"><Present /></el-icon><el-text style="color: #fff; font-weight: 700;">Ưu đãi của bạn</el-text></el-space>
                  </template>
                  <el-text tag="div" style="font-size: 48px; font-weight: 900; color: #fff; text-align: center; line-height: 1; padding: 8px 0;">{{ customer.discountRate || 0 }}</el-text>
                  <el-text style="color: rgba(255,255,255,0.88); display: block; text-align: center; margin-top: 4px;">Giảm giá mọi đơn hàng</el-text>
                </el-card>

                <!-- Total spent -->
                <el-card shadow="never">
                  <template #header>
                    <el-space :size="8"><el-icon style="color: #9333ea;"><Wallet /></el-icon><el-text tag="b">Tổng chi tiêu</el-text></el-space>
                  </template>
                  <el-text style="font-size: 28px; font-weight: 800; color: #9333ea; display: block; padding: 8px 0;">{{ formatCurrency(customer.totalSpent) }}</el-text>
                </el-card>
              </el-col>

              <el-col :xs="24" :lg="16">
                <!-- Personal info form -->
                <el-card shadow="never" style="margin-bottom: 14px;">
                  <template #header><el-text tag="b" style="font-size: 18px;">Thông tin cá nhân</el-text></template>
                  <el-form label-position="top" :model="formData">
                    <el-row :gutter="16">
                      <el-col :xs="24" :md="12">
                        <el-form-item label="Họ và tên">
                          <el-input v-if="editing" v-model="formData.fullName" />
                          <div v-else class="form-static">{{ customer.name }}</div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="Email">
                          <el-input v-if="editing" v-model="formData.email" />
                          <div v-else class="form-static">{{ customer.email }}</div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="Số điện thoại">
                          <el-input v-if="editing" v-model="formData.phone" />
                          <div v-else class="form-static">{{ customer.phone }}</div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="Ngày sinh">
                          <el-date-picker v-if="editing" v-model="formData.birthDate" type="date" format="DD/MM/YYYY" value-format="YYYY-MM-DD" style="width: 100%;" />
                          <div v-else class="form-static">{{ formatDate(customer.dateOfBirth) }}</div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24">
                        <el-form-item label="Địa chỉ">
                          <el-input v-if="editing" v-model="formData.address" placeholder="Nhập địa chỉ" />
                          <div v-else class="form-static">{{ customer.address || '—' }}</div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24">
                        <el-form-item label="Ghi chú">
                          <el-input v-if="editing" v-model="formData.notes" type="textarea" :rows="3" />
                          <div v-else class="form-static" style="white-space: pre-wrap; min-height: 64px; align-items: flex-start; padding-top: 10px;">{{ customer.notes || '—' }}</div>
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                </el-card>

                <!-- Account info -->
                <el-card shadow="never">
                  <template #header><el-text tag="b" style="font-size: 18px;">Thông tin tài khoản</el-text></template>
                  <el-row :gutter="16">
                    <el-col :xs="24" :md="12">
                      <el-form-item label="Hạng thành viên">
                        <el-tag :type="getTierTagType(customer.vipTier)" size="large" effect="dark">{{ customer.vipTierDisplay }}</el-tag>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :md="12">
                      <el-form-item label="Ngày tạo">
                        <div class="form-static">{{ formatDateTime(customer.createdAt) }}</div>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-card>
              </el-col>
            </el-row>
          </el-tab-pane>

          <!-- TAB: LỊCH SỬ ĐIỂM -->
          <el-tab-pane label="Lịch sử điểm" name="loyalty">
            <el-row :gutter="16" style="margin-bottom: 16px;">
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); margin-bottom: 10px;">
                  <el-statistic title="Tổng điểm cộng" :value="loyaltyStats.totalEarned">
                    <template #number><el-text type="success" style="font-size: 26px; font-weight: 900;">+{{ loyaltyStats.totalEarned }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-warning-light-9); border-color: var(--el-color-warning-light-5); margin-bottom: 10px;">
                  <el-statistic title="Tổng điểm trừ" :value="loyaltyStats.totalDeducted">
                    <template #number><el-text type="warning" style="font-size: 26px; font-weight: 900;">{{ loyaltyStats.totalDeducted }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="margin-bottom: 10px;">
                  <el-statistic title="Giao dịch" :value="loyaltyStats.totalTransactions">
                    <template #number><el-text style="font-size: 26px; font-weight: 900;">{{ loyaltyStats.totalTransactions }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
            </el-row>

            <el-card shadow="never">
              <el-row align="top" justify="space-between" style="margin-bottom: 16px; flex-wrap: wrap; gap: 12px;">
                <el-space direction="vertical" :size="6">
                  <el-space :size="8"><el-icon><TrendCharts /></el-icon><el-text tag="b">Lịch sử giao dịch điểm</el-text></el-space>
                  <el-space v-if="activeHistoryFilters.length" wrap :size="6">
                    <el-tag v-for="filter in activeHistoryFilters" :key="filter.type" :type="filter.tagType" closable @close="removeHistoryFilter(filter.type)">{{ filter.label }}</el-tag>
                  </el-space>
                </el-space>
                <el-space wrap :size="8">
                  <el-select v-model="historyTransactionFilter" placeholder="Loại giao dịch" clearable style="width: 160px;" @change="filterLoyaltyHistory">
                    <el-option label="Cộng điểm" value="EARN"><el-tag type="success" size="small">Cộng điểm</el-tag></el-option>
                    <el-option label="Trừ điểm" value="DEDUCT"><el-tag type="warning" size="small">Trừ điểm</el-tag></el-option>
                    <el-option label="Phạt" value="PENALTY"><el-tag type="danger" size="small">Phạt</el-tag></el-option>
                    <el-option label="Thăng hạng" value="TIER_UPGRADE"><el-tag type="success" size="small">Thăng hạng</el-tag></el-option>
                    <el-option label="Hạ hạng" value="TIER_DOWNGRADE"><el-tag type="info" size="small">Hạ hạng</el-tag></el-option>
                  </el-select>
                  <el-date-picker v-model="historyDateRange" type="daterange" range-separator="-" start-placeholder="Từ ngày" end-placeholder="Đến ngày" format="DD/MM/YYYY" value-format="YYYY-MM-DD" style="width: 280px;" @change="filterLoyaltyHistory" />
                  <el-button v-if="hasActiveHistoryFilters" @click="clearHistoryFilters"><el-icon><Close /></el-icon> Xóa bộ lọc</el-button>
                  <el-button @click="loadLoyaltyHistory" :loading="historyLoading"><el-icon><Refresh /></el-icon> Tải lại</el-button>
                </el-space>
              </el-row>

              <el-divider style="margin: 0 0 16px;" />
              <el-skeleton v-if="historyLoading" :rows="5" animated />
              <el-empty v-else-if="!filteredLoyaltyHistory.length" description="Không có giao dịch nào">
                <el-button type="primary" v-if="hasActiveHistoryFilters" @click="clearHistoryFilters">Xóa bộ lọc</el-button>
              </el-empty>

              <el-timeline v-else>
                <el-timeline-item
                  v-for="item in filteredLoyaltyHistory"
                  :key="item.id"
                  :timestamp="formatDateTime(item.createdAt)"
                  placement="top"
                  :type="getTimelineType(item.transactionType)"
                >
                  <el-card
                    shadow="never"
                    :body-style="{ padding: '14px 16px' }"
                    :style="`border-left: 4px solid ${getTransactionIconColor(item.transactionType)};`"
                  >
                    <el-row align="top" :gutter="12">
                      <el-col flex="none">
                        <el-avatar :size="40" :style="`background: transparent; border: 1px solid ${getTransactionIconColor(item.transactionType)};`">
                          <el-icon :size="20" :style="`color: ${getTransactionIconColor(item.transactionType)};`">
                            <component :is="getTransactionIcon(item.transactionType)" />
                          </el-icon>
                        </el-avatar>
                      </el-col>
                      <el-col flex="1">
                        <el-space :size="8" style="margin-bottom: 6px; flex-wrap: wrap;">
                          <el-tag :type="getTransactionTagType(item.transactionType)" effect="dark">{{ item.transactionTypeDisplay }}</el-tag>
                          <el-text size="small" type="info" v-if="item.reason">{{ item.reason }}</el-text>
                        </el-space>
                        <el-text tag="div" style="margin-bottom: 6px;">{{ item.note }}</el-text>
                        <el-space v-if="item.tierBefore || item.tierAfter" :size="6" align="center">
                          <el-tag size="small" :type="getTierTagType(item.tierBefore)" effect="plain">{{ item.tierBeforeDisplay || 'Member' }}</el-tag>
                          <el-icon><Right /></el-icon>
                          <el-tag size="small" :type="getTierTagType(item.tierAfter)" effect="dark">{{ item.tierAfterDisplay || 'Member' }}</el-tag>
                        </el-space>
                        <div v-if="item.referenceType && item.referenceId" style="margin-top: 8px;">
                          <el-button size="small" type="primary" plain round @click="viewOrderBill(item.referenceId, item.pointsDelta)">
                            <el-icon><ShoppingCart /></el-icon> Đơn hàng #{{ item.referenceId }}
                          </el-button>
                        </div>
                      </el-col>
                      <el-col flex="none">
                        <el-tag
                          :type="item.pointsDelta > 0 ? 'success' : 'warning'"
                          effect="dark"
                          style="font-size: 15px; font-weight: 800; height: auto; padding: 8px 12px;"
                        >
                          {{ item.pointsDelta > 0 ? '+' : '' }}{{ Math.abs(item.pointsDelta) }}<br><span style="font-size: 11px; font-weight: 500;">điểm</span>
                        </el-tag>
                      </el-col>
                    </el-row>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </el-tab-pane>

          <!-- TAB: LỊCH SỬ THĂNG HẠNG -->
          <el-tab-pane label="Lịch sử thăng hạng" name="tier">
            <el-card shadow="never">
              <el-row align="middle" justify="space-between" style="margin-bottom: 16px;">
                <el-space :size="8"><el-icon><Trophy /></el-icon><el-text tag="b">Lịch sử thay đổi hạng VIP</el-text></el-space>
                <el-button @click="loadTierHistory" :loading="tierLoading"><el-icon><Refresh /></el-icon> Tải lại</el-button>
              </el-row>
              <el-skeleton v-if="tierLoading" :rows="5" animated />
              <el-empty v-else-if="!tierHistory.length" description="Chưa có lịch sử thay đổi hạng" />
              <el-space v-else direction="vertical" fill :size="12" style="width: 100%;">
                <el-card
                  v-for="item in tierHistory"
                  :key="item.id"
                  shadow="never"
                  :body-style="{ padding: '16px' }"
                  :style="`border-left: 4px solid ${item.transactionType === 'TIER_UPGRADE' ? 'var(--el-color-success)' : 'var(--el-color-danger)'};`"
                >
                  <el-row align="middle" :gutter="12">
                    <el-col flex="none">
                      <el-icon :size="28" :style="`color: ${item.transactionType === 'TIER_UPGRADE' ? 'var(--el-color-success)' : 'var(--el-color-danger)'};`">
                        <component :is="item.transactionType === 'TIER_UPGRADE' ? 'TopRight' : 'BottomRight'" />
                      </el-icon>
                    </el-col>
                    <el-col flex="1">
                      <el-row align="middle" justify="space-between" style="margin-bottom: 8px;">
                        <el-tag :type="item.transactionType === 'TIER_UPGRADE' ? 'success' : 'danger'" effect="dark">{{ item.transactionTypeDisplay }}</el-tag>
                        <el-text size="small" type="info">{{ formatDateTime(item.createdAt) }}</el-text>
                      </el-row>
                      <el-space :size="8" align="center" style="margin-bottom: 6px;">
                        <el-tag size="large" :type="getTierTagType(item.tierBefore)" effect="dark">{{ item.tierBeforeDisplay || 'Member' }}</el-tag>
                        <el-icon :size="16"><Right /></el-icon>
                        <el-tag size="large" :type="getTierTagType(item.tierAfter)" effect="dark">{{ item.tierAfterDisplay || 'Member' }}</el-tag>
                      </el-space>
                      <el-text size="small" type="info">{{ item.note }}</el-text>
                    </el-col>
                  </el-row>
                </el-card>
              </el-space>
            </el-card>
          </el-tab-pane>

          <!-- TAB: LỊCH SỬ ƯU ĐÃI -->
          <el-tab-pane name="promotions">
            <template #label><el-space :size="4"><el-icon><Ticket /></el-icon>Lịch sử ưu đãi</el-space></template>

            <el-row :gutter="16" style="margin-bottom: 16px;">
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="margin-bottom: 10px;">
                  <el-statistic title="Tổng ưu đãi" :value="promoStats.total">
                    <template #number><el-text style="font-size: 26px; font-weight: 900;">{{ promoStats.total }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); margin-bottom: 10px;">
                  <el-statistic title="Đã sử dụng" :value="promoStats.used">
                    <template #number><el-text type="success" style="font-size: 26px; font-weight: 900;">{{ promoStats.used }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-primary-light-9); border-color: var(--el-color-primary-light-5); margin-bottom: 10px;">
                  <el-statistic title="Tổng tiết kiệm">
                    <template #number><el-text type="primary" style="font-size: 22px; font-weight: 900;">{{ formatCurrencyShort(promoStats.totalSaved) }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
            </el-row>

            <el-card shadow="never">
              <el-row align="top" justify="space-between" style="margin-bottom: 16px; flex-wrap: wrap; gap: 12px;">
                <el-space :size="8"><el-icon><Ticket /></el-icon><el-text tag="b">Danh sách ưu đãi đã nhận</el-text></el-space>
                <el-space wrap :size="8">
                  <el-select v-model="promoTypeFilter" placeholder="Loại ưu đãi" clearable style="width: 160px;" @change="filterPromoHistory">
                    <el-option label="Mã khuyến mãi" value="PROMOTION_CODE"><el-tag type="primary" size="small">Mã KM</el-tag></el-option>
                    <el-option label="Vòng quay" value="SPIN_WHEEL"><el-tag type="success" size="small">Spin</el-tag></el-option>
                  </el-select>
                  <el-select v-model="promoStatusFilter" placeholder="Trạng thái" clearable style="width: 150px;" @change="filterPromoHistory">
                    <el-option label="Đã sử dụng" value="Đã sử dụng" />
                    <el-option label="Đang hoạt động" value="Đang hoạt động" />
                    <el-option label="Đã hết hạn" value="Đã hết hạn" />
                  </el-select>
                  <el-button @click="loadPromotionHistory" :loading="promoLoading"><el-icon><Refresh /></el-icon> Tải lại</el-button>
                </el-space>
              </el-row>

              <el-divider style="margin: 0 0 16px;" />
              <el-skeleton v-if="promoLoading" :rows="5" animated />
              <el-empty v-else-if="!filteredPromoHistory.length" description="Không có ưu đãi nào" />

              <el-space v-else direction="vertical" fill :size="12" style="width: 100%;">
                <el-card
                  v-for="item in filteredPromoHistory"
                  :key="`${item.type}-${item.orderId}-${item.promotionCode}`"
                  shadow="never"
                  :body-style="{ padding: '14px 16px' }"
                  :style="`border-left: 4px solid ${item.type === 'SPIN_WHEEL' ? 'var(--el-color-success)' : 'var(--el-color-primary)'};`"
                >
                  <el-row align="middle" :gutter="12">
                    <el-col flex="none">
                      <el-avatar :size="44" :style="`background: ${item.type === 'SPIN_WHEEL' ? 'var(--el-color-success-light-8)' : 'var(--el-color-primary-light-8)'};`">
                        <el-icon :size="22" :style="`color: ${item.type === 'SPIN_WHEEL' ? 'var(--el-color-success)' : 'var(--el-color-primary)'};`">
                          <component :is="item.type === 'SPIN_WHEEL' ? 'Trophy' : 'Ticket'" />
                        </el-icon>
                      </el-avatar>
                    </el-col>
                    <el-col flex="1">
                      <el-space :size="6" style="margin-bottom: 4px; flex-wrap: wrap;">
                        <el-tag :type="item.type === 'SPIN_WHEEL' ? 'success' : 'primary'" effect="dark" size="small">{{ item.type === 'SPIN_WHEEL' ? '🎡 Vòng quay' : '🎟 Mã KM' }}</el-tag>
                        <el-tag :type="getPromoStatusTagType(item.status)" size="small">{{ item.status }}</el-tag>
                        <el-tag type="info" effect="plain" size="small" v-if="item.type === 'PROMOTION_CODE'">{{ item.promotionCode }}</el-tag>
                      </el-space>
                      <el-text tag="div" style="font-weight: 700; margin-bottom: 4px;">{{ item.promotionName }}</el-text>
                      <el-space wrap :size="12">
                        <el-text size="small"><el-icon style="color: #67c23a;"><Money /></el-icon> Giảm: <el-text size="small" type="success" style="font-weight: 700;">{{ item.discountType === 'PERCENT' || item.discountType === 'percent' ? `${item.discountValue}%` : formatCurrency(item.discountValue) }}</el-text></el-text>
                        <el-text size="small" v-if="item.discountTotal"><el-icon style="color: #409eff;"><Wallet /></el-icon> Tiết kiệm: <el-text size="small" type="primary" style="font-weight: 700;">{{ formatCurrency(item.discountTotal) }}</el-text></el-text>
                        <el-text size="small" type="info" v-if="item.expiresAt"><el-icon><Clock /></el-icon> HSD: {{ formatDate(item.expiresAt) }}</el-text>
                        <el-text size="small" type="info" v-if="item.usedAt"><el-icon><Timer /></el-icon> {{ item.status === 'Đã sử dụng' ? 'Dùng lúc' : 'Nhận lúc' }}: {{ formatDateTime(item.usedAt) }}</el-text>
                      </el-space>
                      <div v-if="item.orderId" style="margin-top: 8px;">
                        <el-button size="small" type="primary" plain round @click="viewOrderBill(item.orderId, 0)">
                          <el-icon><ShoppingCart /></el-icon> Xem đơn #{{ item.orderId }}
                        </el-button>
                      </div>
                    </el-col>
                    <el-col flex="none" style="text-align: center;">
                      <el-text :type="item.type === 'SPIN_WHEEL' ? 'success' : 'primary'" style="font-size: 24px; font-weight: 900; display: block;">
                        {{ item.discountType === 'PERCENT' || item.discountType === 'percent' ? `${item.discountValue}%` : formatCurrencyShort(item.discountValue) }}
                      </el-text>
                      <el-text size="small" type="info">giảm giá</el-text>
                    </el-col>
                  </el-row>
                </el-card>
              </el-space>
            </el-card>
          </el-tab-pane>

          <!-- TAB: TỔNG HỢP ĐIỂM -->
          <el-tab-pane name="summary">
            <template #label><el-space :size="4"><el-icon><TrendCharts /></el-icon>Tổng hợp điểm</el-space></template>

            <el-space wrap :size="16" align="center" style="margin-bottom: 20px;">
              <el-space :size="8" align="center">
                <el-text size="small" type="info" style="font-weight: 600;">Chu kỳ:</el-text>
                <el-radio-group v-model="summaryMode" size="small" @change="loadLoyaltySummary">
                  <el-radio-button value="weekly">Theo tuần</el-radio-button>
                  <el-radio-button value="monthly">Theo tháng</el-radio-button>
                </el-radio-group>
              </el-space>
              <el-space :size="8" align="center">
                <el-text size="small" type="info" style="font-weight: 600;">Hiển thị:</el-text>
                <el-select v-model="summaryRange" style="width: 130px;" size="small" @change="loadLoyaltySummary">
                  <template v-if="summaryMode === 'weekly'">
                    <el-option label="4 tuần" :value="4" /><el-option label="8 tuần" :value="8" />
                    <el-option label="12 tuần" :value="12" /><el-option label="26 tuần" :value="26" />
                  </template>
                  <template v-else>
                    <el-option label="3 tháng" :value="3" /><el-option label="6 tháng" :value="6" />
                    <el-option label="12 tháng" :value="12" /><el-option label="24 tháng" :value="24" />
                  </template>
                </el-select>
              </el-space>
              <el-button size="small" @click="loadLoyaltySummary" :loading="summaryLoading"><el-icon><Refresh /></el-icon> Tải lại</el-button>
            </el-space>

            <el-skeleton v-if="summaryLoading" :rows="6" animated />
            <el-empty v-else-if="!loyaltySummary.length" description="Chưa có dữ liệu tổng hợp" />

            <template v-else>
              <el-row :gutter="16" style="margin-bottom: 16px;">
                <el-col :xs="24" :sm="8">
                  <el-card shadow="never" style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); margin-bottom: 10px;">
                    <el-statistic title="Tổng điểm cộng">
                      <template #number><el-text type="success" style="font-size: 26px; font-weight: 900;">+{{ summaryTotals.earned.toLocaleString() }}</el-text></template>
                    </el-statistic>
                  </el-card>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-card shadow="never" style="background: var(--el-color-warning-light-9); border-color: var(--el-color-warning-light-5); margin-bottom: 10px;">
                    <el-statistic title="Tổng điểm trừ">
                      <template #number><el-text type="warning" style="font-size: 26px; font-weight: 900;">{{ summaryTotals.deducted.toLocaleString() }}</el-text></template>
                    </el-statistic>
                  </el-card>
                </el-col>
                <el-col :xs="24" :sm="8">
                  <el-card shadow="never" style="margin-bottom: 10px;">
                    <el-statistic title="Net điểm">
                      <template #number>
                        <el-text :type="summaryTotals.net >= 0 ? 'success' : 'danger'" style="font-size: 26px; font-weight: 900;">
                          {{ summaryTotals.net >= 0 ? '+' : '' }}{{ summaryTotals.net.toLocaleString() }}
                        </el-text>
                      </template>
                    </el-statistic>
                  </el-card>
                </el-col>
              </el-row>

              <el-card shadow="never">
                <template #header>
                  <el-text tag="b">Chi tiết {{ summaryMode === 'weekly' ? 'theo tuần' : 'theo tháng' }}</el-text>
                </template>
                <el-table :data="loyaltySummary" stripe style="width: 100%">
                  <el-table-column label="Kỳ" min-width="160">
                    <template #default="{ row }">
                      <el-text tag="div" style="font-weight: 700;">{{ row.periodLabel }}</el-text>
                      <el-text size="small" type="info">{{ formatDate(row.periodStart) }} – {{ formatDate(row.periodEnd) }}</el-text>
                    </template>
                  </el-table-column>
                  <el-table-column label="Điểm cộng" width="130" align="right">
                    <template #default="{ row }"><el-text type="success" style="font-weight: 700;">+{{ row.totalPointsEarned.toLocaleString() }}</el-text></template>
                  </el-table-column>
                  <el-table-column label="Điểm trừ" width="130" align="right">
                    <template #default="{ row }"><el-text type="warning" style="font-weight: 700;">{{ row.totalPointsDeducted > 0 ? '-' : '' }}{{ row.totalPointsDeducted.toLocaleString() }}</el-text></template>
                  </el-table-column>
                  <el-table-column label="Net" width="120" align="right">
                    <template #default="{ row }">
                      <el-tag :type="row.netPoints >= 0 ? 'success' : 'danger'" effect="dark" size="small">{{ row.netPoints >= 0 ? '+' : '' }}{{ row.netPoints.toLocaleString() }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="GD" width="90" align="center">
                    <template #default="{ row }"><el-text type="info">{{ row.totalTransactions }}</el-text></template>
                  </el-table-column>
                  <el-table-column label="Biểu đồ" min-width="160">
                    <template #default="{ row }">
                      <el-space direction="vertical" :size="4" style="width: 100%;">
                        <div :style="`height: 6px; background: linear-gradient(90deg, #16a34a, #4ade80); border-radius: 3px; width: ${getSummaryBarWidth(row.totalPointsEarned, 'earn')}%; min-width: 3px;`" :title="`+${row.totalPointsEarned} điểm cộng`" />
                        <div :style="`height: 6px; background: linear-gradient(90deg, #f59e0b, #fcd34d); border-radius: 3px; width: ${getSummaryBarWidth(row.totalPointsDeducted, 'deduct')}%; min-width: 3px;`" :title="`-${row.totalPointsDeducted} điểm trừ`" />
                      </el-space>
                    </template>
                  </el-table-column>
                </el-table>
              </el-card>
            </template>
          </el-tab-pane>

          <!-- TAB: LỊCH SỬ GIAO DỊCH -->
          <el-tab-pane label="Lịch sử giao dịch" name="payments">
            <el-row :gutter="16" style="margin-bottom: 16px;">
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-success-light-9); border-color: var(--el-color-success-light-5); margin-bottom: 10px;">
                  <el-statistic title="GD thành công">
                    <template #number><el-text type="success" style="font-size: 26px; font-weight: 900;">{{ paymentStats.totalSuccess }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="background: var(--el-color-primary-light-9); border-color: var(--el-color-primary-light-5); margin-bottom: 10px;">
                  <el-statistic title="Tổng thanh toán">
                    <template #number><el-text type="primary" style="font-size: 20px; font-weight: 900;">{{ formatCurrencyShort(paymentStats.totalAmount) }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-card shadow="never" style="margin-bottom: 10px;">
                  <el-statistic title="Đã hoàn tiền">
                    <template #number><el-text style="font-size: 26px; font-weight: 900;">{{ paymentStats.totalRefunded }}</el-text></template>
                  </el-statistic>
                </el-card>
              </el-col>
            </el-row>

            <el-card shadow="never">
              <el-row align="top" justify="space-between" style="margin-bottom: 16px; flex-wrap: wrap; gap: 12px;">
                <el-space direction="vertical" :size="6">
                  <el-space :size="8"><el-icon><CreditCard /></el-icon><el-text tag="b">Danh sách giao dịch</el-text></el-space>
                  <el-space v-if="activePaymentFilters.length" wrap :size="6">
                    <el-tag v-for="filter in activePaymentFilters" :key="filter.type" :type="filter.tagType" closable @close="removePaymentFilter(filter.type)">{{ filter.label }}</el-tag>
                  </el-space>
                </el-space>
                <el-space wrap :size="8">
                  <el-select v-model="paymentStatusFilter" placeholder="Trạng thái" clearable style="width: 160px;" @change="filterPayments">
                    <el-option label="Thành công" value="SUCCESS"><el-tag type="success" size="small">Thành công</el-tag></el-option>
                    <el-option label="Đang chờ" value="PENDING"><el-tag type="warning" size="small">Đang chờ</el-tag></el-option>
                    <el-option label="Đã hoàn tiền" value="REFUNDED"><el-tag type="info" size="small">Đã hoàn tiền</el-tag></el-option>
                  </el-select>
                  <el-select v-model="paymentMethodFilter" placeholder="Phương thức" clearable style="width: 160px;" @change="filterPayments">
                    <el-option label="Tiền mặt" value="CASH" />
                    <el-option label="Chuyển khoản" value="TRANSFER" />
                    <el-option label="Thẻ tín dụng" value="CREDIT_CARD" />
                  </el-select>
                  <el-date-picker v-model="paymentDateRange" type="daterange" range-separator="-" start-placeholder="Từ ngày" end-placeholder="Đến ngày" format="DD/MM/YYYY" value-format="YYYY-MM-DD" style="width: 280px;" @change="filterPayments" />
                  <el-switch v-model="showDeletedPayments" active-text="Hiện đã xóa" @change="loadPayments" />
                  <el-button v-if="hasActivePaymentFilters" @click="clearPaymentFilters"><el-icon><Close /></el-icon> Xóa bộ lọc</el-button>
                  <el-button @click="loadPayments" :loading="paymentsLoading"><el-icon><Refresh /></el-icon> Tải lại</el-button>
                </el-space>
              </el-row>

              <el-divider style="margin: 0 0 16px;" />
              <el-skeleton v-if="paymentsLoading" :rows="5" animated />
              <el-empty v-else-if="!filteredPayments.length" description="Không có giao dịch nào">
                <el-button type="primary" v-if="hasActivePaymentFilters" @click="clearPaymentFilters">Xóa bộ lọc</el-button>
              </el-empty>

              <el-table v-else :data="paginatedPayments" style="width: 100%;" stripe>
                <el-table-column prop="id" label="ID" width="80" sortable />
                <el-table-column label="Mã GD" width="150">
                  <template #default="{ row }"><el-text size="small" type="info">{{ row.transactionRef }}</el-text></template>
                </el-table-column>
                <el-table-column label="Đơn hàng" width="150">
                  <template #default="{ row }"><el-text style="font-weight: 700;">#{{ row.orderId }}</el-text></template>
                </el-table-column>
                <el-table-column label="Số tiền" width="150" sortable prop="amount">
                  <template #default="{ row }"><el-text style="font-weight: 700;">{{ formatCurrency(row.amount) }}</el-text></template>
                </el-table-column>
                <el-table-column label="Phương thức" width="140">
                  <template #default="{ row }"><el-tag size="small" type="info">{{ getPaymentMethodLabel(row.method) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="Trạng thái" width="120">
                  <template #default="{ row }"><el-tag :type="getPaymentStatusType(row.status)" size="small">{{ formatPaymentStatus(row.status) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="Thời gian" width="180" sortable prop="createdAt">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
                <el-table-column label="Trạng thái" width="100" v-if="showDeletedPayments">
                  <template #default="{ row }">
                    <el-tag v-if="row.deletedAt" type="danger" size="small">Đã xóa</el-tag>
                    <el-tag v-else type="success" size="small">Hoạt động</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="Thao tác" width="200" fixed="right">
                  <template #default="{ row }">
                    <el-space :size="4">
                      <el-button size="small" type="primary" text @click="viewPaymentDetail(row)"><el-icon><View /></el-icon> Chi tiết</el-button>
                      <el-popconfirm v-if="!row.deletedAt" title="Xác nhận xóa giao dịch?" confirm-button-text="Xóa" cancel-button-text="Hủy" @confirm="softDeletePayment(row.id)">
                        <template #reference><el-button size="small" type="danger" text><el-icon><Delete /></el-icon></el-button></template>
                      </el-popconfirm>
                      <el-popconfirm v-else title="Khôi phục giao dịch?" confirm-button-text="Khôi phục" cancel-button-text="Hủy" @confirm="restorePayment(row.id)">
                        <template #reference><el-button size="small" type="success" text><el-icon><RefreshLeft /></el-icon></el-button></template>
                      </el-popconfirm>
                    </el-space>
                  </template>
                </el-table-column>
              </el-table>

              <el-row v-if="filteredPayments.length" justify="center" style="margin-top: 20px;">
                <el-pagination v-model:current-page="paymentCurrentPage" v-model:page-size="paymentPageSize" :page-sizes="[10, 20, 50]" :total="filteredPayments.length" layout="total, sizes, prev, pager, next" />
              </el-row>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>

    <!-- ORDER BILL DIALOG -->
    <el-dialog v-model="showOrderBillDialog" width="700px" :close-on-click-modal="true">
      <template #header>
        <el-space :size="10" align="center">
          <el-icon style="color: var(--el-color-primary);"><ShoppingCart /></el-icon>
          <el-text tag="b">Chi tiết đơn hàng</el-text>
          <el-tag type="primary" effect="dark" size="small" v-if="orderBill">#{{ orderBill.id }}</el-tag>
          <el-tag type="info" size="small" v-if="orderBill">{{ orderBill.orderNumber }}</el-tag>
        </el-space>
      </template>

      <el-skeleton v-if="loadingOrderBill" :rows="8" animated />
      <div v-else-if="orderBill">
        <!-- Points earned banner -->
        <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
          <el-icon style="color: #f59e0b;"><Star /></el-icon>
          Đơn hàng này tích lũy
          <el-text type="warning" style="font-weight: 700; font-size: 16px;"> +{{ Math.abs(currentBillPoints).toLocaleString() }} điểm</el-text>
          <el-text size="small" type="info"> (10.000 VNĐ = 1 điểm)</el-text>
        </el-alert>

        <!-- Meta -->
        <el-descriptions :column="2" border size="small" style="margin-bottom: 16px;">
          <el-descriptions-item label="Ngày đặt">{{ formatDateTime(orderBill.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="Trạng thái"><el-tag :type="getOrderStatusType(orderBill.status)" size="small">{{ formatOrderStatus(orderBill.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="Thanh toán"><el-tag :type="orderBill.paymentStatus === 'PAID' ? 'success' : 'warning'" size="small">{{ formatPaymentStatus(orderBill.paymentStatus) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="Kênh">{{ orderBill.channel || '—' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider style="margin: 12px 0;" />

        <!-- Items table -->
        <el-table :data="orderBill.items" border size="small" style="margin-bottom: 16px;">
          <el-table-column label="Sản phẩm" min-width="180">
            <template #default="{ row }">
              <el-text tag="div" style="font-weight: 600;">{{ row.productName }}</el-text>
              <el-text size="small" type="info">{{ row.variantName }}</el-text>
              <el-text size="small" type="info">SKU: {{ row.sku }}</el-text>
            </template>
          </el-table-column>
          <el-table-column label="SL" width="60" align="center">
            <template #default="{ row }"><el-tag size="small">{{ row.quantity }}</el-tag></template>
          </el-table-column>
          <el-table-column label="Đơn giá" width="120" align="right">
            <template #default="{ row }"><el-text type="info">{{ formatCurrency(row.unitPrice) }}</el-text></template>
          </el-table-column>
          <el-table-column label="Giảm giá" width="120" align="right">
            <template #default="{ row }"><el-text type="success">{{ row.discount > 0 ? '-' + formatCurrency(row.discount) : '—' }}</el-text></template>
          </el-table-column>
          <el-table-column label="Thành tiền" width="130" align="right">
            <template #default="{ row }"><el-text style="font-weight: 700;">{{ formatCurrency(row.lineTotal) }}</el-text></template>
          </el-table-column>
        </el-table>

        <!-- Totals -->
        <el-space direction="vertical" fill :size="8" style="width: 100%;">
          <el-row justify="space-between"><el-text>Tạm tính</el-text><el-text>{{ formatCurrency(orderBill.subtotal) }}</el-text></el-row>
          <el-row v-if="orderBill.discountTotal > 0" justify="space-between"><el-text type="success">Giảm giá</el-text><el-text type="success">-{{ formatCurrency(orderBill.discountTotal) }}</el-text></el-row>
          <el-row v-if="orderBill.taxTotal > 0" justify="space-between"><el-text>Thuế</el-text><el-text>{{ formatCurrency(orderBill.taxTotal) }}</el-text></el-row>
          <el-row v-if="orderBill.shippingFee > 0" justify="space-between"><el-text>Phí vận chuyển</el-text><el-text>{{ formatCurrency(orderBill.shippingFee) }}</el-text></el-row>
          <el-divider style="margin: 4px 0;" />
          <el-row justify="space-between">
            <el-text style="font-weight: 700; font-size: 15px;">Tổng cộng</el-text>
            <el-text type="primary" style="font-weight: 700; font-size: 15px;">{{ formatCurrency(orderBill.totalAmount) }}</el-text>
          </el-row>
          <el-row justify="space-between">
            <el-text size="small"><el-icon style="color: #f59e0b;"><Star /></el-icon> Điểm tích lũy được</el-text>
            <el-text type="warning" style="font-weight: 700;">+{{ Math.abs(currentBillPoints).toLocaleString() }} điểm</el-text>
          </el-row>
        </el-space>

        <el-card v-if="orderBill.notes" shadow="never" style="margin-top: 14px; background: var(--el-fill-color-lighter);" :body-style="{ padding: '12px' }">
          <el-space :size="6"><el-icon><InfoFilled /></el-icon><el-text size="small" style="font-weight: 600;">Ghi chú chiết khấu</el-text></el-space>
          <el-text size="small" type="info" style="display: block; margin-top: 6px;">{{ orderBill.notes }}</el-text>
        </el-card>
      </div>
      <template #footer>
        <el-button @click="showOrderBillDialog = false">Đóng</el-button>
      </template>
    </el-dialog>

    <!-- PAYMENT DETAIL DIALOG -->
    <el-dialog v-model="showPaymentDetailDialog" title="Chi tiết giao dịch" width="800px">
      <el-skeleton v-if="loadingPaymentDetail" :rows="8" animated />
      <div v-else-if="selectedPayment">
        <el-descriptions :column="2" border style="margin-bottom: 16px;">
          <el-descriptions-item label="Mã GD"><el-text style="font-weight: 700;">{{ selectedPayment.transactionRef }}</el-text></el-descriptions-item>
          <el-descriptions-item label="Số tiền"><el-text type="success" style="font-weight: 700;">{{ formatCurrency(selectedPayment.amount) }}</el-text></el-descriptions-item>
          <el-descriptions-item label="Phương thức"><el-tag size="small">{{ getPaymentMethodLabel(selectedPayment.method) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="Trạng thái"><el-tag :type="getPaymentStatusType(selectedPayment.status)">{{ formatPaymentStatus(selectedPayment.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="Thời gian">{{ formatDateTime(selectedPayment.paidAt) }}</el-descriptions-item>
        </el-descriptions>

        <el-text tag="div" style="font-size: 15px; font-weight: 700; margin-bottom: 12px;">Thông tin đơn hàng</el-text>
        <el-descriptions :column="2" border style="margin-bottom: 16px;">
          <el-descriptions-item label="Mã đơn"><el-text style="font-weight: 700;">#{{ selectedPayment.orderId }}</el-text></el-descriptions-item>
          <el-descriptions-item label="Trạng thái đơn"><el-tag :type="getOrderStatusType(selectedPayment.orderStatus)">{{ selectedPayment.orderStatus }}</el-tag></el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedPayment.items?.length">
          <el-text tag="div" style="font-size: 15px; font-weight: 700; margin-bottom: 12px;">Sản phẩm</el-text>
          <el-table :data="selectedPayment.items" border style="margin-bottom: 16px;">
            <el-table-column label="Sản phẩm" min-width="200">
              <template #default="{ row }">
                <el-text tag="div" style="font-weight: 700;">{{ row.productName }}</el-text>
                <el-text size="small" type="info">{{ row.variantName }}</el-text>
              </template>
            </el-table-column>
            <el-table-column label="SL" width="80" align="center" prop="quantity" />
            <el-table-column label="Đơn giá" width="130" align="right">
              <template #default="{ row }">{{ formatCurrency(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="Tổng" width="150" align="right">
              <template #default="{ row }"><el-text style="font-weight: 700;">{{ formatCurrency(row.lineTotal) }}</el-text></template>
            </el-table-column>
          </el-table>
          <el-space direction="vertical" fill :size="8" style="width: 100%;">
            <el-row justify="space-between"><el-text>Tạm tính:</el-text><el-text style="font-weight: 700;">{{ formatCurrency(selectedPayment.subtotal) }}</el-text></el-row>
            <el-row v-if="selectedPayment.discountTotal > 0" justify="space-between"><el-text type="success">Giảm giá (VIP + Spin):</el-text><el-text type="success" style="font-weight: 700;">-{{ formatCurrency(selectedPayment.discountTotal) }}</el-text></el-row>
            <el-row justify="space-between"><el-text>Phí ship:</el-text><el-text style="font-weight: 700;">{{ formatCurrency(selectedPayment.shippingFee) }}</el-text></el-row>
            <el-divider style="margin: 4px 0;" />
            <el-row justify="space-between">
              <el-text style="font-weight: 700; font-size: 15px;">Tổng cộng:</el-text>
              <el-text type="primary" style="font-weight: 700; font-size: 15px;">{{ formatCurrency(selectedPayment.amount) }}</el-text>
            </el-row>
          </el-space>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPaymentDetailDialog = false">Đóng</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, onMounted, computed } from "vue";
import {
  Star,
  Present,
  Wallet,
  TrendCharts,
  Trophy,
  Refresh,
  Right,
  TopRight,
  BottomRight,
  CirclePlus,
  Remove,
  List,
  Close,
  CircleCheck,
  Money,
  RefreshLeft,
  CreditCard,
  View,
  Delete,
  ShoppingCart,
  InfoFilled,
  Ticket,
  Clock,
  Timer,
  DataAnalysis,
  Check,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { customersApi } from "../../api/customers.api";
import { ordersApi } from "../../api/orders.api";

const router = useRouter();

function formatOrderStatus(s) { return { DELIVERED: "Đã giao hàng", SHIPPING: "Đang vận chuyển", CANCELLED: "Đã hủy", PENDING: "Chờ xử lý", PAID: "Đã thanh toán", RETURNED: "Trả hàng", PARTIALLY_RETURNED: "Trả hàng một phần" }[s] || s; }
function formatPaymentStatus(s) { return { PAID: "Đã thanh toán", SUCCESS: "Thành công", REFUNDED: "Hoàn tiền", PENDING: "Chờ xử lý", FAILED: "Thất bại" }[s] || s; }

const loading = ref(true);
const editing = ref(false);
const saving = ref(false);
const customer = ref(null);
const activeTab = ref("dashboard");

// ── Dashboard state (MỚI) ─────────────────────────────────────────────────────
const dashboardLoading = ref(false);
const myDashboard = ref(null);

const historyLoading = ref(false);
const loyaltyHistory = ref([]);
const historyTransactionFilter = ref("");
const historyDateRange = ref(null);

const tierLoading = ref(false);
const tierHistory = ref([]);

const promoLoading = ref(false);
const promotionHistory = ref([]);
const promoTypeFilter = ref("");
const promoStatusFilter = ref("");

const paymentsLoading = ref(false);
const payments = ref([]);
const showDeletedPayments = ref(false);
const paymentStatusFilter = ref("");
const paymentMethodFilter = ref("");
const paymentDateRange = ref(null);
const paymentCurrentPage = ref(1);
const paymentPageSize = ref(10);
const showPaymentDetailDialog = ref(false);
const selectedPayment = ref(null);
const loadingPaymentDetail = ref(false);

const showOrderBillDialog = ref(false);
const loadingOrderBill = ref(false);
const orderBill = ref(null);
const currentBillPoints = ref(0);

const summaryLoading = ref(false);
const loyaltySummary = ref([]);
const summaryMode = ref("monthly");
const summaryRange = ref(6);

const formData = ref({
  fullName: "",
  email: "",
  phone: "",
  address: "",
  notes: "",
  birthDate: "",
});

// ── Computed ──────────────────────────────────────────────────────────────────
const loyaltyStats = computed(() => {
  const earned = loyaltyHistory.value
    .filter((h) => h.pointsDelta > 0)
    .reduce((s, h) => s + h.pointsDelta, 0);
  const deducted = loyaltyHistory.value
    .filter((h) => h.pointsDelta < 0)
    .reduce((s, h) => s + Math.abs(h.pointsDelta), 0);
  return {
    totalEarned: earned.toLocaleString(),
    totalDeducted: deducted.toLocaleString(),
    totalTransactions: loyaltyHistory.value.length,
  };
});

const summaryTotals = computed(() => {
  const earned = loyaltySummary.value.reduce(
    (s, r) => s + (r.totalPointsEarned || 0),
    0,
  );
  const deducted = loyaltySummary.value.reduce(
    (s, r) => s + (r.totalPointsDeducted || 0),
    0,
  );
  return { earned, deducted, net: earned - deducted };
});

const promoStats = computed(() => {
  const used = promotionHistory.value.filter(
    (p) => p.status === "Đã sử dụng",
  ).length;
  const totalSaved = promotionHistory.value
    .filter((p) => p.discountTotal)
    .reduce((s, p) => s + (p.discountTotal || 0), 0);
  return { total: promotionHistory.value.length, used, totalSaved };
});

const filteredPromoHistory = computed(() => {
  let result = [...promotionHistory.value];
  if (promoTypeFilter.value)
    result = result.filter((p) => p.type === promoTypeFilter.value);
  if (promoStatusFilter.value)
    result = result.filter((p) => p.status === promoStatusFilter.value);
  return result;
});

const paymentStats = computed(() => {
  const active = payments.value.filter((p) => !p.deletedAt);
  const success = active.filter((p) => p.status === "SUCCESS");
  const refunded = active.filter((p) => p.status === "REFUNDED");
  return {
    totalSuccess: success.length,
    totalAmount: success.reduce((s, p) => s + (p.amount || 0), 0),
    totalRefunded: refunded.length,
  };
});

const filteredLoyaltyHistory = computed(() => {
  let result = [...loyaltyHistory.value];
  if (historyTransactionFilter.value)
    result = result.filter(
      (h) => h.transactionType === historyTransactionFilter.value,
    );
  if (historyDateRange.value?.length === 2) {
    const [start, end] = historyDateRange.value;
    result = result.filter((h) => {
      const d = new Date(h.createdAt);
      return d >= new Date(start) && d <= new Date(end + "T23:59:59");
    });
  }
  return result;
});

const filteredPayments = computed(() => {
  let result = [...payments.value];
  if (!showDeletedPayments.value) result = result.filter((p) => !p.deletedAt);
  if (paymentStatusFilter.value)
    result = result.filter((p) => p.status === paymentStatusFilter.value);
  if (paymentMethodFilter.value)
    result = result.filter((p) => p.method === paymentMethodFilter.value);
  if (paymentDateRange.value?.length === 2) {
    const [start, end] = paymentDateRange.value;
    result = result.filter((p) => {
      const d = new Date(p.createdAt);
      return d >= new Date(start) && d <= new Date(end + "T23:59:59");
    });
  }
  return result;
});

const paginatedPayments = computed(() => {
  const start = (paymentCurrentPage.value - 1) * paymentPageSize.value;
  return filteredPayments.value.slice(start, start + paymentPageSize.value);
});

const activeHistoryFilters = computed(() => {
  const filters = [];
  if (historyTransactionFilter.value) {
    const labels = {
      EARN: "Cộng điểm",
      DEDUCT: "Trừ điểm",
      PENALTY: "Phạt",
      TIER_UPGRADE: "Thăng hạng",
      TIER_DOWNGRADE: "Hạ hạng",
    };
    filters.push({
      type: "transaction",
      label: labels[historyTransactionFilter.value],
      tagType: getTransactionTagType(historyTransactionFilter.value),
    });
  }
  if (historyDateRange.value) {
    const [s, e] = historyDateRange.value;
    filters.push({
      type: "date",
      label: `${formatDate(s)} - ${formatDate(e)}`,
      tagType: "info",
    });
  }
  return filters;
});

const activePaymentFilters = computed(() => {
  const filters = [];
  if (paymentStatusFilter.value)
    filters.push({
      type: "status",
      label: `Trạng thái: ${paymentStatusFilter.value}`,
      tagType: getPaymentStatusType(paymentStatusFilter.value),
    });
  if (paymentMethodFilter.value)
    filters.push({
      type: "method",
      label: `PT: ${getPaymentMethodLabel(paymentMethodFilter.value)}`,
      tagType: "info",
    });
  if (paymentDateRange.value) {
    const [s, e] = paymentDateRange.value;
    filters.push({
      type: "date",
      label: `${formatDate(s)} - ${formatDate(e)}`,
      tagType: "info",
    });
  }
  return filters;
});

const hasActiveHistoryFilters = computed(
  () => !!(historyTransactionFilter.value || historyDateRange.value),
);
const hasActivePaymentFilters = computed(
  () =>
    !!(
      paymentStatusFilter.value ||
      paymentMethodFilter.value ||
      paymentDateRange.value
    ),
);

// ── Helpers ───────────────────────────────────────────────────────────────────
const unwrap = (response) => {
  const d = response?.data;
  if (d && typeof d === "object" && "data" in d) return d.data;
  return d ?? response;
};

// ── Data loading ──────────────────────────────────────────────────────────────
const loadCustomerData = async () => {
  loading.value = true;
  try {
    const response = await customersApi.getProfile();
    const c = unwrap(response);
    if (!c) {
      ElMessage.error("Không tìm thấy thông tin khách hàng");
      return;
    }
    customer.value = c;
    formData.value = {
      fullName: c.name || "",
      email: c.email || "",
      phone: c.phone || "",
      address: c.address || "",
      notes: c.notes || "",
      birthDate: c.dateOfBirth || "",
    };
  } catch (error) {
    if (error.response?.status === 401) {
      ElMessage.warning("Vui lòng đăng nhập lại");
      router.push("/login");
    } else if (error.response?.status === 404)
      ElMessage.error("Không tìm thấy thông tin khách hàng.");
    else
      ElMessage.error(
        "Lỗi khi tải dữ liệu: " +
          (error.response?.data?.message || error.message),
      );
  } finally {
    loading.value = false;
  }
};

// ── Dashboard loader (MỚI) ────────────────────────────────────────────────────
const loadMyDashboard = async () => {
  dashboardLoading.value = true;
  try {
    myDashboard.value = unwrap(await customersApi.getMyDashboard());
  } catch (e) {
    ElMessage.error(
      "Lỗi khi tải dashboard: " + (e.response?.data?.message || e.message),
    );
  } finally {
    dashboardLoading.value = false;
  }
};

const loadLoyaltyHistory = async () => {
  if (!customer.value?.id) return;
  historyLoading.value = true;
  try {
    loyaltyHistory.value =
      unwrap(await customersApi.getLoyaltyHistory(customer.value.id)) || [];
  } catch {
    ElMessage.error("Lỗi khi tải lịch sử điểm");
  } finally {
    historyLoading.value = false;
  }
};

const loadTierHistory = async () => {
  if (!customer.value?.id) return;
  tierLoading.value = true;
  try {
    tierHistory.value =
      unwrap(await customersApi.getTierHistory(customer.value.id)) || [];
  } catch {
    ElMessage.error("Lỗi khi tải lịch sử thăng hạng");
  } finally {
    tierLoading.value = false;
  }
};

const loadPromotionHistory = async () => {
  if (!customer.value?.id) return;
  promoLoading.value = true;
  try {
    promotionHistory.value =
      unwrap(await customersApi.getPromotionHistory(customer.value.id)) || [];
  } catch {
    ElMessage.error("Lỗi khi tải lịch sử ưu đãi");
  } finally {
    promoLoading.value = false;
  }
};

const loadPayments = async () => {
  if (!customer.value?.id) return;
  paymentsLoading.value = true;
  try {
    payments.value =
      unwrap(
        await customersApi.getPayments(
          customer.value.id,
          showDeletedPayments.value,
        ),
      ) || [];
  } catch {
    ElMessage.error("Lỗi khi tải lịch sử giao dịch");
  } finally {
    paymentsLoading.value = false;
  }
};

const loadLoyaltySummary = async () => {
  if (!customer.value?.id) return;
  summaryLoading.value = true;
  try {
    const res =
      summaryMode.value === "weekly"
        ? await customersApi.getLoyaltyWeeklySummaryMe(summaryRange.value)
        : await customersApi.getLoyaltyMonthlySummaryMe(summaryRange.value);
    loyaltySummary.value = unwrap(res) || [];
  } catch {
    ElMessage.error("Lỗi khi tải tổng hợp điểm");
  } finally {
    summaryLoading.value = false;
  }
};

const getSummaryBarWidth = (value, type) => {
  if (!loyaltySummary.value.length) return 0;
  const maxVal = Math.max(
    ...loyaltySummary.value.map((r) =>
      type === "earn" ? r.totalPointsEarned : r.totalPointsDeducted,
    ),
  );
  return maxVal === 0 ? 0 : Math.round((value / maxVal) * 100);
};

const viewOrderBill = async (orderId, pointsDelta) => {
  showOrderBillDialog.value = true;
  loadingOrderBill.value = true;
  orderBill.value = null;
  currentBillPoints.value = pointsDelta;
  try {
    orderBill.value = unwrap(await ordersApi.getOrderDetail(orderId));
  } catch {
    ElMessage.error("Lỗi khi tải chi tiết đơn hàng");
    showOrderBillDialog.value = false;
  } finally {
    loadingOrderBill.value = false;
  }
};

// ── Filter helpers ────────────────────────────────────────────────────────────
const filterLoyaltyHistory = () => {};
const filterPromoHistory = () => {};
const filterPayments = () => {
  paymentCurrentPage.value = 1;
};
const removeHistoryFilter = (type) => {
  if (type === "transaction") historyTransactionFilter.value = "";
  if (type === "date") historyDateRange.value = null;
};
const removePaymentFilter = (type) => {
  if (type === "status") paymentStatusFilter.value = "";
  if (type === "method") paymentMethodFilter.value = "";
  if (type === "date") paymentDateRange.value = null;
  filterPayments();
};
const clearHistoryFilters = () => {
  historyTransactionFilter.value = "";
  historyDateRange.value = null;
};
const clearPaymentFilters = () => {
  paymentStatusFilter.value = "";
  paymentMethodFilter.value = "";
  paymentDateRange.value = null;
  paymentCurrentPage.value = 1;
};

// ── Payment actions ───────────────────────────────────────────────────────────
const viewPaymentDetail = async (payment) => {
  selectedPayment.value = payment;
  showPaymentDetailDialog.value = true;
  loadingPaymentDetail.value = true;
  try {
    selectedPayment.value =
      unwrap(await customersApi.getPaymentDetail(payment.id)) || payment;
  } catch {
    ElMessage.error("Lỗi khi tải chi tiết giao dịch");
  } finally {
    loadingPaymentDetail.value = false;
  }
};
const softDeletePayment = async (id) => {
  try {
    await customersApi.softDeletePayment(id);
    ElMessage.success("Đã xóa giao dịch");
    loadPayments();
  } catch (error) {
    ElMessage.error(error.response?.data?.message || "Lỗi khi xóa giao dịch");
  }
};
const restorePayment = async (id) => {
  try {
    await customersApi.restorePayment(id);
    ElMessage.success("Đã khôi phục giao dịch");
    loadPayments();
  } catch (error) {
    ElMessage.error(
      error.response?.data?.message || "Lỗi khi khôi phục giao dịch",
    );
  }
};

// ── Edit actions ──────────────────────────────────────────────────────────────
const startEdit = () => {
  editing.value = true;
};
const handleCancel = () => {
  editing.value = false;
  loadCustomerData();
};
const handleSave = async () => {
  saving.value = true;
  try {
    await customersApi.update(customer.value.id, formData.value);
    await loadCustomerData();
    editing.value = false;
    ElMessage.success("Cập nhật thành công!");
  } catch (error) {
    ElMessage.error(
      "Lỗi khi lưu dữ liệu: " +
        (error.response?.data?.message || error.message),
    );
  } finally {
    saving.value = false;
  }
};

// ── Display helpers ───────────────────────────────────────────────────────────
const getTierTagType = (tier) =>
  ({
    BRONZE: "warning",
    SILVER: "info",
    GOLD: "warning",
    PLATINUM: "primary",
    DIAMOND: "danger",
  })[tier] || "info";
const getTransactionTagType = (type) =>
  ({
    EARN: "success",
    DEDUCT: "warning",
    PENALTY: "danger",
    TIER_UPGRADE: "success",
    TIER_DOWNGRADE: "info",
  })[type] || "info";
const getTimelineType = (type) =>
  ({
    EARN: "success",
    DEDUCT: "warning",
    PENALTY: "danger",
    TIER_UPGRADE: "success",
    TIER_DOWNGRADE: "info",
  })[type] || "primary";
const getTransactionIcon = (type) =>
  ({
    EARN: "CirclePlus",
    DEDUCT: "Remove",
    PENALTY: "Warning",
    TIER_UPGRADE: "TopRight",
    TIER_DOWNGRADE: "BottomRight",
  })[type] || "More";
const getTransactionIconColor = (type) =>
  ({
    EARN: "#67c23a",
    DEDUCT: "#e6a23c",
    PENALTY: "#f56c6c",
    TIER_UPGRADE: "#67c23a",
    TIER_DOWNGRADE: "#909399",
  })[type] || "#409eff";
const getHistoryCardClass = (type) =>
  ({
    EARN: "history-earn",
    DEDUCT: "history-deduct",
    PENALTY: "history-penalty",
    TIER_UPGRADE: "history-upgrade",
    TIER_DOWNGRADE: "history-downgrade",
  })[type] || "";
const getPaymentStatusType = (status) =>
  ({
    SUCCESS: "success",
    PENDING: "warning",
    REFUNDED: "info",
    FAILED: "danger",
  })[status] || "info";
const getPaymentMethodLabel = (method) =>
  ({
    CASH: "Tiền mặt",
    TRANSFER: "Chuyển khoản",
    CREDIT_CARD: "Thẻ tín dụng",
  })[method] || method;
const getOrderStatusType = (status) =>
  ({
    PENDING: "warning",
    CONFIRMED: "primary",
    PROCESSING: "primary",
    SHIPPING: "primary",
    DELIVERED: "success",
    CANCELLED: "danger",
  })[status] || "info";
const getPromoStatusTagType = (status) =>
  ({
    "Đã sử dụng": "success",
    "Đang hoạt động": "primary",
    "Đã hết hạn": "info",
  })[status] || "info";
const getPromoCardClass = (item) => {
  if (item.status === "Đã hết hạn") return "promo-expired";
  if (item.type === "SPIN_WHEEL") return "promo-spin";
  return "promo-code";
};

const formatCurrency = (val) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    val || 0,
  );
const formatCurrencyShort = (val) => {
  if (val >= 1e9) return (val / 1e9).toFixed(1) + "B";
  if (val >= 1e6) return (val / 1e6).toFixed(1) + "M";
  if (val >= 1e3) return (val / 1e3).toFixed(1) + "K";
  return formatCurrency(val);
};
const formatDate = (d) => (d ? new Date(d).toLocaleDateString("vi-VN") : "—");
const formatDateTime = (d) =>
  d
    ? new Date(d).toLocaleString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";
const kpiCards = computed(() => {
  if (!myDashboard.value?.summary) return [];
  return [
    { label: 'Điểm tích lũy', value: (myDashboard.value.summary.loyaltyPoints || 0).toLocaleString(), icon: 'Star', color: '#f59e0b', bg: '#fffbeb', border: '#fde68a' },
    { label: 'Tổng chi tiêu', value: formatCurrencyShort(myDashboard.value.summary.totalSpent), icon: 'Wallet', color: '#9333ea', bg: '#faf5ff', border: '#e9d5ff' },
    { label: 'Đơn hàng', value: myDashboard.value.summary.totalOrders || 0, icon: 'ShoppingCart', color: '#2563eb', bg: '#eff6ff', border: '#bfdbfe' },
    { label: 'Ưu đãi hiện tại', value: myDashboard.value.summary.discountDisplay || '0%', icon: 'Present', color: '#16a34a', bg: '#f0fdf4', border: '#bbf7d0' },
  ];
});

const getProgressPercent = () => {
  if (!customer.value) return 0;
  const total =
    (customer.value.loyaltyPoints || 0) +
    (customer.value.pointsToNextTier || 0);
  return total === 0
    ? 0
    : Math.min(100, Math.round((customer.value.loyaltyPoints / total) * 100));
};

onMounted(async () => {
  await loadCustomerData();
  // Load dashboard trước để tab đầu tiên hiển thị ngay
  loadMyDashboard();
  if (customer.value?.id) {
    loadLoyaltyHistory();
    loadTierHistory();
    loadPromotionHistory();
    loadPayments();
    loadLoyaltySummary();
  }
});
</script>

<style scoped>
:deep(.el-tag) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  line-height: 1;
  vertical-align: middle;
}
.form-static {
  padding: 9px 12px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  min-height: 40px;
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>