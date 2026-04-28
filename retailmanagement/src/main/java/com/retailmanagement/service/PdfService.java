package com.retailmanagement.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.retailmanagement.entity.Order;
import com.retailmanagement.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class PdfService {

    // ── Màu sắc ───────────────────────────────────────────────────────────
    private static final Color DARK   = new Color(0x0f, 0x17, 0x2a);
    private static final Color MUTED  = new Color(0x64, 0x74, 0x8b);
    private static final Color BORDER = new Color(0xe2, 0xe8, 0xf0);
    private static final Color GREEN  = new Color(0x16, 0xa3, 0x4a);
    private static final Color LIGHT  = new Color(0xf8, 0xfa, 0xfc);
    private static final Color BLUE   = new Color(0x0e, 0xa5, 0xe9);

    // ── Font: ưu tiên resources → tải về temp → fallback ─────────────────
    // Để bỏ phụ thuộc mạng: đặt file vào src/main/resources/fonts/
    //   DejaVuSans.ttf  +  DejaVuSans-Bold.ttf
    // Tải tại: https://dejavu-fonts.github.io
    private static final String URL_REGULAR =
            "https://github.com/dejavu-fonts/dejavu-fonts/raw/master/ttf/DejaVuSans.ttf";
    private static final String URL_BOLD =
            "https://github.com/dejavu-fonts/dejavu-fonts/raw/master/ttf/DejaVuSans-Bold.ttf";

    private volatile BaseFont cachedRegular;
    private volatile BaseFont cachedBold;

    private BaseFont bf(boolean bold) {
        if (bold  && cachedBold    != null) return cachedBold;
        if (!bold && cachedRegular != null) return cachedRegular;

        BaseFont result = fromResources(bold);
        if (result == null) result = fromCacheOrDownload(bold);
        if (result == null) result = fallback();

        if (bold) cachedBold = result; else cachedRegular = result;
        return result;
    }

    private BaseFont fromResources(boolean bold) {
        String res = bold ? "/fonts/DejaVuSans-Bold.ttf" : "/fonts/DejaVuSans.ttf";
        try (InputStream is = getClass().getResourceAsStream(res)) {
            if (is == null) return null;
            byte[] b = is.readAllBytes();
            return BaseFont.createFont(res, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, b, null);
        } catch (Exception e) { return null; }
    }

    private BaseFont fromCacheOrDownload(boolean bold) {
        String name  = bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf";
        Path   cache = Path.of(System.getProperty("java.io.tmpdir"), "techstore_fonts", name);
        try {
            if (!Files.exists(cache)) {
                Files.createDirectories(cache.getParent());
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder(URI.create(bold ? URL_BOLD : URL_REGULAR)).GET().build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() != 200) return null;
                Files.write(cache, resp.body());
            }
            byte[] b = Files.readAllBytes(cache);
            return BaseFont.createFont(cache.toString(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, b, null);
        } catch (Exception e) {
            System.err.println("[PdfService] Failed to load DejaVu font: " + e.getMessage());
            return null;
        }
    }

    private BaseFont fallback() {
        try { return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Font font(boolean bold, float size, Color color) {
        Font f = new Font(bf(bold), size, bold ? Font.BOLD : Font.NORMAL);
        f.setColor(color);
        return f;
    }

    private String money(BigDecimal v) {
        if (v == null) return "0";
        return NumberFormat.getInstance(new Locale("en", "US")).format(v);
    }

    private boolean pos(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private String safe(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    // ── PDF generator ─────────────────────────────────────────────────────
    public byte[] generateOrderPdf(Order order) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Font definitions
            Font fBrand   = font(true,  18,    DARK);
            Font fSubBrand= font(false, 9,     MUTED);
            Font fSection = font(true,  8,     MUTED);
            Font fLabel   = font(false, 9,     MUTED);
            Font fValue   = font(true,  9,     DARK);
            Font fStatus  = font(true,  9,     GREEN);
            Font fTh      = font(true,  8,     MUTED);
            Font fTd      = font(false, 9,     DARK);
            Font fTdMuted = font(false, 9,     MUTED);
            Font fSumL    = font(false, 9,     MUTED);
            Font fSumV    = font(false, 9,     DARK);
            Font fDisc    = font(false, 9,     GREEN);
            Font fTotalL  = font(true,  10,    Color.WHITE);
            Font fTotalV  = font(true,  13,    Color.WHITE);
            Font fNote    = font(false, 8.5f,  DARK);
            Font fNoteL   = font(true,  8.5f,  MUTED);
            Font fFooter  = font(false, 7.5f,  MUTED);

            float m = 52;
            Document doc = new Document(PageSize.A4, m, m, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            String date = order.getCreatedAt() != null
                    ? order.getCreatedAt().atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
                    : "—";

            // ── 1. HEADER ────────────────────────────────────────────────
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setSpacingAfter(4);

            PdfPCell brandCell = bare(new Phrase("TechStore", fBrand), Element.ALIGN_LEFT);
            PdfPCell tagCell   = bare(new Phrase("HOA DON BAN HANG", fSubBrand), Element.ALIGN_RIGHT);
            tagCell.setPaddingTop(8);
            header.addCell(brandCell);
            header.addCell(tagCell);
            doc.add(header);

            hr(doc, DARK, 1.5f, 20);

            // ── 2. THÔNG TIN ĐƠN HÀNG ───────────────────────────────────
            section(doc, "THONG TIN DON HANG", fSection);
            infoRow(doc, "Ma don hang",     "#" + order.getOrderNumber(), fLabel, fValue);
            infoRow(doc, "Ngay tao",         date,                         fLabel, fSumV);
            infoRow(doc, "Trang thai",        safe(order.getStatus()),      fLabel, fStatus);
            infoRow(doc, "Thanh toan",        safe(order.getPaymentMethod()), fLabel, fValue);
            infoRow(doc, "Dia chi giao hang",
                    safe(order.getShippingAddress()), fLabel, fValue);
            if (order.getAppliedPromotionCode() != null)
                infoRow(doc, "Ma khuyen mai", order.getAppliedPromotionCode(), fLabel, fDisc);

            // ── 3. THÔNG TIN KHÁCH HÀNG ─────────────────────────────────
            spacer(doc, 16);
            section(doc, "THONG TIN KHACH HANG", fSection);
            if (order.getCustomer() != null) {
                infoRow(doc, "Ho ten",       safe(order.getCustomer().getName()),    fLabel, fValue);
                infoRow(doc, "Email",         safe(order.getCustomer().getEmail()),   fLabel, fSumV);
                infoRow(doc, "Dien thoai",    safe(order.getCustomer().getPhone()),   fLabel, fValue);
                infoRow(doc, "Dia chi",       safe(order.getCustomer().getAddress()), fLabel, fValue);
            }

            // ── 4. BẢNG SẢN PHẨM ────────────────────────────────────────
            spacer(doc, 16);
            section(doc, "SAN PHAM", fSection);

            PdfPTable tbl = new PdfPTable(4);
            tbl.setWidthPercentage(100);
            tbl.setWidths(new float[]{4.5f, 0.8f, 1.9f, 1.9f});
            tbl.setSpacingAfter(0);

            // Table header
            String[] ths = {"SAN PHAM", "SL", "DON GIA", "THANH TIEN"};
            int[]    als = {Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                    Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};
            for (int i = 0; i < 4; i++) {
                PdfPCell th = new PdfPCell(new Phrase(ths[i], fTh));
                th.setHorizontalAlignment(als[i]);
                th.setBackgroundColor(LIGHT);
                th.setPaddingTop(8); th.setPaddingBottom(8);
                th.setPaddingLeft(i == 0 ? 8 : 0);
                th.setPaddingRight(i == 3 ? 8 : 0);
                th.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
                th.setBorderColorTop(BORDER); th.setBorderColorBottom(BORDER);
                th.setBorderWidthTop(0.75f);  th.setBorderWidthBottom(0.75f);
                tbl.addCell(th);
            }

            // Table rows
            for (OrderItem item : order.getOrderItems()) {
                tbl.addCell(tdLeft(item.getProductName(), fTd));
                tbl.addCell(tdCenter(String.valueOf(item.getQuantity()), fTdMuted));
                tbl.addCell(tdRight(money(item.getUnitPrice()), fTdMuted));
                tbl.addCell(tdRight(money(item.getLineTotal()), fTd));
            }
            doc.add(tbl);

            // ── 5. TÓM TẮT CHI PHÍ ──────────────────────────────────────
            spacer(doc, 8);
            if (order.getSubtotal() != null)
                sumLine(doc, "Tam tinh",        money(order.getSubtotal()),            fSumL, fSumV);
            if (pos(order.getShippingFee()))
                sumLine(doc, "Phi van chuyen",  money(order.getShippingFee()),          fSumL, fSumV);
            if (pos(order.getVipDiscount()))
                sumLine(doc, "Giam VIP",        "- " + money(order.getVipDiscount()),   fSumL, fDisc);
            if (pos(order.getSpinDiscount()))
                sumLine(doc, "Giam Vong quay",  "- " + money(order.getSpinDiscount()),  fSumL, fDisc);
            if (pos(order.getDiscountTotal()))
                sumLine(doc, "Tong giam",       "- " + money(order.getDiscountTotal()), fSumL, fDisc);

            // ── 6. THANH TỔNG ────────────────────────────────────────────
            spacer(doc, 12);
            PdfPTable totalBar = new PdfPTable(2);
            totalBar.setWidthPercentage(100);
            totalBar.setSpacingAfter(24);

            PdfPCell tl = new PdfPCell(new Phrase("TONG THANH TOAN", fTotalL));
            tl.setBackgroundColor(DARK); tl.setBorder(Rectangle.NO_BORDER);
            tl.setHorizontalAlignment(Element.ALIGN_LEFT);
            tl.setPadding(14); tl.setPaddingLeft(16);

            PdfPCell tv = new PdfPCell(new Phrase(money(order.getTotalAmount()), fTotalV));
            tv.setBackgroundColor(DARK); tv.setBorder(Rectangle.NO_BORDER);
            tv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tv.setPadding(14); tv.setPaddingRight(16);

            totalBar.addCell(tl);
            totalBar.addCell(tv);
            doc.add(totalBar);

            // ── 7. GHI CHÚ ───────────────────────────────────────────────
            if (order.getNotes() != null && !order.getNotes().isBlank()) {
                PdfPTable noteT = new PdfPTable(1);
                noteT.setWidthPercentage(100);
                noteT.setSpacingAfter(16);
                Phrase noteP = new Phrase();
                noteP.add(new Chunk("Ghi chu: ", fNoteL));
                noteP.add(new Chunk(order.getNotes(), fNote));
                PdfPCell nc = new PdfPCell(noteP);
                nc.setBorder(Rectangle.LEFT);
                nc.setBorderColorLeft(BLUE); nc.setBorderWidthLeft(2.5f);
                nc.setBackgroundColor(new Color(0xf0, 0xf9, 0xff));
                nc.setPadding(10); nc.setPaddingLeft(12);
                noteT.addCell(nc);
                doc.add(noteT);
            }

            // ── 8. FOOTER ────────────────────────────────────────────────
            hr(doc, BORDER, 0.75f, 0);
            PdfPTable footer = new PdfPTable(1);
            footer.setWidthPercentage(100);
            PdfPCell fc = new PdfPCell(
                    new Phrase("TechStore  ·  support@nguyenduc.me  ·  Cam on ban da mua hang!", fFooter));
            fc.setHorizontalAlignment(Element.ALIGN_CENTER);
            fc.setBorder(Rectangle.NO_BORDER);
            fc.setPaddingTop(10); fc.setPaddingBottom(4);
            footer.addCell(fc);
            doc.add(footer);

            doc.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    // ── Layout building blocks ────────────────────────────────────────────

    /** Section header with underline */
    private void section(Document doc, String text, Font font) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(2);
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(BORDER); c.setBorderWidthBottom(0.75f);
        c.setBackgroundColor(Color.WHITE);
        c.setPaddingBottom(6); c.setPaddingLeft(0); c.setPaddingTop(0);
        t.addCell(c);
        doc.add(t);
    }

    /** Label → value row with thin bottom border */
    private void infoRow(Document doc, String label, String value,
                         Font lf, Font vf) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.6f, 2.4f});
        t.setSpacingAfter(0);

        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setHorizontalAlignment(Element.ALIGN_LEFT);
        lc.setBackgroundColor(Color.WHITE);
        lc.setPaddingTop(7); lc.setPaddingBottom(7);
        lc.setPaddingLeft(0); lc.setPaddingRight(8);
        lc.setBorder(Rectangle.BOTTOM);
        lc.setBorderColorBottom(BORDER); lc.setBorderWidthBottom(0.4f);

        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBackgroundColor(Color.WHITE);
        vc.setPaddingTop(7); vc.setPaddingBottom(7);
        vc.setPaddingLeft(8); vc.setPaddingRight(0);
        vc.setBorder(Rectangle.BOTTOM);
        vc.setBorderColorBottom(BORDER); vc.setBorderWidthBottom(0.4f);

        t.addCell(lc);
        t.addCell(vc);
        doc.add(t);
    }

    /** Summary cost line, right-aligned */
    private void sumLine(Document doc, String label, String value,
                         Font lf, Font vf) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(48);
        t.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.setSpacingAfter(0);

        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(Color.WHITE);
        lc.setPaddingTop(4); lc.setPaddingBottom(4);
        lc.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(Color.WHITE);
        vc.setPaddingTop(4); vc.setPaddingBottom(4);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);

        t.addCell(lc);
        t.addCell(vc);
        doc.add(t);
    }

    private void hr(Document doc, Color color, float w, float after) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(after);
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(color); c.setBorderWidthBottom(w);
        c.setPadding(0);
        t.addCell(c);
        doc.add(t);
    }

    private void spacer(Document doc, float h) throws DocumentException {
        doc.add(new Paragraph(" ", font(false, h / 2.8f, Color.WHITE)));
    }

    private PdfPCell bare(Phrase ph, int align) {
        PdfPCell c = new PdfPCell(ph);
        c.setHorizontalAlignment(align);
        c.setBackgroundColor(Color.WHITE);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    private PdfPCell tdLeft(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setBackgroundColor(Color.WHITE);
        c.setPaddingTop(9); c.setPaddingBottom(9);
        c.setPaddingLeft(8); c.setPaddingRight(0);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(BORDER); c.setBorderWidthBottom(0.4f);
        return c;
    }

    private PdfPCell tdCenter(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBackgroundColor(Color.WHITE);
        c.setPaddingTop(9); c.setPaddingBottom(9);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(BORDER); c.setBorderWidthBottom(0.4f);
        return c;
    }

    private PdfPCell tdRight(String text, Font f) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBackgroundColor(Color.WHITE);
        c.setPaddingTop(9); c.setPaddingBottom(9);
        c.setPaddingLeft(0); c.setPaddingRight(8);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColorBottom(BORDER); c.setBorderWidthBottom(0.4f);
        return c;
    }
}