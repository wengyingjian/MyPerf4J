package cn.myperf4j.plugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.List;

public abstract class BaseInjectPlugin implements InjectPlugin {

    @Override
    public boolean injectFields(MethodVisitor mv) {
        return false;
    }

    @Override
    public boolean injectParams(AdviceAdapter adapter, MethodVisitor mv) {
        return false;
    }

    protected final boolean injectAllParams(AdviceAdapter adapter, MethodVisitor mv) {
        // 创建一个 Object 数组来存储参数
        Type[] argumentTypes = adapter.getArgumentTypes();
        mv.visitLdcInsn(argumentTypes.length);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < argumentTypes.length; i++) {
            mv.visitInsn(Opcodes.DUP); // 复制数组引用
            mv.visitLdcInsn(i); // 将数组索引加载到栈上
            // 将参数加载到数组中
            adapter.loadArg(i);
            adapter.box(argumentTypes[i]);
            mv.visitInsn(Opcodes.AASTORE);
        }
        return true;
    }

    protected final boolean injectFields(MethodVisitor mv, List<FieldArgs> fields) {
        if (fields == null || fields.size() == 0) {
            return false;
        }

        // 创建一个 Object 数组来存储参数
        mv.visitLdcInsn(fields.size());
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < fields.size(); i++) {
            FieldArgs args = fields.get(i);
            mv.visitInsn(Opcodes.DUP); // 复制数组引用
            mv.visitLdcInsn(i); // 将数组索引加载到栈上

            mv.visitVarInsn(Opcodes.ALOAD, 0); // 将 this 加载到栈上
            mv.visitFieldInsn(Opcodes.GETFIELD, args.getOwner(), args.getName(), args.getDescriptor()); // 获取字段值

            mv.visitInsn(Opcodes.AASTORE); // 将字段值存储到数组中
        }
        return true;
    }

}
