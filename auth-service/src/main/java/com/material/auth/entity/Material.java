package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("material_code")
    private String materialCode;
    @TableField("material_name")
    private String materialName;
    private String category;
    private String unit;
    private String description;
    private Integer status;
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
     * 作用：读取当前对象的物资编码。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getMaterialCode() {
        return materialCode;
    }

    /**
     * 作用：修改当前对象的物资编码。
     * 输入：
     * - materialCode：物资编码，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    /**
     * 作用：读取当前对象的物资名称。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getMaterialName() {
        return materialName;
    }

    /**
     * 作用：修改当前对象的物资名称。
     * 输入：
     * - materialName：物资名称，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    /**
     * 作用：读取当前对象的物资分类。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getCategory() {
        return category;
    }

    /**
     * 作用：修改当前对象的物资分类。
     * 输入：
     * - category：物资分类，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 作用：读取当前对象的计量单位。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getUnit() {
        return unit;
    }

    /**
     * 作用：修改当前对象的计量单位。
     * 输入：
     * - unit：计量单位，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * 作用：读取当前对象的描述。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getDescription() {
        return description;
    }

    /**
     * 作用：修改当前对象的描述。
     * 输入：
     * - description：描述，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 作用：读取当前对象的状态。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示当前对象里这个字段保存的值。
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 作用：修改当前对象的状态。
     * 输入：
     * - status：状态，类型是 Integer；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setStatus(Integer status) {
        this.status = status;
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
