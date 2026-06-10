package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_timeline")
public class OrderTimeline {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private String orderId;
    private String status;
    private String action;
    @TableField("operator_type")
    private String operatorType;
    @TableField("operator_id")
    private Long operatorId;
    private String remark;
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 作用：读取当前对象的编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getId() {
        return id;
    }

    /**
     * 作用：修改当前对象的编号。
     * 输入：
     * - id：编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 作用：读取当前对象的订单编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * 作用：修改当前对象的订单编号。
     * 输入：
     * - orderId：订单编号，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * 作用：读取当前对象的状态。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getStatus() {
        return status;
    }

    /**
     * 作用：修改当前对象的状态。
     * 输入：
     * - status：状态，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 作用：读取当前对象的操作动作。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getAction() {
        return action;
    }

    /**
     * 作用：修改当前对象的操作动作。
     * 输入：
     * - action：操作动作，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * 作用：读取当前对象的操作人类型。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getOperatorType() {
        return operatorType;
    }

    /**
     * 作用：修改当前对象的操作人类型。
     * 输入：
     * - operatorType：操作人类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    /**
     * 作用：读取当前对象的操作人编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getOperatorId() {
        return operatorId;
    }

    /**
     * 作用：修改当前对象的操作人编号。
     * 输入：
     * - operatorId：操作人编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /**
     * 作用：读取当前对象的备注。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 作用：修改当前对象的备注。
     * 输入：
     * - remark：备注，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 作用：读取当前对象的创建时间。
     * 输入：
     * - 无输入参数。
     * 输出：返回 LocalDateTime，表示当前对象里这个字段保存的值。
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 作用：修改当前对象的创建时间。
     * 输入：
     * - createTime：创建时间，类型是 LocalDateTime；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
