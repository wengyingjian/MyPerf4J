package cn.myperf4j.plugin.impl;

import cn.myperf4j.plugin.InjectPlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class BaseInjectPlugin implements InjectPlugin {

    public final boolean injectAllParams(AdviceAdapter adapter, MethodVisitor mv) {
        // 创建一个 Object 数组来存储参数
        Type[] argumentTypes = adapter.getArgumentTypes();
        mv.visitLdcInsn(argumentTypes.length + 1);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < argumentTypes.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            // 将参数加载到数组中
            adapter.loadArg(i);
            adapter.box(argumentTypes[i]);
            mv.visitInsn(Opcodes.AASTORE);
        }
        return true;
    }
//
//    public boolean injectFields(){
//
//    }


}
