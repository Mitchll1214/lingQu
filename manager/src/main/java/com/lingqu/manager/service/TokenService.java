package com.lingqu.manager.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingqu.manager.common.BizException;
import com.lingqu.manager.common.CryptoUtil;
import com.lingqu.manager.common.IdUtil;
import com.lingqu.manager.entity.Project;
import com.lingqu.manager.entity.Token;
import com.lingqu.manager.mapper.ProjectMapper;
import com.lingqu.manager.mapper.TokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Token 管理。
 * 有效期支持「开始时间 ~ 结束时间」配置（startAt/expireAt，均可为空：空=立即生效/永不过期）。
 * 明文仅在生成时返回一次；需要再次查看时通过 reveal 主动解密（需有项目权限）。
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenMapper tokenMapper;
    private final ProjectMapper projectMapper;
    private final PermService permService;

    public TokenService(TokenMapper tokenMapper, ProjectMapper projectMapper, PermService permService) {
        this.tokenMapper = tokenMapper;
        this.projectMapper = projectMapper;
        this.permService = permService;
    }

    /**
     * 创建 Token。返回明文（仅此一次），库中存储 AES 加密值。
     *
     * @param startAt  生效开始时间，null 表示立即生效
     * @param expireAt 过期时间，null 表示永不过期
     */
    public Token create(String projectId, String tokenName, LocalDateTime startAt, LocalDateTime expireAt) {
        permService.checkProjectPermission(projectId);
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        // 校验时间范围合法性
        if (startAt != null && expireAt != null && !startAt.isBefore(expireAt)) {
            throw new BizException("开始时间必须早于结束时间");
        }
        String plainToken = randomToken();
        Token token = new Token();
        token.setId(IdUtil.uuid());
        token.setProjectId(projectId);
        token.setToken(CryptoUtil.encrypt(plainToken));
        token.setTokenName(StringUtils.hasText(tokenName) ? tokenName : "默认Token");
        token.setStartAt(startAt);
        token.setExpireAt(expireAt);
        token.setStatus(1);
        tokenMapper.insert(token);

        // 明文仅返回给调用方，不落库
        Token result = new Token();
        result.setId(token.getId());
        result.setProjectId(projectId);
        result.setToken(plainToken);
        result.setTokenName(token.getTokenName());
        result.setStartAt(startAt);
        result.setExpireAt(expireAt);
        result.setStatus(1);
        return result;
    }

    public List<Token> list(String projectId) {
        LambdaQueryWrapper<Token> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(projectId)) {
            permService.checkProjectPermission(projectId);
            qw.eq(Token::getProjectId, projectId);
        } else {
            List<String> permitted = permService.permittedProjectIds();
            if (permitted != null) {
                if (permitted.isEmpty()) {
                    return Collections.emptyList();
                }
                qw.in(Token::getProjectId, permitted);
            }
        }
        qw.orderByDesc(Token::getCreatedAt);
        List<Token> list = tokenMapper.selectList(qw);
        list.forEach(t -> t.setToken("******"));
        return list;
    }

    /** 查看明文（主动操作，需项目权限） */
    public String reveal(String id) {
        Token token = tokenMapper.selectById(id);
        if (token == null) {
            throw new BizException(404, "Token 不存在");
        }
        permService.checkProjectPermission(token.getProjectId());
        return CryptoUtil.decrypt(token.getToken());
    }

    /** 吊销 */
    public void revoke(String id) {
        Token token = tokenMapper.selectById(id);
        if (token == null) {
            throw new BizException(404, "Token 不存在");
        }
        permService.checkProjectPermission(token.getProjectId());
        token.setStatus(0);
        tokenMapper.updateById(token);
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return "lq_" + sb;
    }
}
