/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.internal;

import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.BinarySpec;

public interface BinarySpecInternal extends BinarySpec {
    ModelType<BinarySpec> PUBLIC_MODEL_TYPE = ModelType.of(BinarySpec.class);

    /**
     * Returns a name for this binary that is unique for all binaries in the current project.
     */
    String getProjectScopedName();

    Class<? extends BinarySpec> getPublicType();

    void setPublicType(Class<? extends BinarySpec> publicType);

    void setBuildable(boolean buildable);

    BinaryBuildAbility getBuildAbility();

    boolean isLegacyBinary();

}
