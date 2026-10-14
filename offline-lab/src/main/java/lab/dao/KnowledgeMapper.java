package lab.dao;

import com.mybatisflex.core.BaseMapper;
import lab.entity.KnowledgePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeMapper extends BaseMapper<KnowledgePO> {}
