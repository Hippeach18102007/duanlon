// print-project-with-code.js
// Run: node print-project-with-code.js
// Output: project-tree-with-code.txt (same folder as this script)

import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const ROOT_DIR = path.resolve(__dirname, "src");

const INCLUDE_FILES = [
  "api/http.js",
  "api/promotions.api.js",
  "api/prices.api.js",
  "pages/system/PricingManager.vue",
  "pages/system/PromotionManager.vue",
  "pages/system/SystemShell.vue",
  "router/index.js",
  "stores/auth.js",
];

const OUTPUT_FILE = path.resolve(__dirname, "project-tree-with-code.txt");

let out = "";

for (const relativePath of INCLUDE_FILES) {
  const absolutePath = path.join(ROOT_DIR, relativePath);
  out += `FILE: src/${relativePath}\n`;
  if (!fs.existsSync(absolutePath)) {
    out += `FILE_NOT_FOUND\n`;
    continue;
  }
  out += fs.readFileSync(absolutePath, "utf8");
}

fs.writeFileSync(OUTPUT_FILE, out, "utf8");
