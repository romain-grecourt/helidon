/*
 * Copyright (c) 2023, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.config.metadata.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenException;
import io.helidon.codegen.ElementInfoPredicates;
import io.helidon.codegen.RoundContext;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.config.metadata.codegen.ConfiguredProperty.AllowedValue;
import io.helidon.config.metadata.codegen.ConfiguredType.ProducerMethod;

import static io.helidon.common.types.ElementKind.ENUM;
import static io.helidon.config.metadata.codegen.Types.CONFIG;
import static io.helidon.config.metadata.codegen.Types.CONFIGURED;
import static io.helidon.config.metadata.codegen.Types.OPTION;
import static io.helidon.config.metadata.codegen.Types.OPTIONS;

/**
 * Type handler.
 */
class TypeHandler {

    /**
     * Result of annotation processing.
     *
     * @param targetType     target type (the result of the builder)
     * @param moduleName     module of the type
     * @param configuredType collected configuration metadata
     */
    record Result(TypeName targetType,
                  String moduleName,
                  ConfiguredType configuredType) {
    }

    static final String UNCONFIGURED_OPTION = "io.helidon.config.metadata.ConfiguredOption.UNCONFIGURED";

    private final CodegenContext ctx;
    private final TypeInfo typeInfo;
    private final TypeName typeName;

    TypeHandler(CodegenContext ctx, TypeInfo typeInfo) {
        this.ctx = ctx;
        this.typeInfo = typeInfo;
        this.typeName = typeInfo.typeName();
    }

    /**
     * Discover all options of the configured type.
     *
     * @return result
     */
    Result handle(RoundContext rc) {
        TypeInfo targetType;
        boolean isBuilder;
        String module;

        var targetTypeName = findBuilderTarget(typeInfo).orElse(null);
        var annotation = ConfiguredAnnotation.create(typeInfo.annotation(CONFIGURED));
        if (!annotation.ignoreBuildMethod() && targetTypeName != null) {
            // we want this both for abstract types and implementations
            // this is a builder, we need the target type Builder<Builder, TargetType>
            targetType = ctx.typeInfo(targetTypeName, ElementInfoPredicates::isMethod)
                    .orElseThrow(() -> new IllegalStateException(
                            "Cannot find target type info for type %s, discovered for type: %s".formatted(
                                    targetTypeName.fqName(),
                                    typeInfo.typeName().fqName())));
            isBuilder = true;
            module = targetType.module().orElse("unknown");
        } else {
            targetType = typeInfo;
            isBuilder = false;
            module = typeInfo.module().orElse("unknown");
        }

        // now we know whether this is
        // - a builder + known target class (result of builder() method)
        // - a standalone class (probably with public static create(Config) method)
        // - an interface/abstract class only used for inheritance
        var type = new ConfiguredType(
                annotation,
                typeName,
                targetType.typeName(),
                new HashSet<>(),
                new ArrayList<>(),
                new ArrayList<>());

        // we also need to know all superclasses / interfaces that are configurable so we can reference them
        // these may be from other modules, so we cannot create a single set of values from all types
        addSuperClasses(type, typeInfo);
        addInterfaces(type, typeInfo);

        if (isBuilder) {
            // builder
            processBuilderType(rc, typeInfo, type, typeName, targetType);
        } else {
            // standalone class with create method(s), or interface/abstract class
            processTargetType(rc, typeInfo, type, typeName, annotation.root());
        }

        return new Result(targetType.typeName(), module, type);
    }

    private List<AllowedValue> enumValues(RoundContext rc,
                                          ConfiguredOptionData option,
                                          TypeName type) {

        if (type.equals(option.type()) || !option.allowedValues().isEmpty()) {
            // this was already processed due to an explicit type defined in the annotation
            // or allowed values explicitly configured in annotation
            return option.allowedValues();
        }
        return enumValues(rc, type);
    }

