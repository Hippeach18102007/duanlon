<!-- \src\components\Receiptprint.vue -->
<template>
  <div>
    <slot :print="handlePrint" />

    <!-- Hóa đơn ẩn để in -->
    <div ref="receiptRef" class="receipt-wrapper" style="display:none">
      <div class="receipt">

        <!-- ══ HEADER ══ -->
        <div class="r-header">
          <div class="store-name">TECH STORE</div>
          <div class="store-tagline">He thong ban le chinh hang</div>
          <div class="store-info">605 Kim Nguu, Ha Noi</div>
          <div class="store-info">Hotline: 1900 1234 | techstore.vn</div>
          <div class="sep-double">================================</div>
          <div class="doc-title">HOA DON BAN HANG</div>
          <div class="sep-double">================================</div>
        </div>

        <!-- ══ THÔNG TIN ĐƠN ══ -->
        <div class="r-section">
          <div class="info-row">
            <span class="info-label">So HD</span>
            <span class="info-value mono">{{ order?.orderNumber }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Ngay in</span>
            <span class="info-value">{{ formatDateTime(new Date()) }}</span>
          </div>
          <div class="info-row" v-if="order?.createdAt">
            <span class="info-label">Ngay tao</span>
            <span class="info-value">{{ formatDateTime(order.createdAt) }}</span>
          </div>
          <div class="info-row" v-if="order?.staffUsername">
            <span class="info-label">Thu ngan</span>
            <span class="info-value">{{ order.staffUsername }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">Kenh ban</span>
            <span class="info-value">{{ (order?.channel || 'OFFLINE') === 'OFFLINE' ? 'Tai quay (POS)' : 'Online' }}</span>
          </div>
        </div>

        <div class="sep-dash">- - - - - - - - - - - - - - - - - - - -</div>

        <!-- ══ KHÁCH HÀNG ══ -->
        <div class="r-section">
          <div class="section-label">KHACH HANG</div>
          <div class="info-row">
            <span class="info-label">Ho ten</span>
            <span class="info-value">{{ order?.customerName || 'Khach le' }}</span>
          </div>
          <div class="info-row" v-if="order?.customerPhone">
            <span class="info-label">SDT</span>
            <span class="info-value mono">{{ order.customerPhone }}</span>
          </div>
          <div class="info-row" v-if="customerTierName">
            <span class="info-label">Hang VIP</span>
            <span class="info-value">{{ customerTierName }}</span>
          </div>
        </div>

        <div class="sep-dash">- - - - - - - - - - - - - - - - - - - -</div>

        <!-- ══ SẢN PHẨM ══ -->
        <div class="r-section">
          <div class="section-label">SAN PHAM</div>
          <div v-for="(item, idx) in order?.items" :key="idx" class="item-block">
            <div class="item-name">{{ idx + 1 }}. {{ item.productName }}</div>
            <div class="item-variant" v-if="item.variantName && item.variantName !== item.productName">
              {{ item.variantName }}
            </div>
            <!-- Serial numbers -->
            <template v-if="item.serialNumbers?.length">
              <div v-for="sn in item.serialNumbers" :key="sn" class="item-serial">
                S/N: {{ sn }}
              </div>
            </template>
            <template v-else-if="item.serials?.length">
              <div v-for="sn in item.serials" :key="sn" class="item-serial">
                S/N: {{ sn }}
              </div>
            </template>
            <div class="item-price-row">
              <span>{{ item.quantity }} x {{ formatMoney(item.unitPrice > 0 ? item.unitPrice : (item.lineTotal + (item.discount || 0)) / item.quantity) }}</span>
              <span class="item-price-right">{{ formatMoney(item.unitPrice > 0 ? item.unitPrice * item.quantity : (item.lineTotal + (item.discount || 0))) }}</span>
            </div>
          </div>
        </div>

        <div class="sep-solid">================================</div>

        <!-- ══ TỔNG TIỀN ══ -->
        <div class="r-section">
          <div class="sum-row">
            <span>Tam tinh</span>
            <span>{{ formatMoney(order?.subtotal) }}</span>
          </div>

          <div v-if="vipDiscountPct > 0 && vipDiscount > 0" class="sum-row">
            <span>Giam VIP ({{ vipDiscountPct }}%)</span>
            <span>-{{ formatMoney(vipDiscount) }}</span>
          </div>

          <div v-if="spinDiscountPct > 0 && spinDiscount > 0" class="sum-row">
            <span>Giam Spin ({{ spinDiscountPct }}%)</span>
            <span>-{{ formatMoney(spinDiscount) }}</span>
          </div>

          <div v-if="promoCode && promoDiscount > 0" class="sum-row">
            <span>Ma KM: {{ promoCode }}</span>
            <span>-{{ formatMoney(promoDiscount) }}</span>
          </div>

          <div v-if="order?.shippingFee > 0" class="sum-row">
            <span>Phi ship</span>
            <span>{{ formatMoney(order.shippingFee) }}</span>
          </div>

          <div class="sep-solid-thin">--------------------------------</div>

          <div class="sum-row sum-total">
            <span>TONG CONG</span>
            <span>{{ formatMoney(order?.totalAmount) }}</span>
          </div>

          <div class="sep-solid-thin">--------------------------------</div>

          <div class="sum-row">
            <span>Phuong thuc TT</span>
            <span>{{ payMethodLabel }}</span>
          </div>
          <div v-if="cashIn > 0 && payMethod === 'CASH'" class="sum-row">
            <span>Khach dua</span>
            <span>{{ formatMoney(cashIn) }}</span>
          </div>
          <div v-if="cashIn > 0 && payMethod === 'CASH'" class="sum-row">
            <span>Tien thua</span>
            <span>{{ formatMoney(cashIn - (order?.totalAmount || 0)) }}</span>
          </div>
        </div>

        <div v-if="order?.notes" class="r-section">
          <div class="sep-dash">- - - - - - - - - - - - - - - - - - - -</div>
          <div class="section-label">GHI CHU</div>
          <div class="note-text">{{ order.notes }}</div>
        </div>

        <!-- ══ FOOTER ══ -->
        <div class="sep-double">================================</div>
        <div class="r-footer">
          <div>Cam on quy khach da mua hang!</div>
          <div class="footer-sub">Hang dien tu khong doi tra sau khi mo seal.</div>
          <div class="footer-sub">Bao hanh theo chinh sach nha san xuat.</div>
          <div class="sep-dash mt-6">- - - - - - - - - - - - - - - - - - - -</div>
          <div class="mono barcode-line">||||| ||| || |||| | ||| || |||| |||||</div>
          <div class="mono order-num">{{ order?.orderNumber }}</div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  order:            { type: Object, default: null },
  customerTierName: { type: String, default: '' },
  vipDiscountPct:   { type: Number, default: 0 },
  vipDiscount:      { type: Number, default: 0 },
  spinDiscountPct:  { type: Number, default: 0 },
  spinDiscount:     { type: Number, default: 0 },
  promoCode:        { type: String, default: '' },
  promoDiscount:    { type: Number, default: 0 },
  payMethod:        { type: String, default: 'CASH' },
  cashIn:           { type: Number, default: 0 }
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
  CASH:        'Tien mat',
  TRANSFER:    'Chuyen khoan',
  CREDIT_CARD: 'Quet the'
})[props.payMethod] || props.payMethod)

