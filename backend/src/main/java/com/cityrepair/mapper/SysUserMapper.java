package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.SysUser;
<<<<<<< HEAD
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
=======
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("SELECT r.role_code FROM sys_user_role ur JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    java.util.List<String> findRolesByUserId(Long userId);
>>>>>>> feat/resident-repair
}
