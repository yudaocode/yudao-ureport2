package cn.iocoder.ureport2.compat;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Bean 数据集：模板中通过 type="spring" bean="demoBeanDataset" 引用
 *
 * @author 芋道源码
 */
@Component("demoBeanDataset")
public class DemoBeanDataset {

    public List<Map<String, Object>> loadDepts(String datasourceName, String datasetName, Map<String, Object> parameters) {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(dept("D01", "BeanDeptAlpha"));
        list.add(dept("D02", "BeanDeptBeta"));
        return list;
    }

    private static Map<String, Object> dept(String code, String name) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("name", name);
        return map;
    }

}
