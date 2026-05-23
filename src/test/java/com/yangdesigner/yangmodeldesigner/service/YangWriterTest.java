package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.parser.YangParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangWriterTest {
    private final YangWriter writer = new YangWriter();
    private final YangParser parser = new YangParser();

    @Test
    void writesEditableModelToYangText() {
        YangNode module = new YangNode(YangNodeType.MODULE, "generated-module");
        module.addConstraint("yang-version", "1.1");
        module.addConstraint("namespace", "urn:generated");
        module.addConstraint("prefix", "gen");

        YangNode container = new YangNode(YangNodeType.CONTAINER, "system");
        container.setDescription("System settings.");
        module.addChild(container);

        YangNode leaf = new YangNode(YangNodeType.LEAF, "hostname");
        leaf.setDataType("string");
        leaf.addConstraint("mandatory", "true");
        container.addChild(leaf);

        String text = writer.write(new YangDocument(module, "", null));

        assertTrue(text.contains("module generated-module {"));
        assertTrue(text.contains("yang-version 1.1;"));
        assertTrue(text.contains("namespace \"urn:generated\";"));
        assertTrue(text.contains("description \"System settings.\";"));
        assertTrue(text.contains("mandatory true;"));
    }

    @Test
    void generatedTextCanBeParsedAgain() {
        YangNode module = new YangNode(YangNodeType.MODULE, "roundtrip");
        module.addConstraint("yang-version", "1.1");
        module.addConstraint("namespace", "urn:roundtrip");
        module.addConstraint("prefix", "rt");
        YangNode leaf = new YangNode(YangNodeType.LEAF, "enabled");
        leaf.setDataType("boolean");
        module.addChild(leaf);

        String text = writer.write(new YangDocument(module, "", null));
        YangParseResult result = parser.parse(text, null);

        assertTrue(result.errors().isEmpty());
        assertEquals("roundtrip", result.document().root().name());
        assertEquals("enabled", result.document().root().children().getFirst().name());
    }

    @Test
    void writesAndParsesAdditionalYang11Statements() {
        YangNode module = new YangNode(YangNodeType.MODULE, "advanced-generated");
        module.addConstraint("yang-version", "1.1");
        module.addConstraint("namespace", "urn:advanced-generated");
        module.addConstraint("prefix", "ag");

        module.addChild(new YangNode(YangNodeType.FEATURE, "telemetry"));
        YangNode identity = new YangNode(YangNodeType.IDENTITY, "derived-state");
        identity.addConstraint("base", "base-state");
        module.addChild(identity);

        YangNode typedef = new YangNode(YangNodeType.TYPEDEF, "admin-state");
        typedef.setDataType("string");
        module.addChild(typedef);

        YangNode rpc = new YangNode(YangNodeType.RPC, "reset");
        YangNode input = new YangNode(YangNodeType.INPUT, "");
        YangNode force = new YangNode(YangNodeType.LEAF, "force");
        force.setDataType("boolean");
        input.addChild(force);
        rpc.addChild(input);
        module.addChild(rpc);

        String text = writer.write(new YangDocument(module, "", null));
        YangParseResult result = parser.parse(text, null);

        assertTrue(result.errors().isEmpty());
        assertTrue(text.contains("feature telemetry {"));
        assertTrue(text.contains("identity derived-state {"));
        assertTrue(text.contains("base base-state;"));
        assertTrue(text.contains("typedef admin-state {"));
        assertEquals(YangNodeType.RPC, result.document().root().children().get(3).type());
    }

    @Test
    void preservesUnknownStatementsWhenKnownNodeChanges() {
        String source = """
                module keep-unknown {
                  yang-version 1.1;
                  namespace "urn:keep";
                  prefix keep;

                  container system {
                    custom:display "Visible in vendor UI";

                    leaf hostname {
                      type string;
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        YangNode container = result.document().root().children().getFirst();
        YangNode leaf = container.children().getFirst();

        leaf.setDescription("Device hostname.");
        String text = writer.write(result.document());

        assertTrue(text.contains("custom:display \"Visible in vendor UI\";"));
        assertTrue(text.contains("description \"Device hostname.\";"));
    }

    @Test
    void insertsNewNodeSubstatementsInPreferredOrder() {
        String source = """
                module ordered {
                  yang-version 1.1;
                  namespace "urn:ordered";
                  prefix ord;
                }
                """;

        YangParseResult result = parser.parse(source, null);
        YangNode leaf = new YangNode(YangNodeType.LEAF, "enabled");
        leaf.setDataType("boolean");
        leaf.setDescription("Administrative state.");
        leaf.addConstraint("must", "../name");
        leaf.addConstraint("when", "../present");
        leaf.addConstraint("config", "true");
        result.document().root().addChild(leaf);

        String text = writer.write(result.document());

        assertTrue(text.indexOf("config true;") < text.indexOf("type boolean;"));
        assertTrue(text.indexOf("type boolean;") < text.indexOf("description \"Administrative state.\";"));
        assertTrue(text.indexOf("description \"Administrative state.\";") < text.indexOf("when \"../present\";"));
        assertTrue(text.indexOf("when \"../present\";") < text.indexOf("must \"../name\";"));
    }

    @Test
    void separatesAstNodesWithSingleBlankLine() {
        String source = """
                module spacing {
                  yang-version 1.1;
                  namespace "urn:spacing";
                  prefix sp;
                  container system {
                    leaf hostname {
                      type string;
                    }
                    leaf domain {
                      type string;
                    }
                  }
                }
                """;

        YangParseResult result = parser.parse(source, null);
        String text = writer.write(result.document());
        String lineSeparator = System.lineSeparator();

        assertTrue(text.contains("prefix sp;" + lineSeparator + lineSeparator + "  container system {"));
        assertTrue(text.contains("    }" + lineSeparator + lineSeparator + "    leaf domain {"));
    }
}
