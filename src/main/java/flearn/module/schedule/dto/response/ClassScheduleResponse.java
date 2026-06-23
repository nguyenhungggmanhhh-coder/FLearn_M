package flearn.module.schedule.dto.response;

import lombok.*;

import java.time.LocalTime;
import java.util.Date;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClassScheduleResponse {
    private Integer id;
    private Integer classId;
    private String  className;

    /** 1=CN, 2=T2, 3=T3, 4=T4, 5=T5, 6=T6, 7=T7 */
    private Integer dayOfWeek;

    /** Tên thứ hiển thị tiếng Việt (VD: "Thứ 2"). */
    private String  dayOfWeekLabel;

    private LocalTime startTime;
    private LocalTime endTime;
    private String  roomOrLink;
    private String  note;
    private Boolean remindOneDayBefore;
    private Boolean remindTwoHoursBefore;
    private Boolean isActive;
    private Date    createdAt;

    /** Tiện ích: tên thứ tiếng Việt từ dayOfWeek. */
    public static String dayLabel(int dow) {
        return switch (dow) {
            case 1 -> "Chủ nhật";
            case 2 -> "Thứ 2";
            case 3 -> "Thứ 3";
            case 4 -> "Thứ 4";
            case 5 -> "Thứ 5";
            case 6 -> "Thứ 6";
            case 7 -> "Thứ 7";
            default -> "Không xác định";
        };
    }
}
