/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.internal.changedetection;

import org.gradle.api.internal.TaskInternal;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.TaskInputs;
import org.gradle.api.tasks.TaskOutputs;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.PersistentCache;
import org.gradle.integtests.TestFile;
import org.gradle.util.TemporaryFolder;
import static org.gradle.util.WrapUtil.*;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.Expectations;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Set;
import java.util.Collections;
import java.util.Collection;
import java.util.Arrays;

@RunWith(JMock.class)
public class DefaultTaskArtifactStateRepositoryTest {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    private final JUnit4Mockery context = new JUnit4Mockery();
    private final CacheRepository cacheRepository = context.mock(CacheRepository.class);
    private final Gradle gradle = context.mock(Gradle.class);
    private final Project project = context.mock(Project.class);
    private final PersistentCache cache = context.mock(PersistentCache.class);
    private final TestFile cacheDir = tmpDir.dir("cache");
    private final TestFile outputFile = tmpDir.file("output-file");
    private final TestFile outputDir = tmpDir.dir("output-dir");
    private final TestFile inputFile = tmpDir.file("input-file");
    private final TestFile inputDir = tmpDir.dir("input-dir");
    private final Set<TestFile> inputFiles = toSet(inputFile, inputDir);
    private final Set<TestFile> outputFiles = toSet(outputFile, outputDir);
    private int counter;
    private final DefaultTaskArtifactStateRepository repository = new DefaultTaskArtifactStateRepository(cacheRepository);

    @Before
    public void setup() {
        context.checking(new Expectations() {{
            allowing(project).getGradle();
            will(returnValue(gradle));
        }});
    }

