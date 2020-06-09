package ca.bc.gov.educ.api.student.profile.saga.mappers;

import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileComments;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.StudentProfileRequestRejectActionSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface StudentProfileCommentsMapper {
  StudentProfileCommentsMapper mapper = Mappers.getMapper(StudentProfileCommentsMapper.class);

  StudentProfileComments toComments(StudentProfileCommentsSagaData studentProfileCommentsSagaData);
  StudentProfileComments toComments(StudentProfileRequestRejectActionSagaData studentProfileRequestRejectActionSagaData);
}
