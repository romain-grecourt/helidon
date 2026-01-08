/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.RoundContext;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.common.types.TypeName;
import io.helidon.metadata.hson.Hson;

class ConfigMetadataCodegenExtension implements CodegenExtension {
    private static final String META_FILE = "META-INF/helidon/config-metadata.json";

    private final Set<TypeName> types = new HashSet<>();
    private final Map<TypeName, ConfiguredType> newOptions = new HashMap<>();
    private final Map<String, List<TypeName>> moduleTypes = new HashMap<>();
    private final CodegenContext ctx;

    ConfigMetadataCodegenExtension(CodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RoundContext roundContext) {
        // we may have multiple rounds, let's collect what we can
        // the type info may change (i.e. we code generate something that is not available in the first round)
        roundContext.annotatedTypes(Types.CONFIGURED)
                .forEach(it -> types.add(it.typeName()));
    }

    @Override
    public void processingOver(RoundContext roundContext) {
        for (var e : types) {
            var typeInfo = ctx.typeInfo(e).orElse(null);
            if (typeInfo != null) {
                var handler = new TypeHandler(ctx, typeInfo);
                var result = handler.handle(roundContext);
                var targetType = result.targetType();
                newOptions.put(targetType, result.configuredType());
                moduleTypes.computeIfAbsent(result.moduleName(), ignored -> new ArrayList<>())
                        .add(targetType);
            }
        }
        storeMetadata();
    }

    private void storeMetadata() {
        if (!moduleTypes.isEmpty()) {
            var root = new ArrayList<Hson.Struct>();
            for (var module : moduleTypes.entrySet()) {
                var moduleName = module.getKey();
                var types = module.getValue();
                var typeArray = new ArrayList<Hson.Struct>();
                for (var e : types) {
                    var option = newOptions.get(e);
                    if (option != null) {
                        root.add(option.toJson());
                    }
                }
                root.add(Hson.structBuilder()
                        .set("module", moduleName)
                        .setStructs("types", typeArray)
                        .build());
            }

            var baos = new ByteArrayOutputStream();
            try (var w = new PrintWriter(baos, true, StandardCharsets.UTF_8)) {
                Hson.Array.create(root).write(w);
                ctx.filer().writeResource(baos.toByteArray(), META_FILE);
            }
        }
    }
}