    private List<TypeName> params(TypedElementInfo info) {
        return info.parameterArguments()
                .stream()
                .map(TypedElementInfo::typeName)
                .toList();
    }

    private void addInterfaces(ConfiguredType type, TypeInfo typeInfo) {
        for (var interfaceInfo : typeInfo.interfaceTypeInfo()) {
            if (interfaceInfo.hasAnnotation(Types.CONFIGURED)) {
                type.inherits().add(interfaceInfo.typeName());
            } else {
                addSuperClasses(type, interfaceInfo);
            }
        }
    }

    private void addSuperClasses(ConfiguredType type, TypeInfo typeInfo) {
        var superClass = typeInfo.superTypeInfo().orElse(null);
        while (superClass != null) {
            if (superClass.hasAnnotation(Types.CONFIGURED)) {
                // we only care about the first one. This one should reference its superclass/interfaces
                // if they are configured as well
                type.inherits().add(superClass.typeName());
                return;
            }
            superClass = superClass.superTypeInfo().orElse(null);
        }
    }

    private List<ConfiguredOptionData> findConfiguredOptionAnnotations(RoundContext rc, TypedElementInfo elementInfo) {

        if (elementInfo.hasAnnotation(OPTIONS)) {
            var metaOptions = elementInfo.annotation(OPTIONS);
            return metaOptions.annotationValues()
                    .stream()
                    .flatMap(List::stream)
                    .map(it -> ConfiguredOptionData.create(ctx, rc, it))
                    .toList();
        }

        if (elementInfo.hasAnnotation(OPTION)) {
            Annotation metaOption = elementInfo.annotation(OPTION);
            return List.of(ConfiguredOptionData.create(ctx, rc, metaOption));
        }

        return List.of();
    }

    private void processBuilderMethod(RoundContext rc,
                                      TypeName typeName,
                                      ConfiguredType configuredType,
                                      TypedElementInfo elementInfo,
                                      BiFunction<TypedElementInfo, ConfiguredOptionData, OptionType> optionTypeMethod,
                                      BiFunction<TypedElementInfo, OptionType, List<TypeName>> builderParamsMethod) {

        var options = findConfiguredOptionAnnotations(rc, elementInfo);
        if (options.isEmpty()) {
            return;
        }

        for (var option : options) {
            if (option.configured()) {
                var name = key(elementInfo, option);
                var description = description(rc, typeInfo, elementInfo, option);
                var defaultValue = UNCONFIGURED_OPTION.equals(option.defaultValue()) ? null : option.defaultValue();
                var type = optionTypeMethod.apply(elementInfo, option);
                var required = option.required() || defaultValue == null;
                var allowedValues = enumValues(rc, option, type.elementType());
                var paramTypes = builderParamsMethod.apply(elementInfo, type);
                var method = new ProducerMethod(typeName, elementInfo.elementName(), paramTypes);
                configuredType.properties().add(new ConfiguredProperty(
                        method.toString(),
                        name,
                        type.elementType(),
                        description,
                        required,
                        defaultValue,
                        option.experimental(),
                        option.provider(),
                        option.providerType(),
                        option.deprecated(),
                        option.merge(),
                        type.kind(),
                        allowedValues,
                        null));
            }
        }
    }

