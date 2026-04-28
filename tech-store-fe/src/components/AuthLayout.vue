<template>
  <div class="auth">
    <header class="topbar">
      <div
        class="container-xl d-flex align-items-center justify-content-between gap-3"
      >
        <div class="logo d-flex align-items-center gap-2">
          <span class="logo__dot"></span>
          <div class="logo__txt">
            <div class="logo__text">Tech Store</div>
            <div class="logo__sub">Secure Access</div>
          </div>
        </div>

        <span class="badge ts-pill" :class="tagClass">{{ tagText }}</span>
      </div>
    </header>

    <div class="container-xl pb-4">
      <div class="shell">
        <section class="promo">
          <div class="promo__card">
            <div class="kicker">{{ kicker }}</div>
            <h1 class="headline">{{ headline }}</h1>
            <p class="desc">{{ desc }}</p>
            <p v-if="hint" class="hint">{{ hint }}</p>

            <div class="promo__badges d-flex flex-wrap gap-2 mt-3">
              <span class="badge text-bg-light border ts-pill"
                >Bảo mật JWT</span
              >
              <span class="badge text-bg-light border ts-pill"
                >Role-based Routing</span
              >
              <span class="badge text-bg-light border ts-pill"
                >Bootstrap 5 UI</span
              >
            </div>

            <div v-if="showPortalSwitch" class="switch mt-3">
              <div
                class="btn-group w-100"
                role="group"
                aria-label="Portal switch"
              >
                <button
                  type="button"
                  class="btn"
                  :class="
                    seg === 'customer' ? 'btn-primary' : 'btn-outline-primary'
                  "
                  @click="choose('customer')"
                >
                  Customer
                </button>
                <button
                  type="button"
                  class="btn"
                  :class="
                    seg === 'system' ? 'btn-success' : 'btn-outline-success'
                  "
                  @click="choose('system')"
                >
                  System
                </button>
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="card ts-card panel__card">
            <div class="card-header bg-transparent border-0 pt-4 px-4">
              <div class="panel__hdr">
                <div class="panel__title">{{ title }}</div>
                <div class="panel__sub">
                  Vui lòng nhập thông tin để tiếp tục
                </div>
              </div>
            </div>

            <div class="card-body px-4 pb-4">
              <slot />
            </div>
          </div>

          <div class="panel__foot">
            <span class="ts-muted"
              >© {{ new Date().getFullYear() }} Tech Store</span
            >
            <span class="dot">•</span>
            <span class="ts-muted">Premium Laptop Commerce UI</span>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watchEffect, computed } from "vue";

const props = defineProps({
  currentPortal: { type: String, default: "customer" },
  title: { type: String, default: "" },
  desc: { type: String, default: "" },
  hint: { type: String, default: "" },
  tagText: { type: String, default: "Portal" },
  tagType: { type: String, default: "info" },
  kicker: { type: String, default: "" },
  headline: { type: String, default: "" },
  onSwitch: { type: Function, default: null },
  showPortalSwitch: { type: Boolean, default: false },
});

const seg = ref(props.currentPortal);
watchEffect(() => (seg.value = props.currentPortal));

const tagClass = computed(() => {
  const t = (props.tagType || "").toLowerCase();
  if (t === "success") return "text-bg-success";
  if (t === "primary") return "text-bg-primary";
  if (t === "warning") return "text-bg-warning";
  if (t === "danger") return "text-bg-danger";
  return "text-bg-secondary";
});

function choose(v) {
  seg.value = v;
  props.onSwitch?.(v);
}
</script>

<style scoped>
.auth {
  min-height: 100vh;
  padding-bottom: 18px;
  background: radial-gradient(
      1100px 520px at 10% -15%,
      rgba(37, 99, 235, 0.18),
      transparent 60%
    ),
    radial-gradient(
      900px 520px at 95% 0%,
      rgba(6, 182, 212, 0.14),
      transparent 55%
    ),
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(246, 247, 251, 1));
}

.topbar {
  padding: 16px 0;
}

.logo__dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--ts-primary);
  box-shadow: 0 0 18px rgba(37, 99, 235, 0.35);
}
.logo__txt {
  line-height: 1.05;
}
.logo__text {
  font-weight: 900;
}
.logo__sub {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.55);
  font-weight: 600;
}

.shell {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}
@media (max-width: 920px) {
  .shell {
    grid-template-columns: 1fr;
  }
}

.promo__card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: 0 10px 26px rgba(2, 6, 23, 0.06);
}

.kicker {
  font-size: 12px;
  opacity: 0.75;
  font-weight: 900;
  letter-spacing: 0.2px;
  text-transform: uppercase;
}
.headline {
  margin: 10px 0 8px;
  font-size: 30px;
  letter-spacing: -0.4px;
}
.desc {
  margin: 0;
  opacity: 0.9;
  line-height: 1.6;
}
.hint {
  margin: 10px 0 0;
  opacity: 0.7;
  font-size: 13px;
  line-height: 1.55;
}

.panel__hdr {
  display: grid;
  gap: 2px;
}
.panel__title {
  font-weight: 900;
}
.panel__sub {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.58);
}

.panel__foot {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
}
.dot {
  opacity: 0.35;
}
</style>
