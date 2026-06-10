package com.material.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.material.auth.dto.LoginRequest;
import com.material.auth.dto.LoginResponse;
import com.material.auth.entity.AdminAccount;
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
import com.material.auth.service.geo.Coordinates;
import com.material.auth.service.geo.GeocodingService;
import com.material.auth.service.impl.AuthServiceImpl;
import com.material.common.constant.RedisConstants;
import com.material.common.enums.AccountStatus;
import com.material.common.enums.ErrorCode;
import com.material.common.enums.UserType;
import com.material.common.exception.BusinessException;
import com.material.common.model.LoginUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private AdminAccountMapper adminAccountMapper;
    @Mock
    private PurchaserAccountMapper purchaserAccountMapper;
    @Mock
    private PurchaserProfileMapper purchaserProfileMapper;
    @Mock
    private SupplierAccountMapper supplierAccountMapper;
    @Mock
    private SupplierProfileMapper supplierProfileMapper;
    @Mock
    private DriverAccountMapper driverAccountMapper;
    @Mock
    private DriverProfileMapper driverProfileMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private GeocodingService geocodingService;

    private AuthServiceImpl authService;

    /**
     * 作用：修改当前对象的Up。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                adminAccountMapper,
                purchaserAccountMapper,
                purchaserProfileMapper,
                supplierAccountMapper,
                supplierProfileMapper,
                driverAccountMapper,
                driverProfileMapper,
                passwordEncoder,
                redisTemplate,
                geocodingService
        );
    }

    /**
     * 作用：完成 loginReturnsTokenForEnabledAdminWithCorrectPassword 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginReturnsTokenForEnabledAdminWithCorrectPassword() {
        AdminAccount account = new AdminAccount();
        account.setId(99L);
        account.setUsername("admin01");
        account.setPasswordHash("hash");
        account.setDisplayName("平台运营管理员");
        account.setStatus(AccountStatus.ENABLED.getCode());

        when(adminAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        LoginResponse response = authService.login(new LoginRequest(UserType.ADMIN, "admin01", "secret"));

        assertThat(response.userId()).isEqualTo(99L);
        assertThat(response.userType()).isEqualTo(UserType.ADMIN);
        assertThat(response.displayName()).isEqualTo("平台运营管理员");
    }

    /**
     * 作用：完成 loginReturnsTokenForEnabledSupplierWithCorrectPassword 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginReturnsTokenForEnabledSupplierWithCorrectPassword() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.ENABLED.getCode());
        SupplierProfile profile = new SupplierProfile();
        profile.setCompanyName("Acme Materials");

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(supplierProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        LoginResponse response = authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "secret"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.userType()).isEqualTo(UserType.SUPPLIER);
        assertThat(response.username()).isEqualTo("supplier-a");
        assertThat(response.displayName()).isEqualTo("Acme Materials");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(keyCaptor.capture(), mapCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith(RedisConstants.LOGIN_TOKEN_KEY_PREFIX);
        assertThat(mapCaptor.getValue()).containsEntry("id", "7")
                .containsEntry("userType", UserType.SUPPLIER.name())
                .containsEntry("username", "supplier-a")
                .containsEntry("displayName", "Acme Materials");
        verify(redisTemplate).expire(keyCaptor.getValue(), RedisConstants.LOGIN_TOKEN_TTL);
    }

    /**
     * 作用：完成 loginRejectsWrongPassword 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginRejectsWrongPassword() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.ENABLED.getCode());

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "wrong")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_INCORRECT));
    }

    /**
     * 作用：完成 loginRejectsDisabledAccount 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginRejectsDisabledAccount() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.DISABLED.getCode());

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);

        assertThatThrownBy(() -> authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "secret")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    /**
     * 作用：完成 logoutDeletesCurrentToken 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void logoutDeletesCurrentToken() {
        authService.logout("token-123");

        verify(redisTemplate).delete(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123");
    }

    /**
     * 作用：完成 loginCleansUpRedisKeyWhenExpireReturnsFalse 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginCleansUpRedisKeyWhenExpireReturnsFalse() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.ENABLED.getCode());
        SupplierProfile profile = new SupplierProfile();
        profile.setCompanyName("Acme Materials");

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(supplierProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.FALSE);

        assertThatThrownBy(() -> authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "secret")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.TOKEN_WRITE_FAILED));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(hashOperations).putAll(keyCaptor.capture(), any(Map.class));
        verify(redisTemplate).delete(keyCaptor.getValue());
    }

    /**
     * 作用：完成 loginFallsBackToUsernameWhenSupplierProfileCompanyNameIsBlank 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginFallsBackToUsernameWhenSupplierProfileCompanyNameIsBlank() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.ENABLED.getCode());
        SupplierProfile profile = new SupplierProfile();
        profile.setCompanyName(" ");

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(supplierProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        LoginResponse response = authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "secret"));

        assertThat(response.displayName()).isEqualTo("supplier-a");
        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(any(String.class), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry("displayName", "supplier-a");
    }

    /**
     * 作用：完成 loginFallsBackToUsernameWhenSupplierProfileCompanyNameIsNull 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void loginFallsBackToUsernameWhenSupplierProfileCompanyNameIsNull() {
        SupplierAccount account = supplierAccount(7L, "supplier-a", "hash", AccountStatus.ENABLED.getCode());
        SupplierProfile profile = new SupplierProfile();

        when(supplierAccountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(supplierProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        LoginResponse response = authService.login(new LoginRequest(UserType.SUPPLIER, "supplier-a", "secret"));

        assertThat(response.displayName()).isEqualTo("supplier-a");
        ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(any(String.class), mapCaptor.capture());
        assertThat(mapCaptor.getValue()).containsEntry("displayName", "supplier-a");
    }

    /**
     * 作用：完成 currentUserReadsValidRedisHash 这一步处理。
     * 输入：
     * - 无输入参数。
     * 输出：无返回值。方法执行成功就表示操作完成。
     */
    @Test
    void currentUserReadsValidRedisHash() {
        Map<Object, Object> loginMap = new HashMap<>();
        loginMap.put("id", "7");
        loginMap.put("userType", UserType.SUPPLIER.name());
        loginMap.put("username", "supplier-a");
        loginMap.put("displayName", "Acme Materials");

        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(RedisConstants.LOGIN_TOKEN_KEY_PREFIX + "token-123")).thenReturn(loginMap);

        LoginUserDTO currentUser = authService.currentUser("token-123");

        assertThat(currentUser.id()).isEqualTo(7L);
        assertThat(currentUser.userType()).isEqualTo(UserType.SUPPLIER);
        assertThat(currentUser.username()).isEqualTo("supplier-a");
        assertThat(currentUser.displayName()).isEqualTo("Acme Materials");
    }

    @Test
    void registerPurchaserGeocodesAddressIntoProfile() {
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(purchaserAccountMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(purchaserAccountMapper.insert(any(PurchaserAccount.class))).thenAnswer(invocation -> {
            PurchaserAccount account = invocation.getArgument(0);
            account.setId(21L);
            return 1;
        });
        when(geocodingService.resolve("北京交通大学"))
                .thenReturn(java.util.Optional.of(new Coordinates(new BigDecimal("116.348000"), new BigDecimal("39.952000"))));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        authService.register(new com.material.auth.dto.RegisterRequest(
                UserType.PURCHASER,
                "purchaser-new",
                "secret123",
                "北京交通大学",
                "13800008888",
                "北京交通大学",
                null,
                null
        ));

        ArgumentCaptor<PurchaserProfile> profileCaptor = ArgumentCaptor.forClass(PurchaserProfile.class);
        verify(purchaserProfileMapper).insert(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getAddress()).isEqualTo("北京交通大学");
        assertThat(profileCaptor.getValue().getLongitude()).isEqualByComparingTo("116.348000");
        assertThat(profileCaptor.getValue().getLatitude()).isEqualByComparingTo("39.952000");
    }

    @Test
    void registerSupplierUsesManualCoordinatesWhenAddressCannotBeGeocoded() {
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        when(supplierAccountMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(supplierAccountMapper.insert(any(SupplierAccount.class))).thenAnswer(invocation -> {
            SupplierAccount account = invocation.getArgument(0);
            account.setId(31L);
            return 1;
        });
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.expire(any(String.class), eq(RedisConstants.LOGIN_TOKEN_TTL))).thenReturn(Boolean.TRUE);

        authService.register(new com.material.auth.dto.RegisterRequest(
                UserType.SUPPLIER,
                "supplier-new",
                "secret123",
                "新供应商",
                "13800009999",
                "无法解析地址",
                new BigDecimal("121.470000"),
                new BigDecimal("31.230000")
        ));

        ArgumentCaptor<SupplierProfile> profileCaptor = ArgumentCaptor.forClass(SupplierProfile.class);
        verify(supplierProfileMapper).insert(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getAddress()).isEqualTo("无法解析地址");
        assertThat(profileCaptor.getValue().getLongitude()).isEqualByComparingTo("121.470000");
        assertThat(profileCaptor.getValue().getLatitude()).isEqualByComparingTo("31.230000");
    }

    /**
     * 作用：完成 supplierAccount 这一步处理。
     * 输入：
     * - id：编号，类型是 Long；方法会读取这个值继续处理。
     * - username：用户名，类型是 String；方法会读取这个值继续处理。
     * - passwordHash：加密后的密码，类型是 String；方法会读取这个值继续处理。
     * - status：状态，类型是 Integer；方法会读取这个值继续处理。
     * 输出：返回 SupplierAccount，也就是这个方法处理后的结果。
     */
    private static SupplierAccount supplierAccount(Long id, String username, String passwordHash, Integer status) {
        SupplierAccount account = new SupplierAccount();
        account.setId(id);
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setStatus(status);
        return account;
    }
}
