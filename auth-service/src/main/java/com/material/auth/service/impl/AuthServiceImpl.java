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

    public AuthServiceImpl(AdminAccountMapper adminAccountMapper,
                           PurchaserAccountMapper purchaserAccountMapper,
                           PurchaserProfileMapper purchaserProfileMapper,
                           SupplierAccountMapper supplierAccountMapper,
                           SupplierProfileMapper supplierProfileMapper,
                           DriverAccountMapper driverAccountMapper,
                           DriverProfileMapper driverProfileMapper,
                           PasswordEncoder passwordEncoder,
                           StringRedisTemplate redisTemplate) {
        this.adminAccountMapper = adminAccountMapper;
        this.purchaserAccountMapper = purchaserAccountMapper;
        this.purchaserProfileMapper = purchaserProfileMapper;
        this.supplierAccountMapper = supplierAccountMapper;
        this.supplierProfileMapper = supplierProfileMapper;
        this.driverAccountMapper = driverAccountMapper;
        this.driverProfileMapper = driverProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

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

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        String username = request.username().trim();
        String passwordHash = passwordEncoder.encode(request.password());
        String displayName = request.displayName().trim();
        String contactPhone = request.contactPhone().trim();
        return switch (request.userType()) {
            case PURCHASER -> registerPurchaser(username, passwordHash, displayName, contactPhone);
            case SUPPLIER -> registerSupplier(username, passwordHash, displayName, contactPhone);
            case DRIVER -> registerDriver(username, passwordHash, displayName, contactPhone);
            case ADMIN -> throw new BusinessException(ErrorCode.FORBIDDEN);
        };
    }

    @Override
    public void logout(String token) {
        validateToken(token);
        redisTemplate.delete(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + token);
    }

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

    private LoginResponse loginAdmin(String username, String password) {
        AdminAccount account = adminAccountMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>().eq(AdminAccount::getUsername, username)
        );
        verifyAccount(account, password);
        String displayName = resolveDisplayName(username, account.getDisplayName());
        return issueToken(account.getId(), UserType.ADMIN, account.getUsername(), displayName);
    }

    private LoginResponse registerPurchaser(String username, String passwordHash, String displayName, String contactPhone) {
        if (purchaserAccountMapper.selectCount(new LambdaQueryWrapper<PurchaserAccount>()
                .eq(PurchaserAccount::getUsername, username)) > 0) {
            throw new IllegalArgumentException("采购方账号已存在");
        }
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
        profile.setAddress("待完善");
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        purchaserProfileMapper.insert(profile);
        return issueToken(account.getId(), UserType.PURCHASER, username, displayName);
    }

    private LoginResponse registerSupplier(String username, String passwordHash, String displayName, String contactPhone) {
        if (supplierAccountMapper.selectCount(new LambdaQueryWrapper<SupplierAccount>()
                .eq(SupplierAccount::getUsername, username)) > 0) {
            throw new IllegalArgumentException("供应商账号已存在");
        }
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
        profile.setAddress("待完善");
        profile.setRatingScore(new BigDecimal("4.50"));
        profile.setCreateTime(now);
        profile.setUpdateTime(now);
        supplierProfileMapper.insert(profile);
        return issueToken(account.getId(), UserType.SUPPLIER, username, displayName);
    }

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

    private void verifyAccount(PurchaserAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

    private void verifyAccount(SupplierAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

    private void verifyAccount(DriverAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

    private void verifyAccount(AdminAccount account, String password) {
        if (account == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (!Integer.valueOf(AccountStatus.ENABLED.getCode()).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
    }

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

    private String resolveDisplayName(String username, String profileDisplayName) {
        if (profileDisplayName == null || profileDisplayName.isBlank()) {
            return username;
        }
        return profileDisplayName;
    }

    private void cleanupToken(String redisKey) {
        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException ignored) {
            // Best effort cleanup only; callers should still receive TOKEN_WRITE_FAILED.
        }
    }

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

    private void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    private String requiredHashValue(Map<Object, Object> loginMap, String key) {
        Object value = loginMap.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return value.toString();
    }
}