function handlePrint() {
  if (!receiptRef.value) return

  const printWindow = window.open('', '_blank', 'width=420,height=750')
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8"/>
      <title>Hoa don ${props.order?.orderNumber || ''}</title>
      <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }

        body {
          font-family: 'Courier New', Courier, monospace;
          background: #fff;
          color: #000;
          display: flex;
          justify-content: center;
          padding: 16px 0;
        }

        .receipt {
          width: 300px;
          font-size: 11px;
          line-height: 1.55;
          color: #000;
        }

        /* ── Header ── */
        .r-header { text-align: center; margin-bottom: 6px; }
        .store-name {
          font-size: 20px;
          font-weight: 900;
          letter-spacing: 4px;
          text-transform: uppercase;
          margin-bottom: 2px;
        }
        .store-tagline { font-size: 10px; font-style: italic; margin-bottom: 2px; }
        .store-info { font-size: 9.5px; color: #333; }
        .doc-title {
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 2px;
          margin: 4px 0;
        }

        /* ── Separators ── */
        .sep-double, .sep-dash, .sep-solid, .sep-solid-thin {
          font-size: 10px;
          color: #555;
          margin: 5px 0;
          white-space: nowrap;
          overflow: hidden;
          text-align: center;
        }

        /* ── Section ── */
        .r-section { margin-bottom: 4px; }
        .section-label {
          font-size: 9px;
          font-weight: 700;
          letter-spacing: 2px;
          text-transform: uppercase;
          color: #333;
          margin-bottom: 4px;
          border-bottom: 1px dashed #aaa;
          padding-bottom: 2px;
        }

        /* ── Info rows ── */
        .info-row {
          display: flex;
          justify-content: space-between;
          margin-bottom: 2px;
          font-size: 11px;
        }
        .info-label { color: #555; flex-shrink: 0; padding-right: 6px; }
        .info-value { font-weight: 600; text-align: right; word-break: break-all; }
        .mono { font-family: 'Courier New', Courier, monospace; }

        /* ── Items ── */
        .item-block { margin-bottom: 8px; padding-bottom: 6px; border-bottom: 1px dashed #ccc; }
        .item-block:last-child { border-bottom: none; }
        .item-name { font-weight: 700; font-size: 11.5px; }
        .item-variant { font-size: 10px; color: #444; margin-bottom: 1px; }
        .item-serial { font-size: 9.5px; font-family: 'Courier New', monospace; color: #333; margin-bottom: 1px; }
        .item-price-row {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          margin-top: 2px;
        }
        .item-price-right { font-weight: 700; }

        /* ── Summary ── */
        .sum-row {
          display: flex;
          justify-content: space-between;
          font-size: 11px;
          margin-bottom: 3px;
        }
        .sum-total {
          font-size: 14px;
          font-weight: 900;
          letter-spacing: 0.5px;
          margin: 4px 0;
        }

        /* ── Notes ── */
        .note-text { font-size: 10px; color: #444; font-style: italic; margin-top: 2px; }

        /* ── Footer ── */
        .r-footer { text-align: center; margin-top: 4px; }
        .r-footer > div:first-child { font-weight: 700; font-size: 12px; margin-bottom: 3px; }
        .footer-sub { font-size: 9px; color: #444; margin-bottom: 1px; }
        .barcode-line { font-size: 16px; letter-spacing: -1px; margin: 6px 0 2px; }
        .order-num { font-size: 9px; margin-bottom: 2px; }
        .mt-6 { margin-top: 6px; }

        @media print {
          body { padding: 0; }
          @page { margin: 4mm; size: 80mm auto; }
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
  }, 600)
}

defineExpose({ handlePrint })
</script>

<style scoped>
.receipt-wrapper { display: none; }
</style>