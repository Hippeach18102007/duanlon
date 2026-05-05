<template>
  <div class="delivery-confirm-page">
    <div class="container">
      <el-card shadow="always" class="main-card">
        <template #header>
          <div class="header-title">
            <el-icon class="icon"><Van /></el-icon>
            <h2>Xác Nhận Giao Hàng</h2>
          </div>
        </template>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="!detail" class="error-state">
          <el-result icon="error" title="Không tìm thấy đơn hàng" sub-title="Liên kết không hợp lệ hoặc đơn hàng không tồn tại." />
        </div>

        <div v-else-if="detail.status !== 'SHIPPING'" class="error-state">
          <el-result icon="warning" title="Trạng thái không hợp lệ" sub-title="Đơn hàng này không ở trạng thái Đang Giao Hàng." />
        </div>

        <div v-else-if="detail.deliveryProofUrl" class="success-state">
          <el-result icon="success" title="Đã tải ảnh lên" sub-title="Đơn hàng đang chờ khách hàng xác nhận nhận hàng." />
          <el-image :src="detail.deliveryProofUrl" fit="contain" class="proof-preview" />
        </div>

        <div v-else class="content">
          <el-alert
            title="Đơn hàng đang giao"
            type="info"
            description="Vui lòng chụp ảnh gói hàng tại nhà khách (có thể gồm khách hàng hoặc cửa nhà) để làm bằng chứng giao hàng thành công."
            show-icon
            :closable="false"
            class="mb-4"
          />

          <el-descriptions :column="1" border class="mb-4">
            <el-descriptions-item label="Mã Đơn">{{ detail.orderNumber || `#${orderId}` }}</el-descriptions-item>
            <el-descriptions-item label="Khách Hàng">{{ detail.customerName }}</el-descriptions-item>
            <el-descriptions-item label="SĐT">{{ detail.customerPhone }}</el-descriptions-item>
            <el-descriptions-item label="Địa Chỉ">{{ detail.shippingAddress }}</el-descriptions-item>
            <el-descriptions-item label="COD" v-if="detail.paymentMethod === 'CASH'">
              <strong style="color: var(--el-color-danger)">{{ formatMoney(detail.totalAmount) }}</strong>
            </el-descriptions-item>
          </el-descriptions>

          <div class="upload-section">
            <div class="upload-title">Tải lên Ảnh Bằng Chứng <span class="required">*</span></div>
            <el-upload
              class="proof-uploader"
              action="#"
              list-type="picture-card"
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              accept="image/*"
            >
              <el-icon><Camera /></el-icon>
            </el-upload>
            <div class="upload-tip">Nhấn vào biểu tượng camera để chụp ảnh hoặc chọn ảnh từ thư viện.</div>
          </div>

          <div v-if="detail.paymentMethod === 'CASH'" class="mb-4" style="margin-top: 16px;">
            <el-checkbox v-model="collectedCash" size="large" border class="checkbox-full">
              Tôi đã thu đủ <strong style="color: var(--el-color-danger)">{{ formatMoney(detail.totalAmount) }}</strong> tiền mặt
            </el-checkbox>
          </div>

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            :disabled="!selectedFile"
            @click="handleSubmit"
          >
            Gửi Ảnh Xác Nhận
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { Van, Camera } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { ordersApi } from "../../api/orders.api";
import http from "../../api/http";
import axios from "axios";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const route = useRoute();
const orderId = route.params.orderId;

const loading = ref(true);
const detail = ref(null);
const selectedFile = ref(null);
const submitting = ref(false);
const collectedCash = ref(false);

function formatMoney(val) {
  if (!val) return "0 ₫";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(val);
}

async function fetchOrder() {
  loading.value = true;
  try {
    // Dùng axios thuần thay vì http.js để KHÔNG bị chặn bởi interceptor đá văng ra login
    const res = await axios.get(`${API_BASE}/api/public/orders/${orderId}/delivery-info`);
    detail.value = res.data?.data || res.data;
  } catch (err) {
    console.error("Lỗi lấy đơn hàng:", err);
    ElMessage.error("Không thể lấy thông tin đơn hàng, vui lòng kiểm tra lại đường dẫn.");
  } finally {
    loading.value = false;
  }
}

function handleFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw;
}

function handleFileRemove() {
  selectedFile.value = null;
}

async function handleSubmit() {
  if (!selectedFile.value) {
    ElMessage.warning("Vui lòng chọn ảnh bằng chứng");
    return;
  }

  if (detail.value?.paymentMethod === 'CASH' && !collectedCash.value) {
    ElMessage.warning("Vui lòng đánh dấu xác nhận đã thu đủ tiền mặt từ khách hàng!");
    return;
  }

  submitting.value = true;
  try {
    // Gửi thẳng file ảnh lên API upload-proof của order
    const formData = new FormData();
    formData.append("image", selectedFile.value); // Backend: @RequestPart("image")
    
    await axios.put(`${API_BASE}/api/orders/public/${orderId}/upload-proof`, formData, {
      headers: { "Content-Type": "multipart/form-data" }
    });

    ElMessage.success("Đã gửi bằng chứng thành công!");
    await fetchOrder(); // Tải lại để hiện success-state
  } catch (error) {
    console.error("Lỗi upload:", error);
    ElMessage.error("Có lỗi xảy ra khi tải ảnh lên. Vui lòng thử lại.");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  if (orderId) {
    fetchOrder();
  }
});
</script>

<style scoped>
.delivery-confirm-page {
  min-height: 100vh;
  background-color: var(--el-bg-color-page);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 20px 16px;
}

.container {
  width: 100%;
  max-width: 500px; /* Phù hợp với màn hình điện thoại */
}

.main-card {
  border-radius: 12px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--el-color-primary);
}

.header-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.icon {
  font-size: 24px;
}

.mb-4 {
  margin-bottom: 16px;
}

.upload-section {
  margin: 24px 0;
  text-align: center;
}

.upload-title {
  font-weight: 600;
  margin-bottom: 12px;
  font-size: 15px;
}

.required {
  color: var(--el-color-danger);
}

.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

.proof-uploader :deep(.el-upload--picture-card) {
  width: 150px;
  height: 150px;
  line-height: 150px;
}

.proof-uploader :deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 150px;
  height: 150px;
}

.submit-btn {
  width: 100%;
  font-weight: 600;
}

.proof-preview {
  width: 100%;
  max-height: 300px;
  border-radius: 8px;
  margin-top: 16px;
}

.checkbox-full {
  width: 100%;
  margin-right: 0;
  display: flex;
  white-space: normal;
  height: auto;
  padding: 12px 15px;
  align-items: center;
}

.checkbox-full :deep(.el-checkbox__label) {
  white-space: normal;
  line-height: 1.4;
}
</style>
