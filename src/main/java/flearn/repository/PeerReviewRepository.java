package flearn.repository;

import flearn.entity.PeerReview;
import flearn.entity.QuizResult;
import flearn.entity.User;
import flearn.enums.PeerReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeerReviewRepository extends JpaRepository<PeerReview, Integer> {

    /** Bài chấm được phân công cho một reviewer. */
    List<PeerReview> findByReviewerAndStatus(User reviewer, PeerReviewStatus status);

    /** Tất cả bài chấm của reviewer (mọi trạng thái). */
    List<PeerReview> findByReviewer(User reviewer);

    /** Tất cả lượt chấm của một bài nộp. */
    List<PeerReview> findByRevieweeResult(QuizResult result);

    /** Kiểm tra reviewer đã được phân công chấm bài này chưa. */
    boolean existsByRevieweeResultAndReviewer(QuizResult result, User reviewer);

    /** Lấy bài chấm cụ thể (reviewer + bài). */
    Optional<PeerReview> findByRevieweeResultAndReviewer(QuizResult result, User reviewer);

    /**
     * Tính điểm TB từ tất cả peer đã nộp cho một bài.
     * Dùng để tính finalScore và deviationFromMean.
     */
    @Query("SELECT AVG(pr.score) FROM PeerReview pr WHERE pr.revieweeResult = :result AND pr.status = 'SUBMITTED' AND pr.score IS NOT NULL")
    Optional<Double> findAverageScoreByResult(@Param("result") QuizResult result);

    /** Đếm số lượt chấm đã nộp. */
    long countByRevieweeResultAndStatus(QuizResult result, PeerReviewStatus status);

    /** Bài chấm theo quiz (dùng cho teacher xem toàn bộ). */
    @Query("SELECT pr FROM PeerReview pr WHERE pr.revieweeResult.quiz.quizId = :quizId")
    List<PeerReview> findByQuizId(@Param("quizId") Integer quizId);
}
