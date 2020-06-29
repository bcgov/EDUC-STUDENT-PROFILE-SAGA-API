package ca.bc.gov.educ.api.student.profile.saga.mappers;


import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestComments;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestCommentsSagaData;
import ca.bc.gov.educ.api.student.profile.saga.struct.gmp.PenRequestReturnSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@SuppressWarnings("squid:S1214")
public interface PenRequestCommentsMapper {
  PenRequestCommentsMapper mapper = Mappers.getMapper(PenRequestCommentsMapper.class);

  PenRequestComments toPenReqComments(PenRequestCommentsSagaData penRequestCommentsSagaData);
  PenRequestComments toPenReqComments(PenRequestReturnSagaData penRequestReturnSagaData);
}
