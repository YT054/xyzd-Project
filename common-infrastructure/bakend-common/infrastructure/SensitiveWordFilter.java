package com.campus.team.infrastructure;

import com.campus.team.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SensitiveWordFilter {

    private final List<String> words;

    public SensitiveWordFilter(@Value("${campus.sensitive-words}") String wordStr) {
        this.words = Arrays.stream(wordStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public void check(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        for (String word : words) {
            if (content.contains(word)) {
                throw new BusinessException("内容包含敏感词，请修改后重试");
            }
        }
    }

    public String filter(String content) {
        if (content == null) {
            return null;
        }
        String result = content;
        for (String word : words) {
            result = result.replace(word, "***");
        }
        return result;
    }
}
