package flearn.repository;

import flearn.entity.QuizResult;
import flearn.entity.QuizSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionQuestionRepository extends JpaRepository<QuizSessionQuestion, Integer> {

    /** Lấy bộ câu hỏi của một session thi, theo thứ tự hiển thị. */
    List<QuizSessionQuestion> findByResultOrderByDisplayOrderAsc(QuizResult result);

    /** Kiểm tra session đã có câu hỏi chưa (tránh rút lại khi submit). */
    boolean existsByResult(QuizResult result);

    /** Xóa câu hỏi session khi xóa kết quả thi. */
    void deleteByResult(QuizResult result);
}
