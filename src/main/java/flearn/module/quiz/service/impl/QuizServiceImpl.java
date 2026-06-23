package flearn.module.quiz.service.impl;

import flearn.module.quiz.dto.request.QuestionRequest;
import flearn.module.quiz.dto.request.QuizRequest;
import flearn.module.quiz.dto.response.AnswerOptionResponse;
import flearn.module.quiz.dto.response.QuestionResponse;
import flearn.module.quiz.dto.response.QuizResponse;
import flearn.module.quiz.dto.response.QuizResultResponse;
import flearn.entity.Classroom;
import flearn.entity.Lesson;
import flearn.entity.Question;
import flearn.entity.Quiz;
import flearn.entity.QuizResult;
import flearn.entity.User;
import flearn.enums.ClassStatus;
import flearn.enums.EnrollmentStatus;
import flearn.enums.QuestionType;
import flearn.enums.QuizSubmissionStatus;
import flearn.common.exception.BusinessException;
import flearn.module.quiz.mapper.QuizMapper;
import flearn.module.quiz.mapper.QuizResultMapper;
import flearn.repository.EnrollmentRepository;
import flearn.repository.LessonRepository;
import flearn.repository.QuestionRepository;
import flearn.repository.QuizRepository;
import flearn.repository.QuizResultRepository;
import flearn.module.quiz.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Validated
public class QuizServiceImpl implements QuizService {
    private static final double MAX_SCORE = 10.0;
    private static final List<QuizSubmissionStatus> COUNTED_ATTEMPT_STATUSES = List.of(
            QuizSubmissionStatus.SUBMITTED,
            QuizSubmissionStatus.LATE,
            QuizSubmissionStatus.LOCKED
    );

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizResultRepository quizResultRepository;
    private final LessonRepository lessonRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizMapper quizMapper;
    private final QuizResultMapper quizResultMapper;

