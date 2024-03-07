package cn.myperf4j.plugin;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FieldArgs {

    //类 "com/xxl/job/core/handler/impl/MethodJobHandler"
    private String owner;
    //需要注入的属性名 "method"
    private String name;
    //属性类型 "Ljava/lang/reflect/Method;"
    private String descriptor;

}
