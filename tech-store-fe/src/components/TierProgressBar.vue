<template>
  <el-card v-if="progress" shadow="never" :body-style="{ padding: 0 }">

    <!-- ── Compact bar ─────────────────────────────── -->
    <div class="tier-bar" @click="expanded = !expanded">

      <!-- Left: tier tag + points -->
      <el-space :size="10" align="center" style="flex-shrink: 0;">
        <el-tag :type="getTierTagType(progress.currentTier)" effect="plain" round>
          {{ progress.currentTierDisplay || 'Member' }}
        </el-tag>
        <el-space :size="4" alignment="baseline">
          <el-text tag="b" style="font-size: 15px;">{{ progress.currentPoints?.toLocaleString() }}</el-text>
          <el-text type="info" size="small">pts</el-text>
        </el-space>
      </el-space>

      <!-- Center: progress bar + next tier -->
      <div class="tier-bar__center">
        <el-space v-if="progress.nextTier" :size="10" align="center" style="width: 100%;">
          <el-progress
            :percentage="Math.min(progress.progressPercentage, 100)"
            :show-text="false"
            :stroke-width="5"
            :color="progressColorStops"
            style="flex: 1; min-width: 60px;"
          />
          <el-tag :type="getTierTagType(progress.nextTier)" effect="plain" size="small" round style="flex-shrink: 0;">
            {{ progress.nextTierDisplay }}
          </el-tag>
        </el-space>
        <el-text v-else>
          <span style="display: inline-flex; align-items: center; gap: 4px;">
            <el-icon><Trophy /></el-icon>
            Hạng cao nhất
          </span>
        </el-text>
      </div>

      <!-- Right: discount + chevron -->
      <el-space :size="12" align="center" style="flex-shrink: 0;">
        <el-space direction="vertical" :size="0" style="align-items: flex-end;">
          <el-text type="success" style="font-weight: 600; line-height: 1.4;">
            {{ getTierBenefitShort(progress.currentTier) }}
          </el-text>
          <el-text type="info" size="small" style="line-height: 1.4;">ưu đãi</el-text>
        </el-space>
        <el-icon
          style="transition: transform 0.22s ease; color: var(--el-text-color-placeholder);"
          :style="{ transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)' }"
        >
          <ArrowDown />
        </el-icon>
      </el-space>

    </div>

    <!-- ── Expanded detail panel ───────────────────── -->
    <el-space
      v-if="expanded"
      direction="vertical"
      fill
      :size="12"
      style="padding: 0 16px 16px; width: 100%;"
    >
      <el-divider style="margin: 0 0 4px;" />

      <!-- Upgrade alerts -->
      <el-alert
        v-if="progress.willUpgradeToVip && progress.currentCustomerType === 'REGULAR'"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #default>
          <el-text>
            Còn <strong>{{ progress.pointsNeededForVip?.toLocaleString() }} điểm</strong>
            (≈ {{ formatMoney(progress.amountNeededForVip) }}) nữa là trở thành <strong>VIP</strong>!
          </el-text>
          <el-space wrap :size="6" style="margin-top: 6px;">
            <el-tag size="small" type="warning" effect="plain">
              <span style="display: inline-flex; align-items: center; gap: 4px;">
                <el-icon><MagicStick /></el-icon> Giảm giá cao hơn
              </span>
            </el-tag>
            <el-tag size="small" type="warning" effect="plain">
              <span style="display: inline-flex; align-items: center; gap: 4px;">
                <el-icon><Present /></el-icon> Ưu đãi độc quyền
              </span>
            </el-tag>
            <el-tag size="small" type="warning" effect="plain">
              <span style="display: inline-flex; align-items: center; gap: 4px;">
                <el-icon><Opportunity /></el-icon> Tích điểm x2
              </span>
            </el-tag>
          </el-space>
        </template>
      </el-alert>

      <el-alert
        v-if="progress.isCloseToUpgrade && progress.nextTier"
        type="warning"
        :closable="false"
        show-icon
      >
        Còn <strong>{{ progress.pointsGap?.toLocaleString() }} điểm</strong>
        (≈ {{ formatMoney(progress.amountGapToNextTier) }}) nữa là lên hạng
        <strong>{{ progress.nextTierDisplay }}</strong>!
      </el-alert>

      <!-- Progress detail -->
      <div v-if="progress.nextTier">
        <el-row justify="space-between" style="margin-bottom: 6px;">
          <el-text type="info" size="small">
            {{ progress.currentPoints?.toLocaleString() }} / {{ progress.nextTierMinPoints?.toLocaleString() }} điểm
          </el-text>
          <el-text type="info" size="small">{{ progress.progressPercentage }}%</el-text>
        </el-row>
        <el-progress
          :percentage="Math.min(progress.progressPercentage, 100)"
          :show-text="false"
          :stroke-width="7"
          :color="progressColorStops"
        />
      </div>

      <!-- Benefits -->
      <el-descriptions :column="3" border size="small" style="width: 100%;">
        <el-descriptions-item label="Hạng hiện tại">
          {{ progress.currentTierDisplay }}
        </el-descriptions-item>
        <el-descriptions-item label="Giảm giá mỗi đơn">
          {{ getTierBenefitFull(progress.currentTier) }}
        </el-descriptions-item>
        <el-descriptions-item
          v-if="progress.nextTier && progress.nextTierDiscountRate"
          label="Sau khi lên hạng"
        >
          <span style="display: inline-flex; align-items: center; gap: 4px;">
            {{ getTierBenefitShort(progress.nextTier) }}
            <el-icon><TopRight /></el-icon>
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- Motivation -->
      <el-alert type="info" :closable="false" show-icon>
        {{ progress.motivationMessage }}
      </el-alert>

      <!-- Next tier benefits -->
      <el-text v-if="progress.nextTier && progress.tierBenefitMessage" size="small">
        <strong>Lợi ích khi lên {{ progress.nextTierDisplay }}:&nbsp;</strong>
        <el-text type="info" size="small">{{ progress.tierBenefitMessage }}</el-text>
      </el-text>

    </el-space>
  </el-card>
