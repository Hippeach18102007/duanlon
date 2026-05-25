<template>
  <div style="max-width: 1000px; margin: 0 auto; padding: 32px 24px 80px;">

    <!-- Not VIP: dùng el-result -->
    <el-card v-if="!status.isVip" shadow="never" class="profile-card">
      <el-result icon="warning" title="Tính năng dành riêng cho VIP">
        <template #sub-title>
          <el-text type="info">Nâng cấp tài khoản để quay vòng may mắn và nhận ưu đãi đặc biệt mỗi tuần.</el-text>
        </template>
        <template #extra>
          <el-button type="primary" plain @click="$router.push('/')">Khám phá gói VIP</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- VIP -->
    <div v-else>

      <!-- Header -->
      <el-row align="flex-start" justify="space-between" style="flex-wrap: wrap; gap: 16px; margin-bottom: 28px;">
        <el-space direction="vertical" :size="6">
          <el-tag type="primary" effect="plain" size="small" round>VIP Exclusive</el-tag>
          <el-text tag="div" class="spin-title">Vòng Quay May Mắn</el-text>
          <el-text size="small" type="info">Quay 1 lần mỗi tuần — nhận thêm % giảm giá cho đơn hàng tiếp theo</el-text>
        </el-space>
        <el-tag
          v-if="status.currentBonus > 0"
          type="success"
          effect="plain"
          round
          size="large"
          style="height: auto; padding: 8px 16px;"
        >
          <el-badge is-dot type="success" style="margin-right: 8px;" />
          Đang có ưu đãi <strong>&nbsp;{{ status.currentBonus }}%</strong>&nbsp;· hết {{ formatDate(status.bonusExpiresAt) }}
        </el-tag>
      </el-row>

      <!-- Spin layout -->
      <div class="spin-layout">

        <!-- Wheel card -->
        <el-card shadow="never" class="profile-card" :body-style="{ padding: '32px 28px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '24px' }">
          <div class="wheel-wrap">
            <div class="pointer">▼</div>
            <canvas
              ref="wheelCanvas"
              width="340"
              height="340"
              @click="handleSpin"
              :class="{ spinning: isSpinning, disabled: !status.canSpin }"
            />
          </div>

          <el-button
            v-if="status.canSpin"
            type="primary"
            size="large"
            round
            style="min-width: 180px; font-size: 16px; font-weight: 700;"
            :disabled="isSpinning"
            :loading="isSpinning"
            @click="handleSpin"
          >
            <span v-if="!isSpinning">🎡 &nbsp;Quay ngay!</span>
            <span v-else>Đang quay...</span>
          </el-button>

          <el-alert
            v-else
            type="info"
            :closable="false"
            show-icon
            style="border-radius: 50px;"
          >
            {{ status.message }} · Quay lại thứ Hai tuần sau
          </el-alert>
        </el-card>

        <!-- Right column -->
        <el-space direction="vertical" fill :size="16" style="width: 100%;">

          <!-- Current bonus -->
          <el-card
            v-if="status.currentBonus > 0"
            shadow="never"
            class="profile-card"
            style="border-color: var(--el-color-warning-light-5); background: var(--el-color-warning-light-9);"
          >
            <template #header>
              <el-space :size="6">
                <el-icon color="var(--el-color-warning)"><Star /></el-icon>
                <el-text type="warning" style="font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em;">Ưu đãi đang áp dụng</el-text>
              </el-space>
            </template>
            <el-row align="middle" :gutter="16">
              <el-col flex="none">
                <el-text type="warning" class="bonus-number">
                  {{ status.currentBonus }}<span style="font-size: 22px;">%</span>
                </el-text>
              </el-col>
              <el-col flex="1">
                <el-space direction="vertical" :size="4">
                  <el-text tag="b">Giảm giá bổ sung</el-text>
                  <el-text size="small" type="info">Hết hạn {{ formatDate(status.bonusExpiresAt) }}</el-text>
                </el-space>
              </el-col>
            </el-row>
          </el-card>

          <!-- History -->
          <el-card shadow="never" class="profile-card">
            <template #header>
              <el-space :size="6">
                <el-icon><Clock /></el-icon>
                <el-text style="font-weight: 700; text-transform: uppercase; letter-spacing: 0.07em;">Lịch sử quay</el-text>
              </el-space>
            </template>

            <el-empty
              v-if="history.length === 0"
              :image-size="40"
              description="Chưa có lịch sử quay thưởng"
            />

            <el-table v-else :data="history" size="small" style="width: 100%;">
              <el-table-column label="" width="40">
                <template #default="{ row }">
                  <span>{{ getHistoryIcon(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="Ưu đãi" min-width="90">
                <template #default="{ row }">
                  <el-text style="font-weight: 700;">{{ row.discountBonus }}% giảm giá</el-text>
                </template>
              </el-table-column>
              <el-table-column label="Thời gian" min-width="130">
                <template #default="{ row }">
                  <el-text size="small" type="info">{{ formatDate(row.spunAt) }}</el-text>
                </template>
              </el-table-column>
              <el-table-column label="Trạng thái" width="110" align="center">
                <template #default="{ row }">
                  <el-tag
                    :type="getHistoryClass(row) === 'active' ? 'success' : getHistoryClass(row) === 'expired' ? 'danger' : 'info'"
                    effect="plain"
                    size="small"
                    round
                  >{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

        </el-space>
      </div>
    </div>

    <!-- Result Dialog: dùng el-result -->
    <el-dialog
      v-model="showResult"
      width="380px"
      align-center
      :show-close="false"
      class="spin-result-dialog"
    >
      <el-result icon="success" title="Chúc mừng!">
        <template #sub-title>
          <el-space direction="vertical" :size="8" align="center">
            <el-text type="primary" class="result-number">
              {{ lastResult?.discountBonus }}<span style="font-size: 28px;">%</span>
            </el-text>
            <el-text>{{ lastResult?.message }}</el-text>
            <el-text size="small" type="info">Có hiệu lực đến {{ formatDate(lastResult?.expiresAt) }}</el-text>
          </el-space>
        </template>
        <template #extra>
          <el-button type="primary" round style="min-width: 140px;" @click="closeResult">Tuyệt vời! 🎉</el-button>
        </template>
      </el-result>
    </el-dialog>

  </div>
</template>

<script>
import { Clock, Star } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { customersApi } from '../../api/customers.api';

export default {
  name: 'SpinWheel',
  components: { Clock, Star },
  data() {
    return {
      status: { isVip: false, canSpin: false, message: '', currentBonus: 0, bonusExpiresAt: null },
      prizes: [],
      history: [],
      isSpinning: false,
      showResult: false,
      lastResult: null,
      currentRotation: 0,
    };
  },
  mounted() { this.loadData(); },
  methods: {
    async loadData() {
      try {
        const [statusRes, prizesRes, historyRes] = await Promise.all([
          customersApi.getSpinWheelStatus(),
          customersApi.getSpinWheelPrizes(),
          customersApi.getSpinWheelHistory(),
        ]);
        this.status  = statusRes.data;
        this.prizes  = prizesRes.data;
        this.history = historyRes.data;
        if (this.status.isVip) this.$nextTick(() => this.drawWheel());
      } catch (e) { console.error(e); }
    },

    drawWheel() {
      const canvas = this.$refs.wheelCanvas;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      const cx = canvas.width / 2, cy = canvas.height / 2, r = 155;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const palette = ['#2563eb','#16a34a','#d97706','#dc2626','#7c3aed','#0891b2','#db2777','#65a30d'];
      const n = this.prizes.length;
      const seg = (2 * Math.PI) / n;

      ctx.save();
      ctx.shadowColor = 'rgba(0,0,0,0.15)';
      ctx.shadowBlur = 24;
      ctx.beginPath(); ctx.arc(cx, cy, r, 0, 2 * Math.PI); ctx.fillStyle = '#fff'; ctx.fill();
      ctx.restore();

      this.prizes.forEach((prize, i) => {
        const s = i * seg + this.currentRotation;
        const e = (i + 1) * seg + this.currentRotation;

        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, r, s, e);
        ctx.closePath();
        ctx.fillStyle = palette[i % palette.length];
        ctx.fill();
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 2.5;
        ctx.stroke();

        ctx.save();
        ctx.translate(cx, cy);
        ctx.rotate(s + seg / 2);
        ctx.textAlign = 'right';
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 15px "Be Vietnam Pro", sans-serif';
        ctx.fillText(prize.label, r - 14, 5);
        ctx.restore();
      });

      ctx.beginPath(); ctx.arc(cx, cy, 22, 0, 2 * Math.PI);
      ctx.fillStyle = '#fff'; ctx.fill();
      ctx.strokeStyle = '#e5e7eb'; ctx.lineWidth = 3; ctx.stroke();
      ctx.fillStyle = '#374151'; ctx.font = 'bold 11px sans-serif';
      ctx.textAlign = 'center'; ctx.fillText('VIP', cx, cy + 4);
    },

    async handleSpin() {
      if (!this.status.canSpin || this.isSpinning) return;
      this.isSpinning = true;
      try {
        const res = await customersApi.spinWheel();
        this.lastResult = res.data;
        await this.animateSpin(res.data.discountBonus);
        this.showResult = true;
        await this.loadData();
      } catch (e) {
        ElMessage.error(e.response?.data?.error || 'Lỗi khi quay thưởng');
        this.isSpinning = false;
      }
    },

    animateSpin(resultDiscount) {
      return new Promise(resolve => {
        const duration = 3500;
        const start = Date.now();
        const startRot = this.currentRotation;
        const idx = this.prizes.findIndex(p => p.discount == resultDiscount);
        const seg = (2 * Math.PI) / this.prizes.length;

        const midAngle = idx * seg + seg / 2;
        const targetAngle = -midAngle - Math.PI / 2;
        const normalizedTarget = ((targetAngle % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI);
        const total = Math.PI * 2 * 6 + normalizedTarget - ((startRot % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI));

        const animate = () => {
          const p = Math.min((Date.now() - start) / duration, 1);
          const ease = 1 - Math.pow(1 - p, 4);
          this.currentRotation = startRot + total * ease;
          this.drawWheel();
          if (p < 1) requestAnimationFrame(animate);
          else { this.isSpinning = false; resolve(); }
        };
        animate();
      });
    },

    closeResult() { this.showResult = false; },

    formatDate(d) {
      if (!d) return '';
      return new Date(d).toLocaleString('vi-VN', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' });
    },
    getHistoryClass(item) {
      if (item.isUsed) return 'used';
      if (new Date(item.expiresAt) < new Date()) return 'expired';
      return 'active';
    },
    getHistoryIcon(s) {
      if (s === 'Đã sử dụng') return '✅';
      if (s === 'Đã hết hạn') return '⏱️';
      return '🎁';
    },
  },
};
</script>

<style scoped>
.spin-layout {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  align-items: start;
}

@media (max-width: 720px) {
  .spin-layout { grid-template-columns: 1fr; }
}

.spin-title {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.bonus-number {
  font-size: 52px;
  font-weight: 900;
  line-height: 1;
}

.result-number {
  font-size: 72px;
  font-weight: 900;
  line-height: 1;
}

.wheel-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pointer {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 22px;
  color: var(--el-color-danger);
  filter: drop-shadow(0 2px 4px rgba(0,0,0,.2));
  z-index: 2;
  line-height: 1;
}

canvas {
  display: block;
  cursor: pointer;
  border-radius: 50%;
}
canvas.disabled { opacity: .55; cursor: not-allowed; }
canvas.spinning  { cursor: wait; }
</style>