package dphe.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;


public class SchemaUtil {
    private final SchemaGenerator generator;
    private final ObjectMapper mapper = new ObjectMapper();


    public SchemaUtil(ClassLoader cl) {
        SchemaGeneratorConfigBuilder cfgBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        cfgBuilder.with(new JacksonModule());
        cfgBuilder.with(Option.DEFINITIONS_FOR_ALL_OBJECTS);
        SchemaGeneratorConfig config = cfgBuilder.build();
        this.generator = new SchemaGenerator(config);
    }


    public String generateSchemaJson(Class<?> type) {
        com.fasterxml.jackson.databind.JsonNode node = generator.generateSchema(type);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}