package flearn.module.management.service.impl;

import flearn.module.management.dto.request.JoinClassRequest;
import flearn.module.management.dto.response.EnrollmentResponse;
import flearn.enums.ClassStatus;
import flearn.entity.Classroom;
import flearn.entity.Enrollment;
import flearn.enums.EnrollmentStatus;
import flearn.entity.User;
import flearn.common.exception.BusinessException;
import flearn.module.management.mapper.EnrollmentMapper;
import flearn.repository.ClassroomRepository;
import flearn.repository.EnrollmentRepository;
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
    private final EnrollmentRepository enrollmentRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    public List<EnrollmentResponse> getJoinedClasses(User student) {
        return enrollmentMapper.toResponseList(
                enrollmentRepository.findActiveClassesForStudent(student, EnrollmentStatus.ACTIVE, ClassStatus.ACTIVE)
        );
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
