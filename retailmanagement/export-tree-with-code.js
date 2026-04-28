// Chạy:
//   node export-tree-with-code.js

const fs = require("fs");
const path = require("path");
const readline = require("readline");

const ROOT_DIR = "src/main/java/com/retailmanagement";
const DEFAULT_OUTPUT = "project-tree-with-code.txt";
const IGNORE_DIRS = new Set([
  "node_modules",
  ".git",
  "target",
  ".idea",
  ".vscode",
]);

const BASE_PACKAGE = "com.retailmanagement.";

/**
 * Preset theo structure mới.
 * Menu chỉ còn:
 * 1) All
 * 6) Pricing & Promotion
 */
const MODULE_PRESETS = {
  all: {
    key: "1",
    label: "All",
    mode: "all",
    seedFiles: [],
  },

  pricing: {
    key: "6",
    label: "Pricing & Promotion (Minimal)",
    mode: "seed",
    seedFiles: [
      // ─── Controllers ───────────────────────────────────────────
      "controller/PriceController.java",
      "controller/PromotionController.java",
      "controller/SettingController.java",
      "controller/ReportController.java",

      // ─── Services ──────────────────────────────────────────────
      "service/PricingService.java",
      "service/PromotionService.java",
      "service/PromotionHistoryService.java",
      "service/SettingService.java",
      "service/ReportService.java",

      // ─── Repositories ──────────────────────────────────────────
      "repository/PriceHistoryRepository.java",
      "repository/PromotionRepository.java",
      "repository/PromotionRedemptionRepository.java",
      "repository/PromotionHistoryRepository.java",
      "repository/ProductVariantRepository.java",
      "repository/SystemSettingRepository.java",
      "repository/OrderItemRepository.java",

      // ─── DTO Requests ──────────────────────────────────────────
      "dto/request/UpsertPriceRequest.java",
      "dto/request/PromotionRequest.java",
      "dto/request/SetDefaultCurrencyRequest.java",

      // ─── DTO Responses ─────────────────────────────────────────
      "dto/response/ApiResponse.java",
      "dto/response/VariantPriceResponse.java",
      "dto/response/PriceHistoryResponse.java",
      "dto/response/PromotionHistoryResponse.java",

      // ─── Entities ──────────────────────────────────────────────
      "entity/PriceHistory.java",
      "entity/Promotion.java",
      "entity/PromotionRedemption.java",
      "entity/ProductVariant.java",
      "entity/Product.java",
      "entity/Customer.java",
      "entity/Customergroup.java",
      "entity/SystemSetting.java",
      "entity/Order.java",
      "entity/OrderItem.java",

      // ─── Exceptions ────────────────────────────────────────────
      "exception/GlobalExceptionHandler.java",
    ],
  },
};

// -------------------- utils --------------------
function exists(p) {
  try {
    fs.accessSync(p);
    return true;
  } catch {
    return false;
  }
}
function normalizeAbs(p) {
  return path.normalize(path.resolve(p));
}
function isIgnoredDir(name) {
  return IGNORE_DIRS.has(name);
}
function isJavaFile(p) {
  return p.endsWith(".java");
}
function readTextSafe(p) {
  try {
    return fs.readFileSync(p, "utf8");
  } catch {
    return "";
  }
}

function listAllFilesRec(dir) {
  let out = [];
  const items = fs.readdirSync(dir, { withFileTypes: true });
  for (const it of items) {
    const full = path.join(dir, it.name);
    if (it.isDirectory()) {
      if (isIgnoredDir(it.name)) continue;
      out = out.concat(listAllFilesRec(full));
    } else {
      out.push(full);
    }
  }
  return out;
}

function parseInternalImports(javaContent) {
  const imports = new Set();
  const re = /^\s*import\s+(?:static\s+)?(com\.retailmanagement\.[\w.]*\*?);/gm;
  let m;
  while ((m = re.exec(javaContent)) !== null) imports.add(m[1]);
  return [...imports];
}

function packageToJavaPath(importName) {
  if (!importName.startsWith(BASE_PACKAGE)) return null;
  const rel = importName.replace(BASE_PACKAGE, "").replace(/\./g, path.sep);
  return path.join(ROOT_DIR, rel) + ".java";
}

function wildcardToDir(importName) {
  if (!importName.endsWith(".*")) return null;
  if (!importName.startsWith(BASE_PACKAGE)) return null;
  const rel = importName
    .replace(BASE_PACKAGE, "")
    .replace(/\.\*$/, "")
    .replace(/\./g, path.sep);
  return path.join(ROOT_DIR, rel);
}

function isAncestorDir(dirAbs, fileAbs) {
  const d = dirAbs.endsWith(path.sep) ? dirAbs : dirAbs + path.sep;
  return fileAbs.startsWith(d);
}

