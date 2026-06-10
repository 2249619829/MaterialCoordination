package com.material.common.enums;

public enum AccountStatus {
    /**
     * 作用：完成 DISABLED 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    DISABLED(0),
    /**
     * 启用状态。
     */
    ENABLED(1);

    private final int code;

    /**
     * 作用：创建 AccountStatus 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - code：编码，类型是 int；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    AccountStatus(int code) {
        this.code = code;
    }

    /**
     * 作用：读取枚举或错误码中的编码。
     * 输入：
     * - 无输入参数。
     * 输出：返回 int，表示当前对象里这个字段保存的值。
     */
    public int getCode() {
        return code;
    }
}
