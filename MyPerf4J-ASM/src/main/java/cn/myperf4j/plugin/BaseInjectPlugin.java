package cn.myperf4j.plugin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.List;

public abstract class BaseInjectPlugin implements InjectPlugin {

    @Override
    public boolean injectFields(AdviceAdapter adapter) {
        return false;
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter) {
        return false;
    }

    protected final boolean injectAllParams(AdviceAdapter adapter) {
        // 创建一个 Object 数组来存储参数
        Type[] argumentTypes = adapter.getArgumentTypes();
        adapter.visitLdcInsn(argumentTypes.length);
        adapter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < argumentTypes.length; i++) {
            adapter.visitInsn(Opcodes.DUP); // 复制数组引用
            adapter.visitLdcInsn(i); // 将数组索引加载到栈上
            // 将参数加载到数组中
            adapter.loadArg(i);
            adapter.box(argumentTypes[i]);
            adapter.visitInsn(Opcodes.AASTORE);
        }
        return true;
    }

    protected final boolean injectFields(AdviceAdapter adapter, List<FieldArgs> fields) {
        if (fields == null || fields.size() == 0) {
            return false;
        }

        // 创建一个 Object 数组来存储参数
        adapter.visitLdcInsn(fields.size());
        adapter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < fields.size(); i++) {
            FieldArgs args = fields.get(i);
            adapter.visitInsn(Opcodes.DUP); // 复制数组引用
            adapter.visitLdcInsn(i); // 将数组索引加载到栈上

            adapter.visitVarInsn(Opcodes.ALOAD, 0); // 将 this 加载到栈上
            adapter.visitFieldInsn(Opcodes.GETFIELD, args.getOwner(), args.getName(), args.getDescriptor()); // 获取字段值

            adapter.visitInsn(Opcodes.AASTORE); // 将字段值存储到数组中
        }
        return true;
    }

}
