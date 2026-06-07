package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class YangXmlSampleGeneratorTest {
    private final YangXmlSampleGenerator generator = new YangXmlSampleGenerator();

    @Test
    void generatesMinimalXmlSampleFromDataNodes() {
        YangNode module = new YangNode(YangNodeType.MODULE, "sample-module");
        module.addConstraint("namespace", "urn:sample");
        YangNode container = new YangNode(YangNodeType.CONTAINER, "system");
        YangNode enabled = new YangNode(YangNodeType.LEAF, "enabled");
        enabled.setDataType("boolean");
        YangNode hostname = new YangNode(YangNodeType.LEAF, "hostname");
        hostname.setDataType("string");
        hostname.addConstraint("default", "router-1");
        container.addChild(enabled);
        container.addChild(hostname);
        module.addChild(container);

        String xml = generator.generate(new YangDocument(module, "", null));

        assertTrue(xml.contains("<data xmlns=\"urn:sample\">"));
        assertTrue(xml.contains("    <system>"));
        assertTrue(xml.contains("        <enabled>true</enabled>"));
        assertTrue(xml.contains("        <hostname>router-1</hostname>"));
    }

    @Test
    void expandsUsesFromGroupingInXmlSample() {
        YangNode module = new YangNode(YangNodeType.MODULE, "sample-module");
        YangNode grouping = new YangNode(YangNodeType.GROUPING, "common-settings");
        YangNode mtu = new YangNode(YangNodeType.LEAF, "mtu");
        mtu.setDataType("uint16");
        grouping.addChild(mtu);
        YangNode container = new YangNode(YangNodeType.CONTAINER, "interface");
        container.addChild(new YangNode(YangNodeType.USES, "common-settings"));
        module.addChild(grouping);
        module.addChild(container);

        String xml = generator.generate(new YangDocument(module, "", null));

        assertTrue(xml.contains("    <interface>"));
        assertTrue(xml.contains("        <mtu>1</mtu>"));
    }

    @Test
    void expandsPrefixedUsesFromImportedDocument() {
        YangNode module = new YangNode(YangNodeType.MODULE, "main-module");
        YangNode importNode = new YangNode(YangNodeType.IMPORT, "common-module");
        importNode.addConstraint("prefix", "cmn");
        YangNode container = new YangNode(YangNodeType.CONTAINER, "system");
        container.addChild(new YangNode(YangNodeType.USES, "cmn:common-settings"));
        module.addChild(importNode);
        module.addChild(container);

        YangNode importedModule = new YangNode(YangNodeType.MODULE, "common-module");
        YangNode grouping = new YangNode(YangNodeType.GROUPING, "common-settings");
        YangNode hostname = new YangNode(YangNodeType.LEAF, "hostname");
        hostname.setDataType("string");
        grouping.addChild(hostname);
        importedModule.addChild(grouping);

        String xml = generator.generate(
                new YangDocument(module, "", null),
                List.of(new YangDocument(importedModule, "", null))
        );

        assertTrue(xml.contains("    <system>"));
        assertTrue(xml.contains("        <hostname>sample</hostname>"));
    }
}
