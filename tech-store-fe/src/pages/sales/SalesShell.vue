<template>
  <el-container style="height: 100vh; overflow: hidden;">

    <!-- ═══════════════════════════════════
         SIDEBAR
    ═══════════════════════════════════ -->
    <el-aside
      width="256px"
      style="position: fixed; top: 0; left: 0; height: 100vh; background: #0f172a;
             display: flex; flex-direction: column; overflow: hidden;
             border-right: 1px solid rgba(255,255,255,0.06); z-index: 100;"
    >
      <!-- Brand -->
      <el-button
        text
        style="padding: 20px 20px 16px; height: auto; justify-content: flex-start; width: 100%;"
      >
        <el-space :size="10">
          <el-avatar
            :size="36"
            shape="square"
            style="background: linear-gradient(135deg, #f59e0b, #d97706); border-radius: 8px; flex-shrink: 0;"
          >
            <el-icon :size="18"><ShoppingBag /></el-icon>
          </el-avatar>
          <el-space direction="vertical" :size="1">
            <el-text tag="b" style="color: #f8fafc; font-size: 15px; letter-spacing: -0.3px;">TechStore</el-text>
            <el-text size="small" style="color: #f59e0b; font-weight: 700; letter-spacing: 0.5px;">Sales POS</el-text>
          </el-space>
        </el-space>
      </el-button>

      <!-- User pill -->
      <el-card
        shadow="never"
        style="margin: 0 14px 12px; background: rgba(255,255,255,0.05);
               border: 1px solid rgba(255,255,255,0.08); border-radius: 8px;"
        :body-style="{ padding: '9px 11px' }"
      >
        <el-space :size="9">
          <el-avatar
            :size="30"
            shape="square"
            style="background: linear-gradient(135deg, #f59e0b, #d97706); border-radius: 7px;
                   flex-shrink: 0; font-size: 10px; font-weight: 700;"
          >
            {{ initials(auth.displayName) }}
          </el-avatar>
          <el-space direction="vertical" :size="3" style="min-width: 0;">
            <el-text
              size="small"
              style="color: #e2e8f0; font-weight: 600; display: block;
                     overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 148px;"
            >{{ auth.displayName }}</el-text>
            <el-tag
              size="small"
              effect="plain"
              style="color: #f59e0b; background: rgba(245,158,11,0.1);
                     border-color: rgba(245,158,11,0.25); font-size: 9px; letter-spacing: 0.8px;
                     height: 18px; padding: 0 6px;"
            >SALES</el-tag>
          </el-space>
        </el-space>
      </el-card>

      <el-divider style="margin: 0 0 4px; border-color: rgba(255,255,255,0.06);" />

      <!-- Scrollable menu -->
      <el-scrollbar style="flex: 1;">
        <el-menu
          router
          background-color="#0f172a"
          text-color="#94a3b8"
          active-text-color="#fbbf24"
          style="border: none; padding: 0 8px;"
        >
          <!-- Bán hàng -->
          <el-menu-item-group title="BÁN HÀNG">
            <el-menu-item index="/sales/pos">
              <el-icon><Monitor /></el-icon>
              <span>Bán hàng tại quầy</span>
            </el-menu-item>

            <el-menu-item index="/sales/pickup">
              <el-icon><Box /></el-icon>
              <span>Nhận hàng tại quầy</span>
            </el-menu-item>
          </el-menu-item-group>

          <!-- Khách hàng -->
          <el-menu-item-group title="KHÁCH HÀNG">
            <el-menu-item index="/sales/customers">
              <el-icon><UserFilled /></el-icon>
              <template #title>
                <el-space :size="6" style="width: 100%; justify-content: space-between;">
                  <span>Quản lý khách hàng</span>
                </el-space>
              </template>
            </el-menu-item>
          </el-menu-item-group>

          <!-- Trả hàng -->
          <el-menu-item-group title="TRẢ HÀNG">
            <el-menu-item index="/sales/returns">
              <el-icon><RefreshLeft /></el-icon>
              <span>Trả hàng tại quầy</span>
            </el-menu-item>
          </el-menu-item-group>
        </el-menu>
      </el-scrollbar>

      <!-- Sidebar footer -->
      <el-space
        direction="vertical"
        fill
        :size="6"
        style="padding: 10px 14px 14px; border-top: 1px solid rgba(255,255,255,0.06);"
      >
        <el-button type="danger" plain @click="doLogout" style="width: 100%;">
          <el-icon><SwitchButton /></el-icon>
          Đăng xuất
        </el-button>
      </el-space>
    </el-aside>

    <!-- ═══════════════════════════════════
         MAIN CONTENT
    ═══════════════════════════════════ -->
    <el-main :style="`margin-left: 256px; min-height: 100vh; overflow: auto; padding: ${mainPadding};`">
      <router-view />
    </el-main>

  </el-container>
</template>

<script setup>
import { Box, Files, Monitor, RefreshLeft, ShoppingBag, SwitchButton, UserFilled } from "@element-plus/icons-vue";
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../../stores/auth";
import { confirmModal } from "../../ui/confirm";
import { toast } from "../../ui/toast";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const mainPadding = computed(() => route.name === "sales-pos" ? "0" : "32px 40px 60px");

function initials(name = "") {
  return (
    (name || "")
      .split(" ")
      .map((w) => w[0] || "")
      .slice(0, 2)
      .join("")
      .toUpperCase() || "SL"
  );
}

async function doLogout() {
  const ok = await confirmModal(
    "Bạn có chắc muốn đăng xuất?",
    "Đăng xuất",
    "Đăng xuất",
    true,
  );
  if (ok) {
    auth.clearSession();
    toast("Đã đăng xuất thành công.", "success");
    router.replace("/sales/login");
  }
}
</script>

<style></style>