package com.material.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("supplier_profile")
public class SupplierProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("supplier_id")
    private Long supplierId;
    @TableField("company_name")
    private String companyName;
    @TableField("contact_name")
    private String contactName;
    @TableField("contact_phone")
    private String contactPhone;
    @TableField("license_no")
    private String licenseNo;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    @TableField("rating_score")
    private BigDecimal ratingScore;
    @TableField("business_license_url")
    private String businessLicenseUrl;
    @TableField("safety_cert_url")
    private String safetyCertUrl;
    @TableField("insurance_cert_url")
    private String insuranceCertUrl;
    @TableField("audit_status")
    private String auditStatus;
    @TableField("audit_remark")
    private String auditRemark;
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
     * 作用：读取当前对象的公司名称。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getCompanyName() {
        return companyName;
    }

    /**
     * 作用：修改当前对象的公司名称。
     * 输入：
     * - companyName：公司名称，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    /**
     * 作用：读取当前对象的联系人姓名。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getContactName() {
        return contactName;
    }

    /**
     * 作用：修改当前对象的联系人姓名。
     * 输入：
     * - contactName：联系人姓名，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setContactName(String contactName) {
        this.contactName = contactName;
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
     * 作用：读取当前对象的资质编号。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getLicenseNo() {
        return licenseNo;
    }

    /**
     * 作用：修改当前对象的资质编号。
     * 输入：
     * - licenseNo：资质编号，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setLicenseNo(String licenseNo) {
        this.licenseNo = licenseNo;
    }

    /**
     * 作用：读取当前对象的地址。
     * 输入：
     * - 无输入参数。
     * 输出：返回 String，表示当前对象里这个字段保存的值。
     */
    public String getAddress() {
        return address;
    }

    /**
     * 作用：修改当前对象的地址。
     * 输入：
     * - address：地址，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    public void setAddress(String address) {
        this.address = address;
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

    public String getBusinessLicenseUrl() {
        return businessLicenseUrl;
    }

    public void setBusinessLicenseUrl(String businessLicenseUrl) {
        this.businessLicenseUrl = businessLicenseUrl;
    }

    public String getSafetyCertUrl() {
        return safetyCertUrl;
    }

    public void setSafetyCertUrl(String safetyCertUrl) {
        this.safetyCertUrl = safetyCertUrl;
    }

    public String getInsuranceCertUrl() {
        return insuranceCertUrl;
    }

    public void setInsuranceCertUrl(String insuranceCertUrl) {
        this.insuranceCertUrl = insuranceCertUrl;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
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
