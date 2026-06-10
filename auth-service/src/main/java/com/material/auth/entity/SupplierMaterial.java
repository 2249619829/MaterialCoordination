package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("supplier_material")
public class SupplierMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("supplier_id")
    private Long supplierId;
    @TableField("material_id")
    private Long materialId;
    @TableField("supply_price")
    private BigDecimal supplyPrice;
    @TableField("stock_quantity")
    private Integer stockQuantity;
    @TableField("daily_capacity")
    private Integer dailyCapacity;
    @TableField("delivery_radius_km")
    private BigDecimal deliveryRadiusKm;
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
     * 作用：读取当前对象的供应价格。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BigDecimal，表示当前对象里这个字段保存的值。
     */
    public BigDecimal getSupplyPrice() {
        return supplyPrice;
    }

    /**
     * 作用：修改当前对象的供应价格。
     * 输入：
     * - supplyPrice：供应价格，类型是 BigDecimal；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setSupplyPrice(BigDecimal supplyPrice) {
        this.supplyPrice = supplyPrice;
    }

    /**
     * 作用：读取当前对象的库存数量。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示当前对象里这个字段保存的值。
     */
    public Integer getStockQuantity() {
        return stockQuantity;
    }

    /**
     * 作用：修改当前对象的库存数量。
     * 输入：
     * - stockQuantity：库存数量，类型是 Integer；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    /**
     * 作用：读取当前对象的每日供应能力。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Integer，表示当前对象里这个字段保存的值。
     */
    public Integer getDailyCapacity() {
        return dailyCapacity;
    }

    /**
     * 作用：修改当前对象的每日供应能力。
     * 输入：
     * - dailyCapacity：每日供应能力，类型是 Integer；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setDailyCapacity(Integer dailyCapacity) {
        this.dailyCapacity = dailyCapacity;
    }

    /**
     * 作用：读取当前对象的配送半径。
     * 输入：
     * - 无输入参数。
     * 输出：返回 BigDecimal，表示当前对象里这个字段保存的值。
     */
    public BigDecimal getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    /**
     * 作用：修改当前对象的配送半径。
     * 输入：
     * - deliveryRadiusKm：配送半径，类型是 BigDecimal；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setDeliveryRadiusKm(BigDecimal deliveryRadiusKm) {
        this.deliveryRadiusKm = deliveryRadiusKm;
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
