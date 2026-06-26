package com.campus.team.infrastructure;

import com.campus.team.config.WeChatProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatService {

    private final WeChatProperties weChatProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, String> code2Session(String code) {
        if (weChatProperties.isMockEnabled()) {
            Map<String, String> mock = new HashMap<>();
            mock.put("openid", "mock_" + (code == null ? UUID.randomUUID().toString().substring(0, 8) : code));
            mock.put("session_key", "mock_session");
            return mock;
        }
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                weChatProperties.getAppId(), weChatProperties.getAppSecret(), code);
        try {
            String resp = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(resp);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new RuntimeException("微信登录失败: " + node.get("errmsg").asText());
            }
            Map<String, String> result = new HashMap<>();
            result.put("openid", node.get("openid").asText());
            result.put("session_key", node.get("session_key").asText());
            if (node.has("unionid")) {
                result.put("unionid", node.get("unionid").asText());
            }
            return result;
        } catch (Exception e) {
            log.error("微信接口调用失败", e);
            throw new RuntimeException("微信登录失败，请稍后重试");
        }
    }
}
