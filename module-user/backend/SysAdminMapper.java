package com.campus.team.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.team.data.entity.SysAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysAdminMapper extends BaseMapper<SysAdmin> {

    @Select("""
            SELECT r.role_code FROM sys_role r
            INNER JOIN sys_admin a ON r.id = a.role_id
            WHERE a.id = #{adminId}
            """)
    String selectRoleCodeByAdminId(Long adminId);
}
