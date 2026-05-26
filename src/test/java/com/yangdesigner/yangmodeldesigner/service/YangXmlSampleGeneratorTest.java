package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import org.junit.jupiter.api.Test;

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
        assertTrue(xml.contains("<system>"));
        assertTrue(xml.contains("<enabled>true</enabled>"));
        assertTrue(xml.contains("<hostname>router-1</hostname>"));
    }
}
