<template>
  <div class="return-manager">
    <!-- Header -->
    <div style="display:flex; align-items:flex-start; justify-content:space-between; gap:20px; margin-bottom:22px; flex-wrap:wrap;">
      <div>
        <div style="font-size:11px; font-weight:700; letter-spacing:0.08em; text-transform:uppercase; color:var(--el-text-color-secondary); margin-bottom:6px; display:flex; align-items:center; gap:5px;">
            <el-icon><RefreshLeft /></el-icon> Đơn hàng & CSKH
          </div>
        <div style="font-size:28px; font-weight:800; letter-spacing:-0.03em; margin-bottom:4px;">Quản lý hoàn hàng</div>
        <div style="font-size:13px; color:var(--el-text-color-secondary);">Xét duyệt yêu cầu trả hàng từ khách — xem ảnh minh chứng và quyết định mức hoàn tiền</div>
      </div>
      <div style="display:flex; align-items:center; gap:8px; flex-shrink:0; padding-top:4px; flex-wrap:wrap;">
        <el-button plain @click="loadReturns" :loading="loading">
          <el-icon><Refresh /></el-icon> Thay mới
        </el-button>
      </div>
    </div>

    <!-- ══════════ TABS ══════════ -->
    <el-tabs
      v-model="activeTab"
      @tab-change="loadReturns"
      style="margin-bottom: 0"
    >
      <el-tab-pane name="PENDING">
        <template #label>
          <el-badge
            :value="pendingCount"
            :hidden="pendingCount === 0"
            type="danger"
          >
            <span style="padding-right: 6px">Chờ duyệt</span>
          </el-badge>
        </template>
      </el-tab-pane>
      <el-tab-pane label="Đã duyệt" name="APPROVED" />
      <el-tab-pane label="Từ chối" name="REJECTED" />
      <el-tab-pane label="Tất cả" name="ALL" />
    </el-tabs>

    <!-- ══════════ TABLE ══════════ -->
    <el-card
      shadow="never"
      style="border-top: none; border-radius: 0 0 4px 4px"
    >
      <el-table
        :data="returns"
        v-loading="loading"
        row-key="id"
        stripe
        empty-text="Không có dữ liệu"
      >
        <!-- Mã đơn -->
        <el-table-column label="Mã đơn" width="140">
          <template #default="{ row }">
            <el-text tag="b" size="small">{{ row.orderNumber }}</el-text>
          </template>
        </el-table-column>

        <!-- Khách hàng -->
        <el-table-column label="Khách hàng" width="140">
          <template #default="{ row }">
            <el-text size="small">{{ row.customerName || "—" }}</el-text>
          </template>
        </el-table-column>

        <!-- Sản phẩm -->
        <el-table-column label="Sản phẩm" min-width="180">
          <template #default="{ row }">
            <el-space direction="vertical" :size="0" align="start">
              <el-text tag="b" size="small">{{ row.productName }}</el-text>
              <el-text type="info" size="small">{{ row.variantName }}</el-text>
            </el-space>
          </template>
        </el-table-column>

        <!-- Số lượng -->
        <el-table-column label="SL" width="55" align="center" prop="quantity" />

        <!-- Lý do -->
        <el-table-column label="Lý do" min-width="160">
          <template #default="{ row }">
            <el-text
              size="small"
              type="info"
              style="
                display: -webkit-box;
                -webkit-line-clamp: 2;
                -webkit-box-orient: vertical;
                overflow: hidden;
              "
            >
              {{ row.reason || "—" }}
            </el-text>
          </template>
        </el-table-column>

        <!-- Ảnh minh chứng -->
        <el-table-column label="Ảnh minh chứng" width="110" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              style="
                width: 56px;
                height: 56px;
                border-radius: 4px;
                cursor: zoom-in;
                border: 1px solid #dcdfe6;
              "
              preview-teleported
            />
            <el-tag v-else type="info" plain size="small">Không có</el-tag>
          </template>
        </el-table-column>

        <!-- Trạng thái -->
        <el-table-column label="Trạng thái" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" plain size="small">{{
              row.status
            }}</el-tag>
          </template>
        </el-table-column>

        <!-- Loại hoàn tiền — chỉ hiện khi đã xử lý -->
        <el-table-column
          v-if="activeTab !== 'PENDING'"
          label="Loại hoàn"
          width="160"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.refundMethod === 'FULL'"
              type="success"
              plain
              size="small"
              >Hoàn 100% (lỗi CH)</el-tag
            >
            <el-tag
              v-else-if="row.refundMethod === 'PARTIAL'"
              type="warning"
              plain
              size="small"
              >Hoàn 80% (lỗi KH)</el-tag
            >
            <el-tag
              v-else-if="row.refundMethod === 'CHANGE_OF_MIND'"
              type="primary"
              plain
              size="small"
            >
              Khách đổi ý
            </el-tag>
            <el-tag
              v-else-if="row.refundMethod === 'REJECT'"
              type="danger"
              plain
              size="small"
              >Từ chối</el-tag
            >
            <el-text v-else type="info" size="small">—</el-text>
          </template>
        </el-table-column>

        <!-- Số tiền hoàn -->
        <el-table-column label="Hoàn tiền" width="130" align="right">
          <template #default="{ row }">
            <el-text
              v-if="row.refundAmount > 0"
              type="danger"
              tag="b"
              size="small"
            >
              {{ formatMoney(row.refundAmount) }}
            </el-text>
            <el-text v-else type="info" size="small">Chưa xác định</el-text>
          </template>
        </el-table-column>

        <!-- Ngày yêu cầu -->
        <el-table-column label="Ngày yêu cầu" width="130">
          <template #default="{ row }">
            <el-text size="small">{{ formatDate(row.createdAt) }}</el-text>
          </template>
        </el-table-column>

        <!-- Ghi chú (khi đã xử lý) -->
        <el-table-column
          v-if="activeTab !== 'PENDING'"
          label="Ghi chú"
          min-width="140"
        >
          <template #default="{ row }">
            <el-text size="small" type="info">{{ row.note || "—" }}</el-text>
          </template>
        </el-table-column>

        <!-- Hành động (chỉ hiện khi PENDING) -->
        <el-table-column
          v-if="activeTab === 'PENDING'"
          label="Hành động"
          width="110"
          align="center"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              plain
              @click="openDecision(row)"
            >
              Xét duyệt
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- ══════════ DIALOG XÉT DUYỆT ══════════ -->
    <el-dialog
      v-model="decisionDialog.show"
      title="Xét duyệt yêu cầu hoàn hàng"
      width="540px"
      :close-on-click-modal="false"
    >
      <div v-if="decisionDialog.item">
        <!-- Thông tin return -->
        <el-descriptions
          :column="2"
          border
          size="small"
          style="margin-bottom: 16px"
        >
          <el-descriptions-item label="Mã đơn" :span="2">
            <el-text tag="b">{{ decisionDialog.item.orderNumber }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Sản phẩm" :span="2">
            <el-text>{{ decisionDialog.item.productName }}</el-text>
            <el-text type="info" size="small">
              — {{ decisionDialog.item.variantName }}</el-text
            >
          </el-descriptions-item>
          <el-descriptions-item label="Số lượng trả">
            <el-text tag="b">{{ decisionDialog.item.quantity }}</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Khách hàng">
            {{ decisionDialog.item.customerName || "—" }}
          </el-descriptions-item>
          <el-descriptions-item label="Lý do" :span="2">
            <el-text type="info">"{{ decisionDialog.item.reason }}"</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="Ngày yêu cầu" :span="2">
            {{ formatDate(decisionDialog.item.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- Ảnh minh chứng -->
        <div v-if="decisionDialog.item.imageUrl" style="margin-bottom: 16px">
          <el-text
            tag="b"
            size="small"
            style="display: block; margin-bottom: 8px"
          >
            Ảnh minh chứng từ khách:
          </el-text>
          <el-image
            :src="decisionDialog.item.imageUrl"
            :preview-src-list="[decisionDialog.item.imageUrl]"
            fit="contain"
            style="
              width: 100%;
              max-height: 280px;
              border: 1px solid #dcdfe6;
              border-radius: 4px;
            "
            preview-teleported
          />
        </div>
        <el-alert
          v-else
          title="Khách hàng không đính kèm ảnh minh chứng"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 16px"
        />

        <!-- Hướng dẫn 3 mức -->
        <el-alert style="margin-bottom: 16px" :closable="false">
          <template #default>
            <el-text size="small">
              <strong>Hoàn 100%</strong> — Lỗi do cửa hàng (giao sai, hàng hỏng
              từ kho)<br />
              <strong>Hoàn 80%</strong> — Lỗi do khách hàng (va đập, sử dụng sai
              cách)<br />
              <strong>Khách đổi ý</strong> — Không lỗi sản phẩm<br />
              <strong>Từ chối</strong> — Không đủ điều kiện hoàn trả (bắt buộc
              nhập lý do)
            </el-text>
          </template>
        </el-alert>

        <!-- Ghi chú admin -->
        <el-form label-position="top">
          <el-form-item label="Ghi chú / Lý do quyết định">
            <el-input
              v-model="decisionDialog.note"
              type="textarea"
              :rows="2"
              placeholder="Ghi chú thêm (bắt buộc khi từ chối)..."
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-row justify="space-between" align="middle">
          <el-button plain @click="decisionDialog.show = false">Đóng</el-button>
          <el-space>
            <el-button
              type="danger"
              plain
              :loading="submitting"
              :disabled="!decisionDialog.note.trim()"
              @click="submitDecision('REJECT')"
            >
              Từ chối
            </el-button>
            <el-button
              type="primary"
              plain
              :loading="submitting"
              @click="submitDecision('CHANGE_OF_MIND')"
            >
              Khách đổi ý
            </el-button>
            <el-button
              type="warning"
              plain
              :loading="submitting"
              @click="submitDecision('PARTIAL')"
            >
              Hoàn 80% (lỗi khách)
            </el-button>
            <el-button
              type="success"
              :loading="submitting"
              @click="submitDecision('FULL')"
            >
              Hoàn 100% (lỗi CH)
            </el-button>
          </el-space>
        </el-row>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from "vue";
import { Refresh } from "@element-plus/icons-vue";
import { returnsApi } from "../../api/returns.api";
import { toast } from "../../ui/toast";
import { confirmModal } from "../../ui/confirm";

// ── State ──────────────────────────────────────────────────────────────
const loading = ref(false);
const submitting = ref(false);
const activeTab = ref("PENDING");
const returns = ref([]);

const decisionDialog = reactive({
  show: false,
  item: null,
  note: "",
});

// ── Computed ───────────────────────────────────────────────────────────
const pendingCount = computed(() =>
  activeTab.value === "PENDING" ? returns.value.length : 0,
);

// ── Helpers ────────────────────────────────────────────────────────────
const formatMoney = (v) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    v || 0,
  );

const formatDate = (d) => (d ? new Date(d).toLocaleString("vi-VN") : "—");

const statusType = (s) =>
  ({ PENDING: "warning", APPROVED: "success", REJECTED: "danger" })[s] ||
  "info";

// ── Load data ──────────────────────────────────────────────────────────
async function loadReturns() {
  loading.value = true;
  try {
    const res =
      activeTab.value === "ALL"
        ? await returnsApi.getAll()
        : await returnsApi.getByStatus(activeTab.value);
    returns.value = res?.data?.data ?? res?.data ?? [];
  } catch {
    toast("Không thể tải danh sách hoàn hàng", "error");
  } finally {
    loading.value = false;
  }
}

// ── Decision ───────────────────────────────────────────────────────────
function openDecision(item) {
  decisionDialog.item = item;
  decisionDialog.note = "";
  decisionDialog.show = true;
}

async function submitDecision(refundType) {
  if (refundType === "REJECT" && !decisionDialog.note.trim()) {
    toast("Vui lòng nhập lý do từ chối", "warning");
    return;
  }

  const confirmMessages = {
    FULL: "Xác nhận HOÀN 100% cho khách? (lỗi do cửa hàng)",
    PARTIAL: "Xác nhận HOÀN 80% cho khách? (lỗi do khách hàng)",
    CHANGE_OF_MIND: 'Xác nhận khách đổi ý — hoàn tiền theo chính sách?',
    REJECT: "Xác nhận TỪ CHỐI yêu cầu hoàn hàng?",
  };

  const ok = await confirmModal(
    confirmMessages[refundType],
    "Xác nhận quyết định",
  );
  if (!ok) return;

  submitting.value = true;
  try {
    await returnsApi.decide(decisionDialog.item.id, {
      refundType,
      note: decisionDialog.note.trim(),
    });

    const successMessages = {
      FULL: "Đã duyệt — Hoàn 100% cho khách",
      PARTIAL: "Đã duyệt — Hoàn 80% cho khách",
      CHANGE_OF_MIND: 'Đã xử lý — Khách đổi ý',
      REJECT: "Đã từ chối yêu cầu hoàn hàng",
    };
    toast(
      successMessages[refundType],
      refundType === "REJECT" ? "info" : "success",
    );
    decisionDialog.show = false;
    loadReturns();
  } catch (err) {
    toast(
      err?.response?.data?.message || "Lỗi xử lý, vui lòng thử lại",
      "error",
    );
  } finally {
    submitting.value = false;
  }
}

onMounted(loadReturns);
</script>

<style scoped>
.return-manager {
  padding: 24px;
  min-height: 100vh;
  background: #f5f7fa;
}
</style>
