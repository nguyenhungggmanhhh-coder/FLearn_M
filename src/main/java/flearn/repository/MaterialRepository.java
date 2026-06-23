package flearn.repository;

import flearn.entity.Lesson;
import flearn.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Integer> {
    List<Material> findByLessonOrderByCreatedAtAsc(Lesson lesson);

    List<Material> findByLessonAndPublishedTrueOrderByCreatedAtAsc(Lesson lesson);

    /** Đếm tổng số tài liệu đã published trong danh sách lessons (dùng cho tiến độ lớp). */
    @Query("SELECT COUNT(m) FROM Material m WHERE m.lesson IN :lessons AND m.published = true")
    long countPublishedByLessons(@Param("lessons") java.util.List<Lesson> lessons);
}
