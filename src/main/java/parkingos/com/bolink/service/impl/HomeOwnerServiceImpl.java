package parkingos.com.bolink.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import parkingos.com.bolink.dao.spring.CommonDao;
import parkingos.com.bolink.models.HomeOwnerTb;
import parkingos.com.bolink.models.ParkLogTb;
import parkingos.com.bolink.service.HomeOwnerService;
import parkingos.com.bolink.service.SaveLogService;
import parkingos.com.bolink.service.SupperSearchService;
import parkingos.com.bolink.utils.OrmUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class HomeOwnerServiceImpl implements HomeOwnerService {

    Logger logger = Logger.getLogger(HomeOwnerServiceImpl.class);

    @Autowired
    private CommonDao commonDao;
    @Autowired
    private SupperSearchService<HomeOwnerTb> supperSearchService;
    @Autowired
    private SaveLogService saveLogService;


    @Override
    public JSONObject selectResultByConditions(Map<String, String> reqmap) {
        HomeOwnerTb homeOwnerTb = new HomeOwnerTb();
        homeOwnerTb.setComid(Long.parseLong(reqmap.get("comid")));
        JSONObject result = supperSearchService.supperSearch(homeOwnerTb,reqmap);
        return result;
    }

    @Override
    public JSONObject deleteOwner(Long id) {
        return null;
    }

    @Override
    public JSONObject addOwner(HomeOwnerTb homeOwnerTb) {
        JSONObject result = new JSONObject();
        result.put("state",0);
        result.put("msg","新建业主失败");
        int update = 0;
        HomeOwnerTb condition = new HomeOwnerTb();
        condition.setPhone(homeOwnerTb.getPhone());
        condition.setComid(homeOwnerTb.getComid());
        int count = commonDao.selectCountByConditions(condition);
        logger.info("===>>>>count:"+count);
        logger.info("===>>>>homeOwnerTb:"+homeOwnerTb);
        if(homeOwnerTb.getId()==null) {
            if(count==0) {
                update = commonDao.insert(homeOwnerTb);
            }else{
                condition = (HomeOwnerTb) commonDao.selectObjectByConditions(condition);
                logger.info("===>>>>condition:"+condition);
                homeOwnerTb.setId(condition.getId());
                update = commonDao.updateByPrimaryKey(homeOwnerTb);
            }
        }else {
            if(count==0){
                update = commonDao.updateByPrimaryKey(homeOwnerTb);
            }else{
                HomeOwnerTb con = new HomeOwnerTb();
                con.setId(homeOwnerTb.getId());
                condition = (HomeOwnerTb) commonDao.selectObjectByConditions(con);
                logger.info("===>>>>condition:"+condition);
                logger.info("===>>>>homeOwnerTb:"+homeOwnerTb);
                if(condition.getPhone().equals(homeOwnerTb.getPhone())){
                    update = commonDao.updateByPrimaryKey(homeOwnerTb);
                }else{
                    result.put("state",0);
                    result.put("msg","更新业主失败,已存在该手机号的业主信息。");
                }
            }

        }
        if(update==1){
            result.put("state",1);
            result.put("msg","新建业主成功");
        }
        return result;
    }

    @Override
    public List<List<Object>> exportExcel(Map<String, String> reqParameterMap) {
        //删除分页条件  查询该条件下所有  不然为一页数据
        reqParameterMap.remove("orderby");

        //获得要导出的结果
        JSONObject result = selectResultByConditions(reqParameterMap);

        List<HomeOwnerTb> blackList = JSON.parseArray(result.get("rows").toString(), HomeOwnerTb.class);

        List<List<Object>> bodyList = new ArrayList<List<Object>>();
        if (blackList != null && blackList.size() > 0) {
            String[] f = new String[]{"name","home_number", "phone","identity_card",  "state", "remark"};
            for (HomeOwnerTb homeOwnerTb : blackList) {
                List<Object> values = new ArrayList<Object>();
                OrmUtil<HomeOwnerTb> otm = new OrmUtil<HomeOwnerTb>();
                Map map = otm.pojoToMap(homeOwnerTb);
                for (String field : f) {
                    Object v = map.get(field);
                    if("state".equals(field)){
                        switch(Integer.valueOf(v + "")){
                            case 0:values.add("正常");break;
                            case 1:values.add("禁用");break;
                        }
                    }else {
                        values.add(v+"");
                    }
                }
                bodyList.add(values);
            }
        }
        return bodyList;
    }

    @Override
    @Transactional
    public void saveOrUpdateAll(List<HomeOwnerTb> homeOwnerTbList,String nickname,Long uin) {
        if(homeOwnerTbList!=null&&homeOwnerTbList.size()>0){
            for(int i= 0;i<homeOwnerTbList.size();i++) {
                HomeOwnerTb homeOwnerTb = homeOwnerTbList.get(i);
                HomeOwnerTb conditions = new HomeOwnerTb();
                conditions.setComid(homeOwnerTb.getComid());
                conditions.setPhone(homeOwnerTb.getPhone());
                int count = commonDao.selectCountByConditions(conditions);
                if (count > 0) {//更新操作
                    commonDao.updateByConditions(homeOwnerTb, conditions);
                    ParkLogTb parkLogTb = new ParkLogTb();
                    parkLogTb.setOperateUser(nickname);
                    parkLogTb.setOperateTime(System.currentTimeMillis() / 1000);
                    parkLogTb.setOperateType(5);
                    parkLogTb.setContent(uin + "(" + nickname + ")" + "导入更新了业主信息"+homeOwnerTb.getPhone());
                    parkLogTb.setType("homeowner");
                    parkLogTb.setParkId(homeOwnerTb.getComid());
                    saveLogService.saveLog(parkLogTb);
                } else {
                    ParkLogTb parkLogTb = new ParkLogTb();
                    parkLogTb.setOperateUser(nickname);
                    parkLogTb.setOperateTime(System.currentTimeMillis() / 1000);
                    parkLogTb.setOperateType(5);
                    parkLogTb.setContent(uin + "(" + nickname + ")" + "导入增加了业主信息:"+homeOwnerTb.getPhone());
                    parkLogTb.setType("homeowner");
                    parkLogTb.setParkId(homeOwnerTb.getComid());
                    saveLogService.saveLog(parkLogTb);
                    commonDao.insert(homeOwnerTb);
                }
            }
        }
    }
}