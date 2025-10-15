package com.odc.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Component
@Data
public class NotificationRoutingConfig {

    private DefaultRule defaults;
    private Map<String, Rule> rules;

    @Data
    public static class DefaultRule {
        private List<String> channels;
        private String target_strategy;
        private List<String> roles;
    }

    @Data
    public static class Rule {
        private List<String> channels;
        private String target_strategy;
        private List<String> roles;
    }

    @PostConstruct
    public void loadConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config/notification-routing.yml")) {
            NotificationRoutingConfig config = mapper.readValue(is, NotificationRoutingConfig.class);
            this.defaults = config.defaults;
            this.rules = config.rules;
        }
    }

    public Rule getRule(String type) {
        return rules.getOrDefault(type, toRule(defaults));
    }

    private Rule toRule(DefaultRule def) {
        Rule r = new Rule();
        r.setChannels(def.getChannels());
        r.setRoles(def.getRoles());
        r.setTarget_strategy(def.getTarget_strategy());
        return r;
    }
}