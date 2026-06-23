package flearn.module.management.mapper;

import flearn.module.management.dto.response.ClassroomResponse;
import flearn.module.auth.mapper.UserMapper;
import flearn.entity.Classroom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = UserMapper.class, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface ClassroomMapper {
    @Mapping(target = "classCode", source = "classCode")
    @Mapping(target = "status", expression = "java(classroom.getStatus() == null ? null : classroom.getStatus().name())")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.courseName")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "inviteCodeVisible", source = "inviteCodeVisible")
    @Mapping(target = "inviteCodeGeneratedAt", source = "inviteCodeGeneratedAt")
    ClassroomResponse toResponse(Classroom classroom);

    List<ClassroomResponse> toResponseList(List<Classroom> classrooms);
}