    @Override
    public List<QuizResponse> getTeacherQuizzesByLesson(Integer lessonId, User teacher) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);
        return quizMapper.toResponseList(quizRepository.findByLessonOrderByCreatedAtDesc(lesson));
    }

    @Override
    public QuizResponse getTeacherQuiz(Integer quizId, User teacher) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        return toQuizResponse(quiz, true, null);
    }

    @Override
    @Transactional
    public void createQuiz(Integer lessonId, User teacher, QuizRequest request) {
        Lesson lesson = findTeacherLesson(lessonId, teacher);

        // Mỗi lesson chỉ được có 1 quiz (ràng buộc DB UNIQUE KEY)
        if (quizRepository.existsByLesson(lesson)) {
            throw new BusinessException("Lesson này đã có quiz. Bạn chỉ có thể chỉnh sửa quiz hiện tại.");
        }

        Quiz quiz = Quiz.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .description(request.getDescription())
                .published(false)
                .build();
        applyQuizSettings(quiz, request);
        quizRepository.save(quiz);
    }


    @Override
    @Transactional
    public void updateQuiz(Integer quizId, User teacher, QuizRequest request) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        applyQuizSettings(quiz, request);
        quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public void deleteQuiz(Integer quizId, User teacher) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        quizResultRepository.deleteAll(quizResultRepository.findByQuizOrderByScoreDescSubmittedAtAsc(quiz));
        quizRepository.delete(quiz);
    }

    @Override
    @Transactional
    public void togglePublish(Integer quizId, User teacher) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        if (!Boolean.TRUE.equals(quiz.getPublished())
                && questionRepository.findByQuizOrderByOrderIndexAscQuestionIdAsc(quiz).isEmpty()) {
            throw new BusinessException("Quiz cần có ít nhất một câu hỏi trước khi publish.");
        }
        quiz.setPublished(!Boolean.TRUE.equals(quiz.getPublished()));
        quizRepository.save(quiz);
    }

    @Override
    @Transactional
    public void createQuestion(Integer quizId, User teacher, QuestionRequest request) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        Question question = Question.builder().quiz(quiz).build();
        applyQuestion(question, request);
        questionRepository.save(question);
    }

    @Override
    public QuestionRequest getQuestionForEdit(Integer questionId, User teacher) {
        Question question = findTeacherQuestion(questionId, teacher);
        return QuestionRequest.builder()
                .type(question.getType())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctAnswer(question.getCorrectAnswer())
                .orderIndex(question.getOrderIndex())
                .build();
    }

    @Override
    public Integer getQuestionQuizId(Integer questionId, User teacher) {
        return findTeacherQuestion(questionId, teacher).getQuiz().getQuizId();
    }

    @Override
    @Transactional
    public void updateQuestion(Integer questionId, User teacher, QuestionRequest request) {
        Question question = findTeacherQuestion(questionId, teacher);
        applyQuestion(question, request);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Integer questionId, User teacher) {
        Question question = findTeacherQuestion(questionId, teacher);
        questionRepository.delete(question);
    }

    @Override
    public List<QuizResultResponse> getTeacherQuizResults(Integer quizId, User teacher) {
        Quiz quiz = findTeacherQuiz(quizId, teacher);
        return quizResultMapper.toResponseList(quizResultRepository.findByQuizOrderByScoreDescSubmittedAtAsc(quiz));
    }

    @Override
    public List<QuizResponse> getStudentQuizzesByLesson(Integer lessonId, User student) {
        Lesson lesson = findStudentLesson(lessonId, student);
        List<Quiz> quizzes = quizRepository.findByLessonAndPublishedTrueOrderByCreatedAtDesc(lesson);
        return quizzes.stream()
                .map(quiz -> toQuizResponse(quiz, false, student))
                .toList();
    }

    @Override
    public QuizResponse startQuiz(Integer quizId, User student) {
        Quiz quiz = findStudentQuiz(quizId, student);
        validateCanStartQuiz(quiz, student);
        return toQuizResponse(quiz, true, student);
    }

    @Override
    @Transactional
    public QuizResultResponse submitQuiz(Integer quizId, User student, Map<String, String> answers) {
        Quiz quiz = findStudentQuiz(quizId, student);
        Date now = new Date();
        if (isDeadlinePassed(quiz, now)) {
            throw new BusinessException("Quiz đã quá deadline, không thể nộp bài.");
        }

        long submittedAttempts = countSubmittedAttempts(student, quiz);
        if (submittedAttempts >= quiz.getMaxAttempts()) {
            throw new BusinessException("Bạn đã hết số lần làm quiz này.");
        }

        Date startedAt = resolveStartedAt(answers, now);
        boolean lockedByTime = isTimeLimitExceeded(quiz, startedAt, now);
        List<Question> questions = questionRepository.findByQuizOrderByOrderIndexAscQuestionIdAsc(quiz);
        ScoreSummary score = grade(questions, answers);

        QuizResult result = QuizResult.builder()
                .student(student)
                .quiz(quiz)
                .attemptNo((int) submittedAttempts + 1)
                .startedAt(startedAt)
                .submittedAt(now)
                .completedAt(now)
                .status(lockedByTime ? QuizSubmissionStatus.LOCKED : QuizSubmissionStatus.SUBMITTED)
                .score(score.score())
                .correctCount(score.correctCount())
                .totalQuestions(score.totalQuestions())
                .build();
        quizResultRepository.save(result);
        return quizResultMapper.toResponse(result);
    }

    @Override
    public List<QuizResultResponse> getStudentQuizHistory(User student) {
        return quizResultMapper.toResponseList(quizResultRepository.findByStudentOrderByStartedAtDesc(student));
    }

    @Override
    public List<QuizResultResponse> getStudentQuizAttempts(Integer quizId, User student) {
        Quiz quiz = findStudentQuiz(quizId, student);
        return quizResultMapper.toResponseList(quizResultRepository.findByStudentAndQuizOrderByStartedAtDesc(student, quiz));
    }

    private void applyQuizSettings(Quiz quiz, QuizRequest request) {
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes());
        quiz.setDeadline(request.getDeadline());
        quiz.setShuffleQuestions(Boolean.TRUE.equals(request.getShuffleQuestions()));
        quiz.setShuffleAnswers(Boolean.TRUE.equals(request.getShuffleAnswers()));
        quiz.setMaxAttempts(request.getMaxAttempts() == null ? 1 : request.getMaxAttempts());
        quiz.setVideoTimestamp(request.getVideoTimestamp()); // null = không gate
        quiz.setQuestionPoolSize(request.getQuestionPoolSize()); // [FEAT-01] null = dùng tất cả
    }

    private void applyQuestion(Question question, QuestionRequest request) {
        QuestionType type = request.getType() == null ? QuestionType.MULTIPLE_CHOICE : request.getType();
        question.setType(type);
        question.setQuestionText(request.getQuestionText());
        question.setOrderIndex(request.getOrderIndex() == null ? 0 : request.getOrderIndex());
        question.setModelAnswer(request.getModelAnswer()); // [FEAT-02] Đáp án mẫu cho tự luận

        if (type == QuestionType.TRUE_FALSE) {
            question.setOptionA("Đúng");
            question.setOptionB("Sai");
            question.setOptionC(null);
            question.setOptionD(null);
            String correct = request.getCorrectAnswer() == null ? "" : request.getCorrectAnswer().trim().toUpperCase();
            if (!correct.equals("TRUE") && !correct.equals("FALSE")) {
                throw new BusinessException("Câu hỏi Đúng/Sai chỉ nhận đáp án TRUE hoặc FALSE.");
            }
            question.setCorrectAnswer(correct);
            return;
        }

        // [FEAT-02] ESSAY – không cần options và correctAnswer
        if (type == QuestionType.ESSAY) {
            question.setOptionA(null);
            question.setOptionB(null);
            question.setOptionC(null);
            question.setOptionD(null);
            question.setCorrectAnswer(null); // Chấm thủ công
            return;
        }

        // MULTIPLE_CHOICE và MULTI_SELECT đều cần 4 options
        requireOption(request.getOptionA(), "Đáp án A không được để trống.");
        requireOption(request.getOptionB(), "Đáp án B không được để trống.");
        requireOption(request.getOptionC(), "Đáp án C không được để trống.");
        requireOption(request.getOptionD(), "Đáp án D không được để trống.");
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());

        if (type == QuestionType.MULTIPLE_CHOICE) {
            String correct = request.getCorrectAnswer() == null ? "" : request.getCorrectAnswer().trim().toUpperCase();
            if (!List.of("A", "B", "C", "D").contains(correct)) {
                throw new BusinessException("Câu hỏi trắc nghiệm chỉ nhận đáp án đúng A, B, C hoặc D.");
            }
            question.setCorrectAnswer(correct);
        } else if (type == QuestionType.MULTI_SELECT) {
            // [FEAT-02] Đa đáp án: "A,C" hoặc "A,B,D"
            if (request.getCorrectAnswer() == null || request.getCorrectAnswer().isBlank()) {
                throw new BusinessException("Câu hỏi nhiều đáp án phải có ít nhất 1 đáp án đúng.");
            }
            String[] parts = request.getCorrectAnswer().trim().toUpperCase().split(",");
            for (String part : parts) {
                if (!List.of("A", "B", "C", "D").contains(part.trim())) {
                    throw new BusinessException("Đáp án '" + part.trim() + "' không hợp lệ. Chỉ nhận A, B, C, D.");
                }
            }
            // Lưu dạng chuẩn: sắp xếp và nối bằng dấu phẩy
            String normalized = java.util.Arrays.stream(parts)
                    .map(String::trim)
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(","));
            question.setCorrectAnswer(normalized);
        }
    }

    private void requireOption(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }

    private QuizResponse toQuizResponse(Quiz quiz, boolean includeQuestions, User student) {
        QuizResponse response = quizMapper.toResponse(quiz);
        Date now = new Date();
        response.setDeadlinePassed(isDeadlinePassed(quiz, now));
        if (student != null) {
            response.setSubmittedAttempts(countSubmittedAttempts(student, quiz));
        }
        if (includeQuestions) {
            List<Question> questions = new ArrayList<>(questionRepository.findByQuizOrderByOrderIndexAscQuestionIdAsc(quiz));
            // [FEAT-01] Rút ngẫu nhiên từ ngân hàng đề nếu questionPoolSize được cấu hình
            if (quiz.getQuestionPoolSize() != null && quiz.getQuestionPoolSize() > 0
                    && questions.size() > quiz.getQuestionPoolSize()) {
                Collections.shuffle(questions);
                questions = new ArrayList<>(questions.subList(0, quiz.getQuestionPoolSize()));
            } else if (Boolean.TRUE.equals(quiz.getShuffleQuestions())) {
                Collections.shuffle(questions);
            }
            response.setQuestions(questions.stream()
                    .map(question -> toQuestionResponse(question, Boolean.TRUE.equals(quiz.getShuffleAnswers())))
                    .toList());
        }
        return response;
    }

    private QuestionResponse toQuestionResponse(Question question, boolean shuffleAnswers) {
        List<AnswerOptionResponse> answers = new ArrayList<>();
        if (question.getType() == QuestionType.TRUE_FALSE) {
            answers.add(new AnswerOptionResponse("TRUE", "Đúng"));
            answers.add(new AnswerOptionResponse("FALSE", "Sai"));
        } else {
            answers.add(new AnswerOptionResponse("A", question.getOptionA()));
            answers.add(new AnswerOptionResponse("B", question.getOptionB()));
            answers.add(new AnswerOptionResponse("C", question.getOptionC()));
            answers.add(new AnswerOptionResponse("D", question.getOptionD()));
        }
        if (shuffleAnswers) {
            Collections.shuffle(answers);
        }
        return QuestionResponse.builder()
                .questionId(question.getQuestionId())
                .type(question.getType())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctAnswer(question.getCorrectAnswer())
                .orderIndex(question.getOrderIndex())
                .answers(answers)
                .build();
    }

    private void validateCanStartQuiz(Quiz quiz, User student) {
        if (isDeadlinePassed(quiz, new Date())) {
            throw new BusinessException("Quiz đã quá deadline.");
        }
        if (countSubmittedAttempts(student, quiz) >= quiz.getMaxAttempts()) {
            throw new BusinessException("Bạn đã hết số lần làm quiz này.");
        }
        if (questionRepository.findByQuizOrderByOrderIndexAscQuestionIdAsc(quiz).isEmpty()) {
            throw new BusinessException("Quiz chưa có câu hỏi.");
        }
    }

    private ScoreSummary grade(List<Question> questions, Map<String, String> answers) {
        if (questions.isEmpty()) {
            return new ScoreSummary(0.0, 0, 0);
        }
        int correctCount = 0;
        // Chỉ tính điểm các câu có thể tự chấm (loại ESSAY ra)
        List<Question> gradableQuestions = questions.stream()
                .filter(q -> q.getType() != QuestionType.ESSAY)
                .toList();
        if (gradableQuestions.isEmpty()) {
            // Toàn bộ là tự luận – chờ chấm thủ công
            return new ScoreSummary(0.0, 0, questions.size());
        }
        for (Question question : gradableQuestions) {
            String answer = answers == null ? null : answers.get("question_" + question.getQuestionId());
            if (answer == null) continue;
            if (question.getType() == QuestionType.MULTI_SELECT) {
                // [FEAT-02] So sánh tập hợp đáp án, không phụ thuộc thứ tự
                String[] submitted = answer.trim().toUpperCase().split(",");
                String[] correct = question.getCorrectAnswer() == null ? new String[0]
                        : question.getCorrectAnswer().split(",");
                java.util.Set<String> submittedSet = new java.util.HashSet<>(java.util.Arrays.asList(submitted));
                java.util.Set<String> correctSet = new java.util.HashSet<>(java.util.Arrays.asList(correct));
                if (submittedSet.equals(correctSet)) {
                    correctCount++;
                }
            } else {
                // TRUE_FALSE và MULTIPLE_CHOICE – so sánh trực tiếp
                if (answer.trim().equalsIgnoreCase(question.getCorrectAnswer())) {
                    correctCount++;
                }
            }
        }
        double score = Math.round(((double) correctCount / gradableQuestions.size()) * MAX_SCORE * 10.0) / 10.0;
        return new ScoreSummary(score, correctCount, gradableQuestions.size());
    }

    private Date resolveStartedAt(Map<String, String> answers, Date fallback) {
        if (answers == null || answers.get("startedAt") == null) {
            return fallback;
        }
        try {
            return new Date(Long.parseLong(answers.get("startedAt")));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isDeadlinePassed(Quiz quiz, Date now) {
        return quiz.getDeadline() != null && now.after(quiz.getDeadline());
    }

    private boolean isTimeLimitExceeded(Quiz quiz, Date startedAt, Date submittedAt) {
        if (quiz.getTimeLimitMinutes() == null || quiz.getTimeLimitMinutes() <= 0) {
            return false;
        }
        long allowedMillis = quiz.getTimeLimitMinutes() * 60_000L;
        return submittedAt.getTime() - startedAt.getTime() > allowedMillis;
    }

    private long countSubmittedAttempts(User student, Quiz quiz) {
        return quizResultRepository.countByStudentAndQuizAndStatusIn(student, quiz, COUNTED_ATTEMPT_STATUSES);
    }

    private Question findTeacherQuestion(Integer questionId, User teacher) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy câu hỏi."));
        assertTeacherOwnsLesson(question.getQuiz().getLesson(), teacher);
        return question;
    }

    private Quiz findTeacherQuiz(Integer quizId, User teacher) {
        Quiz quiz = findQuizById(quizId);
        assertTeacherOwnsLesson(quiz.getLesson(), teacher);
        return quiz;
    }

    private Quiz findStudentQuiz(Integer quizId, User student) {
        Quiz quiz = findQuizById(quizId);
        if (!Boolean.TRUE.equals(quiz.getPublished())) {
            throw new BusinessException("Quiz chưa được publish.");
        }
        findStudentLesson(quiz.getLesson().getLessonId(), student);
        return quiz;
    }

    private Quiz findQuizById(Integer quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy quiz."));
    }

    private Lesson findTeacherLesson(Integer lessonId, User teacher) {
        Lesson lesson = findLessonById(lessonId);
        assertTeacherOwnsLesson(lesson, teacher);
        return lesson;
    }

    private Lesson findStudentLesson(Integer lessonId, User student) {
        Lesson lesson = findLessonById(lessonId);
        if (lesson.getRoadmap() == null
                || !Boolean.TRUE.equals(lesson.getRoadmap().getPublished())
                || !Boolean.TRUE.equals(lesson.getVisible())) {
            throw new BusinessException("Lesson hiện không khả dụng.");
        }
        Classroom classroom = resolveLessonClass(lesson);
        if (classroom.getStatus() != ClassStatus.ACTIVE
                || !enrollmentRepository.existsByStudentAndClassRoomAndStatus(student, classroom, EnrollmentStatus.ACTIVE)) {
            throw new BusinessException("Bạn không có quyền làm quiz của lớp này.");
        }
        return lesson;
    }

    private Lesson findLessonById(Integer lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy lesson."));
    }

    private void assertTeacherOwnsLesson(Lesson lesson, User teacher) {
        Classroom classroom = resolveLessonClass(lesson);
        if (classroom.getTeacher() == null || !classroom.getTeacher().getUserId().equals(teacher.getUserId())) {
            throw new BusinessException("Bạn không có quyền thao tác quiz của lesson này.");
        }
    }

    private Classroom resolveLessonClass(Lesson lesson) {
        if (lesson.getClassroom() != null) {
            return lesson.getClassroom();
        }
        if (lesson.getRoadmap() != null) {
            return lesson.getRoadmap().getClassRoom();
        }
        throw new BusinessException("Lesson chưa gắn với lớp học.");
    }

    private record ScoreSummary(double score, int correctCount, int totalQuestions) {
    }
}
