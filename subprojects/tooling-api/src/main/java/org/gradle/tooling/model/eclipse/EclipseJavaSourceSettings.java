/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.tooling.model.eclipse;

import org.gradle.api.Incubating;
import org.gradle.tooling.model.java.JavaSourceSettings;

/**
 * Describes Java source settings for an Eclipse project.
 *
 * The values are calculated as follows:
 * <ol>
 *     <li>the values are loaded from the {@code org.gradle.plugins.ide.eclipse.EclipsePlugin} configuration if available, otherwise</li>
 *     <li>the values are loaded from the {@code org.gradle.api.plugins.JavaPlugin} configuration if available, otherwise</li>
 *     <li>the values from the current Java runtime are loaded</li>
 * </ol>
 *
 * @since 2.10
 */
@Incubating
public interface EclipseJavaSourceSettings extends JavaSourceSettings {
    // Java source settings specific to Eclipse comes here
}
