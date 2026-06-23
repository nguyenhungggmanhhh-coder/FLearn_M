package flearn.module.management.dto.response;

import flearn.module.auth.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomResponse {
    private Integer classId;
    private String classCode;
    private String className;
    private String description;
    private UserResponse teacher;
    private String inviteCode;
    private String status;
    private Boolean isActive;
    private Boolean isJoinable;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private Date createdAt;

    private Integer courseId;
    private String courseName;
    private String courseCode;

    /** [FEAT-04] Trạng thái hiển thị mã mời (admin/teacher toggle). */
    private Boolean inviteCodeVisible;

    /** [FEAT-04] Thời điểm mã mời được bật gần nhất – dùng cho countdown 15 phút. */
    private Date inviteCodeGeneratedAt;
}
