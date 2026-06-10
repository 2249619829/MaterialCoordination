package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("driver_follow")
public class DriverFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("driver_id")
    private Long driverId;
    @TableField("purchaser_id")
    private Long purchaserId;
    @TableField("follow_type")
    private String followType;
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
     * 作用：读取当前对象的司机编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getDriverId() {
        return driverId;
    }

    /**
     * 作用：修改当前对象的司机编号。
     * 输入：
     * - driverId：司机编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    /**
     * 作用：读取当前对象的采购方编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getPurchaserId() {
        return purchaserId;
    }

    /**
     * 作用：修改当前对象的采购方编号。
     * 输入：
     * - purchaserId：采购方编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setPurchaserId(Long purchaserId) {
        this.purchaserId = purchaserId;
    }

    /**
     * 作用：读取当前对象的关注类型。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getFollowType() {
        return followType;
    }

    /**
     * 作用：修改当前对象的关注类型。
     * 输入：
     * - followType：关注类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setFollowType(String followType) {
        this.followType = followType;
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
