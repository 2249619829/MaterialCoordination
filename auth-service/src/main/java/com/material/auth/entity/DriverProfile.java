package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("driver_profile")
public class DriverProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("driver_id")
    private Long driverId;
    @TableField("real_name")
    private String realName;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("vehicle_no")
    private String vehicleNo;
    @TableField("vehicle_type")
    private String vehicleType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @TableField("attendance_status")
    private Integer attendanceStatus;
    @TableField("rating_score")
    private BigDecimal ratingScore;
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
     * 作用：读取当前对象的真实姓名。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getRealName() {
        return realName;
    }

    /**
     * 作用：修改当前对象的真实姓名。
     * 输入：
     * - realName：真实姓名，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setRealName(String realName) {
        this.realName = realName;
    }

    /**
     * 作用：读取当前对象的联系电话。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getContactPhone() {
        return contactPhone;
    }

    /**
     * 作用：修改当前对象的联系电话。
     * 输入：
     * - contactPhone：联系电话，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    /**
     * 作用：读取当前对象的车辆牌照。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getVehicleNo() {
        return vehicleNo;
    }

    /**
     * 作用：修改当前对象的车辆牌照。
     * 输入：
     * - vehicleNo：车辆牌照，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }

    /**
     * 作用：读取当前对象的车辆类型。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getVehicleType() {
        return vehicleType;
    }

    /**
     * 作用：修改当前对象的车辆类型。
     * 输入：
     * - vehicleType：车辆类型，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    /**
     * 作用：读取当前对象的经度。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BigDecimal，表示当前对象里这个字段保存的值。
     */
    public BigDecimal getLongitude() {
        return longitude;
    }

    /**
     * 作用：修改当前对象的经度。
     * 输入：
     * - longitude：经度，类型是 BigDecimal；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    /**
     * 作用：读取当前对象的纬度。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BigDecimal，表示当前对象里这个字段保存的值。
     */
    public BigDecimal getLatitude() {
        return latitude;
    }

    /**
     * 作用：修改当前对象的纬度。
     * 输入：
     * - latitude：纬度，类型是 BigDecimal；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    /**
     * 作用：读取当前对象的出勤状态。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示当前对象里这个字段保存的值。
     */
    public Integer getAttendanceStatus() {
        return attendanceStatus;
    }

    /**
     * 作用：修改当前对象的出勤状态。
     * 输入：
     * - attendanceStatus：出勤状态，类型是 Integer；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setAttendanceStatus(Integer attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    /**
     * 作用：读取当前对象的评分。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BigDecimal，表示当前对象里这个字段保存的值。
     */
    public BigDecimal getRatingScore() {
        return ratingScore;
    }

    /**
     * 作用：修改当前对象的评分。
     * 输入：
     * - ratingScore：评分，类型是 BigDecimal；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setRatingScore(BigDecimal ratingScore) {
        this.ratingScore = ratingScore;
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
