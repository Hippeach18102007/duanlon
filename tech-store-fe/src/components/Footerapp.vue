<template>
  <div style="background: #0f172a; color: #94a3b8; margin-top: 64px; padding: 48px 0 0;">

    <!-- Main Grid -->
    <el-row :gutter="32" style="max-width: 1400px; margin: 0 auto; padding: 0 24px 40px;">

      <!-- Brand -->
      <el-col :md="8" :sm="24" :xs="24">
        <el-space direction="vertical" :size="12" alignment="flex-start" style="width: 100%;">
          <el-space :size="8">
            <el-icon :size="22" color="#818cf8"><Lightning /></el-icon>
            <span style="font-size: 18px; font-weight: 700; color: #f8fafc; letter-spacing: -0.02em;">
              RetailPro <span style="color: #818cf8;">TechStore</span>
            </span>
          </el-space>
          <p style="font-size: 13px; line-height: 1.7; color: #94a3b8; margin: 0; max-width: 280px;">
            Hệ thống bán lẻ laptop chính hãng hàng đầu — chất lượng được kiểm định, giá cả minh bạch, dịch vụ tận tâm.
          </p>
          <el-space :size="8">
            <el-button circle size="small" v-for="item in socialLinks" :key="item.label" style="background: rgba(255,255,255,0.06); border-color: rgba(255,255,255,0.12); color: #94a3b8;">
              <el-icon><component :is="item.icon" /></el-icon>
            </el-button>
          </el-space>
        </el-space>
      </el-col>

      <!-- Sản phẩm -->
      <el-col :md="4" :sm="8" :xs="12">
        <el-space direction="vertical" :size="10" alignment="flex-start">
          <span style="font-size: 11px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: #f8fafc;">Sản phẩm</span>
          <el-link :underline="false" style="color: #94a3b8; font-size: 13px;" v-for="item in productLinks" :key="item">{{ item }}</el-link>
        </el-space>
      </el-col>

      <!-- Hỗ trợ -->
      <el-col :md="4" :sm="8" :xs="12">
        <el-space direction="vertical" :size="10" alignment="flex-start">
          <span style="font-size: 11px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: #f8fafc;">Hỗ trợ</span>
          <el-link :underline="false" style="color: #94a3b8; font-size: 13px;" v-for="item in supportLinks" :key="item">{{ item }}</el-link>
        </el-space>
      </el-col>

      <!-- Liên hệ -->
      <el-col :md="8" :sm="8" :xs="24">
        <el-space direction="vertical" :size="12" alignment="flex-start" style="width: 100%;">
          <span style="font-size: 11px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: #f8fafc;">Liên hệ</span>
          <el-space :size="8" v-for="c in contactItems" :key="c.text">
            <el-icon color="#6366f1"><component :is="c.icon" /></el-icon>
            <span style="font-size: 13px; color: #94a3b8;">{{ c.text }}</span>
          </el-space>
          <!-- Hotline badge -->
          <div style="background: rgba(99,102,241,0.15); border: 1px solid rgba(99,102,241,0.3); border-radius: 10px; padding: 10px 16px; margin-top: 4px;">
            <div style="font-size: 10px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: #818cf8; margin-bottom: 4px;">Hotline miễn phí</div>
            <div style="font-size: 20px; font-weight: 800; color: #f8fafc; letter-spacing: 0.02em;">1900 1234</div>
          </div>
        </el-space>
      </el-col>
    </el-row>

    <el-divider style="border-color: rgba(255,255,255,0.07); margin: 0;" />

    <!-- Trust Bar -->
    <el-row justify="space-around" align="middle" style="max-width: 1400px; margin: 0 auto; padding: 20px 24px;">
      <el-col :xs="12" :sm="8" :md="4" v-for="trust in trustItems" :key="trust.title">
        <el-space :size="10" style="padding: 8px 0;">
          <el-icon :size="26" color="#818cf8"><component :is="trust.icon" /></el-icon>
          <div>
            <div style="font-size: 12px; font-weight: 600; color: #e2e8f0; line-height: 1.3;">{{ trust.title }}</div>
            <div style="font-size: 11px; color: #64748b;">{{ trust.sub }}</div>
          </div>
        </el-space>
      </el-col>
    </el-row>

    <el-divider style="border-color: rgba(255,255,255,0.07); margin: 0;" />

    <!-- Bottom Bar -->
    <el-row align="middle" justify="space-between" style="max-width: 1400px; margin: 0 auto; padding: 14px 24px; flex-wrap: wrap; gap: 8px;">
      <el-col :xs="24" :sm="8">
        <span style="font-size: 12px; color: #475569;">© {{ year }} RetailPro TechStore — DATN FPT University</span>
      </el-col>
      <el-col :xs="24" :sm="8" style="text-align: center;">
        <el-space :size="8">
          <el-link :underline="false" style="font-size: 12px; color: #475569;">Điều khoản</el-link>
          <span style="color: #334155;">·</span>
          <el-link :underline="false" style="font-size: 12px; color: #475569;">Bảo mật</el-link>
          <span style="color: #334155;">·</span>
          <el-link :underline="false" style="font-size: 12px; color: #475569;">Sitemap</el-link>
        </el-space>
      </el-col>
      <el-col :xs="24" :sm="8" style="text-align: right;">
        <el-space :size="6" wrap>
          <el-tag size="small" type="info" effect="plain" style="color: #64748b; border-color: rgba(255,255,255,0.1); background: rgba(255,255,255,0.04);"
            v-for="p in payments" :key="p">{{ p }}</el-tag>
        </el-space>
      </el-col>
    </el-row>

  </div>
</template>

<script setup>
import {
  Lightning, Monitor, ChatDotRound, VideoCamera, Headset,
  Location, Phone, Message, Clock,
  Lock, Van, RefreshRight, CircleCheck, CreditCard
} from '@element-plus/icons-vue'

const year = new Date().getFullYear()

const socialLinks = [
  { label: 'Facebook', icon: Monitor },
  { label: 'Zalo', icon: ChatDotRound },
  { label: 'YouTube', icon: VideoCamera },
  { label: 'TikTok', icon: Headset },
]

const productLinks = ['Laptop Gaming', 'Laptop Văn phòng', 'MacBook', 'Laptop học sinh – SV', 'Laptop đồ họa']

const supportLinks = ['Tra cứu đơn hàng', 'Chính sách đổi trả', 'Bảo hành sản phẩm', 'Hướng dẫn mua hàng', 'Câu hỏi thường gặp', 'Liên hệ hỗ trợ']

const contactItems = [
  { icon: Location, text: 'Số 605 Kim Ngưu, Hà Nội' },
  { icon: Phone, text: '1900 1234' },
  { icon: Message, text: 'support@retailpro.vn' },
  { icon: Clock, text: 'T2 – T7: 8:00 – 21:00' },
]

const trustItems = [
  { icon: Lock, title: 'Thanh toán an toàn', sub: 'SSL 256-bit' },
  { icon: Van, title: 'Giao hàng toàn quốc', sub: '2 – 5 ngày làm việc' },
  { icon: RefreshRight, title: 'Đổi trả miễn phí', sub: 'Trong vòng 30 ngày' },
  { icon: CircleCheck, title: 'Bảo hành chính hãng', sub: '12 – 36 tháng' },
]

// const payments = ['VISA', 'MasterCard', 'MoMo', 'ZaloPay', 'VNPay']
</script>