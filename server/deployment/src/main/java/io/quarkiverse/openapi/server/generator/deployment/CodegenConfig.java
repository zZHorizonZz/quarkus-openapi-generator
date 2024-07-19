package io.quarkiverse.openapi.server.generator.deployment;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = CodegenConfig.CODEGEN_TIME_CONFIG_PREFIX, phase = ConfigPhase.BUILD_TIME)
public class CodegenConfig {

    static final String CODEGEN_TIME_CONFIG_PREFIX = "quarkus.openapi.generator";
    private static final String CODEGEN_BASE_PACKAGE = CODEGEN_TIME_CONFIG_PREFIX + ".base-package";
    private static final String CODEGEN_SPEC = CODEGEN_TIME_CONFIG_PREFIX + ".spec";
    private static final String INPUT_BASE_DIR = CODEGEN_TIME_CONFIG_PREFIX + ".input-base-dir";
    private static final String CODEGEN_REACTIVE = CODEGEN_TIME_CONFIG_PREFIX + ".reactive";
    private static final String CODEGEN_REACTIVE_MUTINY = CODEGEN_REACTIVE + ".mutiny";
    private static final String CODEGEN_RETURN_RESPONSE = CODEGEN_TIME_CONFIG_PREFIX + ".return-response";

    public static String getBasePackagePropertyName() {
        return CODEGEN_BASE_PACKAGE;
    }

    public static String getSpecPropertyName() {
        return CODEGEN_SPEC;
    }

    public static String getInputBaseDirPropertyName() {
        return INPUT_BASE_DIR;
    }

    public static String getCodegenReactive() {
        return CODEGEN_REACTIVE;
    }

    public static String getCodegenReactiveMutiny() {
        return CODEGEN_REACTIVE_MUTINY;
    }

    public static String getCodegenReturnResponse() {
        return CODEGEN_RETURN_RESPONSE;
    }
}