// -------------------- build included set --------------------
function buildIncludedSet(preset) {
  const rootAbs = normalizeAbs(ROOT_DIR);
  const allFilesAbs = listAllFilesRec(ROOT_DIR).map(normalizeAbs);
  const allJavaAbs = allFilesAbs.filter(isJavaFile);

  const included = new Set();

  if (preset.mode === "all") {
    for (const f of allFilesAbs) included.add(f);
    return { included, rootAbs, allFilesAbs, allJavaAbs };
  }

  const queue = [];
  for (const rel of preset.seedFiles) {
    const abs = normalizeAbs(path.join(ROOT_DIR, rel));
    if (exists(abs)) {
      included.add(abs);
      if (isJavaFile(abs)) queue.push(abs);
    }
  }

  while (queue.length) {
    const fileAbs = queue.pop();
    const content = readTextSafe(fileAbs);
    if (!content) continue;

    const imports = parseInternalImports(content);

    for (const imp of imports) {
      if (imp.endsWith(".*")) {
        const dir = wildcardToDir(imp);
        if (!dir) continue;
        const dirAbs = normalizeAbs(dir);
        if (!exists(dirAbs)) continue;

        for (const jf of allJavaAbs) {
          if (isAncestorDir(dirAbs, jf) && !included.has(jf)) {
            included.add(jf);
            queue.push(jf);
          }
        }
      } else {
        const javaPath = packageToJavaPath(imp);
        if (!javaPath) continue;
        const depAbs = normalizeAbs(javaPath);
        if (exists(depAbs) && !included.has(depAbs)) {
          included.add(depAbs);
          queue.push(depAbs);
        }
      }
    }
  }

  return { included, rootAbs, allFilesAbs, allJavaAbs };
}

// -------------------- tree writer (compact format) --------------------
function writeTreeWithCode(rootAbs, includedSet) {
  function dirHasIncluded(dirAbs) {
    const d = dirAbs.endsWith(path.sep) ? dirAbs : dirAbs + path.sep;
    for (const f of includedSet) if (f.startsWith(d)) return true;
    return false;
  }

  function walk(dirAbs, prefix = "") {
    let items = fs
      .readdirSync(dirAbs, { withFileTypes: true })
      .filter((it) => {
        const fullAbs = normalizeAbs(path.join(dirAbs, it.name));
        if (it.isDirectory()) {
          if (isIgnoredDir(it.name)) return false;
          return dirHasIncluded(fullAbs);
        }
        return includedSet.has(fullAbs);
      })
      .sort((a, b) => a.name.localeCompare(b.name));

    let result = "";
    items.forEach((it, idx) => {
      const isLast = idx === items.length - 1;
      const pointer = isLast ? "└── " : "├── ";
      const fullAbs = normalizeAbs(path.join(dirAbs, it.name));

      if (it.isDirectory()) {
        result += `${prefix}${pointer}${it.name}\n`;
        const childPrefix = prefix + (isLast ? "    " : "│   ");
        result += walk(fullAbs, childPrefix);
      } else {
        // File: hiển thị tên file và code ngay sau, không có separator
        result += `${prefix}${pointer}${it.name}\n`;
        const content = readTextSafe(fullAbs).trimEnd(); // Bỏ trailing whitespace
        if (content) {
          const childPrefix = prefix + (isLast ? "    " : "│   ");
          result += content
            .split("\n")
            .map((line) => `${childPrefix}${line}`)
            .join("\n");
          result += "\n";
        }
      }
    });
    return result;
  }

  return walk(rootAbs);
}

// -------------------- CLI/menu --------------------
function parseArgs(argv) {
  const args = {};
  for (const a of argv.slice(2)) {
    if (a.startsWith("--module=")) args.module = a.split("=")[1];
    if (a.startsWith("--out=")) args.out = a.split("=")[1];
    if (a === "--help" || a === "-h") args.help = true;
  }
  return args;
}

function resolveModule(input) {
  if (!input) return null;
  const normalized = String(input).trim().toLowerCase();

  for (const k of Object.keys(MODULE_PRESETS)) {
    if (MODULE_PRESETS[k].key === normalized) return k;
  }

  if (MODULE_PRESETS[normalized]) return normalized;

  return null;
}

function printHelp() {
  console.log(`
Usage:
  node export-tree-with-code.js
  node export-tree-with-code.js --module=6
  node export-tree-with-code.js --module=pricing --out=pricing.txt

Modules:
  1. All
  6. Pricing & Promotion
`);
}

async function askMenu() {
  console.log("Chọn module để export:");
  const entries = Object.entries(MODULE_PRESETS).sort(
    (a, b) => Number(a[1].key) - Number(b[1].key),
  );
  for (const [, v] of entries) console.log(`${v.key}. ${v.label}`);

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });
  const answer = await new Promise((res) =>
    rl.question("Nhập lựa chọn (1 hoặc 6): ", res),
  );
  rl.close();
  return answer;
}

// -------------------- main --------------------
(async function main() {
  const args = parseArgs(process.argv);
  if (args.help) {
    printHelp();
    process.exit(0);
  }

  if (!exists(ROOT_DIR)) {
    console.error(`ROOT_DIR not found: ${ROOT_DIR}`);
    process.exit(1);
  }

  let moduleKey = resolveModule(args.module);
  if (!moduleKey) {
    const ans = await askMenu();
    moduleKey = resolveModule(ans) || "all";
  }

  const preset = MODULE_PRESETS[moduleKey] || MODULE_PRESETS.all;

  const outFile = args.out
    ? args.out
    : moduleKey === "all"
      ? DEFAULT_OUTPUT
      : `project-tree-with-code.${moduleKey}.txt`;

  const { included, rootAbs } = buildIncludedSet(preset);
  const tree = writeTreeWithCode(rootAbs, included);

  fs.writeFileSync(outFile, tree, "utf8");
  console.log(`Exported module: ${preset.label}`);
  console.log(`Files included: ${included.size}`);
  console.log("Output:", outFile);
})();
