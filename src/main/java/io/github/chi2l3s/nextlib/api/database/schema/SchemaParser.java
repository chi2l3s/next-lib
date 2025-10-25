package io.github.chi2l3s.nextlib.api.database.schema;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SchemaParser {
    private final Yaml yaml;

    public SchemaParser() {
        Constructor constructor = new Constructor(SchemaDefinition.class, new LoaderOptions());
        PropertyUtils propertyUtils = new PropertyUtils();
        propertyUtils.setSkipMissingProperties(true);
        constructor.setPropertyUtils(propertyUtils);
        this.yaml = new Yaml(constructor);
    }

    public SchemaDefinition parse(Path schemaPath) {
        try (Reader reader = Files.newBufferedReader(schemaPath)) {
            SchemaDefinition definition = yaml.load(reader);
            if (definition == null) {
                throw new SchemaParseException("Schema file is empty: " + schemaPath);
            }
            definition.validate();
            return definition;
        } catch (IOException exception) {
            throw new SchemaParseException("Failed to read schema file: " + schemaPath, exception);
        } catch (RuntimeException exception) {
            throw new SchemaParseException("Failed to parse schema file: " + schemaPath, exception);
        }
    }
}