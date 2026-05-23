package com.yangdesigner.yangmodeldesigner.parser;

import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangParserTest {
    private final YangParser parser = new YangParser();

    @Test
    void parsesModuleTreeAndLeafMetadata() {
        String source = """
                module example {
                  yang-version 1.1;
                  namespace "urn:example";
                  prefix ex;

                  container system {
                    description "System settings.";
                    leaf hostname {
                      type string;
                      mandatory true;
                      description "Device hostname.";
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);

        assertFalse(result.hasErrors());
        YangNode module = result.document().root();
        YangNode container = module.children().getFirst();
        YangNode leaf = container.children().getFirst();

        assertEquals(YangNodeType.MODULE, module.type());
        assertEquals("example", module.name());
        assertEquals(1, module.line());
        assertEquals(YangNodeType.CONTAINER, container.type());
        assertEquals("System settings.", container.description());
        assertEquals(YangNodeType.LEAF, leaf.type());
        assertEquals("hostname", leaf.name());
        assertEquals("string", leaf.dataType());
        assertEquals("Device hostname.", leaf.description());
        assertEquals("true", leaf.constraints().get("mandatory").getFirst());
    }

    @Test
    void parsesListKeyAsConstraint() {
        String source = """
                module example {
                  yang-version 1.1;
                  namespace "urn:example";
                  prefix ex;

                  list interface {
                    key name;
                    leaf name {
                      type string;
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);

        YangNode list = result.document().root().children().getFirst();
        assertEquals(YangNodeType.LIST, list.type());
        assertEquals("name", list.constraints().get("key").getFirst());
    }

    @Test
    void parsesAdditionalYang11Statements() {
        String source = """
                module advanced {
                  yang-version 1.1;
                  namespace "urn:advanced";
                  prefix adv;

                  feature telemetry;

                  identity base-state;

                  typedef admin-state {
                    type string;
                  }

                  rpc reset {
                    input {
                      leaf force {
                        type boolean;
                      }
                    }
                    output {
                      leaf accepted {
                        type boolean;
                      }
                    }
                  }

                  augment "/system" {
                    leaf location {
                      type string;
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);

        assertTrue(result.errors().isEmpty());
        assertEquals(YangNodeType.FEATURE, result.document().root().children().get(0).type());
        assertEquals(YangNodeType.IDENTITY, result.document().root().children().get(1).type());
        assertEquals(YangNodeType.TYPEDEF, result.document().root().children().get(2).type());
        assertEquals(YangNodeType.RPC, result.document().root().children().get(3).type());
        assertEquals(YangNodeType.INPUT, result.document().root().children().get(3).children().get(0).type());
        assertEquals(YangNodeType.OUTPUT, result.document().root().children().get(3).children().get(1).type());
        assertEquals(YangNodeType.AUGMENT, result.document().root().children().get(4).type());
    }
}
