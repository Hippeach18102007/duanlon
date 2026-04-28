package com.retailmanagement.service;

import com.retailmanagement.entity.Order;
import com.retailmanagement.entity.OrderItem;
import com.resend.Resend;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrderEmailService {

    private final PdfService pdfService;

    @Value("${resend.api.key}")
    private String apiKey;

    @Async
    public void sendDeliveredEmail(Order order) {
        try {
            if (order.getCustomer() == null ||
                    order.getCustomer().getEmail() == null) return;

            byte[] pdf = pdfService.generateOrderPdf(order);

            String htmlContent = buildDeliveredEmail(order);

            Resend resend = new Resend(apiKey);

            Attachment attachment = Attachment.builder()
                    .fileName("invoice-" + order.getOrderNumber() + ".pdf")
                    .content(Base64.getEncoder().encodeToString(pdf))
                    .build();

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(order.getCustomer().getEmail())
                    .subject("Don hang da giao thanh cong #" + order.getOrderNumber())
                    .html(htmlContent)
                    .attachments(new Attachment[]{attachment})
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(v);
    }

    private boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private String safe(String s, String fallback) {
        return (s != null && !s.isBlank()) ? s : fallback;
    }

    private String buildItemRows(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getOrderItems()) {
            sb.append("""
              <tr>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;font-size:13px;color:#222;">
                  %s
                </td>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;text-align:center;
                            font-size:13px;color:#555;white-space:nowrap;">%d</td>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;text-align:right;
                            font-size:13px;color:#555;white-space:nowrap;">%s d</td>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;text-align:right;
                            font-size:13px;font-weight:bold;color:#222;white-space:nowrap;">%s d</td>
              </tr>
            """.formatted(
                    item.getProductName(),
                    item.getQuantity(),
                    fmt(item.getUnitPrice()),
                    fmt(item.getLineTotal())
            ));
        }
        return sb.toString();
    }

    private String buildSummaryRows(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getSubtotal() != null)
            sb.append(sRow("Tam tinh",        fmt(order.getSubtotal()) + " d",            "#555",    false));
        if (isPositive(order.getShippingFee()))
            sb.append(sRow("Phi van chuyen",  fmt(order.getShippingFee()) + " d",         "#555",    false));
        if (isPositive(order.getVipDiscount()))
            sb.append(sRow("Giam VIP",        "- " + fmt(order.getVipDiscount()) + " d",  "#2a6e2a", false));
        if (isPositive(order.getSpinDiscount()))
            sb.append(sRow("Giam Vong quay",  "- " + fmt(order.getSpinDiscount()) + " d", "#2a6e2a", false));
        if (order.getAppliedPromotionCode() != null)
            sb.append(sRow("Ma KM",           order.getAppliedPromotionCode(),             "#2a6e2a", false));
        if (isPositive(order.getDiscountTotal()))
            sb.append(sRow("Tong giam",       "- " + fmt(order.getDiscountTotal()) + " d", "#2a6e2a", true));
        return sb.toString();
    }

    private String sRow(String label, String value, String color, boolean topBorder) {
        String border = topBorder ? "border-top:1px solid #ddd;padding-top:8px;" : "";
        return """
          <tr>
            <td style="padding:4px 0;font-size:13px;color:#777;%s">%s</td>
            <td style="padding:4px 0;font-size:13px;color:%s;text-align:right;%s">%s</td>
          </tr>
        """.formatted(border, label, color, border, value);
    }

    // ── Main HTML builder — Delivered Email ───────────────────────────────
    private String buildDeliveredEmail(Order order) {
        String customerName  = safe(order.getCustomer().getName(),  "Khach hang");
        String customerEmail = safe(order.getCustomer().getEmail(), "—");
        String customerPhone = safe(order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : null, "—");
        String customerAddr  = safe(order.getCustomer().getAddress() != null ? order.getCustomer().getAddress() : null, "—");
        String paymentMethod = safe(order.getPaymentMethod(), "—");

        String deliveredAt = order.getUpdatedAt() != null
                ? order.getUpdatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
                : "—";

        String itemRows    = buildItemRows(order);
        String summaryRows = buildSummaryRows(order);
        String total       = fmt(order.getTotalAmount());
        String invoiceName = "invoice-" + order.getOrderNumber() + ".pdf";
        String orderNote   = order.getNotes() != null && !order.getNotes().isBlank()
                ? "<tr><td colspan=\"2\" style=\"padding:6px 0 2px;\"><div style=\"border-left:3px solid #aaa;padding:8px 12px;background:#f9f9f9;font-size:13px;color:#555;\"><strong>Ghi chu:</strong> " + order.getNotes() + "</div></td></tr>"
                : "";

        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
          <title>Don hang da giao</title>
        </head>
        <body style="margin:0;padding:0;background:#f2f2f2;font-family:Arial,Helvetica,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;padding:30px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="max-width:600px;width:100%%;background:#ffffff;border:1px solid #cccccc;">

                <!-- HEADER -->
                <tr>
                  <td style="background:#1a2744;padding:24px 32px;">
                    <p style="margin:0;font-size:11px;letter-spacing:3px;color:#aabbd4;
                               text-transform:uppercase;font-weight:bold;">TECHSTORE</p>
                    <p style="margin:8px 0 0;font-size:18px;font-weight:bold;color:#ffffff;">
                      Don hang da giao thanh cong
                    </p>
                  </td>
                </tr>

                <!-- STATUS BAR -->
                <tr>
                  <td style="background:#e8f4e8;border-bottom:1px solid #c3dcc3;padding:10px 32px;">
                    <p style="margin:0;font-size:13px;color:#2a6e2a;font-weight:bold;">
                      Da giao luc %s — Hoa don dinh kem trong email nay
                    </p>
                  </td>
                </tr>

                <!-- GREETING -->
                <tr>
                  <td style="padding:24px 32px 0;">
                    <p style="margin:0;font-size:14px;color:#333;">
                      Xin chao <strong>%s</strong>,
                    </p>
                    <p style="margin:10px 0 0;font-size:13px;color:#555;line-height:1.6;">
                      Don hang <strong>#%s</strong> cua ban da duoc giao thanh cong.
                      Hoa don chi tiet (<strong>%s</strong>) da duoc dinh kem trong email nay.
                    </p>
                  </td>
                </tr>

                <!-- CUSTOMER INFO -->
                <tr>
                  <td style="padding:20px 32px 0;">
                    <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                               text-transform:uppercase;color:#888;">THONG TIN KHACH HANG</p>
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="border:1px solid #ddd;border-collapse:collapse;">
                      %s
                      <tr>
                        <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                    background:#fafafa;width:40%%;border-right:1px solid #eee;">Email</td>
                        <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                    background:#fafafa;border-right:1px solid #eee;">Dien thoai</td>
                        <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                    background:#fafafa;border-right:1px solid #eee;">Dia chi giao hang</td>
                        <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:7px 12px;font-size:13px;color:#666;background:#fafafa;
                                    border-right:1px solid #eee;">Thanh toan</td>
                        <td style="padding:7px 12px;font-size:13px;color:#222;">%s</td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- ORDER ITEMS -->
                <tr>
                  <td style="padding:20px 32px 0;">
                    <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                               text-transform:uppercase;color:#888;">SAN PHAM DA DAT</p>
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="border:1px solid #ddd;border-collapse:collapse;">
                      <tr style="background:#f5f5f5;">
                        <th style="padding:8px 8px;font-size:12px;color:#555;text-align:left;
                                    border-bottom:1px solid #ddd;font-weight:bold;">San pham</th>
                        <th style="padding:8px 8px;font-size:12px;color:#555;text-align:center;
                                    border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">S.L</th>
                        <th style="padding:8px 8px;font-size:12px;color:#555;text-align:right;
                                    border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">Don gia</th>
                        <th style="padding:8px 8px;font-size:12px;color:#555;text-align:right;
                                    border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">Thanh tien</th>
                      </tr>
                      %s
                    </table>
                  </td>
                </tr>

                <!-- SUMMARY + INVOICE -->
                <tr>
                  <td style="padding:12px 32px 0;">
                    <table width="100%%" cellpadding="0" cellspacing="0">
                      <tr>
                        <!-- Invoice badge left -->
                        <td width="46%%" valign="top">
                          <table cellpadding="0" cellspacing="0"
                                 style="border:1px solid #c3dcc3;background:#f4fbf4;border-collapse:collapse;">
                            <tr>
                              <td style="padding:10px 14px;">
                                <p style="margin:0 0 2px;font-size:11px;font-weight:bold;color:#888;
                                           letter-spacing:1px;text-transform:uppercase;">Hoa don dinh kem</p>
                                <p style="margin:0;font-size:13px;color:#2a6e2a;font-weight:bold;">%s</p>
                              </td>
                            </tr>
                          </table>
                        </td>
                        <td width="8%%"></td>
                        <!-- Summary right -->
                        <td width="46%%" valign="top">
                          <table width="100%%" cellpadding="0" cellspacing="0">
                            %s
                          </table>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- TOTAL -->
                <tr>
                  <td style="padding:12px 32px 0;">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="background:#1a2744;border-collapse:collapse;">
                      <tr>
                        <td style="padding:14px 16px;font-size:13px;font-weight:bold;color:#aabbd4;
                                    text-transform:uppercase;letter-spacing:1px;">
                          Tong thanh toan
                        </td>
                        <td style="padding:14px 16px;text-align:right;">
                          <span style="font-size:20px;font-weight:bold;color:#ffffff;">%s d</span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- DIVIDER -->
                <tr>
                  <td style="padding:24px 32px 0;">
                    <hr style="border:none;border-top:1px solid #ddd;margin:0;"/>
                  </td>
                </tr>

                <!-- FOOTER -->
                <tr>
                  <td style="padding:18px 32px 28px;text-align:center;">
                    <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">TechStore</p>
                    <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                      Lien he ho tro:
                      <a href="mailto:support@nguyenduc.me" style="color:#1a2744;text-decoration:underline;">
                        support@nguyenduc.me
                      </a><br/>
                      Email nay duoc gui tu dong, vui long khong tra loi truc tiep.
                    </p>
                    <p style="margin:12px 0 0;font-size:11px;color:#bbb;">
                      (c) 2025 TechStore. All rights reserved.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(
                deliveredAt,
                customerName,
                order.getOrderNumber(),
                invoiceName,
                orderNote,
                customerEmail,
                customerPhone,
                customerAddr,
                paymentMethod,
                itemRows,
                invoiceName,
                summaryRows,
                total
        );
    }

    @Async
    public void sendAutoCompleteWarningEmail(Order order, int daysRemaining) {
        try {
            if (order.getCustomer() == null
                    || order.getCustomer().getEmail() == null) return;

            String customerName = safe(order.getCustomer().getName(), "Khach hang");
            String orderNumber  = order.getOrderNumber();

            String html = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
              <title>Xac nhan don hang</title>
            </head>
            <body style="margin:0;padding:0;background:#f2f2f2;
                         font-family:Arial,Helvetica,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background:#f2f2f2;padding:30px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="max-width:600px;width:100%%;background:#ffffff;
                                border:1px solid #cccccc;">
 
                    <!-- HEADER -->
                    <tr>
                      <td style="background:#1a2744;padding:24px 32px;">
                        <p style="margin:0;font-size:11px;letter-spacing:3px;
                                   color:#aabbd4;text-transform:uppercase;">
                          TECHSTORE
                        </p>
                        <p style="margin:6px 0 0;font-size:18px;font-weight:bold;
                                   color:#ffffff;">
                          Nhac nhan xac nhan don hang
                        </p>
                      </td>
                    </tr>
 
                    <!-- BODY -->
                    <tr>
                      <td style="padding:28px 32px;">
                        <p style="margin:0 0 12px;font-size:14px;color:#333;">
                          Xin chao <strong>%s</strong>,
                        </p>
                        <p style="margin:0 0 12px;font-size:13px;color:#555;
                                   line-height:1.7;">
                          Don hang <strong>#%s</strong> cua ban da duoc van chuyen
                          va co the da den noi.
                          <br/>
                          Neu ban da nhan duoc hang, vui long xac nhan de chung toi
                          hoan tat don hang.
                        </p>
 
                        <!-- Warning box -->
                        <table cellpadding="0" cellspacing="0" width="100%%"
                               style="border:1px solid #f5c518;background:#fffbea;
                                      border-collapse:collapse;margin:16px 0;">
                          <tr>
                            <td style="padding:14px 16px;">
                              <p style="margin:0;font-size:13px;color:#7a5800;">
                                Neu khong co xac nhan trong vong
                                <strong>1 ngay</strong>,
                                don hang se tu dong chuyen sang trang thai
                                <strong>Da giao</strong>.
                              </p>
                            </td>
                          </tr>
                        </table>
 
                        <p style="margin:0;font-size:13px;color:#555;">
                          Cam on ban da mua sam tai TechStore.
                        </p>
                      </td>
                    </tr>
 
                    <!-- FOOTER -->
                    <tr>
                      <td style="padding:18px 32px 28px;text-align:center;
                                  border-top:1px solid #ddd;">
                        <p style="margin:0 0 4px;font-size:12px;
                                   font-weight:bold;color:#555;">TechStore</p>
                        <p style="margin:0;font-size:12px;color:#999;
                                   line-height:1.7;">
                          Lien he ho tro:
                          <a href="mailto:support@nguyenduc.me"
                             style="color:#1a2744;text-decoration:underline;">
                            support@nguyenduc.me
                          </a><br/>
                          Email nay duoc gui tu dong, vui long khong tra loi.
                        </p>
                        <p style="margin:12px 0 0;font-size:11px;color:#bbb;">
                          (c) 2025 TechStore. All rights reserved.
                        </p>
                      </td>
                    </tr>
 
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(customerName, orderNumber);

            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(order.getCustomer().getEmail())
                    .subject("Xac nhan don hang #" + orderNumber + " truoc khi tu dong hoan tat")
                    .html(html)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}