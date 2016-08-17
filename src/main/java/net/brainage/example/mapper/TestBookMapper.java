package net.brainage.example.mapper;

import net.brainage.example.model.TestBook;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestBookMapper {

    List<TestBook> getAll();

    int insert(TestBook paramObject);

    int insertBatch(@Param("list") List<TestBook> list);

    int update(TestBook paramObject);

    int updateBatch(@Param("list") List<TestBook> list);

}

