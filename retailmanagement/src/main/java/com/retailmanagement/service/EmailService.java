package com.retailmanagement.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.retailmanagement.entity.Order;
import com.retailmanagement.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Async
    public void sendOrderCreatedEmail(Order order) {
        try {
            if (order.getCustomer() == null ||
                    order.getCustomer().getEmail() == null) return;

            Resend resend = new Resend(apiKey);

            String htmlContent = buildOrderConfirmationEmail(order);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(order.getCustomer().getEmail())
                    .subject("Xac nhan don hang #" + order.getOrderNumber())
                    .html(htmlContent)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendSecurityAlertToAdmins(
            List<String> emails,
            String subject,
            String content
    ) {
        try {
            if (emails == null || emails.isEmpty()) return;

            emails = emails.stream().distinct().toList();

            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("Security <security@nguyenduc.me>")
                    .to(emails.toArray(new String[0]))
                    .subject(subject)
                    .html(content)
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

    @Async
    public void sendDeliveryProofEmail(Order order) {
        try {
            if (order.getCustomer() == null || order.getCustomer().getEmail() == null) return;

            Resend resend = new Resend(apiKey);

            String customerName = safe(order.getCustomer().getName(), "Khách hàng");
            String orderNumber = order.getOrderNumber();

            // Link URL gắn mã động gọi thẳng về Frontend bật Popup
            String frontendUrl = "http://localhost:5173/orders/" + order.getId() + "?action=confirm_delivered";

            String html = """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
              <title>Thông báo giao hàng</title>
            </head>
            <body style="margin:0;padding:0;background:#f2f2f2;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;padding:30px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="max-width:600px;width:100%%;background:#ffffff;border:1px solid #cccccc;">
                    <tr>
                      <td style="background:#1a2744;padding:24px 32px;">
                        <p style="margin:0;font-size:11px;letter-spacing:3px;color:#aabbd4;
                                   text-transform:uppercase;font-weight:bold;">TECHSTORE</p>
                        <p style="margin:8px 0 0;font-size:18px;font-weight:bold;color:#ffffff;">
                          Đơn hàng đang chờ xác nhận
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 32px 0;">
                        <p style="margin:0;font-size:14px;color:#333;">Xin chào <strong>%s</strong>,</p>
                        <p style="margin:10px 0 0;font-size:13px;color:#555;line-height:1.6;">
                          Shipper vừa xác nhận đã giao đơn hàng <strong>#%s</strong> tới bạn.<br/>
                          Vui lòng kiểm tra hàng hoá và bấm xác nhận bên dưới để hoàn tất đơn hàng nhé!
                        </p>
                        
                        <!-- NÚT BẤM XÁC NHẬN -->
                        <div style="text-align:center; margin: 32px 0;">
                          <a href="%s" style="background-color:#2a6e2a; color:#ffffff; padding:14px 28px; font-size:14px; font-weight:bold; text-decoration:none; border-radius:6px; display:inline-block; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                            ✅ XÁC NHẬN ĐÃ NHẬN HÀNG
                          </a>
                        </div>
                        
                        <div style="background:#fffbea; border:1px solid #f5c518; padding:12px; margin-top:20px;">
                            <p style="font-size:12px;color:#7a5800;margin:0;line-height:1.5;">
                              *Lưu ý: Nếu bạn không có phản hồi trong vòng <strong>3 ngày</strong>, hệ thống sẽ tự động chuyển đơn hàng sang trạng thái Đã Giao.
                            </p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 32px 28px;text-align:center;border-top:1px solid #ddd;">
                        <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">TechStore</p>
                        <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                          Nếu cần hỗ trợ, vui lòng liên hệ
                          <a href="mailto:support@nguyenduc.me" style="color:#1a2744;text-decoration:underline;">
                            support@nguyenduc.me
                          </a>
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(customerName, orderNumber, frontendUrl);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(order.getCustomer().getEmail())
                    .subject("Đơn hàng #" + orderNumber + " đang chờ bạn xác nhận")
                    .html(html)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Build item rows ───────────────────────────────────────────────────
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
                            font-size:13px;color:#555;white-space:nowrap;">
                  %d
                </td>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;text-align:right;
                            font-size:13px;color:#555;white-space:nowrap;">
                  %s d
                </td>
                <td style="padding:9px 8px;border-bottom:1px solid #e5e5e5;text-align:right;
                            font-size:13px;font-weight:bold;color:#222;white-space:nowrap;">
                  %s d
                </td>
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

    // ── Build discount rows ───────────────────────────────────────────────
    private String buildDiscountRows(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getSubtotal() != null) {
            sb.append(summaryRow("Tam tinh", fmt(order.getSubtotal()) + " d", "#555", false));
        }
        if (isPositive(order.getShippingFee())) {
            sb.append(summaryRow("Phi van chuyen", fmt(order.getShippingFee()) + " d", "#555", false));
        }
        if (isPositive(order.getVipDiscount())) {
            sb.append(summaryRow("Giam VIP", "- " + fmt(order.getVipDiscount()) + " d", "#2a6e2a", false));
        }
        if (isPositive(order.getSpinDiscount())) {
            sb.append(summaryRow("Giam Vong quay", "- " + fmt(order.getSpinDiscount()) + " d", "#2a6e2a", false));
        }
        if (order.getAppliedPromotionCode() != null) {
            sb.append(summaryRow("Ma khuyen mai", order.getAppliedPromotionCode(), "#2a6e2a", false));
        }
        if (isPositive(order.getDiscountTotal())) {
            sb.append(summaryRow("Tong giam", "- " + fmt(order.getDiscountTotal()) + " d", "#2a6e2a", true));
        }
        return sb.toString();
    }

    private String summaryRow(String label, String value, String valueColor, boolean topBorder) {
        String borderStyle = topBorder ? "border-top:1px solid #ddd;padding-top:8px;" : "";
        return """
          <tr>
            <td style="padding:4px 0;font-size:13px;color:#777;%s">%s</td>
            <td style="padding:4px 0;font-size:13px;color:%s;text-align:right;%s">%s</td>
          </tr>
        """.formatted(borderStyle, label, valueColor, borderStyle, value);
    }

    // ── Main HTML builder — Order Confirmation ────────────────────────────
    private String buildOrderConfirmationEmail(Order order) {
        String customerName   = safe(order.getCustomer().getName(), "Khach hang");
        String customerEmail  = safe(order.getCustomer().getEmail(), "—");
        String customerPhone  = safe(order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : null, "—");
        String customerAddr   = safe(order.getCustomer().getAddress() != null ? order.getCustomer().getAddress() : null, "—");
        String paymentMethod  = safe(order.getPaymentMethod(), "—");
        String orderNote      = order.getNotes() != null && !order.getNotes().isBlank()
                ? "<tr><td colspan=\"2\" style=\"padding:8px 0 2px;\"><div style=\"border-left:3px solid #aaa;padding:8px 12px;background:#f9f9f9;font-size:13px;color:#555;\"><strong>Ghi chu:</strong> " + order.getNotes() + "</div></td></tr>"
                : "";

        String createdAt = order.getCreatedAt() != null
                ? order.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
                : "—";

        String itemRows     = buildItemRows(order);
        String discountRows = buildDiscountRows(order);
        String totalAmount  = fmt(order.getTotalAmount());

        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
          <title>Xac nhan don hang</title>
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
                      Xac nhan don hang
                    </p>
                  </td>
                </tr>

                <!-- STATUS BAR -->
                <tr>
                  <td style="background:#e8f4e8;border-bottom:1px solid #c3dcc3;padding:10px 32px;">
                    <p style="margin:0;font-size:13px;color:#2a6e2a;font-weight:bold;">
                      Da thanh toan — %s
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
                      Don hang <strong>#%s</strong> cua ban da duoc tiep nhan luc <strong>%s</strong>.
                      Chung toi se thong bao ngay khi hang duoc giao den ban.
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

                <!-- ORDER SUMMARY -->
                <tr>
                  <td style="padding:12px 32px 0;">
                    <table width="100%%" cellpadding="0" cellspacing="0">
                      <tr>
                        <td width="55%%"></td>
                        <td width="45%%">
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
                    <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">
                      TechStore
                    </p>
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
                createdAt,
                customerName,
                order.getOrderNumber(),
                createdAt,
                orderNote,
                customerEmail,
                customerPhone,
                customerAddr,
                paymentMethod,
                itemRows,
                discountRows,
                totalAmount
        );
    }

    // ── Welcome Password Email ────────────────────────────────────────────
    @Async
    public void sendWelcomePasswordEmail(String toEmail, String fullName, String phone, String rawPassword) {
        try {
            if (toEmail == null || toEmail.isBlank()) return;

            Resend resend = new Resend(apiKey);

            String html = buildWelcomeEmail(fullName, phone, rawPassword);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(toEmail)
                    .subject("Chao mung den voi TechStore — Thong tin dang nhap")
                    .html(html)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildWelcomeEmail(String fullName, String phone, String rawPassword) {
        String safeName  = (fullName != null && !fullName.isBlank()) ? fullName : "Khach hang";
        String safePhone = (phone != null && !phone.isBlank()) ? phone : "—";

        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
          <title>Chao mung den TechStore</title>
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
                      Chao mung ban den voi TechStore
                    </p>
                    <p style="margin:6px 0 0;font-size:13px;color:#aabbd4;">
                      Tai khoan cua ban da duoc tao thanh cong
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
                      Nhan vien TechStore vua tao tai khoan cho ban. Duoi day la thong tin dang nhap.
                      Vui long <strong>doi mat khau</strong> sau khi dang nhap lan dau de bao mat tai khoan.
                    </p>
                  </td>
                </tr>

                <!-- CREDENTIALS -->
                <tr>
                  <td style="padding:20px 32px 0;">
                    <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                               text-transform:uppercase;color:#888;">THONG TIN DANG NHAP</p>
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="border:1px solid #ddd;border-collapse:collapse;">
                      <tr>
                        <td style="padding:9px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                    background:#fafafa;width:40%%;border-right:1px solid #eee;">So dien thoai</td>
                        <td style="padding:9px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;
                                    font-weight:bold;">%s</td>
                      </tr>
                      <tr>
                        <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                                    border-right:1px solid #eee;">Mat khau tam thoi</td>
                        <td style="padding:9px 12px;">
                          <span style="font-size:16px;font-weight:bold;color:#222;
                                        font-family:'Courier New',monospace;
                                        background:#fffbeb;padding:3px 10px;
                                        border:1px solid #e5c84a;letter-spacing:2px;">
                            %s
                          </span>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- WARNING -->
                <tr>
                  <td style="padding:16px 32px 0;">
                    <table width="100%%" cellpadding="0" cellspacing="0"
                           style="border:1px solid #f4c26a;background:#fffbeb;border-collapse:collapse;">
                      <tr>
                        <td style="padding:12px 16px;font-size:13px;color:#7a4f00;line-height:1.6;">
                          <strong>Luu y bao mat:</strong> Day la mat khau tam thoi do nhan vien tao.
                          Vui long dang nhap va doi mat khau ngay de bao ve tai khoan.
                          Khong chia se thong tin nay voi bat ky ai.
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
        """.formatted(safeName, safePhone, rawPassword);
    }

    // ── OTP Email ─────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            Resend resend = new Resend(apiKey);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(toEmail)
                    .subject("Ma OTP dat lai mat khau TechStore")
                    .html(buildOtpEmail(otp))
                    .build();

            resend.emails().send(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildOtpEmail(String otp) {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
          <title>Ma OTP</title>
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
                      Dat lai mat khau
                    </p>
                  </td>
                </tr>

                <!-- BODY -->
                <tr>
                  <td style="padding:32px;text-align:center;">
                    <p style="margin:0 0 20px;font-size:14px;color:#555;">
                      Ma OTP xac thuc cua ban la:
                    </p>
                    <div style="display:inline-block;border:1px solid #cccccc;background:#f9f9f9;
                                padding:18px 48px;margin:0 auto;">
                      <span style="font-size:34px;font-weight:bold;letter-spacing:12px;color:#1a2744;
                                   font-family:'Courier New',monospace;">%s</span>
                    </div>
                    <p style="margin:20px 0 0;font-size:13px;color:#999;">
                      Ma co hieu luc trong <strong style="color:#555;">5 phut</strong>.
                      Khong chia se ma nay voi ai.
                    </p>
                  </td>
                </tr>

                <!-- DIVIDER -->
                <tr>
                  <td style="padding:0 32px;">
                    <hr style="border:none;border-top:1px solid #ddd;margin:0;"/>
                  </td>
                </tr>

                <!-- FOOTER -->
                <tr>
                  <td style="padding:18px 32px 28px;text-align:center;">
                    <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                      (c) 2025 TechStore.
                      Neu ban khong yeu cau dat lai mat khau, hay bo qua email nay.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(otp);
    }

    private String buildReturnApprovedEmail(com.retailmanagement.entity.Return ret) {
        String customerName = safe(ret.getOrder().getCustomer().getName(), "Khach hang");
        String orderNumber  = ret.getOrder().getOrderNumber();
        String productName  = ret.getOrderItem().getProductName();
        String variantName  = ret.getOrderItem().getVariantName() != null
                ? " (" + ret.getOrderItem().getVariantName() + ")" : "";
        String refundAmount = fmt(ret.getRefundAmount());
        boolean isFull      = "FULL".equals(ret.getRefundMethod());
        String refundLabel  = isFull ? "Hoan 100%% (loi cua hang)" : "Hoan 80%% (loi khach hang)";
        String note         = ret.getNote() != null && !ret.getNote().isBlank()
                ? "<tr><td style=\"padding:9px 12px;font-size:13px;color:#666;background:#fafafa;"
                + "border-right:1px solid #eee;\">Ghi chu</td>"
                + "<td style=\"padding:9px 12px;font-size:13px;color:#222;\">"
                + ret.getNote() + "</td></tr>"
                : "";

        return """
    <!DOCTYPE html><html lang="vi">
    <head><meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
    <title>Tra hang duoc chap thuan</title></head>
    <body style="margin:0;padding:0;background:#f2f2f2;font-family:Arial,Helvetica,sans-serif;">
      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;padding:30px 0;">
        <tr><td align="center">
          <table width="600" cellpadding="0" cellspacing="0"
                 style="max-width:600px;width:100%%;background:#fff;border:1px solid #ccc;">

            <!-- HEADER -->
            <tr>
              <td style="background:#1a4d2e;padding:24px 32px;">
                <p style="margin:0;font-size:11px;letter-spacing:3px;color:#a8dab5;
                           text-transform:uppercase;font-weight:bold;">TECHSTORE</p>
                <p style="margin:8px 0 0;font-size:18px;font-weight:bold;color:#fff;">
                  Yeu cau tra hang duoc chap thuan ✓
                </p>
                <p style="margin:6px 0 0;font-size:13px;color:#a8dab5;">Don hang #%s</p>
              </td>
            </tr>

            <!-- GREETING -->
            <tr><td style="padding:24px 32px 0;">
              <p style="margin:0;font-size:14px;color:#333;">Xin chao <strong>%s</strong>,</p>
              <p style="margin:10px 0 0;font-size:13px;color:#555;line-height:1.6;">
                Yeu cau tra hang cua ban da duoc <strong>chap thuan</strong>.
                So tien hoan se duoc xu ly theo phuong thuc thanh toan ban da dung khi dat hang.
              </p>
            </td></tr>

            <!-- DETAIL TABLE -->
            <tr><td style="padding:20px 32px 0;">
              <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                         text-transform:uppercase;color:#888;">CHI TIET HOAN TIEN</p>
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="border:1px solid #ddd;border-collapse:collapse;">
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;width:40%%;">San pham</td>
                  <td style="padding:9px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">
                    %s%s
                  </td>
                </tr>
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;">So luong</td>
                  <td style="padding:9px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%d</td>
                </tr>
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;">Hinh thuc hoan</td>
                  <td style="padding:9px 12px;font-size:13px;color:#1a4d2e;font-weight:bold;
                              border-bottom:1px solid #eee;">%s</td>
                </tr>
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;">So tien hoan</td>
                  <td style="padding:9px 12px;font-size:15px;font-weight:bold;color:#1a4d2e;
                              border-bottom:1px solid #eee;">%s d</td>
                </tr>
                %s
              </table>
            </td></tr>

            <!-- DIVIDER -->
            <tr><td style="padding:24px 32px 0;">
              <hr style="border:none;border-top:1px solid #ddd;margin:0;"/>
            </td></tr>

            <!-- FOOTER -->
            <tr><td style="padding:18px 32px 28px;text-align:center;">
              <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">TechStore</p>
              <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                Lien he ho tro:
                <a href="mailto:support@nguyenduc.me" style="color:#1a2744;text-decoration:underline;">
                  support@nguyenduc.me
                </a><br/>
                Email nay duoc gui tu dong, vui long khong tra loi truc tiep.
              </p>
              <p style="margin:12px 0 0;font-size:11px;color:#bbb;">(c) 2025 TechStore. All rights reserved.</p>
            </td></tr>

          </table>
        </td></tr>
      </table>
    </body></html>
    """.formatted(orderNumber, customerName, productName, variantName,
                ret.getQuantity(), refundLabel, refundAmount, note);
    }

    private String buildReturnRejectedEmail(com.retailmanagement.entity.Return ret) {
        String customerName = safe(ret.getOrder().getCustomer().getName(), "Khach hang");
        String orderNumber  = ret.getOrder().getOrderNumber();
        String productName  = ret.getOrderItem().getProductName();
        String variantName  = ret.getOrderItem().getVariantName() != null
                ? " (" + ret.getOrderItem().getVariantName() + ")" : "";
        String reason       = safe(ret.getNote(), "Khong co ly do cu the");

        return """
    <!DOCTYPE html><html lang="vi">
    <head><meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
    <title>Tra hang bi tu choi</title></head>
    <body style="margin:0;padding:0;background:#f2f2f2;font-family:Arial,Helvetica,sans-serif;">
      <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;padding:30px 0;">
        <tr><td align="center">
          <table width="600" cellpadding="0" cellspacing="0"
                 style="max-width:600px;width:100%%;background:#fff;border:1px solid #ccc;">

            <!-- HEADER -->
            <tr>
              <td style="background:#6b1a1a;padding:24px 32px;">
                <p style="margin:0;font-size:11px;letter-spacing:3px;color:#f4b8b8;
                           text-transform:uppercase;font-weight:bold;">TECHSTORE</p>
                <p style="margin:8px 0 0;font-size:18px;font-weight:bold;color:#fff;">
                  Yeu cau tra hang bi tu choi
                </p>
                <p style="margin:6px 0 0;font-size:13px;color:#f4b8b8;">Don hang #%s</p>
              </td>
            </tr>

            <!-- GREETING -->
            <tr><td style="padding:24px 32px 0;">
              <p style="margin:0;font-size:14px;color:#333;">Xin chao <strong>%s</strong>,</p>
              <p style="margin:10px 0 0;font-size:13px;color:#555;line-height:1.6;">
                Rat tiec, yeu cau tra hang cua ban da <strong>bi tu choi</strong> sau khi xem xet.
                Don hang cua ban van giu nguyen trang thai da giao.
              </p>
            </td></tr>

            <!-- DETAIL TABLE -->
            <tr><td style="padding:20px 32px 0;">
              <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                         text-transform:uppercase;color:#888;">THONG TIN YEU CAU</p>
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="border:1px solid #ddd;border-collapse:collapse;">
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;width:40%%;">San pham</td>
                  <td style="padding:9px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">
                    %s%s
                  </td>
                </tr>
                <tr>
                  <td style="padding:9px 12px;font-size:13px;color:#666;background:#fafafa;
                              border-bottom:1px solid #eee;border-right:1px solid #eee;">Ly do tu choi</td>
                  <td style="padding:9px 12px;font-size:13px;color:#6b1a1a;font-weight:bold;">%s</td>
                </tr>
              </table>
            </td></tr>

            <!-- SUPPORT NOTE -->
            <tr><td style="padding:16px 32px 0;">
              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="border:1px solid #f4c6c6;background:#fff5f5;border-collapse:collapse;">
                <tr>
                  <td style="padding:12px 16px;font-size:13px;color:#7a2020;line-height:1.6;">
                    Neu ban co thac mac ve quyet dinh nay, vui long lien he
                    <a href="mailto:support@nguyenduc.me" style="color:#6b1a1a;">support@nguyenduc.me</a>
                    de duoc ho tro.
                  </td>
                </tr>
              </table>
            </td></tr>

            <!-- DIVIDER -->
            <tr><td style="padding:24px 32px 0;">
              <hr style="border:none;border-top:1px solid #ddd;margin:0;"/>
            </td></tr>

            <!-- FOOTER -->
            <tr><td style="padding:18px 32px 28px;text-align:center;">
              <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">TechStore</p>
              <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                Lien he ho tro:
                <a href="mailto:support@nguyenduc.me" style="color:#1a2744;text-decoration:underline;">
                  support@nguyenduc.me
                </a><br/>
                Email nay duoc gui tu dong, vui long khong tra loi truc tiep.
              </p>
              <p style="margin:12px 0 0;font-size:11px;color:#bbb;">(c) 2025 TechStore. All rights reserved.</p>
            </td></tr>

          </table>
        </td></tr>
      </table>
    </body></html>
    """.formatted(orderNumber, customerName, productName, variantName, reason);
    }

    @Async
    public void sendReturnApprovedEmail(com.retailmanagement.entity.Return ret) {
        try {
            var customer = ret.getOrder().getCustomer();
            if (customer == null || customer.getEmail() == null) return;

            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(customer.getEmail())
                    .subject("Yeu cau tra hang #" + ret.getOrder().getOrderNumber() + " da duoc chap thuan")
                    .html(buildReturnApprovedEmail(ret))
                    .build();
            resend.emails().send(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendReturnRejectedEmail(com.retailmanagement.entity.Return ret) {
        try {
            var customer = ret.getOrder().getCustomer();
            if (customer == null || customer.getEmail() == null) return;

            Resend resend = new Resend(apiKey);
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(customer.getEmail())
                    .subject("Yeu cau tra hang #" + ret.getOrder().getOrderNumber() + " bi tu choi")
                    .html(buildReturnRejectedEmail(ret))
                    .build();
            resend.emails().send(params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Async
    public void sendOrderCancelledEmail(Order order, String cancelReason) {
        try {
            if (order.getCustomer() == null ||
                    order.getCustomer().getEmail() == null) return;

            Resend resend = new Resend(apiKey);

            String htmlContent = buildOrderCancelledEmail(order, cancelReason);

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("TechStore <noreply@nguyenduc.me>")
                    .to(order.getCustomer().getEmail())
                    .subject("Don hang #" + order.getOrderNumber() + " da bi huy")
                    .html(htmlContent)
                    .build();

            resend.emails().send(params);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Main HTML builder — Order Cancelled ──────────────────────────────
    private String buildOrderCancelledEmail(Order order, String cancelReason) {
        String customerName  = safe(order.getCustomer().getName(), "Khach hang");
        String customerEmail = safe(order.getCustomer().getEmail(), "—");
        String customerPhone = safe(order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : null, "—");
        String paymentMethod = safe(order.getPaymentMethod(), "—");
        String reasonHtml    = (cancelReason != null && !cancelReason.isBlank())
                ? cancelReason
                : "Khong co ly do cu the";

        String createdAt = order.getCreatedAt() != null
                ? order.getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy"))
                : "—";

        String itemRows    = buildItemRows(order);
        String totalAmount = fmt(order.getTotalAmount());

        return """
        <!DOCTYPE html><html lang="vi">
        <head><meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
        <title>Don hang bi huy</title></head>
        <body style="margin:0;padding:0;background:#f2f2f2;font-family:Arial,Helvetica,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;padding:30px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="max-width:600px;width:100%%;background:#fff;border:1px solid #ccc;">
 
                <!-- HEADER -->
                <tr>
                  <td style="background:#7a2e00;padding:24px 32px;">
                    <p style="margin:0;font-size:11px;letter-spacing:3px;color:#f5c4a1;
                               text-transform:uppercase;font-weight:bold;">TECHSTORE</p>
                    <p style="margin:8px 0 0;font-size:18px;font-weight:bold;color:#fff;">
                      Don hang da bi huy
                    </p>
                    <p style="margin:6px 0 0;font-size:13px;color:#f5c4a1;">Don hang #%s — %s</p>
                  </td>
                </tr>
 
                <!-- STATUS BAR -->
                <tr>
                  <td style="background:#fff3ec;border-bottom:1px solid #f5c4a1;padding:10px 32px;">
                    <p style="margin:0;font-size:13px;color:#7a2e00;font-weight:bold;">
                      Da huy don hang
                    </p>
                  </td>
                </tr>
 
                <!-- GREETING -->
                <tr><td style="padding:24px 32px 0;">
                  <p style="margin:0;font-size:14px;color:#333;">Xin chao <strong>%s</strong>,</p>
                  <p style="margin:10px 0 0;font-size:13px;color:#555;line-height:1.6;">
                    Don hang <strong>#%s</strong> cua ban da bi huy. Neu ban da thanh toan,
                    so tien se duoc hoan lai theo phuong thuc thanh toan goc trong vong
                    <strong>3–5 ngay lam viec</strong>.
                  </p>
                </td></tr>
 
                <!-- CANCEL REASON -->
                <tr><td style="padding:16px 32px 0;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #f5c4a1;background:#fff8f3;border-collapse:collapse;">
                    <tr>
                      <td style="padding:12px 16px;">
                        <p style="margin:0 0 4px;font-size:11px;font-weight:bold;letter-spacing:2px;
                                   text-transform:uppercase;color:#a04000;">LY DO HUY</p>
                        <p style="margin:0;font-size:13px;color:#7a2e00;font-weight:bold;">%s</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
 
                <!-- CUSTOMER INFO -->
                <tr><td style="padding:20px 32px 0;">
                  <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                             text-transform:uppercase;color:#888;">THONG TIN DON HANG</p>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #ddd;border-collapse:collapse;">
                    <tr>
                      <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                  background:#fafafa;width:40%%;border-right:1px solid #eee;">Khach hang</td>
                      <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                  background:#fafafa;border-right:1px solid #eee;">Email</td>
                      <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:7px 12px;font-size:13px;color:#666;border-bottom:1px solid #eee;
                                  background:#fafafa;border-right:1px solid #eee;">Dien thoai</td>
                      <td style="padding:7px 12px;font-size:13px;color:#222;border-bottom:1px solid #eee;">%s</td>
                    </tr>
                    <tr>
                      <td style="padding:7px 12px;font-size:13px;color:#666;background:#fafafa;
                                  border-right:1px solid #eee;">Thanh toan</td>
                      <td style="padding:7px 12px;font-size:13px;color:#222;">%s</td>
                    </tr>
                  </table>
                </td></tr>
 
                <!-- ORDER ITEMS -->
                <tr><td style="padding:20px 32px 0;">
                  <p style="margin:0 0 8px;font-size:11px;font-weight:bold;letter-spacing:2px;
                             text-transform:uppercase;color:#888;">SAN PHAM TRONG DON</p>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #ddd;border-collapse:collapse;">
                    <tr style="background:#f5f5f5;">
                      <th style="padding:8px;font-size:12px;color:#555;text-align:left;
                                  border-bottom:1px solid #ddd;font-weight:bold;">San pham</th>
                      <th style="padding:8px;font-size:12px;color:#555;text-align:center;
                                  border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">S.L</th>
                      <th style="padding:8px;font-size:12px;color:#555;text-align:right;
                                  border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">Don gia</th>
                      <th style="padding:8px;font-size:12px;color:#555;text-align:right;
                                  border-bottom:1px solid #ddd;font-weight:bold;white-space:nowrap;">Thanh tien</th>
                    </tr>
                    %s
                  </table>
                </td></tr>
 
                <!-- TOTAL -->
                <tr><td style="padding:12px 32px 0;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="background:#7a2e00;border-collapse:collapse;">
                    <tr>
                      <td style="padding:14px 16px;font-size:13px;font-weight:bold;color:#f5c4a1;
                                  text-transform:uppercase;letter-spacing:1px;">
                        Tong gia tri don hang
                      </td>
                      <td style="padding:14px 16px;text-align:right;">
                        <span style="font-size:20px;font-weight:bold;color:#fff;">%s d</span>
                      </td>
                    </tr>
                  </table>
                </td></tr>
 
                <!-- SUPPORT NOTE -->
                <tr><td style="padding:16px 32px 0;">
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border:1px solid #ddd;background:#fafafa;border-collapse:collapse;">
                    <tr>
                      <td style="padding:12px 16px;font-size:13px;color:#555;line-height:1.6;">
                        Neu ban co thac mac ve don hang nay, vui long lien he
                        <a href="mailto:support@nguyenduc.me" style="color:#7a2e00;">support@nguyenduc.me</a>
                        de duoc ho tro.
                      </td>
                    </tr>
                  </table>
                </td></tr>
 
                <!-- DIVIDER -->
                <tr><td style="padding:24px 32px 0;">
                  <hr style="border:none;border-top:1px solid #ddd;margin:0;"/>
                </td></tr>
 
                <!-- FOOTER -->
                <tr><td style="padding:18px 32px 28px;text-align:center;">
                  <p style="margin:0 0 4px;font-size:12px;font-weight:bold;color:#555;">TechStore</p>
                  <p style="margin:0;font-size:12px;color:#999;line-height:1.7;">
                    Lien he ho tro:
                    <a href="mailto:support@nguyenduc.me" style="color:#1a2744;text-decoration:underline;">
                      support@nguyenduc.me
                    </a><br/>
                    Email nay duoc gui tu dong, vui long khong tra loi truc tiep.
                  </p>
                  <p style="margin:12px 0 0;font-size:11px;color:#bbb;">(c) 2025 TechStore. All rights reserved.</p>
                </td></tr>
 
              </table>
            </td></tr>
          </table>
        </body></html>
        """.formatted(
                order.getOrderNumber(), createdAt,
                customerName,
                order.getOrderNumber(),
                reasonHtml,
                customerName, customerEmail, customerPhone, paymentMethod,
                itemRows,
                totalAmount
        );
    }
}