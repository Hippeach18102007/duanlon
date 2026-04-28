<!-- \src\components\Receiptprint.vue -->
<template>
  <div>
    <!-- Nút in (dùng trong POS dialog thành công) -->
    <slot :print="handlePrint" />

    <!-- Hóa đơn ẩn để in -->
    <div ref="receiptRef" class="receipt-wrapper" style="display:none">
      <div class="receipt">
        <!-- Header -->
        <div class="r-header">
          <div class="store-logo">🛍️</div>
          <div class="store-name">RETAIL STORE</div>
          <div class="store-sub">Hệ thống bán lẻ chính hãng</div>
          <div class="store-sub">📞 1900 1234 &nbsp;|&nbsp; 📍 TP. Hà Nội</div>
          <div class="divider">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</div>
          <div class="receipt-title">HÓA ĐƠN BÁN HÀNG</div>
          <div class="divider">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</div>
        </div>

        <!-- Thông tin đơn hàng -->
        <div class="r-meta">
          <div class="meta-row">
            <span class="meta-label">Số HĐ</span>
            <span class="meta-value mono">{{ order?.orderNumber }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Thời gian</span>
            <span class="meta-value">{{ formatDateTime(order?.createdAt) }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Nhân viên</span>
            <span class="meta-value">{{ order?.staffUsername }}</span>
          </div>
          <div class="meta-row">
            <span class="meta-label">Kênh</span>
<span class="meta-value">{{ (order?.channel || 'OFFLINE') === 'OFFLINE' ? 'Tại quầy' : 'Online' }}</span>          </div>
        </div>

        <div class="divider">- - - - - - - - - - - - - - - - - - - - - - -</div>

        <!-- Khách hàng -->
        <div class="r-customer">
          <div class="section-title">THÔNG TIN KHÁCH HÀNG</div>
          <div class="meta-row">
            <span class="meta-label">Họ tên</span>
            <span class="meta-value">{{ order?.customerName }}</span>
          </div>
          <div class="meta-row" v-if="order?.customerPhone">
            <span class="meta-label">SĐT</span>
            <span class="meta-value">{{ order?.customerPhone }}</span>
          </div>
          <div class="meta-row" v-if="order?.customerEmail">
            <span class="meta-label">Email</span>
            <span class="meta-value">{{ order?.customerEmail }}</span>
          </div>
          <div class="meta-row" v-if="customerTierName">
            <span class="meta-label">Hạng VIP</span>
            <span class="meta-value tier-badge">⭐ {{ customerTierName }}</span>
          </div>
        </div>

        <div class="divider">- - - - - - - - - - - - - - - - - - - - - - -</div>

        <!-- Danh sách sản phẩm -->
        <div class="r-items">
          <div class="section-title">SẢN PHẨM</div>
          <div v-for="(item, idx) in order?.items" :key="idx" class="item-block">
            <div class="item-name">{{ idx + 1 }}. {{ item.productName }}</div>
            <div class="item-variant">{{ item.variantName }}</div>
            <div class="item-serial-list" v-if="item.serials?.length">
              <div v-for="sn in item.serials" :key="sn" class="item-serial">
                📋 S/N: <span class="mono">{{ sn }}</span>
              </div>
            </div>
            <div class="item-price-row">
<!-- SAU (fix) -->
<span>
  {{ item.quantity }} x 
  {{ formatMoney(item.unitPrice > 0 ? item.unitPrice : (item.lineTotal + item.discount) / item.quantity) }}
</span>
<span class="price-right">
  {{ formatMoney(item.unitPrice > 0 ? item.unitPrice * item.quantity : item.lineTotal + item.discount) }}
</span>
            </div>
            <div v-if="item.discount > 0" class="item-discount-row">
              <span>Giảm VIP</span>
              <span class="price-right discount-val">-{{ formatMoney(item.discount) }}</span>
            </div>
          </div>
        </div>

        <div class="divider">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</div>

        <!-- Tổng tiền & giảm giá -->
        <div class="r-summary">
          <div class="sum-row">
            <span>Tạm tính</span>
            <span>{{ formatMoney(order?.subtotal) }}</span>
          </div>

          <!-- VIP discount -->
          <div v-if="vipDiscountInfo" class="sum-row discount-row">
            <span>
              ⭐ Giảm VIP {{ customerTierName }}
              <span class="pct-badge">{{ vipDiscountInfo.pct }}%</span>
            </span>
            <span class="discount-val">-{{ formatMoney(vipDiscountInfo.amount) }}</span>
          </div>

          <!-- Spin discount -->
          <div v-if="spinDiscountInfo" class="sum-row discount-row">
            <span>
              🎡 Giảm Spin Wheel
              <span class="pct-badge">{{ spinDiscountInfo.pct }}%</span>
            </span>
            <span class="discount-val">-{{ formatMoney(spinDiscountInfo.amount) }}</span>
          </div>

          <!-- Promo code -->
          <div v-if="promoInfo" class="sum-row discount-row promo-row">
            <span>
              🏷️ Mã: <b class="mono">{{ promoInfo.code }}</b>
              <span v-if="promoInfo.pct" class="pct-badge">{{ promoInfo.pct }}%</span>
              <span v-else class="pct-badge fixed">Cố định</span>
            </span>
            <span class="discount-val">-{{ formatMoney(promoInfo.amount) }}</span>
          </div>

          <div v-if="order?.shippingFee > 0" class="sum-row">
            <span>Phí vận chuyển</span>
            <span>{{ formatMoney(order?.shippingFee) }}</span>
          </div>

          <div class="sum-row total-row">
            <span>TỔNG CỘNG</span>
            <span>{{ formatMoney(order?.totalAmount) }}</span>
          </div>

          <div class="sum-row">
            <span>Phương thức TT</span>
            <span>{{ payMethodLabel }}</span>
          </div>

          <div v-if="cashIn > 0 && payMethod === 'CASH'" class="sum-row">
            <span>Khách đưa</span>
            <span>{{ formatMoney(cashIn) }}</span>
          </div>
          <div v-if="cashIn > 0 && payMethod === 'CASH'" class="sum-row">
            <span>Tiền thừa</span>
            <span>{{ formatMoney(cashIn - order?.totalAmount) }}</span>
          </div>
        </div>

        <div class="divider">━━━━━━━━━━━━━━━━━━━━━━━━━━━━</div>

        <!-- Ghi chú -->
        <div v-if="order?.notes" class="r-notes">
          <div class="section-title">GHI CHÚ</div>
          <div class="note-text">{{ order.notes }}</div>
        </div>

        <!-- Footer -->
        <div class="r-footer">
          <div>Cảm ơn quý khách đã mua hàng!</div>
          <div>Hàng điện tử không đổi trả sau khi mở seal</div>
          <div class="divider mt-8">- - - - - - - - - - - - - - - - - - - - - - -</div>
          <div class="footer-barcode">
            ▌▌▌▐▌▌▐▌▌▐▌▌▌▐▌▐▌▌▌▐▌▌▐▌▌
          </div>
          <div class="mono small-text">{{ order?.orderNumber }}</div>
          <div class="small-text mt-4">In lúc: {{ formatDateTime(new Date()) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  order: { type: Object, default: null },
  customerTierName: { type: String, default: '' },
  vipDiscountPct: { type: Number, default: 0 },
  vipDiscount: { type: Number, default: 0 },
  spinDiscountPct: { type: Number, default: 0 },
  spinDiscount: { type: Number, default: 0 },
  promoCode: { type: String, default: '' },
  promoDiscount: { type: Number, default: 0 },
  payMethod: { type: String, default: 'CASH' },
  cashIn: { type: Number, default: 0 }
})

const receiptRef = ref(null)

const formatMoney = (v) =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v || 0)

const formatDateTime = (val) => {
  if (!val) return ''
  const d = new Date(val)
  return d.toLocaleString('vi-VN', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

const payMethodLabel = computed(() => ({
  CASH: '💵 Tiền mặt',
  TRANSFER: '🏦 Chuyển khoản',
  CREDIT_CARD: '💳 Quẹt thẻ'
})[props.payMethod] || props.payMethod)

const vipDiscountInfo = computed(() => {
  if (!props.vipDiscountPct || !props.vipDiscount) return null
  return { pct: props.vipDiscountPct, amount: props.vipDiscount }
})

const spinDiscountInfo = computed(() => {
  if (!props.spinDiscountPct || !props.spinDiscount) return null
  return { pct: props.spinDiscountPct, amount: props.spinDiscount }
})

const promoInfo = computed(() => {
  if (!props.promoCode || !props.promoDiscount) return null
  const subtotal = props.order?.subtotal || 0
  // Kiểm tra xem có phải % không bằng cách so sánh với subtotal
  const isPercent = subtotal > 0 &&
    Math.abs(props.promoDiscount / subtotal * 100 - Math.round(props.promoDiscount / subtotal * 100)) < 0.5
  const pct = isPercent ? Math.round(props.promoDiscount / subtotal * 100) : null
  return { code: props.promoCode, amount: props.promoDiscount, pct }
})

function handlePrint() {
  if (!receiptRef.value) return

  const printWindow = window.open('', '_blank', 'width=400,height=700')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8"/>
      <title>Hóa đơn ${props.order?.orderNumber || ''}</title>
      <style>
        @import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;600;700&family=Sarabun:wght@400;600;700&display=swap');

        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
          font-family: 'Sarabun', sans-serif;
          background: #fff;
          display: flex;
          justify-content: center;
          padding: 20px 0;
        }

        .receipt {
          width: 300px;
          font-size: 12px;
          line-height: 1.5;
          color: #111;
        }

        .r-header { text-align: center; margin-bottom: 8px; }
        .store-logo { font-size: 28px; margin-bottom: 4px; }
        .store-name { font-size: 18px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase; }
        .store-sub { font-size: 10px; color: #555; margin-top: 2px; }
        .receipt-title { font-size: 14px; font-weight: 700; letter-spacing: 3px; margin: 6px 0; }

        .divider { color: #888; font-size: 10px; margin: 6px 0; white-space: nowrap; overflow: hidden; }

        .section-title { font-weight: 700; font-size: 10px; letter-spacing: 1.5px; text-transform: uppercase; color: #444; margin-bottom: 6px; }

        .r-meta, .r-customer, .r-items, .r-summary, .r-notes { margin-bottom: 6px; }

        .meta-row { display: flex; justify-content: space-between; margin-bottom: 3px; }
        .meta-label { color: #666; font-size: 11px; flex-shrink: 0; }
        .meta-value { font-weight: 600; font-size: 11px; text-align: right; max-width: 60%; }
        .tier-badge { color: #7c3aed; }

        .item-block { margin-bottom: 10px; }
        .item-name { font-weight: 700; font-size: 12px; }
        .item-variant { color: #555; font-size: 10px; margin-bottom: 2px; }
        .item-serial-list { margin: 2px 0 4px; }
        .item-serial { font-size: 10px; color: #444; }
        .item-price-row { display: flex; justify-content: space-between; font-size: 11px; }
        .item-discount-row { display: flex; justify-content: space-between; font-size: 10px; color: #888; }
        .price-right { font-weight: 600; }
        .discount-val { color: #16a34a; font-weight: 600; }

        .sum-row { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 12px; }
        .discount-row { color: #16a34a; }
        .promo-row { background: #f0fdf4; padding: 3px 4px; border-radius: 4px; margin: 2px 0; }
        .total-row { font-size: 16px; font-weight: 700; margin: 8px 0; padding: 6px 0; border-top: 2px solid #111; border-bottom: 2px solid #111; }

        .pct-badge { background: #f59e0b; color: #fff; font-size: 9px; font-weight: 700; padding: 1px 4px; border-radius: 3px; margin-left: 4px; }
        .pct-badge.fixed { background: #6366f1; }

        .r-notes .note-text { font-size: 10px; color: #555; font-style: italic; }
        .r-footer { text-align: center; font-size: 10px; color: #555; margin-top: 8px; }
        .footer-barcode { font-size: 22px; letter-spacing: -2px; color: #111; margin: 8px 0 4px; }

        .mono { font-family: 'IBM Plex Mono', monospace; }
        .small-text { font-size: 9px; }
        .mt-4 { margin-top: 4px; }
        .mt-8 { margin-top: 8px; }

        @media print {
          body { padding: 0; }
          @page { margin: 5mm; size: 80mm auto; }
        }
      </style>
    </head>
    <body>
      ${receiptRef.value.innerHTML}
    </body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 500)
}

defineExpose({ handlePrint })
</script>

<style scoped>
/* Chỉ dùng để preview trong Vue devtools, không ảnh hưởng bản in */
.receipt-wrapper { display: none; }
</style>