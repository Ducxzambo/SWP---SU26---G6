package com.petclinic.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cung cấp đoạn mô tả ngắn cho từng nhóm dịch vụ (ServiceCategory) để hiển thị
 * trên trang /home và /services, giúp khách hàng hiểu rõ hơn về từng hạng mục
 * thay vì chỉ thấy tên nhóm và danh sách dịch vụ trần trụi.
 *
 * Vì bảng ServiceCategories trong CSDL không có cột mô tả riêng, lớp này dùng
 * cách khớp từ khoá trên tên nhóm (không phân biệt hoa/thường) để trả về nội
 * dung phù hợp nhất; nếu không khớp mẫu nào, trả về mô tả mặc định chung chung
 * để trang không bao giờ hiển thị trống.
 */
public final class ServiceCategoryDescriptions {

    private ServiceCategoryDescriptions() {}

    private static final Map<String, String> KEYWORD_DESCRIPTIONS = new LinkedHashMap<>();
    static {
        KEYWORD_DESCRIPTIONS.put("khám",
                "Khám sức khỏe định kỳ giúp phát hiện sớm các vấn đề tiềm ẩn trước khi trở nên " +
                "nghiêm trọng. Bác sĩ kiểm tra cân nặng, thân nhiệt, tim mạch, răng miệng và tư vấn " +
                "chế độ dinh dưỡng phù hợp với từng giai đoạn phát triển của thú cưng.");
        KEYWORD_DESCRIPTIONS.put("vaccine",
                "Tiêm phòng đầy đủ và đúng lịch là cách hiệu quả nhất để bảo vệ thú cưng khỏi các " +
                "bệnh truyền nhiễm nguy hiểm. Lịch tiêm được cá nhân hoá theo độ tuổi, giống loài và " +
                "tình trạng sức khỏe của từng bé.");
        KEYWORD_DESCRIPTIONS.put("tiêm",
                "Tiêm phòng đầy đủ và đúng lịch là cách hiệu quả nhất để bảo vệ thú cưng khỏi các " +
                "bệnh truyền nhiễm nguy hiểm. Lịch tiêm được cá nhân hoá theo độ tuổi, giống loài và " +
                "tình trạng sức khỏe của từng bé.");
        KEYWORD_DESCRIPTIONS.put("phẫu thuật",
                "Từ triệt sản, xử lý dị vật đến các ca ngoại khoa phức tạp, đội ngũ bác sĩ giàu kinh " +
                "nghiệm cùng phòng mổ vô trùng đạt chuẩn đảm bảo an toàn tối đa trong suốt quá trình " +
                "gây mê và hồi phục.");
        KEYWORD_DESCRIPTIONS.put("grooming",
                "Tắm gội, cắt tỉa lông, vệ sinh tai và cắt móng thường xuyên giúp thú cưng luôn sạch " +
                "sẽ, thoải mái, đồng thời hạn chế các bệnh ngoài da, ve rận và nấm — đặc biệt quan " +
                "trọng với những giống lông dài.");
        KEYWORD_DESCRIPTIONS.put("chẩn đoán",
                "Hệ thống xét nghiệm máu, siêu âm và chẩn đoán hình ảnh hiện đại hỗ trợ bác sĩ xác " +
                "định chính xác nguyên nhân bệnh lý, từ đó xây dựng phác đồ điều trị phù hợp và kịp " +
                "thời.");
        KEYWORD_DESCRIPTIONS.put("xét nghiệm",
                "Hệ thống xét nghiệm máu, siêu âm và chẩn đoán hình ảnh hiện đại hỗ trợ bác sĩ xác " +
                "định chính xác nguyên nhân bệnh lý, từ đó xây dựng phác đồ điều trị phù hợp và kịp " +
                "thời.");
        KEYWORD_DESCRIPTIONS.put("điều trị",
                "Đội ngũ bác sĩ theo dõi sát sao quá trình điều trị nội khoa cho từng ca bệnh, từ các " +
                "vấn đề tiêu hóa, hô hấp đến bệnh mãn tính, đảm bảo thú cưng được chăm sóc đúng phác " +
                "đồ và phục hồi nhanh chóng.");
        KEYWORD_DESCRIPTIONS.put("nội trú",
                "Không gian lưu trú sạch sẽ, thoáng mát với đội ngũ theo dõi sát sao dành cho thú " +
                "cưng cần nghỉ dưỡng, hồi sức sau phẫu thuật hoặc điều trị dài ngày, giúp chủ nuôi " +
                "hoàn toàn an tâm.");
        KEYWORD_DESCRIPTIONS.put("cấp cứu",
                "Sẵn sàng tiếp nhận và xử trí kịp thời các trường hợp khẩn cấp, từ tai nạn, ngộ độc " +
                "đến các biến chứng sức khỏe đột ngột, giúp thú cưng được can thiệp y tế nhanh nhất " +
                "có thể.");
        KEYWORD_DESCRIPTIONS.put("nha khoa",
                "Chăm sóc răng miệng định kỳ giúp ngăn ngừa cao răng, viêm nướu và hôi miệng — những " +
                "vấn đề rất phổ biến nhưng thường bị bỏ qua ở thú cưng.");
    }

    private static final String DEFAULT_DESCRIPTION =
            "Đội ngũ bác sĩ và kỹ thuật viên tận tâm của chúng tôi luôn sẵn sàng tư vấn và chăm sóc " +
            "chu đáo cho thú cưng của bạn ở hạng mục dịch vụ này.";

    /** Trả về mô tả phù hợp nhất dựa trên tên nhóm dịch vụ; không bao giờ trả về null. */
    public static String describe(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return DEFAULT_DESCRIPTION;
        String lower = categoryName.toLowerCase();
        for (Map.Entry<String, String> entry : KEYWORD_DESCRIPTIONS.entrySet()) {
            if (lower.contains(entry.getKey())) return entry.getValue();
        }
        return DEFAULT_DESCRIPTION;
    }
}