    // annotated type or type methods (not a builder)
    private void processTargetType(RoundContext rc,
                                   TypeInfo typeInfo,
                                   ConfiguredType type,
                                   TypeName typeName,
                                   boolean standalone) {

        // go through all methods, find all create methods and create appropriate configured producers for them
        // if there is a builder, add the builder producer as well
        var methods = typeInfo.elementInfo().stream()
                .filter(ElementInfoPredicates::isMethod)
                // public, package local or protected
                .filter(Predicate.not(ElementInfoPredicates::isPrivate))
                // static
                .filter(ElementInfoPredicates::isStatic)
                .toList();

        // either this is a target class (such as an interface with create method)
        // or this is an interface/abstract class inherits by builders
        var isTargetType = false;
        var validMethods = new ArrayList<TypedElementInfo>();
        TypedElementInfo configCreator = null;

        // now we have just public static methods, let's look for create/builder
        for (var method : methods) {
            var name = method.elementName();
            if ("create".equals(name)) {
                if (method.typeName().genericTypeName().equals(typeName.genericTypeName())) {
                    validMethods.add(method);
                    var parameters = method.parameterArguments();
                    if (parameters.size() == 1) {
                        var paramType = parameters.getFirst().typeName();
                        if (paramType.equals(CONFIG)) {
                            configCreator = method;
                        }
                    }
                    isTargetType = true;
                }
            } else if (name.equals("builder")) {
                throw new CodegenException(
                        "Type %s is marked with @Configured, yet it has a static builder() method.".formatted(
                                typeName.fqName()),
                        typeInfo.originatingElementValue());
            }
        }

        if (isTargetType) {
            if (configCreator != null) {
                type.producers().add(new ProducerMethod(
                        typeName,
                        configCreator.elementName(),
                        params(configCreator)));
            }

            // now let's find all methods with @ConfiguredOption
            for (var method : validMethods) {
                var options = findConfiguredOptionAnnotations(rc, method);
                for (var option : options) {
                    if ((option.key() == null || option.key().isBlank()) && !option.merge()) {
                        throw new CodegenException(
                                "ConfiguredOption on %s.%s requires a value defined".formatted(
                                        typeName.fqName(),
                                        method),
                                typeInfo.originatingElementValue());
                    }

                    if (option.description() == null || option.description().isBlank()) {
                        throw new CodegenException(
                                "ConfiguredOption on %s.%s requires a description".formatted(
                                        typeName.fqName(),
                                        method),
                                typeInfo.originatingElementValue());
                    }

                    type.properties().add(new ConfiguredProperty(
                            null,
                            option.key(),
                            option.type() != null ? option.type() : TypeNames.STRING,
                            option.description(),
                            option.required(),
                            option.defaultValue(),
                            option.experimental(),
                            option.provider(),
                            option.providerType(),
                            option.deprecated(),
                            option.merge(),
                            option.kind(),
                            option.allowedValues(),
                            null));
                }
            }
        } else {
            // this must be a class/interface used by other classes to extend
            // so we care about all builder style methods
            if (standalone) {
                throw new CodegenException(
                        "Type %s does not have a builder method, or a create method".formatted(
                                typeName.fqName()),
                        typeInfo.originatingElementValue());
            }

            typeInfo.elementInfo()
                    .stream()
                    .filter(ElementInfoPredicates::isMethod) // methods
                    .filter(Predicate.not(ElementInfoPredicates::isPrivate)) // public, package or protected
                    .filter(Predicate.not(ElementInfoPredicates::isStatic)) // not static
                    .filter(it -> isType(it, typeName)) // declared on this type
                    .forEach(it -> processBuilderMethod(rc, typeName, type, it));
        }
    }

