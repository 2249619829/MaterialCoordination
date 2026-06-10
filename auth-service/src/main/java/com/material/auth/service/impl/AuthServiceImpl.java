package com.material.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.material.auth.dto.LoginRequest;
import com.material.auth.dto.LoginResponse;
import com.material.auth.dto.RegisterRequest;
import com.material.auth.entity.AdminAccount;
import com.material.auth.entity.DriverAccount;
import com.material.auth.entity.DriverProfile;
import com.material.auth.entity.PurchaserAccount;
import com.material.auth.entity.PurchaserProfile;
import com.material.auth.entity.SupplierAccount;
import com.material.auth.entity.SupplierProfile;
import com.material.auth.mapper.AdminAccountMapper;
import com.material.auth.mapper.DriverAccountMapper;
import com.material.auth.mapper.DriverProfileMapper;
import com.material.auth.mapper.PurchaserAccountMapper;
import com.material.auth.mapper.PurchaserProfileMapper;
import com.material.auth.mapper.SupplierAccountMapper;
import com.material.auth.mapper.SupplierProfileMapper;
import com.material.auth.service.AuthService;
import com.material.auth.service.geo.Coordinates;
import com.material.auth.service.geo.GeocodingService;
import com.material.common.constant.RedisConstants;
import com.material.common.enums.AccountStatus;
import com.material.common.enums.ErrorCode;
import com.material.common.enums.UserType;
import com.material.common.exception.BusinessException;
import com.material.common.model.LoginUserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AdminAccountMapper adminAccountMapper;
    private final PurchaserAccountMapper purchaserAccountMapper;
    private final PurchaserProfileMapper purchaserProfileMapper;
    private final SupplierAccountMapper supplierAccountMapper;
    private final SupplierProfileMapper supplierProfileMapper;
    private final DriverAccountMapper driverAccountMapper;
    private final DriverProfileMapper driverProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final GeocodingService geocodingService;

    /**
     * 作用：创建 AuthServiceImpl 对象，并把外部传进来的依赖保存起来。
     * 输入：
     * - adminAccountMapper：管理员账号数据库操作对象，类型是 AdminAccountMapper；方法会读取这个值继续处理。
     * - purchaserAccountMapper：采购方账号数据库操作对象，类型是 PurchaserAccountMapper；方法会读取这个值继续处理。
     * - purchaserProfileMapper：采购方资料数据库操作对象，类型是 PurchaserProfileMapper；方法会读取这个值继续处理。
     * - supplierAccountMapper：供应商账号数据库操作对象，类型是 SupplierAccountMapper；方法会读取这个值继续处理。
     * - supplierProfileMapper：供应商资料数据库操作对象，类型是 SupplierProfileMapper；方法会读取这个值继续处理。
     * - driverAccountMapper：司机账号数据库操作对象，类型是 DriverAccountMapper；方法会读取这个值继续处理。
     * - driverProfileMapper：司机资料数据库操作对象，类型是 DriverProfileMapper；方法会读取这个值继续处理。
     * - passwordEncoder：密码加密工具，类型是 PasswordEncoder；方法会读取这个值继续处理。
     * - redisTemplate：Redis 操作工具，类型是 StringRedisTemplate；方法会读取这个值继续处理。
     * 输出：无返回值。构造器的结果是创建好的对象本身。
     */
    public AuthServiceImpl(AdminAccountMapper adminAccountMapper,
                           PurchaserAccountMapper purchaserAccountMapper,
                           PurchaserProfileMapper purchaserProfileMapper,
                           SupplierAccountMapper supplierAccountMapper,
                           SupplierProfileMapper supplierProfileMapper,
                           DriverAccountMapper driverAccountMapper,
                           DriverProfileMapper driverProfileMapper,
                           PasswordEncoder passwordEncoder,
                           StringRedisTemplate redisTemplate,
                           GeocodingService geocodingService) {
        this.adminAccountMapper = adminAccountMapper;
        this.purchaserAccountMapper = purchaserAccountMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.supplierAccountMapper = supplierAccountMapper;
        this.supplierProfileMapper = supplierProfileMapper;
        this.driverAccountMapper = driverAccountMapper;
        this.driverProfileMapper = driverProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.geocodingService = geocodingService;
    }

    /**
     * 作用：根据用户名、密码和用户类型完成登录，并生成登录结果。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);
        return switch (request.userType()) {
            case PURCHASER -> loginPurchaser(request.username().trim(), request.password());
            case SUPPLIER -> loginSupplier(request.username().trim(), request.password());
            case DRIVER -> loginDriver(request.username().trim(), request.password());
            case ADMIN -> loginAdmin(request.username().trim(), request.password());
        };
    }

    /**
     * 作用：根据注册信息创建账号和资料，并在注册成功后直接登录。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        String username = request.username().trim();
        String passwordHash = passwordEncoder.encode(request.password());
        String displayName = request.displayName().trim();
        String contactPhone = request.contactPhone().trim();
        String address = request.address() == null ? "" : request.address().trim();
        return switch (request.userType()) {
            case PURCHASER -> registerPurchaser(username, passwordHash, displayName, contactPhone, address, request.longitude(), request.latitude());
            case SUPPLIER -> registerSupplier(username, passwordHash, displayName, contactPhone, address, request.longitude(), request.latitude());
            case DRIVER -> registerDriver(username, passwordHash, displayName, contactPhone);
            case ADMIN -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }

    /**
     * 作用：删除 Redis 中的登录 Token，让用户退出登录。
     * 输入：
     * - token：登录 Token，用来证明用户已经登录。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Override
    public void logout(String token) {
        validateToken(token);
        redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + token);
    }

    /**
     * 作用：根据登录 Token 或用户请求头读取当前登录用户。
     * 输入：
     * - token：登录 Token，用来证明用户已经登录。
     * 输出：返回 LoginUserDTO，这是跨层传递用的数据对象。
     */
    @Override
    public LoginUserDTO currentUser(String token) {
        validateToken(token);
        Map<Object, Object> loginMap = redisTemplate.opsForHash()
                .entries(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + token);
        if (loginMap == null || loginMap.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            Long id = Long.valueOf(requiredHashValue(loginMap, "id"));
            UserType userType = UserType.valueOf(requiredHashValue(loginMap, "userType"));
            String username = requiredHashValue(loginMap, "username");
            String displayName = requiredHashValue(loginMap, "displayName");
            return new LoginUserDTO(id, userType, username, displayName);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 作用：登录采购方账号。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse loginPurchaser(String username, String password) {
        PurchaserAccount account = purchaserAccountMapper.selectOne(
                new LambdaQueryWrapper<PurchaserAccount>().eq(PurchaserAccount::getUsername, username)
        );
        verifyAccount(account, password);
        PurchaserProfile profile = purchaserProfileMapper.selectOne(
                new LambdaQueryWrapper<PurchaserProfile>().eq(PurchaserProfile::getPurchaserId, account.getId())
        );
        String displayName = resolveDisplayName(username, profile == null ? null : profile.getCompanyName());
        return issueToken(account.getId(), UserType.PURCHASER, account.getUsername(), displayName);
    }

    /**
     * 作用：登录供应商账号。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse loginSupplier(String username, String password) {
        SupplierAccount account = supplierAccountMapper.selectOne(
                new LambdaQueryWrapper<SupplierAccount>().eq(SupplierAccount::getUsername, username)
        );
        verifyAccount(account, password);
        SupplierProfile profile = supplierProfileMapper.selectOne(
                new LambdaQueryWrapper<SupplierProfile>().eq(SupplierProfile::getSupplierId, account.getId())
        );
        String displayName = resolveDisplayName(username, profile == null ? null : profile.getCompanyName());
        return issueToken(account.getId(), UserType.SUPPLIER, account.getUsername(), displayName);
    }

    /**
     * 作用：登录司机账号。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse loginDriver(String username, String password) {
        DriverAccount account = driverAccountMapper.selectOne(
                new LambdaQueryWrapper<DriverAccount>().eq(DriverAccount::getUsername, username)
        );
        verifyAccount(account, password);
        DriverProfile profile = driverProfileMapper.selectOne(
                new LambdaQueryWrapper<DriverProfile>().eq(DriverProfile::getDriverId, account.getId())
        );
        String displayName = resolveDisplayName(username, profile == null ? null : profile.getRealName());
        return issueToken(account.getId(), UserType.DRIVER, account.getUsername(), displayName);
    }

    /**
     * 作用：登录管理员账号。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse loginAdmin(String username, String password) {
        AdminAccount account = adminAccountMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getUsername, username)
        );
        verifyAccount(account, password);
        String displayName = resolveDisplayName(username, account.getDisplayName());
        return issueToken(account.getId(), UserType.ADMIN, account.getUsername(), displayName);
    }

    /**
     * 作用：创建采购方账号和采购方资料。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - passwordHash：加密后的密码，类型是 String；方法会读取这个值继续处理。
     * - displayName：显示名称，类型是 String；方法会读取这个值继续处理。
     * - contactPhone：联系电话，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse registerPurchaser(String username,
                                            String passwordHash,
                                            String displayName,
                                            String contactPhone,
                                            String address,
                                            BigDecimal longitude,
                                            BigDecimal latitude) {
        if (purchaserAccountMapper.selectCount(new LambdaQueryWrapper<PurchaserAccount>()
                .eq(PurchaserAccount::getUsername, username)) > 0) {
            throw new IllegalArgumentException("采购方账号已存在");
        }
        Coordinates coordinates = resolveRequiredCoordinates(address, longitude, latitude);
        LocalDateTime now = LocalDateTime.now();
        PurchaserAccount account = new PurchaserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setStatus(AccountStatus.ENABLED.getCode());
        account.setCreateTime(now);
        account.setUpdateTime(now);
        purchaserAccountMapper.insert(account);

        PurchaserProfile profile = new PurchaserProfile();
        profile.setPurchaserId(account.getId());
        profile.setCompanyName(displayName);
        profile.setContactName(displayName);
        profile.setContactPhone(contactPhone);
        profile.setAddress(address);
        profile.setLongitude(coordinates.longitude());
        profile.setLatitude(coordinates.latitude());
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        purchaserProfileMapper.insert(profile);
        return issueToken(account.getId(), UserType.PURCHASER, username, displayName);
    }

    /**
     * 作用：创建供应商账号和供应商资料。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - passwordHash：加密后的密码，类型是 String；方法会读取这个值继续处理。
     * - displayName：显示名称，类型是 String；方法会读取这个值继续处理。
     * - contactPhone：联系电话，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse registerSupplier(String username,
                                           String passwordHash,
                                           String displayName,
                                           String contactPhone,
                                           String address,
                                           BigDecimal longitude,
                                           BigDecimal latitude) {
        if (supplierAccountMapper.selectCount(new LambdaQueryWrapper<SupplierAccount>()
                .eq(SupplierAccount::getUsername, username)) > 0) {
            throw new IllegalArgumentException("供应商账号已存在");
        }
        Coordinates coordinates = resolveRequiredCoordinates(address, longitude, latitude);
        LocalDateTime now = LocalDateTime.now();
        SupplierAccount account = new SupplierAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setStatus(AccountStatus.ENABLED.getCode());
        account.setCreateTime(now);
        account.setUpdateTime(now);
        supplierAccountMapper.insert(account);

        SupplierProfile profile = new SupplierProfile();
        profile.setSupplierId(account.getId());
        profile.setCompanyName(displayName);
        profile.setContactName(displayName);
        profile.setContactPhone(contactPhone);
        profile.setLicenseNo("LIC-SUPPLIER-" + String.format("%04d", account.getId()));
        profile.setAddress(address);
        profile.setLongitude(coordinates.longitude());
        profile.setLatitude(coordinates.latitude());
        profile.setRatingScore(new BigDecimal("4.50"));
        profile.setAuditStatus("PENDING");
        profile.setAuditRemark("新注册供应商，待管理员完善准入审核");
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        supplierProfileMapper.insert(profile);
        return issueToken(account.getId(), UserType.SUPPLIER, username, displayName);
    }

    /**
     * 作用：创建司机账号和司机资料。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - passwordHash：加密后的密码，类型是 String；方法会读取这个值继续处理。
     * - displayName：显示名称，类型是 String；方法会读取这个值继续处理。
     * - contactPhone：联系电话，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse registerDriver(String username, String passwordHash, String displayName, String contactPhone) {
        if (driverAccountMapper.selectCount(new LambdaQueryWrapper<DriverAccount>()
                .eq(DriverAccount::getUsername, username)) > 0) {
            throw new IllegalArgumentException("司机账号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        DriverAccount account = new DriverAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setStatus(AccountStatus.ENABLED.getCode());
        account.setCreateTime(now);
        account.setUpdateTime(now);
        driverAccountMapper.insert(account);

        DriverProfile profile = new DriverProfile();
        profile.setDriverId(account.getId());
        profile.setRealName(displayName);
        profile.setContactPhone(contactPhone);
        profile.setVehicleNo("待完善");
        profile.setVehicleType("厢式货车");
        profile.setAttendanceStatus(0);
        profile.setRatingScore(new BigDecimal("4.50"));
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        driverProfileMapper.insert(profile);
        return issueToken(account.getId(), UserType.DRIVER, username, displayName);
    }

    private Coordinates resolveRequiredCoordinates(String address, BigDecimal longitude, BigDecimal latitude) {
        if (longitude != null || latitude != null) {
            if (longitude == null || latitude == null) {
                throw new IllegalArgumentException("经纬度需要成对填写");
            }
            validateCoordinateRange(longitude, latitude);
            return new Coordinates(longitude, latitude);
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("请填写地址，或手动填写经纬度");
        }
        return geocodingService.resolve(address)
                .orElseThrow(() -> new IllegalArgumentException("未能根据地址获取经纬度，请手动填写"));
    }

    private void validateCoordinateRange(BigDecimal longitude, BigDecimal latitude) {
        if (longitude.compareTo(new BigDecimal("-180")) < 0
                || longitude.compareTo(new BigDecimal("180")) > 0
                || latitude.compareTo(new BigDecimal("-90")) < 0
                || latitude.compareTo(new BigDecimal("90")) > 0) {
            throw new IllegalArgumentException("经纬度超出有效范围");
        }
    }

    /**
     * 作用：检查账号是否存在、是否启用，以及密码是否正确。
     * 输入：
     * - account：账号对象，包含用户名、密码和状态。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void verifyAccount(PurchaserAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
    }

    /**
     * 作用：检查账号是否存在、是否启用，以及密码是否正确。
     * 输入：
     * - account：账号对象，包含用户名、密码和状态。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void verifyAccount(SupplierAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
    }

    /**
     * 作用：检查账号是否存在、是否启用，以及密码是否正确。
     * 输入：
     * - account：账号对象，包含用户名、密码和状态。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void verifyAccount(DriverAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
    }

    /**
     * 作用：检查账号是否存在、是否启用，以及密码是否正确。
     * 输入：
     * - account：账号对象，包含用户名、密码和状态。
     * - password：密码，类型是 String；方法会读取这个值继续处理。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void verifyAccount(AdminAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }
    }

    /**
     * 作用：生成一个新的登录 Token，并把用户信息写入 Redis。
     * 输入：
     * - userId：用户编号，类型是 Long；方法会读取这个值继续处理。
     * - userType：用户类型，类型是 UserType；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - displayName：显示名称，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 LoginResponse，这是接口返回给前端的结果对象。
     */
    private LoginResponse issueToken(Long userId, UserType userType, String username, String displayName) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String redisKey = RedisConstants.LOGIN_TOKEN_KEY_PREFIX + token;
        Map<String, String> loginMap = new HashMap<>();
        loginMap.put("id", String.valueOf(userId));
        loginMap.put("userType", userType.name());
        loginMap.put("username", username);
        loginMap.put("displayName", displayName);
        try {
            redisTemplate.opsForHash().putAll(redisKey, loginMap);
            Boolean expireResult = redisTemplate.expire(redisKey, RedisConstants.LOGIN_TOKEN_TTL);
            if (!Boolean.TRUE.equals(expireResult)) {
                cleanupToken(redisKey);
                throw new BusinessException(ErrorCode.TOKEN_WRITE_FAILED);
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            cleanupToken(redisKey);
            throw new BusinessException(ErrorCode.TOKEN_WRITE_FAILED);
        }
        log.info("business_event event=login_success userId={} userType={} username={}", userId, userType, username);
        return new LoginResponse(token, userId, userType, username, displayName);
    }

    /**
     * 作用：决定页面上应该显示的用户名称。
     * 输入：
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - profileDisplayName：资料里的显示名称，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String resolveDisplayName(String username, String profileDisplayName) {
        if (profileDisplayName == null || profileDisplayName.isBlank()) {
            return username;
        }
        return profileDisplayName;
    }

    /**
     * 作用：删除写入失败或过期处理失败时残留的 Token。
     * 输入：
     * - redisKey：Redis 中保存数据时使用的键名。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void cleanupToken(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException ignored) {
            // Best effort cleanup only; callers should still receive TOKEN_WRITE_FAILED.
        }
    }

    /**
     * 作用：检查登录请求里的必填内容是否完整。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void validateLoginRequest(LoginRequest request) {
        if (request == null
                || request.userType() == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 作用：检查注册请求里的必填内容是否完整。
     * 输入：
     * - request：前端传来的请求数据对象，里面包含本次操作需要的信息。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null
                || request.userType() == null
                || request.username() == null
                || request.username().isBlank()
                || request.password() == null
                || request.password().isBlank()
                || request.displayName() == null
                || request.displayName().isBlank()
                || request.contactPhone() == null
                || request.contactPhone().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        if (request.username().trim().length() < 4 || request.username().trim().length() > 64) {
            throw new IllegalArgumentException("账号长度需要在 4 到 64 位之间");
        }
        if (request.password().length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
    }

    /**
     * 作用：检查 Token 字符串是否为空。
     * 输入：
     * - token：登录 Token，用来证明用户已经登录。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 作用：从 Redis Hash 中取出一个必须存在的字段值。
     * 输入：
     * - loginMap：Redis 里的登录信息表，类型是 Map<Object, Object>；方法会读取这个值继续处理。
     * - key：字段名，类型是 String；方法会读取这个值继续处理。
     * 输出：返回 String，也就是一段文本结果。
     */
    private String requiredHashValue(Map<Object, Object> loginMap, String key) {
        Object value = loginMap.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return value.toString();
    }
}
