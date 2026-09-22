/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.commons.csv;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Approved Scenarios runner.
 * <p>
 * Reads every {@code approved/*.approved.md} fixture, parses the declared input with the declared format settings,
 * regenerates the {@code ## 解析结果} section from the actual run, and asserts the regenerated text equals the file on disk.
 * Every other section is the human-authored business fact.
 * </p>
 * <p>
 * Review is a Markdown diff, never assertion code. On mismatch a sibling {@code .received.md} is written; set the
 * environment variable {@code APPROVE=1} to overwrite the fixtures with actual output instead (then review {@code git diff}).
 * </p>
 */
class ApprovedScenariosTest {

    /** Read from the source tree, not target/test-classes, so that APPROVE=1 rewrites the files under version control. */
    private static final Path APPROVED_DIR = Paths.get("src/test/resources/org/apache/commons/csv/approved");

    private static final String RESULT_HEADING = "解析结果";

    private static final Pattern INPUT = section("输入 `([^`]+)`");

    private static final Pattern CONFIG = section("格式配置");

    private static final Pattern RESULT = section(RESULT_HEADING);

    private static Pattern section(final String heading) {
        return Pattern.compile("^## " + heading + "[^\\n]*\\n\\n```[a-z]*\\n(.*?)```", Pattern.MULTILINE | Pattern.DOTALL);
    }

    private static CSVFormat format(final String config) {
        final CSVFormat.Builder builder = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true);
        for (final String line : config.split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            final String[] kv = trimmed.split("=", 2);
            final String key = kv[0].trim();
            final String value = kv.length > 1 ? kv[1].trim() : "";
            if ("requiredHeaders".equals(key)) {
                builder.setRequiredHeaders(value.split(","));
            } else if ("ignoreHeaderCase".equals(key)) {
                builder.setIgnoreHeaderCase(Boolean.parseBoolean(value));
            } else {
                throw new IllegalStateException("Unknown format setting in fixture: " + key);
            }
        }
        return builder.get();
    }

    private static String run(final String fixture) {
        final Matcher input = INPUT.matcher(fixture);
        final Matcher config = CONFIG.matcher(fixture);
        if (!input.find() || !config.find()) {
            throw new IllegalStateException("Fixture needs '## 输入 `<name>`' and '## 格式配置' sections");
        }
        final CSVFormat format = format(config.group(1));
        final StringBuilder out = new StringBuilder();
        try (CSVParser parser = CSVParser.parse(new StringReader(input.group(2)), format)) {
            final List<String> lines = new ArrayList<>();
            int n = 0;
            for (final CSVRecord record : parser) {
                lines.add("记录 " + ++n + ": " + record.toList());
            }
            out.append("结论: 通过\n");
            out.append("实际表头: ").append(parser.getHeaderNames()).append('\n');
            for (final String line : lines) {
                out.append(line).append('\n');
            }
        } catch (final IllegalArgumentException | IOException | IllegalStateException e) {
            out.append("结论: 拒绝\n");
            out.append("异常: ").append(e.getClass().getSimpleName()).append('\n');
            out.append("消息: ").append(e.getMessage()).append('\n');
        }
        return out.toString();
    }

    private static String regenerate(final String fixture, final String actual) {
        final Matcher result = RESULT.matcher(fixture);
        if (!result.find()) {
            throw new IllegalStateException("Fixture needs a '## " + RESULT_HEADING + "' section");
        }
        return fixture.substring(0, result.start(1)) + actual + fixture.substring(result.end(1));
    }

    private static void verify(final Path path) throws IOException {
        final String approved = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        final String received = regenerate(approved, run(approved));
        final Path receivedPath = path.resolveSibling(path.getFileName().toString().replace(".approved.md", ".received.md"));
        if (System.getenv("APPROVE") != null) {
            Files.write(path, received.getBytes(StandardCharsets.UTF_8));
            return;
        }
        if (!received.equals(approved)) {
            Files.write(receivedPath, received.getBytes(StandardCharsets.UTF_8));
            fail(String.format("%s: actual output differs from the approved facts.%nInspect %s, or rerun with APPROVE=1 to accept and review the diff.",
                    path.getFileName(), receivedPath.getFileName()));
        }
        Files.deleteIfExists(receivedPath);
    }

    @TestFactory
    Stream<DynamicTest> approvedScenarios() throws IOException {
        final List<Path> fixtures;
        try (Stream<Path> files = Files.list(APPROVED_DIR)) {
            fixtures = files.filter(p -> p.getFileName().toString().endsWith(".approved.md")).sorted().collect(Collectors.toList());
        }
        if (fixtures.isEmpty()) {
            throw new IllegalStateException("No fixtures found in " + APPROVED_DIR.toAbsolutePath());
        }
        return fixtures.stream().map(p -> DynamicTest.dynamicTest(p.getFileName().toString(), () -> verify(p)));
    }
}
