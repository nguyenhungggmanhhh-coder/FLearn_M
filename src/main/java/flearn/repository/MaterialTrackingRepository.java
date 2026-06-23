package flearn.repository;

import flearn.entity.Lesson;
import flearn.entity.Material;
import flearn.entity.MaterialTracking;
import flearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialTrackingRepository extends JpaRepository<MaterialTracking, Integer> {
    Optional<MaterialTracking> findByStudentAndMaterial(User student, Material material);

    List<MaterialTracking> findByMaterial(Material material);

    List<MaterialTracking> findByStudentOrderByViewedAtDesc(User student);

    /**
     * Đếm số tài liệu sinh viên đã xem trong danh sách lesson của lớp.
     * Dùng để hiển thị tiến độ "X / Y bài" trên thẻ lớp.
     */
    @Query("SELECT COUNT(mt) FROM MaterialTracking mt " +
           "WHERE mt.student = :student " +
           "AND mt.viewed = true " +
           "AND mt.material IN (SELECT m FROM Material m WHERE m.lesson IN :lessons AND m.published = true)")
    long countViewedByStudentInLessons(@Param("student") User student,
                                       @Param("lessons") List<Lesson> lessons);
}
