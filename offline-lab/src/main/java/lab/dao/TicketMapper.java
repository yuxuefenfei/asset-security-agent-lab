package lab.dao;

import com.mybatisflex.core.BaseMapper;
import lab.entity.TicketPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseMapper<TicketPO> {}
