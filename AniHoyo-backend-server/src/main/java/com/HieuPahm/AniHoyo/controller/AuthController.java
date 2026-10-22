package com.HieuPahm.AniHoyo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.HieuPahm.AniHoyo.model.dtos.auth.LoginDTO;
import com.HieuPahm.AniHoyo.model.dtos.auth.ResLoginDTO;
import com.HieuPahm.AniHoyo.model.dtos.auth.RoleDTO;
import com.HieuPahm.AniHoyo.model.dtos.auth.UserDTO;
import com.HieuPahm.AniHoyo.model.entities.RefreshToken;
import com.HieuPahm.AniHoyo.model.entities.Role;
import com.HieuPahm.AniHoyo.model.entities.User;
import com.HieuPahm.AniHoyo.services.IRefreshTokenService;
import com.HieuPahm.AniHoyo.services.IUserService;
import com.HieuPahm.AniHoyo.utils.SecurityUtils;
import com.HieuPahm.AniHoyo.utils.anotation.MessageApi;
import com.HieuPahm.AniHoyo.utils.error.BadActionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    @Value("${anihoyo.jwt.refresh-token-validity-in-seconds}")
    private long refreshTokenExpire;

    private final IUserService userService;
    private final IRefreshTokenService refreshTokenService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final SecurityUtils securityUtils;

    public AuthController(AuthenticationManagerBuilder authenticationManagerBuilder,
            IUserService userService, SecurityUtils securityUtils, IRefreshTokenService refreshTokenService) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userService = userService;

        this.securityUtils = securityUtils;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResLoginDTO> login(@Valid @RequestBody LoginDTO loginData, HttpServletRequest request) {
        // transfer input include username/password
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginData.getUsername(), loginData.getPassword());
        // xác thực người dùng ==> cần có loadUserByUserName
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ResLoginDTO resLoginDTO = new ResLoginDTO();
        User realUser = this.userService.handleGetUserByUsername(loginData.getUsername());
        if (realUser != null) {
            ResLoginDTO.UserData userLog = new ResLoginDTO.UserData(
                    realUser.getId(),
                    realUser.getFullName(),
                    realUser.getEmail(),
                    realUser.getRole());
            resLoginDTO.setUser(userLog);
        }
        // generate access token
        String access_token = this.securityUtils.generateAccessToken(authentication.getName(), resLoginDTO);
        resLoginDTO.setAccessToken(access_token);
        // gen refresh token
        String refresh_token = this.securityUtils.generateRefreshToken(loginData.getUsername(), resLoginDTO);
        // Mỗi lần login ghi một phiên riêng vào refresh_tokens, nên login ở thiết bị
        // khác không còn làm cookie của phiên đang chạy mất hiệu lực.
        this.refreshTokenService.create(refresh_token, realUser, refreshTokenExpire,
                request.getHeader("User-Agent"));
        // setup cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh-token", refresh_token)
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTokenExpire)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString()).body(resLoginDTO);
    }

    @PostMapping("/auth/register")
    @MessageApi("Register account")
    public ResponseEntity<UserDTO> registerAccount(@Valid @RequestBody User dataUser) throws BadActionException {
        // User accUser = this.userService.handleCreateUser(dataUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.create(dataUser));
    }

    @GetMapping("/auth/account")
    public ResponseEntity<ResLoginDTO.GetAccountUser> getAccount() {
        String emailLogin = SecurityUtils.getCurrentUserLogin().isPresent() ? SecurityUtils.getCurrentUserLogin().get()
                : "";
        User userCreated = this.userService.handleGetUserByUsername(emailLogin);
        ResLoginDTO.UserData userData = new ResLoginDTO.UserData();
        ResLoginDTO.GetAccountUser info = new ResLoginDTO.GetAccountUser();
        if (userCreated != null) {
            userData.setId(userCreated.getId());
            userData.setEmail(userCreated.getEmail());
            userData.setName(userCreated.getFullName());
            userData.setRole(userCreated.getRole());
            info.setUser(userData);
        }
        return ResponseEntity.ok().body(info);
    }

    @GetMapping("/auth/refresh")
    @MessageApi("Renew token action")
    public ResponseEntity<ResLoginDTO> getRefreshToken(
            @CookieValue(name = "refresh-token", defaultValue = "error") String refreshToken,
            HttpServletRequest request)
            throws BadActionException {
        if ("error".equals(refreshToken)) {
            throw new BadActionException("Refresh token not be attached in request");
        }
        Jwt correctToken;
        try {
            // running check valid
            correctToken = this.securityUtils.confirmValidRefreshToken(refreshToken);
        } catch (Exception ex) {
            // Chữ ký sai hoặc token hết hạn → 401 để client hiểu là hết phiên
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        String email = correctToken.getSubject();
        // Phiên phải còn trong refresh_tokens: chưa logout, chưa hết hạn, và nếu vừa
        // bị xoay thì còn trong cửa sổ grace (2 tab refresh cùng lúc).
        RefreshToken session = this.refreshTokenService.verify(refreshToken);
        if (session == null || session.getUser() == null
                || !email.equalsIgnoreCase(session.getUser().getEmail())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        ResLoginDTO resLoginDTO = new ResLoginDTO();
        User realUser = this.userService.handleGetUserByUsername(email);
        if (realUser != null) {
            // Chú ý thứ tự tham số của UserData: (id, name, email, role) — giống
            // nhánh login. Trước đây email/fullName bị truyền ngược nên response
            // refresh trả name = email và email = fullName.
            ResLoginDTO.UserData userLog = new ResLoginDTO.UserData(
                    realUser.getId(),
                    realUser.getFullName(),
                    realUser.getEmail(),
                    realUser.getRole());
            resLoginDTO.setUser(userLog);
        }
        // generate access token
        String access_token = this.securityUtils.generateAccessToken(email, resLoginDTO);
        resLoginDTO.setAccessToken(access_token);
        // gen refresh token
        String refresh_token = this.securityUtils.generateRefreshToken(email, resLoginDTO);
        // Xoay token: phiên cũ bị thu hồi (còn grace ngắn cho tab song song) và một
        // dòng mới được ghi cho token vừa phát hành.
        this.refreshTokenService.rotate(session, refresh_token, refreshTokenExpire,
                request.getHeader("User-Agent"));
        // setup cookies
        ResponseCookie resCookies = ResponseCookie
                .from("refresh-token", refresh_token)
                .httpOnly(true)
                .path("/")
                .maxAge(refreshTokenExpire)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, resCookies.toString()).body(resLoginDTO);
    }

    @PostMapping("/auth/logout")
    @MessageApi("sign out action")
    public ResponseEntity<Void> LogoutAccount(
            @CookieValue(name = "refresh-token", required = false) String refreshToken) {
        // Thu hồi đúng phiên gắn với cookie này; các thiết bị khác giữ nguyên phiên
        // của họ. Không cần access token còn hạn nên logout vẫn chạy được khi access
        // token đã hết hạn.
        this.refreshTokenService.revoke(refreshToken);
        ResponseCookie removeCookies = ResponseCookie
                .from("refresh-token", null)
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, removeCookies.toString())
                .body(null);
    }
}
