package cn.iocoder.ureport2.compat;

import com.bstek.ureport.build.Context;
import com.bstek.ureport.expression.function.Function;
import com.bstek.ureport.expression.model.data.ExpressionData;
import com.bstek.ureport.model.Cell;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 宿主自定义函数：模板中以 yudaoHello() 调用
 *
 * @author 芋道源码
 */
@Component
public class DemoFunction implements Function {

    public static final String RESULT = "HelloFromHostFunction";

    @Override
    public Object execute(List<ExpressionData<?>> dataList, Context context, Cell currentCell) {
        return RESULT;
    }

    @Override
    public String name() {
        return "yudaoHello";
    }

}
