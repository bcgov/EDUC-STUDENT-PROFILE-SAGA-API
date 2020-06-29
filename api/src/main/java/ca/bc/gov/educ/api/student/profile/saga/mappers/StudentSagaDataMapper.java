package ca.bc.gov.educ.api.student.profile.saga.mappers;

import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.ump.StudentProfileCompleteSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.base.StudentSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface StudentSagaDataMapper {
  StudentSagaDataMapper mapper = Mappers.getMapper(StudentSagaDataMapper.class);
  StudentSagaData toStudentSaga(StudentProfileCompleteSagaData studentProfileCompleteSagaData);
  StudentSagaData toStudentSaga(PenRequestCompleteSagaData penRequestCompleteSagaData);
}
