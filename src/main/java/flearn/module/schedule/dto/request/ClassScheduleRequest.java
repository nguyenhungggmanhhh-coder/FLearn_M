package flearn.module.schedule.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClassScheduleRequest {

    @NotNull(message = "Vui lòng chọn thứ trong tuần.")
    @Min(value = 1, message = "Thứ không hợp lệ (1=CN, 2=T2, ..., 7=T7).")
    @Max(value = 7, message = "Thứ không hợp lệ.")
    private Integer dayOfWeek;

    @NotNull(message = "Vui lòng nhập giờ bắt đầu.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Size(max = 500, message = "Phòng/Link không được vượt quá 500 ký tự.")
    private String roomOrLink;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự.")
    private String note;

    private Boolean remindOneDayBefore   = true;
    private Boolean remindTwoHoursBefore = true;
    private Boolean isActive             = true;
}
