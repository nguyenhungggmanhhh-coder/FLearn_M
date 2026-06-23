package flearn.module.quiz.mapper;

import flearn.module.quiz.dto.response.QuizResponse;
import flearn.entity.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = QuestionMapper.class, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface QuizMapper {
    @Mapping(target = "lessonId", source = "lesson.lessonId")
    @Mapping(target = "lessonTitle", source = "lesson.title")
    @Mapping(target = "classId", source = "lesson.classroom.classId")
    @Mapping(target = "deadlinePassed", ignore = true)
    @Mapping(target = "submittedAttempts", ignore = true)
    @Mapping(target = "questions", ignore = true) // Populated manually in QuizServiceImpl
    QuizResponse toResponse(Quiz quiz);

    List<QuizResponse> toResponseList(List<Quiz> quizzes);
}
