package com.odc.common.constant;

public enum ReportStatus {
    SUBMITTED,      // Đã nộp (Chờ review) - Tương đương PENDING
    PENDING_ADMIN_CHECK,
    UNDER_REVIEW,   // Đang xem xét (Optional - nếu quy trình dài)
    APPROVED,       // Đã duyệt (Nội bộ hoặc cấp trên)
    REJECTED,       // Từ chối / Yêu cầu sửa lại
    FINAL           // Đã chốt / Đã gửi khách hàng (Status cuối cùng)
}
