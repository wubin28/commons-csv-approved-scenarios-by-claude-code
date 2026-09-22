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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

/**
 * Interface contracts of {@link CSVFormat.Builder#setRequiredHeaders(String...)}; business facts live in the Approved Scenarios fixtures.
 * See docs/specs/required-headers.md sections 5 and 7.2.
 */
class CSVRequiredHeadersTest {

    private static CSVFormat.Builder readHeader() {
        return CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true);
    }

    @Test
    void testBlankRequiredHeaderRejectedAtBuild() {
        assertThrows(IllegalArgumentException.class, () -> readHeader().setRequiredHeaders("id", " ").get());
    }

    @Test
    void testDefaultIsDisabled() {
        assertArrayEquals(new String[0], CSVFormat.DEFAULT.getRequiredHeaders());
        assertArrayEquals(new String[0], readHeader().setRequiredHeaders((String[]) null).get().getRequiredHeaders());
    }

    @Test
    void testDuplicateRequiredHeaderRejectedAtBuild() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> readHeader().setRequiredHeaders("id", "id").get());
        assertEquals("Required headers contain a duplicate name: \"id\" in [id, id]", e.getMessage());
    }

    @Test
    void testEmptyInputMissesAllRequiredHeaders() {
        final CSVFormat format = readHeader().setRequiredHeaders("id", "amount").get();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CSVParser.parse(new StringReader(""), format));
        assertEquals("Missing required headers [id, amount]; actual header: []", e.getMessage());
    }

    @Test
    void testEqualsAndHashCodeIncludeRequiredHeaders() {
        final CSVFormat a = readHeader().setRequiredHeaders("id").get();
        final CSVFormat b = readHeader().setRequiredHeaders("id", "amount").get();
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
        assertEquals(a, readHeader().setRequiredHeaders("id").get());
    }

    @Test
    void testExplicitHeaderIsChecked() {
        final CSVFormat format = CSVFormat.DEFAULT.builder().setHeader("id", "name").setRequiredHeaders("id", "amount").get();
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CSVParser.parse(new StringReader("1,a\n"), format));
        assertEquals("Missing required headers [amount]; actual header: [id, name]", e.getMessage());
    }

    @Test
    void testGetterReturnsCopy() {
        final String[] declared = { "id" };
        final CSVFormat format = readHeader().setRequiredHeaders(declared).get();
        declared[0] = "changed";
        format.getRequiredHeaders()[0] = "changed";
        assertArrayEquals(new String[] { "id" }, format.getRequiredHeaders());
    }

    @Test
    void testIgnoreHeaderCaseMatchesCaseInsensitively() throws IOException {
        final CSVFormat format = readHeader().setIgnoreHeaderCase(true).setRequiredHeaders("order_id").get();
        try (CSVParser parser = CSVParser.parse(new StringReader("Order_ID\nA-1\n"), format)) {
            assertEquals("A-1", parser.nextRecord().get("order_id"));
        }
    }

    @Test
    void testNullRequiredHeaderRejectedAtBuild() {
        assertThrows(IllegalArgumentException.class, () -> readHeader().setRequiredHeaders("id", null).get());
    }

    @Test
    void testRequiredHeadersWithoutHeaderRejectedAtBuild() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> CSVFormat.DEFAULT.builder().setRequiredHeaders("id").get());
        assertTrue(e.getMessage().startsWith("Required headers [id] need a header"), e.getMessage());
    }

    @Test
    void testToStringShowsRequiredHeaders() {
        assertTrue(readHeader().setRequiredHeaders("id").get().toString().contains("RequiredHeaders:[id]"));
    }
}
