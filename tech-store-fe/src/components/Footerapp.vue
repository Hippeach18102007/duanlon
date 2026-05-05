<template>
  <footer class="ft-root">
    <div class="ft-inner">

      <!-- ── MAIN ROW ── -->
      <el-row :gutter="40" class="ft-main">

        <!-- Brand -->
        <el-col :md="8" :sm="24" :xs="24">
          <div class="ft-brand">
            <el-space :size="6">
              <el-icon :size="20" color="#818cf8"><Lightning /></el-icon>
              <span class="ft-logo">RetailPro <em>TechStore</em></span>
            </el-space>
            <p class="ft-desc">Hệ thống bán lẻ laptop chính hãng — chất lượng kiểm định, giá minh bạch, dịch vụ tận tâm.</p>
            <el-space :size="6">
              <el-button v-for="s in social" :key="s.label" circle size="small" class="ft-social">
                <el-icon><component :is="s.icon" /></el-icon>
              </el-button>
            </el-space>
          </div>
        </el-col>

        <!-- Links -->
        <el-col :md="8" :sm="12" :xs="12">
          <el-row :gutter="16">
            <el-col :span="12">
              <div class="ft-col-title">Sản phẩm</div>
              <div v-for="l in productLinks" :key="l" class="ft-link">{{ l }}</div>
            </el-col>
            <el-col :span="12">
              <div class="ft-col-title">Hỗ trợ</div>
              <div v-for="l in supportLinks" :key="l" class="ft-link">{{ l }}</div>
            </el-col>
          </el-row>
        </el-col>

        <!-- Contact -->
        <el-col :md="8" :sm="12" :xs="24">
          <div class="ft-col-title">Liên hệ</div>
          <div class="ft-contact-list">
            <div v-for="c in contact" :key="c.text" class="ft-contact-row">
              <el-icon color="#6366f1" :size="13"><component :is="c.icon" /></el-icon>
              <span>{{ c.text }}</span>
            </div>
          </div>
          <div class="ft-hotline">
            <div class="ft-hotline-label">Hotline miễn phí</div>
            <div class="ft-hotline-num">1900 1234</div>
          </div>
        </el-col>
      </el-row>

      <el-divider class="ft-divider" />

      <!-- ── BOTTOM ROW ── -->
      <div class="ft-bottom">
        <!-- Trust badges -->
        <el-space :size="20" wrap class="ft-trust">
          <el-space v-for="t in trust" :key="t.title" :size="6">
            <el-icon color="#6366f1" :size="14"><component :is="t.icon" /></el-icon>
            <span class="ft-trust-text">{{ t.title }}</span>
          </el-space>
        </el-space>

        <!-- Copyright + links -->
        <div class="ft-copy">
          <el-space :size="8">
            <el-link :underline="false" class="ft-policy">Điều khoản</el-link>
            <span class="ft-dot">·</span>
            <el-link :underline="false" class="ft-policy">Bảo mật</el-link>
          </el-space>
          <span>© {{ year }} RetailPro TechStore</span>
        </div>
      </div>

    </div>
  </footer>
</template>

<script setup>
import {
  Lightning, Monitor, ChatDotRound, VideoCamera, Headset,
  Location, Phone, Message, Clock,
  Lock, Van, RefreshRight, CircleCheck,
} from '@element-plus/icons-vue'

const year = new Date().getFullYear()

const social = [
  { label: 'Facebook', icon: Monitor },
  { label: 'Zalo',    icon: ChatDotRound },
  { label: 'YouTube', icon: VideoCamera },
  { label: 'TikTok',  icon: Headset },
]

const productLinks = ['Laptop Gaming', 'Laptop Văn phòng', 'MacBook', 'Laptop học sinh – SV']
const supportLinks = ['Tra cứu đơn hàng', 'Chính sách đổi trả', 'Bảo hành sản phẩm', 'Câu hỏi thường gặp']

const contact = [
  { icon: Location, text: 'Số 605 Kim Ngưu, Hà Nội' },
  { icon: Phone,    text: '1900 1234' },
  { icon: Message,  text: 'support@retailpro.vn' },
  { icon: Clock,    text: 'T2 – T7: 8:00 – 21:00' },
]

const trust = [
  { icon: Lock,        title: 'Thanh toán an toàn' },
  { icon: Van,         title: 'Giao hàng toàn quốc' },
  { icon: RefreshRight,title: 'Đổi trả 30 ngày' },
  { icon: CircleCheck, title: 'Bảo hành chính hãng' },
]
</script>

<style scoped>
/* ── Root ── */
.ft-root {
  background: #0f172a;
  color: #94a3b8;
  margin-top: 0;
  border-radius: 20px 20px 0 0;
  overflow: hidden;
}

.ft-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 32px 28px 20px;
}

/* ── Main ── */
.ft-main { padding-bottom: 24px; }

/* Brand */
.ft-brand { display: flex; flex-direction: column; gap: 10px; }
.ft-logo  { font-size: 16px; font-weight: 700; color: #f8fafc; letter-spacing: -0.02em; }
.ft-logo em { color: #818cf8; font-style: normal; }
.ft-desc  { font-size: 12px; line-height: 1.65; color: #64748b; margin: 0; max-width: 260px; }

.ft-social {
  background: rgba(255,255,255,0.05) !important;
  border-color: rgba(255,255,255,0.1) !important;
  color: #94a3b8 !important;
}

/* Columns */
.ft-col-title {
  font-size: 10px; font-weight: 700;
  letter-spacing: 0.12em; text-transform: uppercase;
  color: #f8fafc; margin-bottom: 10px;
}
.ft-link {
  font-size: 12px; color: #64748b;
  margin-bottom: 7px; cursor: pointer;
  transition: color 0.15s;
}
.ft-link:hover { color: #94a3b8; }

/* Contact */
.ft-contact-list { display: flex; flex-direction: column; gap: 7px; margin-bottom: 12px; }
.ft-contact-row  { display: flex; align-items: center; gap: 7px; font-size: 12px; color: #64748b; }

.ft-hotline {
  display: inline-block;
  background: rgba(99,102,241,0.12);
  border: 1px solid rgba(99,102,241,0.25);
  border-radius: 8px;
  padding: 7px 14px;
}
.ft-hotline-label { font-size: 9px; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: #818cf8; margin-bottom: 2px; }
.ft-hotline-num   { font-size: 17px; font-weight: 800; color: #f8fafc; letter-spacing: 0.02em; }

/* ── Divider ── */
.ft-divider { border-color: rgba(255,255,255,0.07) !important; margin: 0 !important; }

/* ── Bottom ── */
.ft-bottom {
  display: flex; align-items: center;
  justify-content: space-between; flex-wrap: wrap; gap: 10px;
  padding: 14px 0 0;
}

.ft-trust-text { font-size: 11px; color: #475569; }

.ft-copy {
  display: flex; align-items: center; gap: 16px;
  font-size: 11px; color: #334155;
}
.ft-dot    { color: #334155; }
.ft-policy { font-size: 11px; color: #475569 !important; }
</style>