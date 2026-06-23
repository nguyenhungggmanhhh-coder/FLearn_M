package flearn.module.management.service.impl;

import flearn.module.management.dto.request.JoinClassRequest;
import flearn.module.management.dto.response.EnrollmentResponse;
import flearn.enums.ClassStatus;
import flearn.entity.Classroom;
import flearn.entity.Enrollment;
import flearn.entity.Lesson;
import flearn.enums.EnrollmentStatus;
import flearn.entity.User;
import flearn.common.exception.BusinessException;
import flearn.module.management.mapper.EnrollmentMapper;
import flearn.repository.ClassroomRepository;
import flearn.repository.EnrollmentRepository;
import flearn.repository.LessonRepository;
import flearn.repository.MaterialRepository;
import flearn.repository.MaterialTrackingRepository;
import flearn.module.management.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class StudentServiceImpl implements StudentService {
    private final EnrollmentRepository     enrollmentRepository;
    private final ClassroomRepository      classroomRepository;
    private final LessonRepository         lessonRepository;
    private final MaterialRepository       materialRepository;
    private final MaterialTrackingRepository materialTrackingRepository;
    private final EnrollmentMapper         enrollmentMapper;

    @Override
    public List<EnrollmentResponse> getJoinedClasses(User student) {
        List<Enrollment> enrollments = enrollmentRepository
                .findActiveClassesForStudent(student, EnrollmentStatus.ACTIVE, ClassStatus.ACTIVE);

        return enrollments.stream().map(enrollment -> {
            EnrollmentResponse resp = enrollmentMapper.toResponse(enrollment);

            // Tính tiến độ học tập cho thẻ lớp
            Classroom classroom = enrollment.getClassRoom();
            List<Lesson> lessons = lessonRepository.findByClassroom(classroom);

            if (lessons.isEmpty()) {
                resp.setTotalMaterials(0);
                resp.setViewedMaterials(0);
            } else {
                long total  = materialRepository.countPublishedByLessons(lessons);
                long viewed = materialTrackingRepository.countViewedByStudentInLessons(student, lessons);
                resp.setTotalMaterials((int) total);
                resp.setViewedMaterials((int) Math.min(viewed, total));
            }
            return resp;
        }).toList();
    }

    @Override
    @Transactional
    public void joinClass(JoinClassRequest request, User student) {
        String classCode = request.getInviteCode().trim().toUpperCase(Locale.ROOT);
        Classroom classroom = classroomRepository.findByInviteCode(classCode)
                .orElseThrow(() -> new BusinessException("Mã lớp không hợp lệ."));

        if (classroom.getStatus() != ClassStatus.ACTIVE) {
            throw new BusinessException("Lớp hiện không mở cho học sinh tham gia.");
        }

        if (Boolean.FALSE.equals(classroom.getIsJoinable())) {
            throw new BusinessException("Mã mời tham gia lớp học này đã bị khóa.");
        }

        if (classroom.getCourse() == null || classroom.getCourse().getStatus() != flearn.enums.CourseStatus.ACTIVE) {
            throw new BusinessException("Khóa học liên kết hiện không ở trạng thái hoạt động.");
        }

        boolean existing = enrollmentRepository.existsByStudentAndClassRoomAndStatusIn(
                student,
                classroom,
                List.of(EnrollmentStatus.PENDING, EnrollmentStatus.ACTIVE)
        );
        if (existing) {
            throw new BusinessException("Bạn đã gửi yêu cầu hoặc đang ở trong lớp này.");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .classRoom(classroom)
                .status(EnrollmentStatus.PENDING)
                .requestMessage(request.getRequestMessage())
                .build();
        enrollmentRepository.save(enrollment);
    }
}
