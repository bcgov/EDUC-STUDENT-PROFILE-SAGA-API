package ca.bc.gov.educ.api.student.profile.saga.mappers;

import ca.bc.gov.educ.api.student.profile.saga.model.Saga;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface SagaMapper {
  SagaMapper mapper = Mappers.getMapper(SagaMapper.class);

  ca.bc.gov.educ.api.student.profile.saga.struct.Saga toStruct(Saga saga);
}
