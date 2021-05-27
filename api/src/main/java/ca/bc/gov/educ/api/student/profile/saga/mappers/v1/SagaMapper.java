package ca.bc.gov.educ.api.student.profile.saga.mappers.v1;

import ca.bc.gov.educ.api.student.profile.saga.mappers.UUIDMapper;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.Saga;
import ca.bc.gov.educ.api.student.profile.saga.model.v1.SagaEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface SagaMapper {
  SagaMapper mapper = Mappers.getMapper(SagaMapper.class);

  ca.bc.gov.educ.api.student.profile.saga.struct.Saga toStruct(Saga saga);

  @Mapping(target = "sagaId", source = "saga.sagaId")
  ca.bc.gov.educ.api.student.profile.saga.struct.SagaEvent toEventStruct(SagaEvent sagaEvent);
}
