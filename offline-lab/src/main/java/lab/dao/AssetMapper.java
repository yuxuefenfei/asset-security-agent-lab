package lab.dao;

import com.mybatisflex.core.BaseMapper;
import lab.entity.AssetPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetMapper extends BaseMapper<AssetPO> {}
