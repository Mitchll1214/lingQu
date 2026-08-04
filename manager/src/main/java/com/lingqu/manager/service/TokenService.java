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
import java.util.List;

/**
 * Token 管理。
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenMapper tokenMapper;
    private final ProjectMapper projectMapper;

    public TokenService(TokenMapper tokenMapper, ProjectMapper projectMapper) {
        this.tokenMapper = tokenMapper;
        this.projectMapper = projectMapper;
    }

    /**
     * 创建 Token。返回明文（仅此一次），库中存储 AES 加密值。
     */
    public Token create(String projectId, String tokenName, Integer expireDays) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(404, "项目不存在");
        }
        String plainToken = randomToken();
        Token token = new Token();
        token.setId(IdUtil.uuid());
        token.setProjectId(projectId);
        token.setToken(CryptoUtil.encrypt(plainToken));
        token.setTokenName(StringUtils.hasText(tokenName) ? tokenName : "默认Token");
        if (expireDays != null && expireDays > 0) {
            token.setExpireAt(LocalDateTime.now().plusDays(expireDays));
        }
        token.setStatus(1);
        tokenMapper.insert(token);

        // 明文仅返回给调用方，不落库
        Token result = new Token();
        result.setId(token.getId());
        result.setProjectId(projectId);
        result.setToken(plainToken);
        result.setTokenName(token.getTokenName());
        result.setExpireAt(token.getExpireAt());
        result.setStatus(1);
        return result;
    }

    public List<Token> list(String projectId) {
        LambdaQueryWrapper<Token> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(projectId)) {
            qw.eq(Token::getProjectId, projectId);
        }
        qw.orderByDesc(Token::getCreatedAt);
        List<Token> list = tokenMapper.selectList(qw);
        list.forEach(t -> t.setToken("******"));
        return list;
    }

    /** 吊销 */
    public void revoke(String id) {
        Token token = tokenMapper.selectById(id);
        if (token == null) {
            throw new BizException(404, "Token 不存在");
        }
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