</template>

<script setup>
import { ArrowDown, Trophy, MagicStick, Present, Opportunity, TopRight } from '@element-plus/icons-vue';
import { ref, onMounted } from 'vue';
import http from '../api/http';

const progress = ref(null);
const expanded = ref(false);

const tierColors = {
  BRONZE:   '#cd7f32',
  SILVER:   '#8b9eb5',
  GOLD:     '#f59e0b',
  PLATINUM: '#64748b',
  DIAMOND:  '#38bdf8',
};

const TIER_BENEFITS = {
  BRONZE:   { short: '50k',  full: 'Giảm 50.000₫' },
  SILVER:   { short: '100k', full: 'Giảm 100.000₫' },
  GOLD:     { short: '150k', full: 'Giảm 150.000₫' },
  PLATINUM: { short: '3%',   full: 'Giảm 3%' },
  DIAMOND:  { short: '5%',   full: 'Giảm 5%' },
};

function getTierBenefitShort(tier) { return TIER_BENEFITS[tier]?.short ?? '—'; }
function getTierBenefitFull(tier)  { return TIER_BENEFITS[tier]?.full  ?? 'Chưa có ưu đãi'; }
function getTierColor(tier)        { return tierColors[tier] || '#6b7280'; }

function getTierTagType(tier) {
  const map = { BRONZE: '', SILVER: 'info', GOLD: 'warning', PLATINUM: 'success', DIAMOND: 'primary' };
  return map[tier] ?? 'info';
}

function tierPillStyle(tier) {
  const c = getTierColor(tier);
  return { background: `${c}14`, border: `1px solid ${c}30`, color: c };
}

function getProgressColor(pct) {
  if (pct >= 80) return '#10b981';
  if (pct >= 50) return '#4f6ef7';
  if (pct >= 25) return '#f59e0b';
  return '#94a3b8';
}

function getProgressGradient(pct) {
  if (pct >= 80) return 'linear-gradient(90deg,#34d399,#10b981)';
  if (pct >= 50) return 'linear-gradient(90deg,#818cf8,#4f6ef7)';
  if (pct >= 25) return 'linear-gradient(90deg,#fcd34d,#f59e0b)';
  return 'linear-gradient(90deg,#cbd5e1,#94a3b8)';
}

const progressColorStops = [
  { color: '#94a3b8', percentage: 25 },
  { color: '#f59e0b', percentage: 50 },
  { color: '#4f6ef7', percentage: 80 },
  { color: '#10b981', percentage: 100 },
];

function formatMoney(amount) {
  if (!amount) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

async function loadTierProgress() {
  try {
    const res = await http.get('/api/auth/tier-progress/me');
    progress.value = res.data;
  } catch (e) {
    console.error('Load tier progress error:', e);
  }
}

onMounted(loadTierProgress);
defineExpose({ refresh: loadTierProgress });
</script>

<style scoped>
.tier-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
}

.tier-bar__center {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
}
</style>