package ca.bc.gov.educ.api.student.profile.saga.mappers.v1;

import ca.bc.gov.educ.api.student.profile.saga.mappers.UUIDMapper;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface StudentSagaDataMapper {
  StudentSagaDataMapper mapper = Mappers.getMapper(StudentSagaDataMapper.class);

  @Mapping(target = "trueStudentID", ignore = true)
  @Mapping(target = "statusCode", ignore = true)
  @Mapping(target = "memo", ignore = true)
  @Mapping(target = "gradeYear", ignore = true)
  @Mapping(target = "documentTypeCode", ignore = true)
  @Mapping(target = "demogCode", ignore = true)
  @Mapping(target = "dateOfConfirmation", ignore = true)
  StudentSagaData toStudentSaga(StudentProfileCompleteSagaData studentProfileCompleteSagaData);

  @Mapping(target = "trueStudentID", ignore = true)
  @Mapping(target = "statusCode", ignore = true)
  @Mapping(target = "memo", ignore = true)
  @Mapping(target = "gradeYear", ignore = true)
  @Mapping(target = "documentTypeCode", ignore = true)
  @Mapping(target = "demogCode", ignore = true)
  @Mapping(target = "dateOfConfirmation", ignore = true)
  StudentSagaData toStudentSaga(PenRequestCompleteSagaData penRequestCompleteSagaData);
}
