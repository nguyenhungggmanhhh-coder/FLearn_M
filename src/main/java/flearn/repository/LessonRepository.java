package flearn.repository;

import flearn.entity.Classroom;
import flearn.entity.Lesson;
import flearn.entity.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    List<Lesson> findByClassroomOrderByCreatedAtDesc(Classroom classroom);

    List<Lesson> findByRoadmapOrderByOrderIndexAscCreatedAtAsc(Roadmap roadmap);

    List<Lesson> findByRoadmapAndVisibleTrueOrderByOrderIndexAscCreatedAtAsc(Roadmap roadmap);

    /** Lấy tất cả lesson của lớp (dùng cho tiến độ lớp). */
    List<Lesson> findByClassroom(Classroom classroom);
}
