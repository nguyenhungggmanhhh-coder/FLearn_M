package flearn.enums;

public enum QuestionType {
    TRUE_FALSE,
    MULTIPLE_CHOICE,  // 1 đáp án đúng (hiện tại)
    MULTI_SELECT,     // [FEAT-02] Nhiều đáp án đúng (VD correctAnswer = "A,C")
    ESSAY             // [FEAT-02] Tự luận
}
