package flearn.module.content.service.impl;

import flearn.module.content.dto.request.LearningLessonRequest;
import flearn.module.content.dto.request.MaterialRequest;
import flearn.module.content.dto.request.RoadmapRequest;
import flearn.module.content.dto.response.LessonResponse;
import flearn.module.content.dto.response.MaterialResponse;
import flearn.module.content.dto.response.MaterialTrackingResponse;
import flearn.module.content.dto.response.RoadmapResponse;
import flearn.enums.ClassStatus;
import flearn.entity.Classroom;
import flearn.enums.EnrollmentStatus;
import flearn.entity.Lesson;
import flearn.entity.Material;
import flearn.entity.MaterialTracking;
import flearn.enums.MaterialType;
import flearn.entity.Roadmap;
import flearn.entity.User;
import flearn.common.exception.BusinessException;
import flearn.module.content.mapper.LessonMapper;
import flearn.common.util.EmbedUrlUtil;
import flearn.module.content.mapper.MaterialMapper;
import flearn.module.content.mapper.MaterialTrackingMapper;
import flearn.module.content.mapper.RoadmapMapper;
import flearn.repository.ClassroomRepository;
import flearn.repository.EnrollmentRepository;
import flearn.repository.LessonRepository;
import flearn.repository.MaterialRepository;
import flearn.repository.MaterialTrackingRepository;
import flearn.repository.RoadmapRepository;
import flearn.module.content.service.LearningContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class LearningContentServiceImpl implements LearningContentService {
    private static final Path MATERIAL_UPLOAD_DIR = Path.of("uploads", "materials").toAbsolutePath().normalize();

    private final RoadmapRepository roadmapRepository;
    private final LessonRepository lessonRepository;
    private final MaterialRepository materialRepository;
    private final MaterialTrackingRepository materialTrackingRepository;
    private final ClassroomRepository classroomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RoadmapMapper roadmapMapper;
    private final LessonMapper lessonMapper;
    private final MaterialMapper materialMapper;
    private final MaterialTrackingMapper materialTrackingMapper;

    @Override
    public List<RoadmapResponse> getTeacherRoadmaps(Integer classId, User teacher) {
        Classroom classroom = findTeacherClass(classId, teacher);
        return roadmapMapper.toResponseList(roadmapRepository.findByClassRoomOrderByCreatedAtDesc(classroom));
    }

    @Override
    public RoadmapResponse getTeacherRoadmap(Integer roadmapId, User teacher) {
        return roadmapMapper.toResponse(findTeacherRoadmap(roadmapId, teacher));
    }

    @Override
    public RoadmapResponse getTeacherRoadmapWithContent(Integer roadmapId, User teacher) {
        Roadmap roadmap = findTeacherRoadmap(roadmapId, teacher);
        RoadmapResponse response = roadmapMapper.toResponse(roadmap);
        response.setLessons(lessonMapper.toResponseList(lessonRepository.findByRoadmapOrderByOrderIndexAscCreatedAtAsc(roadmap)));
        return response;
    }

    @Override
    @Transactional
    public void createRoadmap(Integer classId, User teacher, RoadmapRequest request) {
        Classroom classroom = findTeacherClass(classId, teacher);
        roadmapRepository.save(Roadmap.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .classRoom(classroom)
                .published(false)
                .build());
    }

    @Override
    @Transactional
    public void updateRoadmap(Integer roadmapId, User teacher, RoadmapRequest request) {
        Roadmap roadmap = findTeacherRoadmap(roadmapId, teacher);
        roadmap.setTitle(request.getTitle());
        roadmap.setDescription(request.getDescription());
        roadmapRepository.save(roadmap);
    }

    @Override
    @Transactional
    public void deleteRoadmap(Integer roadmapId, User teacher) {
        Roadmap roadmap = findTeacherRoadmap(roadmapId, teacher);
        List<Lesson> lessons = lessonRepository.findByRoadmapOrderByOrderIndexAscCreatedAtAsc(roadmap);
        for (Lesson lesson : lessons) {
            List<Material> materials = materialRepository.findByLessonOrderByCreatedAtAsc(lesson);
            for (Material material : materials) {
                materialTrackingRepository.deleteAll(materialTrackingRepository.findByMaterial(material));
            }
            materialRepository.deleteAll(materials);
        }
        lessonRepository.deleteAll(lessons);
        roadmapRepository.delete(roadmap);
    }

    @Override
    @Transactional
    public void toggleRoadmapPublished(Integer roadmapId, User teacher) {
        Roadmap roadmap = findTeacherRoadmap(roadmapId, teacher);
        roadmap.setPublished(!Boolean.TRUE.equals(roadmap.getPublished()));
        roadmapRepository.save(roadmap);
    }

    @Override
    @Transactional
    public void createLesson(Integer roadmapId, User teacher, LearningLessonRequest request) {
        Roadmap roadmap = findTeacherRoadmap(roadmapId, teacher);
        lessonRepository.save(Lesson.builder()
                .title(request.getTitle())
                .content(request.getDescription())
                .videoUrl(request.getVideoUrl())
                .classroom(roadmap.getClassRoom())
                .roadmap(roadmap)
                .orderIndex(request.getOrderIndex())
                .visible(true)
                .build());
    }

    @Override
    public LessonResponse getTeacherLesson(Integer lessonId, User teacher) {
        return lessonMapper.toResponse(findTeacherLesson(lessonId, teacher));
    }

    @Override
    public List<MaterialResponse> getTeacherLessonMaterials(Integer lessonId, User teacher) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        return materialMapper.toResponseList(materialRepository.findByLessonOrderByCreatedAtAsc(lesson));
    }

    @Override
    @Transactional
    public void updateLesson(Integer lessonId, User teacher, LearningLessonRequest request) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getDescription());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setOrderIndex(request.getOrderIndex());
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void toggleLessonVisible(Integer lessonId, User teacher) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        lesson.setVisible(!Boolean.TRUE.equals(lesson.getVisible()));
        lessonRepository.save(lesson);
    }

    @Override
    @Transactional
    public void createMaterial(Integer lessonId, User teacher, MaterialRequest request) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        Material material = Material.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .lesson(lesson)
                .type(request.getType())
                .externalUrl(normalizeExternalUrl(request))
                .filePath(saveFileIfPresent(request.getFile()))
                .published(false)
                .build();
        validateMaterialContent(material);
        materialRepository.save(material);
    }

    @Override
    public MaterialResponse getTeacherMaterial(Integer materialId, User teacher) {
        return materialMapper.toResponse(findTeacherMaterial(materialId, teacher));
    }

    @Override
    @Transactional
    public void updateMaterial(Integer materialId, User teacher, MaterialRequest request) {
        Material material = findTeacherMaterial(materialId, teacher);
        material.setTitle(request.getTitle());
        material.setDescription(request.getDescription());
        material.setType(request.getType());
        material.setExternalUrl(normalizeExternalUrl(request));
        String filePath = saveFileIfPresent(request.getFile());
        if (filePath != null) {
            material.setFilePath(filePath);
        }
        validateMaterialContent(material);
        materialRepository.save(material);
    }

    @Override
    @Transactional
    public void toggleMaterialPublished(Integer materialId, User teacher) {
        Material material = findTeacherMaterial(materialId, teacher);
        material.setPublished(!Boolean.TRUE.equals(material.getPublished()));
        materialRepository.save(material);
    }

    @Override
    public List<RoadmapResponse> getStudentRoadmaps(Integer classId, User student) {
        Classroom classroom = findStudentClass(classId, student);
        List<RoadmapResponse> roadmaps = roadmapMapper.toResponseList(
                roadmapRepository.findByClassRoomAndPublishedTrueOrderByCreatedAtDesc(classroom)
        );
        roadmaps.forEach(roadmap -> roadmap.setLessons(lessonMapper.toResponseList(
                lessonRepository.findByRoadmapAndVisibleTrueOrderByOrderIndexAscCreatedAtAsc(findRoadmapById(roadmap.getId()))
        )));
        return roadmaps;
    }

    @Override
    public LessonResponse getStudentLesson(Integer lessonId, User student) {
        Lesson lesson = findStudentLesson(lessonId, student);
        LessonResponse response = lessonMapper.toResponse(lesson);
        // Populate embed video URL nếu lesson có videoUrl
        response.setEmbedVideoUrl(EmbedUrlUtil.toEmbedUrl(lesson.getVideoUrl()));
        return response;
    }

    @Override
    public List<MaterialResponse> getStudentLessonMaterials(Integer lessonId, User student) {
        Lesson lesson = findStudentLesson(lessonId, student);
        List<MaterialResponse> materials = materialMapper.toResponseList(
                materialRepository.findByLessonAndPublishedTrueOrderByCreatedAtAsc(lesson)
        );
        // Đặt viewed=false mặc định cho tất cả, sau đó ghi đè nếu có tracking record
        materials.forEach(material -> {
            material.setViewed(false);
            material.setEmbedUrl(EmbedUrlUtil.toEmbedUrl(material.getExternalUrl()));
            materialTrackingRepository.findByStudentAndMaterial(student, findMaterialById(material.getId()))
                    .ifPresent(tracking -> material.setViewed(Boolean.TRUE.equals(tracking.getViewed())));
        });
        return materials;
    }

    @Override
    public MaterialResponse getStudentMaterial(Integer materialId, User student) {
        Material material = findStudentMaterial(materialId, student);
        MaterialResponse response = materialMapper.toResponse(material);
        response.setViewed(materialTrackingRepository.findByStudentAndMaterial(student, material)
                .map(MaterialTracking::getViewed)
                .orElse(false));
        // Populate embed URL nếu material có externalUrl có thể nhúng
        response.setEmbedUrl(EmbedUrlUtil.toEmbedUrl(material.getExternalUrl()));
        return response;
    }

    @Override
    @Transactional
    public void markMaterialViewed(Integer materialId, User student) {
        Material material = findStudentMaterial(materialId, student);
        MaterialTracking tracking = materialTrackingRepository.findByStudentAndMaterial(student, material)
                .orElseGet(() -> MaterialTracking.builder()
                        .student(student)
                        .material(material)
                        .build());
        tracking.setViewed(true);
        tracking.setViewedAt(new Date());
        materialTrackingRepository.save(tracking);
    }

    @Override
    public List<MaterialTrackingResponse> getLearningHistory(User student) {
        return materialTrackingMapper.toResponseList(materialTrackingRepository.findByStudentOrderByViewedAtDesc(student));
    }

    @Override
    public MaterialType[] getMaterialTypes() {
        return MaterialType.values();
    }

    @Override
    @Transactional
    public void deleteLesson(Integer lessonId, User teacher) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        List<Material> materials = materialRepository.findByLessonOrderByCreatedAtAsc(lesson);
        for (Material material : materials) {
            materialTrackingRepository.deleteAll(materialTrackingRepository.findByMaterial(material));
        }
        materialRepository.deleteAll(materials);
        lessonRepository.delete(lesson);
    }

    @Override
    @Transactional
    public void deleteMaterial(Integer materialId, User teacher) {
        Material material = findTeacherMaterial(materialId, teacher);
        materialTrackingRepository.deleteAll(materialTrackingRepository.findByMaterial(material));
        materialRepository.delete(material);
    }

    private Roadmap findTeacherRoadmap(Integer roadmapId, User teacher) {
        Roadmap roadmap = findRoadmapById(roadmapId);
        assertTeacherOwnsClass(roadmap.getClassRoom(), teacher);
        return roadmap;
    }

    private Lesson findTeacherLesson(Integer lessonId, User teacher) {
        Lesson lesson = findLessonById(lessonId);
        assertTeacherOwnsClass(resolveLessonClass(lesson), teacher);
        return lesson;
    }

    private Material findTeacherMaterial(Integer materialId, User teacher) {
        Material material = findMaterialById(materialId);
        assertTeacherOwnsClass(resolveLessonClass(material.getLesson()), teacher);
        return material;
    }

    private Classroom findTeacherClass(Integer classId, User teacher) {
        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Khong tim thay lop hoc."));
        assertTeacherOwnsClass(classroom, teacher);
        return classroom;
    }

    private void assertTeacherOwnsClass(Classroom classroom, User teacher) {
        if (classroom.getTeacher() == null || !classroom.getTeacher().getUserId().equals(teacher.getUserId())) {
            throw new BusinessException("Ban khong co quyen thao tac hoc lieu cua lop nay.");
        }
    }

    private Classroom findStudentClass(Integer classId, User student) {
        Classroom classroom = classroomRepository.findById(classId)
                .orElseThrow(() -> new BusinessException("Khong tim thay lop hoc."));
        if (classroom.getStatus() != ClassStatus.ACTIVE
                || !enrollmentRepository.existsByStudentAndClassRoomAndStatus(student, classroom, EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Ban khong co quyen xem hoc lieu cua lop nay.");
        }
        return classroom;
    }

    private Lesson findStudentLesson(Integer lessonId, User student) {
        Lesson lesson = findLessonById(lessonId);
        Roadmap roadmap = lesson.getRoadmap();
        if (roadmap == null || !Boolean.TRUE.equals(roadmap.getPublished()) || !Boolean.TRUE.equals(lesson.getVisible())) {
            throw new BusinessException("Lesson hien khong kha dung.");
        }
        findStudentClass(roadmap.getClassRoom().getClassId(), student);
        return lesson;
    }

    private Material findStudentMaterial(Integer materialId, User student) {
        Material material = findMaterialById(materialId);
        if (!Boolean.TRUE.equals(material.getPublished())) {
            throw new BusinessException("Material hien khong kha dung.");
        }
        findStudentLesson(material.getLesson().getLessonId(), student);
        return material;
    }

    private Roadmap findRoadmapById(Integer roadmapId) {
        return roadmapRepository.findById(roadmapId)
                .orElseThrow(() -> new BusinessException("Khong tim thay roadmap."));
    }

    private Lesson findLessonById(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("Khong tim thay lesson."));
    }

    private Material findMaterialById(Integer materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException("Khong tim thay material."));
    }

    private Classroom resolveLessonClass(Lesson lesson) {
        if (lesson.getClassroom() != null) {
            return lesson.getClassroom();
        }
        if (lesson.getRoadmap() != null) {
            return lesson.getRoadmap().getClassRoom();
        }
        throw new BusinessException("Lesson chua gan voi lop hoc.");
    }

    private String normalizeExternalUrl(MaterialRequest request) {
        if (request.getExternalUrl() == null || request.getExternalUrl().isBlank()) {
            return null;
        }
        return request.getExternalUrl().trim();
    }

    private String saveFileIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        // [FEAT-05] Chỉ chấp nhận file PDF
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "material" : file.getOriginalFilename());
        String lowerName = originalName.toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!lowerName.endsWith(".pdf") || !contentType.contains("pdf")) {
            throw new BusinessException("Chỉ chấp nhận file PDF. Vui lòng upload file có đuôi .pdf.");
        }
        try {
            Files.createDirectories(MATERIAL_UPLOAD_DIR);
            String storedName = UUID.randomUUID() + "-" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = MATERIAL_UPLOAD_DIR.resolve(storedName).normalize();
            if (!target.startsWith(MATERIAL_UPLOAD_DIR)) {
                throw new BusinessException("Tên file upload không hợp lệ.");
            }
            file.transferTo(target);
            return "/uploads/materials/" + storedName;
        } catch (IOException exception) {
            throw new BusinessException("Không thể lưu file upload.");
        }
    }

    private void validateMaterialContent(Material material) {
        // [FEAT-05] VIDEO bắt buộc phải có link, không cho upload file
        if (material.getType() == MaterialType.VIDEO) {
            if (material.getExternalUrl() == null || material.getExternalUrl().isBlank()) {
                throw new BusinessException("Tài liệu dạng VIDEO phải nhập URL video. Không hỗ trợ upload file video trực tiếp.");
            }
        }
        boolean linkType = material.getType() == MaterialType.YOUTUBE_LINK
                || material.getType() == MaterialType.OTHER_LINK
                || material.getType() == MaterialType.VIDEO;
        if (linkType && (material.getExternalUrl() == null || material.getExternalUrl().isBlank())) {
            throw new BusinessException("Material dạng link cần có externalUrl.");
        }
        // [FEAT-05] Kiểm tra URL YouTube hợp lệ
        if (material.getType() == MaterialType.YOUTUBE_LINK) {
            String url = material.getExternalUrl();
            if (!isValidYoutubeUrl(url)) {
                throw new BusinessException("URL YouTube không hợp lệ. Chỉ chấp nhận liên kết từ youtube.com hoặc youtu.be.");
            }
        }
        if (!linkType && material.getFilePath() == null && (material.getExternalUrl() == null || material.getExternalUrl().isBlank())) {
            throw new BusinessException("Material cần có file upload hoặc externalUrl.");
        }
    }

    /** [FEAT-05] Validate URL YouTube theo pattern youtube.com hoặc youtu.be. */
    private boolean isValidYoutubeUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.trim().toLowerCase();
        return lower.contains("youtube.com/watch") ||
               lower.contains("youtube.com/embed") ||
               lower.contains("youtu.be/") ||
               lower.contains("youtube.com/shorts");
    }
}
