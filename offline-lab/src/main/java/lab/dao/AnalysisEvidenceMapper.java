package lab.dao;

import com.mybatisflex.core.BaseMapper;
import lab.entity.AnalysisEvidencePO;
import org.apache.ibatis.annotations.Mapper;

/** AnalysisEvidence 数据表的 MyBatis-Flex 映射入口；BaseMapper 提供基础增删改查，@Mapper 使其由框架创建实现。 */
@Mapper
public interface AnalysisEvidenceMapper extends BaseMapper<AnalysisEvidencePO> {}
