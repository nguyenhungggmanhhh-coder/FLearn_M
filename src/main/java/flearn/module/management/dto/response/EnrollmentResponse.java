package flearn.module.management.dto.response;

import flearn.module.auth.dto.response.UserResponse;
import flearn.module.management.dto.response.ClassroomResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private Integer id;
    private UserResponse student;
    private ClassroomResponse classRoom;
    private String status;
    private String requestMessage;
    private String rejectReason;
    private Date requestedAt;
    private Date approvedAt;
    private Date rejectedAt;
    private Date removedAt;

    /** Tiến độ học: tổng số tài liệu đã published trong lớp. */
    private int totalMaterials;

    /** Số tài liệu sinh viên đã xem. */
    private int viewedMaterials;

    /** Số tài liệu còn chưa xem. */
    public int getPendingMaterials() {
        return Math.max(0, totalMaterials - viewedMaterials);
    }

    /** Phần trăm tiến độ (0-100), dùng cho progress bar. */
    public int getProgressPercent() {
        if (totalMaterials == 0) return 0;
        return (int) Math.round((viewedMaterials * 100.0) / totalMaterials);
    }
}
