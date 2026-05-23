package com.yangdesigner.yangmodeldesigner.validation;

import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.parser.YangParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangValidatorTest {
    private final YangParser parser = new YangParser();
    private final YangValidator validator = new YangValidator();

    @Test
    void acceptsMinimalYang11Module() {
        String source = """
                module valid-module {
                  yang-version 1.1;
                  namespace "urn:valid";
                  prefix valid;

                  leaf enabled {
                    type boolean;
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);

        assertTrue(validator.validate(result.document()).isEmpty());
    }

    @Test
    void reportsRequiredModuleFieldsAndLeafType() {
        String source = """
                module invalid-module {
                  leaf enabled {
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        List<ValidationIssue> issues = validator.validate(result.document());

        assertEquals(4, issues.size());
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("yang-version")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("namespace")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("prefix")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("не указан type")));
    }

    @Test
    void reportsUnsupportedYangVersion() {
        String source = """
                module old-module {
                  yang-version 1;
                  namespace "urn:old";
                  prefix old;
                }
                """;

        YangParseResult result = parser.parse(source, null);
        List<ValidationIssue> issues = validator.validate(result.document());

        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().message().contains("YANG 1.1"));
        assertTrue(issues.getFirst().message().contains("Строка 1"));
    }

    @Test
    void reportsListWithoutKeyAndKeyWithoutLeaf() {
        String source = """
                module keyed-module {
                  yang-version 1.1;
                  namespace "urn:keyed";
                  prefix keyed;

                  list missing-key {
                    leaf name {
                      type string;
                    }
                  }

                  list broken-key {
                    key id;
                    leaf name {
                      type string;
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        List<ValidationIssue> issues = validator.validate(result.document());

        assertEquals(2, issues.size());
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("нужно указать key")));
        assertTrue(issues.stream().anyMatch(issue -> issue.message().contains("должен ссылаться на дочерний leaf")));
    }

    @Test
    void reportsDuplicateChildNamesAcrossNodeTypes() {
        String source = """
                module duplicate-module {
                  yang-version 1.1;
                  namespace "urn:duplicate";
                  prefix dup;

                  container system {
                  }

                  leaf system {
                    type string;
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        List<ValidationIssue> issues = validator.validate(result.document());

        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().message().contains("Повторяющийся дочерний узел"));
    }

    @Test
    void reportsLeafListDefaultWithMinElements() {
        String source = """
                module leaf-list-module {
                  yang-version 1.1;
                  namespace "urn:leaf-list";
                  prefix ll;

                  leaf-list server {
                    type string;
                    min-elements 1;
                    default "localhost";
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        List<ValidationIssue> issues = validator.validate(result.document());

        assertEquals(1, issues.size());
        assertTrue(issues.getFirst().message().contains("leaf-list"));
    }
}
