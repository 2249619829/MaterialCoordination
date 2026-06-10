package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.INPUT)
    private String id;
    @TableField("purchaser_id")
    private Long purchaserId;
    @TableField("purchaser_name")
    private String purchaserName;
    @TableField("supplier_id")
    private Long supplierId;
    @TableField("supplier_name")
    private String supplierName;
    @TableField("material_id")
    private Long materialId;
    @TableField("material_name")
    private String materialName;
    private String category;
    private String quantity;
    private String amount;
    private String status;
    private String source;
    @TableField("pushed_to")
    private String pushedTo;
    @TableField("driver_id")
    private Long driverId;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 作用：读取当前对象的编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getId() {
        return id;
    }

    /**
     * 作用：修改当前对象的编号。
     * 输入：
     * - id：编号，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setId(String id) {
        this.id = id;
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
     * 作用：读取当前对象的采购方名称。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getPurchaserName() {
        return purchaserName;
    }

    /**
     * 作用：修改当前对象的采购方名称。
     * 输入：
     * - purchaserName：采购方名称，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setPurchaserName(String purchaserName) {
        this.purchaserName = purchaserName;
    }

    /**
     * 作用：读取当前对象的供应商编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getSupplierId() {
        return supplierId;
    }

    /**
     * 作用：修改当前对象的供应商编号。
     * 输入：
     * - supplierId：供应商编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    /**
     * 作用：读取当前对象的供应商名称。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getSupplierName() {
        return supplierName;
    }

    /**
     * 作用：修改当前对象的供应商名称。
     * 输入：
     * - supplierName：供应商名称，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    /**
     * 作用：读取当前对象的物资编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Long，表示当前对象里这个字段保存的值。
     */
    public Long getMaterialId() {
        return materialId;
    }

    /**
     * 作用：修改当前对象的物资编号。
     * 输入：
     * - materialId：物资编号，类型是 Long；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
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
     * 作用：读取当前对象的采购数量。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getQuantity() {
        return quantity;
    }

    /**
     * 作用：修改当前对象的采购数量。
     * 输入：
     * - quantity：采购数量，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    /**
     * 作用：读取当前对象的金额。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getAmount() {
        return amount;
    }

    /**
     * 作用：修改当前对象的金额。
     * 输入：
     * - amount：金额，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setAmount(String amount) {
        this.amount = amount;
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
     * 作用：读取当前对象的来源说明。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getSource() {
        return source;
    }

    /**
     * 作用：修改当前对象的来源说明。
     * 输入：
     * - source：来源说明，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * 作用：读取当前对象的推送说明。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getPushedTo() {
        return pushedTo;
    }

    /**
     * 作用：修改当前对象的推送说明。
     * 输入：
     * - pushedTo：推送说明，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setPushedTo(String pushedTo) {
        this.pushedTo = pushedTo;
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
