package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_review")
public class OrderReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("order_id")
    private String orderId;
    @TableField("reviewer_type")
    private String reviewerType;
    @TableField("reviewer_id")
    private Long reviewerId;
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Long targetId;
    private Integer score;
    private String content;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

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
     * 作用：读取当前对象的评价人类型。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getReviewerType() {
        return reviewerType;
    }

    /**
     * 作用：修改当前对象的评价人类型。
     * 输入：
     * - reviewerType：评价人类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setReviewerType(String reviewerType) {
        this.reviewerType = reviewerType;
    }

    /**
     * 作用：读取当前对象的评价人编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getReviewerId() {
        return reviewerId;
    }

    /**
     * 作用：修改当前对象的评价人编号。
     * 输入：
     * - reviewerId：评价人编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    /**
     * 作用：读取当前对象的被评价对象类型。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 作用：修改当前对象的被评价对象类型。
     * 输入：
     * - targetType：被评价对象类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /**
     * 作用：读取当前对象的被评价对象编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getTargetId() {
        return targetId;
    }

    /**
     * 作用：修改当前对象的被评价对象编号。
     * 输入：
     * - targetId：被评价对象编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    /**
     * 作用：读取当前对象的评分。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示当前对象里这个字段保存的值。
     */
    public Integer getScore() {
        return score;
    }

    /**
     * 作用：修改当前对象的评分。
     * 输入：
     * - score：评分，类型是 Integer；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setScore(Integer score) {
        this.score = score;
    }

    /**
     * 作用：读取当前对象的评价内容。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getContent() {
        return content;
    }

    /**
     * 作用：修改当前对象的评价内容。
     * 输入：
     * - content：评价内容，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setContent(String content) {
        this.content = content;
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

    /**
     * 作用：读取当前对象的更新时间。
     * 输入：
     * - 无输入参数。
     * 输出：返回 LocalDateTime，表示当前对象里这个字段保存的值。
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 作用：修改当前对象的更新时间。
     * 输入：
     * - updateTime：更新时间，类型是 LocalDateTime；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
