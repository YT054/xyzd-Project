package com.campus.team.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.team.data.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    IPage<Activity> searchActivities(Page<Activity> page,
                                     @Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("activityStatus") Integer activityStatus,
                                     @Param("creatorId") Long creatorId);
}
