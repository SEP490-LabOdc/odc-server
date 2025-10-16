package com.odc.apigateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
@Slf4j
public class SwaggerAggregatorController {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final DiscoveryClient discoveryClient;

    @GetMapping("/merged")
    public ResponseEntity<JsonNode> mergedSwaggerDocs() {
        List<String> serviceIds = discoveryClient.getServices();

        ObjectNode merged = mapper.createObjectNode();
        merged.put("openapi", "3.0.1");

        ObjectNode pathsNode = mapper.createObjectNode();
        ObjectNode schemasNode = mapper.createObjectNode();
        ArrayNode tagsNode = mapper.createArrayNode();

        for (String serviceId : serviceIds) {
            String url = "http://" + serviceId + "/" + serviceId + "/v3/api-docs";

            try {
                JsonNode serviceDoc = restTemplate.getForObject(url, JsonNode.class);

                // Merge paths
                JsonNode paths = serviceDoc.get("paths");
                if (paths != null && paths.isObject()) {
                    paths.fields().forEachRemaining(entry ->
                            pathsNode.set(entry.getKey(), entry.getValue()));
                }

                // Merge schemas
                JsonNode schemas = serviceDoc.path("components").path("schemas");
                if (schemas != null && schemas.isObject()) {
                    schemas.fields().forEachRemaining(entry ->
                            schemasNode.set(entry.getKey(), entry.getValue()));
                }

                // Merge tags
                JsonNode tags = serviceDoc.get("tags");
                if (tags != null && tags.isArray()) {
                    tags.forEach(tagsNode::add);
                }

            } catch (Exception e) {
                log.error("Can not fetch data from: {}", url, e);
            }
        }

        ObjectNode securitySchemesNode = mapper.createObjectNode();
        ObjectNode bearerAuthScheme = mapper.createObjectNode();
        bearerAuthScheme.put("type", "http");
        bearerAuthScheme.put("scheme", "bearer");
        bearerAuthScheme.put("bearerFormat", "JWT");
        securitySchemesNode.set("bearerAuth", bearerAuthScheme);

        ObjectNode components = mapper.createObjectNode();
        components.set("schemas", schemasNode);
        components.set("securitySchemes", securitySchemesNode);

        ObjectNode info = mapper.createObjectNode();
        info.put("title", "LabOdc API");
        info.put("version", "1.0.0");

        ArrayNode securityArray = mapper.createArrayNode();
        ObjectNode securityRequirement = mapper.createObjectNode();
        securityRequirement.set("bearerAuth", mapper.createArrayNode());
        securityArray.add(securityRequirement);

        pathsNode.fields().forEachRemaining(pathEntry -> {
            JsonNode pathItem = pathEntry.getValue();
            if (pathItem.isObject()) {
                ObjectNode pathObj = (ObjectNode) pathItem;
                pathObj.fields().forEachRemaining(operationEntry -> {
                    JsonNode operation = operationEntry.getValue();
                    if (operation.isObject()) {
                        ObjectNode operationObj = (ObjectNode) operation;
                        if (!operationObj.has("security")) {
                            ArrayNode opSecurityArray = mapper.createArrayNode();
                            ObjectNode opSecurityRequirement = mapper.createObjectNode();
                            opSecurityRequirement.set("bearerAuth", mapper.createArrayNode());
                            opSecurityArray.add(opSecurityRequirement);
                            operationObj.set("security", opSecurityArray);
                        }
                    }
                });
            }
        });

        merged.set("info", info);
        merged.set("paths", pathsNode);
        merged.set("components", components);
        merged.set("tags", tagsNode);
        merged.set("security", securityArray);

        return ResponseEntity.ok(merged);
    }
}
