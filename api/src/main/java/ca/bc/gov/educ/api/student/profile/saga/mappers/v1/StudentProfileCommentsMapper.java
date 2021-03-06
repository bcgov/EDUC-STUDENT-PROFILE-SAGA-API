package ca.bc.gov.educ.api.student.profile.saga.mappers.v1;

import ca.bc.gov.educ.api.student.profile.saga.mappers.UUIDMapper;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileComments;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileReturnActionSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface StudentProfileCommentsMapper {
  StudentProfileCommentsMapper mapper = Mappers.getMapper(StudentProfileCommentsMapper.class);

  @Mapping(target = "studentRequestID", source = "studentProfileRequestID")
  StudentProfileComments toComments(StudentProfileCommentsSagaData studentProfileCommentsSagaData);

  @Mapping(target = "studentRequestID", source = "studentProfileRequestID")
  StudentProfileComments toComments(StudentProfileReturnActionSagaData studentProfileReturnActionSagaData);
}
