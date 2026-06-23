package flearn.module.management.service.impl;

import flearn.module.management.dto.request.AssignTeacherRequest;
import flearn.module.management.dto.request.ClassroomRequest;
import flearn.module.management.dto.response.AdminStatisticsResponse;
import flearn.module.management.dto.response.ClassroomResponse;
import flearn.enums.ClassStatus;
import flearn.entity.Classroom;
import flearn.enums.Role;
import flearn.entity.Course;
import flearn.entity.User;
import flearn.enums.UserStatus;
import flearn.common.exception.BusinessException;
import flearn.module.management.mapper.ClassroomMapper;
import flearn.repository.ClassroomRepository;
import flearn.repository.CourseRepository;
import flearn.repository.UserRepository;
import flearn.module.management.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class ClassroomServiceImpl implements ClassroomService {
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int MAX_INVITE_CODE_ATTEMPTS = 10;

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ClassroomMapper classroomMapper;

    @Override
    public AdminStatisticsResponse getAdminStatistics() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, -7);
        java.util.Date sevenDaysAgo = cal.getTime();

        return AdminStatisticsResponse.builder()
                .totalUsers(userRepository.count())
                .students(userRepository.countByRole(Role.STUDENT.getCode()))
                .teachers(userRepository.countByRole(Role.TEACHER.getCode()))
                .classes(classroomRepository.count())
                .activeClasses(classroomRepository.countByStatus(ClassStatus.ACTIVE))
                .blockedTeachers(userRepository.countByRoleAndStatus(Role.TEACHER.getCode(), UserStatus.BLOCKED))
                .newUsers(userRepository.countByCreatedAtAfter(sevenDaysAgo))
                .build();
    }

    @Override
    public List<ClassroomResponse> getAllClasses() {
        return classroomMapper.toResponseList(classroomRepository.findAll());
    }

    @Override
    public List<ClassroomResponse> searchAllClasses(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllClasses();
        }
        return classroomMapper.toResponseList(classroomRepository.searchAll(keyword.trim()));
    }

    @Override
    public List<ClassroomResponse> getClassesByTeacher(User teacher) {
        return classroomMapper.toResponseList(classroomRepository.findByTeacherAndStatusNot(teacher, ClassStatus.CLOSED));
    }

    @Override
    public ClassroomResponse getClassById(Integer classId) {
        return classroomMapper.toResponse(findClassById(classId));
    }

    @Override
    @Transactional
    public void createClass(ClassroomRequest request) {
        if (request.getCourseId() == null) {
            throw new BusinessException("Vui lòng chọn khóa học.");
        }
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy khóa học."));

        User teacherUser = null;
        if (request.getTeacherId() != null) {
            teacherUser = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy giáo viên."));
            if (teacherUser.getRoleType() != Role.TEACHER || teacherUser.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("Chỉ có thể gán giáo viên đang ACTIVE vào lớp.");
            }
        }

        Classroom newClass = Classroom.builder()
                .className(request.getClassName())
                .description(request.getDescription())
                .course(course)
                .teacher(teacherUser)
                .inviteCode(generateUniqueInviteCode())
                .status(ClassStatus.ACTIVE)
                .isActive(true)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        classroomRepository.save(newClass);
    }

    @Override
    @Transactional
    public void updateClass(Integer classId, ClassroomRequest request) {
        Classroom classroom = findClassById(classId);
        
        if (request.getCourseId() == null) {
            throw new BusinessException("Vui lòng chọn khóa học.");
        }
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy khóa học."));

        User teacherUser = null;
        if (request.getTeacherId() != null) {
            teacherUser = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new BusinessException("Không tìm thấy giáo viên."));
            if (teacherUser.getRoleType() != Role.TEACHER || teacherUser.getStatus() != UserStatus.ACTIVE) {
                throw new BusinessException("Chỉ có thể gán giáo viên đang ACTIVE vào lớp.");
            }
        }

        classroom.setClassName(request.getClassName());
        classroom.setDescription(request.getDescription());
        classroom.setCourse(course);
        classroom.setTeacher(teacherUser);
        classroom.setStartDate(request.getStartDate());
        classroom.setEndDate(request.getEndDate());
        classroomRepository.save(classroom);
    }

    @Override
    public ClassroomResponse getTeacherClassById(Integer classId, User teacher) {
        return classroomMapper.toResponse(findTeacherClassById(classId, teacher));
    }

    @Override
    @Transactional
    public void toggleClassStatus(Integer classId) {
        Classroom classroom = findClassById(classId);
        toggleStatus(classroom);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void toggleTeacherClassStatus(Integer classId, User teacher) {
        Classroom classroom = findTeacherClassById(classId, teacher);
        toggleStatus(classroom);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void softDeleteClass(Integer classId, User teacher) {
        Classroom classroom = findClassById(classId);
        if (teacher != null) {
            if (classroom.getTeacher() == null || !classroom.getTeacher().getUserId().equals(teacher.getUserId())) {
                throw new BusinessException("Ban khong co quyen thao tac lop nay.");
            }
        }
        classroom.setStatus(ClassStatus.CLOSED);
        classroom.setIsActive(false);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void toggleJoinable(Integer classId, User teacher) {
        Classroom classroom = findTeacherClassById(classId, teacher);
        classroom.setIsJoinable(!classroom.getIsJoinable());
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void assignTeacher(Integer classId, AssignTeacherRequest request) {
        Classroom classroom = findClassById(classId);
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy giáo viên."));
        if (teacher.getRoleType() != Role.TEACHER || teacher.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Chỉ có thể gán giáo viên đang ACTIVE vào lớp.");
        }
        classroom.setTeacher(teacher);
        classroomRepository.save(classroom);
    }

    private Classroom findClassById(Integer classId) {
        return classroomRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lớp học."));
    }

    private Classroom findTeacherClassById(Integer classId, User teacher) {
        Classroom classroom = findClassById(classId);
        if (classroom.getTeacher() == null || !classroom.getTeacher().getUserId().equals(teacher.getUserId())) {
            throw new BusinessException("Bạn không có quyền thao tác lớp này.");
        }
        return classroom;
    }

    private void toggleStatus(Classroom classroom) {
        if (classroom.getStatus() == ClassStatus.ACTIVE) {
            classroom.setStatus(ClassStatus.INACTIVE);
            classroom.setIsActive(false);
        } else if (classroom.getStatus() == ClassStatus.INACTIVE) {
            classroom.setStatus(ClassStatus.ACTIVE);
            classroom.setIsActive(true);
        } else {
            throw new BusinessException("Lớp đã đóng không thể mở lại bằng chức năng toggle.");
        }
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_CODE_ATTEMPTS; attempt++) {
            String inviteCode = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, INVITE_CODE_LENGTH)
                    .toUpperCase();
            if (!classroomRepository.existsByInviteCode(inviteCode)) {
                return inviteCode;
            }
        }
        throw new BusinessException("Không thể tạo mã lớp. Vui lòng thử lại.");
    }

    @Override
    @Transactional
    public void toggleInviteCodeVisible(Integer classId) {
        Classroom classroom = findClassById(classId);
        doToggleInviteCode(classroom);
        classroomRepository.save(classroom);
    }

    @Override
    @Transactional
    public void toggleTeacherInviteCode(Integer classId, User teacher) {
        Classroom classroom = findTeacherClassById(classId, teacher);
        doToggleInviteCode(classroom);
        classroomRepository.save(classroom);
    }

    /**
     * Logic toggle mã mời:
     * - Nếu hiện đang visible → tắt (visible=false)
     * - Nếu đang ẩn → bật + cập nhật inviteCodeGeneratedAt = now
     *   (Frontend tự ẩn sau 15 phút tới bằng countdown JS)
     */
    private void doToggleInviteCode(Classroom classroom) {
        boolean currentlyVisible = Boolean.TRUE.equals(classroom.getInviteCodeVisible());
        classroom.setInviteCodeVisible(!currentlyVisible);
        if (!currentlyVisible) {
            // Đang bật lên – stamp thời gian để auto-hide sau 15 phút
            classroom.setInviteCodeGeneratedAt(new java.util.Date());
        }
    }
}
