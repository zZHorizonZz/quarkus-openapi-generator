package io.apicurio.hub.api.codegen;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.jboss.forge.roaster.model.util.Types;

import io.apicurio.hub.api.codegen.beans.CodegenBeanAnnotationDirective;
import io.apicurio.hub.api.codegen.beans.CodegenInfo;
import io.apicurio.hub.api.codegen.beans.CodegenJavaInterface;
import io.apicurio.hub.api.codegen.util.CodegenUtil;

public class OpenApi2QuarkusServerGenerator extends OpenApi2JaxRs {

    private final boolean mutiny;
    private final boolean resteasy;

    public OpenApi2QuarkusServerGenerator(boolean mutiny, boolean resteasy) {
        super();

        this.mutiny = mutiny;
        this.resteasy = resteasy;
    }

    @Override
    protected String generateJavaInterface(CodegenInfo info, CodegenJavaInterface interfaceInfo, String topLevelPackage) {
        String jaxRsPath = info.getContextRoot() + interfaceInfo.getPath();
        final Parser markdownParser = Parser.builder().build();
        final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        final String iClassName = CodegenUtil.toClassName(settings, interfaceInfo.getName());
        final String iPackageName = interfaceInfo.getPackage();

        JavaInterfaceSource resourceInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(iPackageName)
                .setPublic()
                .setName(iClassName)
                .getJavaDoc()
                .setFullText("A JAX-RS interface. An implementation of this interface must be provided.")
                .getOrigin()
                .addAnnotation(String.format("%s.ws.rs.Path", topLevelPackage))
                .setStringValue(jaxRsPath)
                .getOrigin();

        interfaceInfo.getMethods().forEach(methodInfo -> {
            MethodSource<JavaInterfaceSource> operationMethod = resourceInterface.addMethod()
                    .setName(methodInfo.getName());

            Optional.ofNullable(methodInfo.getDescription()).ifPresent(description -> {
                operationMethod.getJavaDoc()
                        .setFullText(htmlRenderer.render(markdownParser.parse(description)));
            });

            Optional.ofNullable(methodInfo.getPath()).ifPresent(path -> operationMethod
                    .addAnnotation(String.format("%s.ws.rs.Path", topLevelPackage)).setStringValue(path));

            operationMethod.addAnnotation(String.format("%s.ws.rs.%s", topLevelPackage, methodInfo.getMethod().toUpperCase()));

            Optional.ofNullable(methodInfo.getProduces())
                    .filter(Predicate.not(Collection::isEmpty))
                    .map(OpenApi2JaxRs::toStringArrayLiteral)
                    .ifPresent(produces -> operationMethod.addAnnotation(String.format("%s.ws.rs.Produces", topLevelPackage))
                            .setLiteralValue(produces));

            Optional.ofNullable(methodInfo.getConsumes())
                    .filter(Predicate.not(Collection::isEmpty))
                    .map(OpenApi2JaxRs::toStringArrayLiteral)
                    .ifPresent(consumes -> operationMethod.addAnnotation(String.format("%s.ws.rs.Consumes", topLevelPackage))
                            .setLiteralValue(consumes));

            final boolean reactive;

            if (getSettings().isReactive()) {
                // Reactive mode but this operation is explicitly synchronous
                reactive = !Boolean.FALSE.equals(methodInfo.getAsync());
            } else {
                // Non-reactive mode but this operation is explicitly asynchronous
                reactive = Boolean.TRUE.equals(methodInfo.getAsync());
            }

            Optional.ofNullable(methodInfo.getReturn())
                    .map(rt -> generateTypeName(
                            rt,
                            true,
                            String.format("%s.ws.rs.core.Response", topLevelPackage)))
                    .map(rt -> reactive ? generateReactiveTypeName(rt) : rt)
                    .map(Object::toString)
                    .ifPresentOrElse(
                            operationMethod::setReturnType,
                            () -> setVoidReturnType(operationMethod, reactive));

            Optional.ofNullable(methodInfo.getArguments())
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .forEach(arg -> {
                        String methodArgName = paramNameToJavaArgName(arg.getName());
                        String defaultParamType = Object.class.getName();

                        if (arg.getIn().equals("body")) {
                            // Swagger 2.0?
                            defaultParamType = InputStream.class.getName();
                        }

                        Type<?> paramType = generateTypeName(arg, arg.getRequired(), defaultParamType);

                        if (arg.getTypeSignature() != null) {
                            // TODO try to find a re-usable data type that matches the type signature
                        }

                        resourceInterface.addImport(paramType);
                        var paramTypeName = Types.toSimpleName(paramType.getQualifiedNameWithGenerics());
                        ParameterSource<JavaInterfaceSource> param = operationMethod.addParameter(paramTypeName, methodArgName);

                        switch (arg.getIn()) {
                            case "path":
                                param.addAnnotation(String.format("%s.ws.rs.PathParam", topLevelPackage))
                                        .setStringValue(arg.getName());
                                break;
                            case "query":
                                param.addAnnotation(String.format("%s.ws.rs.QueryParam", topLevelPackage))
                                        .setStringValue(arg.getName());
                                break;
                            case "header":
                                param.addAnnotation(String.format("%s.ws.rs.HeaderParam", topLevelPackage))
                                        .setStringValue(arg.getName());
                                break;
                            case "cookie":
                                param.addAnnotation(String.format("%s.ws.rs.CookieParam", topLevelPackage))
                                        .setStringValue(arg.getName());
                                break;
                            default:
                                break;
                        }

                        boolean forbidNotNull = paramType.isPrimitive() ||
                                "path".equals(arg.getIn()) ||
                                !arg.getRequired();

                        super.addValidationConstraints(param, arg, forbidNotNull, topLevelPackage);

                        Optional.ofNullable(arg.getAnnotations())
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .map(CodegenBeanAnnotationDirective::getAnnotation)
                                .map(this::parseParameterAnnotation)
                                .forEach(source -> {
                                    AnnotationSource<?> target = param.addAnnotation(source.getQualifiedName());
                                    source.getValues()
                                            .forEach(value -> target.setLiteralValue(value.getName(), value.getLiteralValue()));
                                });
                    });
        });

        sortImports(resourceInterface);

        return Roaster.format(getFormatterProperties(), resourceInterface.toUnformattedString());
    }

    @Override
    protected Type<?> generateReactiveTypeName(Type<?> coreType) {
        if (!mutiny) {
            return parseType(String.format("java.util.concurrent.CompletionStage<%s>", generateResponseReturnType(coreType)));
        } else {
            return parseType(String.format("io.smallrye.mutiny.Uni<%s>", generateResponseReturnType(coreType)));
        }
    }

    private String generateResponseReturnType(Type<?> coreType) {
        if (!resteasy) {
            return coreType.toString();
        } else {
            if (coreType.isType(Void.class)) {
                return "jakarta.ws.rs.core.Response";
            } else {
                return String.format("org.jboss.resteasy.reactive.RestResponse<%s>", coreType);
            }
        }
    }
}
