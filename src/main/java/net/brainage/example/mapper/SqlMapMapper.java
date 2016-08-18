package net.brainage.example.mapper;

import org.apache.ibatis.session.ResultHandler;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.omg.CORBA.Object;

/**
 * Created by ms29.seo on 2016-08-18.
 */
public class SqlMapMapper extends SqlSessionDaoSupport {

    public void select(String sqlId, Object parameterObject, ResultHandler resultHandler) {
        getSqlSession().select(sqlId, parameterObject, resultHandler);
    }

    public void select(String sqlId, ResultHandler resultHandler) {
        getSqlSession().select(sqlId, resultHandler);
    }

}
