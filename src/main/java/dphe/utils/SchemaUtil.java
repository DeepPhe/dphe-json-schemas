package dphe.utils;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.generator.*;

public class SchemaUtil {
    private final SchemaGenerator generator;
    private final String baseUrl;

    public SchemaUtil(ClassLoader classLoader) {
        this(classLoader, null);
    }

    public SchemaUtil(ClassLoader classLoader, String baseUrl) {
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(
                SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
                .with(new JacksonModule());

        // Add $id field if baseUrl is provided
        if (baseUrl != null && !baseUrl.isEmpty()) {
            final String url = baseUrl;
            configBuilder.forTypesInGeneral()
                .withIdResolver(scope -> {
                    String className = scope.getType().getErasedType().getSimpleName();
                    String schemaId = url;
                    if (!schemaId.endsWith("/")) {
                        schemaId += "/";
                    }
                    return schemaId + className + ".schema.json";
                });
        }

        SchemaGeneratorConfig config = configBuilder.build();
        this.generator = new SchemaGenerator(config);
        this.baseUrl = baseUrl;
    }

    public String generateSchemaJson(Class<?> clazz) {
        return generator.generateSchema(clazz).toString();
    }
}