    // annotated builder methods
    private void processBuilderType(RoundContext rc,
                                    TypeInfo typeInfo,
                                    ConfiguredType type,
                                    TypeName typeName,
                                    TypeInfo targetType) {
        type.producers().add(new ProducerMethod(typeName, "build", List.of()));

        // check if static TargetType create(Config) exists
        var targetTypeName = targetType.typeName();
        if (targetType.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(ElementInfoPredicates::isStatic)
                .filter(Predicate.not(ElementInfoPredicates::isPrivate))
                .filter(ElementInfoPredicates.elementName("create"))
                .filter(TypeHandler::hasConfigParam)
                .anyMatch(it -> isType(it, targetTypeName))) {

            type.producers().add(new ProducerMethod(
                    targetTypeName,
                    "create",
                    List.of(CONFIG)));
        }

        // find all public methods annotated with @ConfiguredOption
        typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod) // methods
                .filter(Predicate.not(ElementInfoPredicates::isPrivate)) // not private
                .filter(it -> isType(it, typeName)) // declared on this type
                .filter(it -> it.hasAnnotation(OPTION) || it.hasAnnotation(OPTIONS))
                .forEach(it -> processBuilderMethod(rc, typeName, type, it));
    }

    private List<TypeName> builderMethodParams(TypedElementInfo elementInfo, OptionType type) {
        return params(elementInfo);
    }

    private void processBuilderMethod(RoundContext rc,
                                      TypeName typeName,
                                      ConfiguredType configuredType,
                                      TypedElementInfo elementInfo) {

        processBuilderMethod(rc, typeName, configuredType, elementInfo, this::optionType, this::builderMethodParams);
    }

    private List<AllowedValue> enumValues(RoundContext rc, TypeName type) {
        return ctx.typeInfo(type)
                .filter(it -> it.kind() == ENUM)
                .map(it -> ConfiguredOptionData.enumValues(rc, it))
                .orElseGet(List::of);
    }

    private OptionType optionType(TypedElementInfo elementInfo, ConfiguredOptionData option) {
        if (option.type() == null || option.type().equals(OPTION)) {
            // guess from method
            var parameters = elementInfo.parameterArguments();
            if (parameters.size() != 1) {
                throw new CodegenException(
                        "Method %s does not have explicit type, or exactly one parameter".formatted(
                                elementInfo.elementName()),
                        typeInfo.originatingElementValue());
            } else {
                var parameter = parameters.getFirst();
                var paramType = parameter.typeName();
                if (paramType.isList() || paramType.isSet()) {
                    return new OptionType(paramType.typeArguments().getFirst(), "LIST");
                }
                if (paramType.isMap()) {
                    return new OptionType(paramType.typeArguments().get(1), "MAP");
                }
                return new OptionType(paramType.boxed(), option.kind());
            }
        } else {
            // use the one defined on annotation
            return new OptionType(option.type(), option.kind());
        }
    }

    private static Optional<TypeName> findBuilderTarget(TypeInfo typeInfo) {
        // non-private build method exists that has no parameters, not static, and returns a type
        return typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(Predicate.not(ElementInfoPredicates::isStatic))
                .filter(ElementInfoPredicates::hasNoArgs)
                .filter(ElementInfoPredicates.elementName("build"))
                .filter(Predicate.not(ElementInfoPredicates::isVoid))
                .filter(Predicate.not(ElementInfoPredicates::isPrivate))
                .findFirst()
                .map(TypedElementInfo::typeName);
    }

    private static boolean isType(TypedElementInfo elementInfo, TypeName type) {
        var withoutGenerics = type.genericTypeName();
        return elementInfo.enclosingType()
                .map(TypeName::genericTypeName)
                .map(withoutGenerics::equals)
                .orElse(true);
    }

    private static boolean hasConfigParam(TypedElementInfo info) {
        var arguments = info.parameterArguments();
        if (arguments.size() != 1) {
            return false;
        }
        var argumentType = arguments.getFirst().typeName();
        return CONFIG.equals(argumentType);
    }

    private static String key(TypedElementInfo elementInfo, ConfiguredOptionData option) {
        var name = option.key();
        if (name == null || name.isBlank()) {
            return snakeCase(elementInfo.elementName());
        }
        return name;
    }

    private static String description(RoundContext rc,
                                      TypeInfo typeInfo,
                                      TypedElementInfo elementInfo,
                                      ConfiguredOptionData option) {

        String desc = option.description();
        if (desc == null) {
            return JavadocProcessor.process(rc, typeInfo, elementInfo, true);
        }
        return desc;
    }

    private static String snakeCase(String str) {
        var sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (sb.isEmpty()) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append('-');
                    sb.append(Character.toLowerCase(c));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