    @Test
    public void artifactsAreNotUpToDateWhenCacheIsEmpty() {
        expectEmptyCacheLocated();

        TaskArtifactState state = repository.getStateFor(task());
        assertNotNull(state);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyOutputFileDoesNotExist() {
        writeTaskState();

        outputFile.delete();

        TaskArtifactState state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyOutputFileHasChangedType() {
        writeTaskState();

        outputFile.delete();
        outputFile.createDir();

        TaskArtifactState state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyOutputFilesAdded() {
        writeTaskState();

        TaskInternal task = builder().withOutputFiles(outputFile, outputDir, tmpDir.file("output-file-2")).task();

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyOutputFilesRemoved() {
        writeTaskState();

        TaskInternal task = builder().withOutputFiles(outputFile).task();

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenTaskWithDifferentPathGeneratedAnyOutputFiles() {
        TaskInternal task = builder().withPath("other").withOutputFiles(outputFile, tmpDir.file("other-output")).task();

        writeTaskState(task, task());

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenTaskWithDifferentTypeGeneratedAnyOutputFiles() {
        TaskInternal task = builder().withType(TaskSubType.class).withOutputFiles(outputFile, tmpDir.file("other-output")).task();

        writeTaskState(task, task());

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyInputFilesAdded() {
        writeTaskState();

        TaskInternal task = builder().withInputFiles(inputFile, inputDir, tmpDir.file("other-input")).task();
        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyInputFilesRemoved() {
        writeTaskState();

        TaskInternal task = builder().withInputFiles(inputFile).task();
        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyInputFileHasDifferentHash() {
        writeTaskState();

        inputFile.write("some new content");

        TaskArtifactState state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyInputFileHasChangedType() {
        writeTaskState();

        inputFile.delete();
        inputFile.createDir();

        TaskArtifactState state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenAnyInputFileNoLongerExists() {
        writeTaskState();

        inputFile.delete();

        TaskArtifactState state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenStateHasBeenInvalidated() {
        writeTaskState();

        TaskArtifactState state = repository.getStateFor(task());
        assertTrue(state.isUpToDate());

        state.invalidate();

        state = repository.getStateFor(task());
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreUpToDateWhenNothingHasChangedSinceOutputFilesWereGenerated() {
        writeTaskState();

        TaskArtifactState state = repository.getStateFor(task());
        assertTrue(state.isUpToDate());

        state = repository.getStateFor(task());
        assertTrue(state.isUpToDate());
    }

    @Test
    public void multipleTasksCanProduceTheSameOutputDirectory() {
        TaskInternal task1 = task();
        TaskInternal task2 = builder().withPath("other").withOutputFiles(outputDir).task();
        writeTaskState(task1, task2);

        TaskArtifactState state = repository.getStateFor(task1);
        assertTrue(state.isUpToDate());

        state = repository.getStateFor(task2);
        assertTrue(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenTaskHasNoInputs() {
        TaskInternal task = builder().withInputFiles().task();
        writeTaskState(task);

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    @Test
    public void artifactsAreNotUpToDateWhenTaskHasNoOutputs() {
        TaskInternal task = builder().withOutputFiles().task();
        writeTaskState(task);

        TaskArtifactState state = repository.getStateFor(task);
        assertFalse(state.isUpToDate());
    }

    private void writeTaskState() {
        writeTaskState(task());
    }

    private void writeTaskState(TaskInternal... tasks) {
        expectEmptyCacheLocated();
        context.checking(new Expectations() {{
            atLeast(1).of(cache).update();
        }});
        for (TaskInternal task : tasks) {
            repository.getStateFor(task).update();
        }
        cacheDir.file("tasks.bin").assertIsFile();
    }
    
    private void expectEmptyCacheLocated() {
        context.checking(new Expectations(){{
            one(cacheRepository).getCacheFor(gradle, "taskArtifacts", Collections.EMPTY_MAP);
            will(returnValue(cache));
            one(cache).isValid();
            will(returnValue(false));
            allowing(cache).getBaseDir();
            will(returnValue(cacheDir));
        }});
    }

    private TaskInternal task() {
        return builder().task();
    }

    private TaskBuilder builder() {
        return new TaskBuilder();
    }

    private class TaskBuilder {
        private String path = "task";
        private Collection<? extends File> inputs = inputFiles;
        private Collection<? extends File> outputs = outputFiles;
        private Class<? extends TaskInternal> type = TaskInternal.class;

        TaskBuilder withInputFiles(File... inputFiles) {
            inputs = Arrays.asList(inputFiles);
            return this;
        }

        TaskBuilder withOutputFiles(File... outputFiles) {
            outputs = Arrays.asList(outputFiles);
            return this;
        }

        TaskBuilder withPath(String path) {
            this.path = path;
            return this;
        }

        TaskBuilder withType(Class<? extends TaskInternal> type) {
            this.type = type;
            return this;
        }

        TaskInternal task() {
            final TaskInternal task = context.mock(type, String.format("task%d", counter++));
            context.checking(new Expectations(){{
                TaskInputs taskInputs = context.mock(TaskInputs.class, String.format("inputs%d", counter++));
                TaskOutputs taskOutputs = context.mock(TaskOutputs.class, String.format("outputs%d", counter++));
                FileCollection outputFileCollection = context.mock(FileCollection.class, String.format("taskOutputFiles%d", counter++));
                FileCollection inputFileCollection = context.mock(FileCollection.class, String.format(
                        "taskInputFiles%d", counter++));

                allowing(task).getProject();
                will(returnValue(project));
                allowing(task).getPath();
                will(returnValue(path));
                allowing(task).getInputs();
                will(returnValue(taskInputs));
                allowing(taskInputs).getInputFiles();
                will(returnValue(inputFileCollection));
                allowing(inputFileCollection).iterator();
                will(returnIterator(inputs));
                allowing(task).getOutputs();
                will(returnValue(taskOutputs));
                allowing(taskOutputs).getOutputFiles();
                will(returnValue(outputFileCollection));
                allowing(outputFileCollection).iterator();
                will(returnIterator(outputs));
            }});
            return task;
        }
    }

    public interface TaskSubType extends TaskInternal {
    }
}
