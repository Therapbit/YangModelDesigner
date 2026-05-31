package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.service.YangXmlSampleGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangDocumentControllerTest {
    private final YangDocumentController controller = new YangDocumentController(
            new YangDocumentService(),
            new YangXmlSampleGenerator()
    );

    @TempDir
    Path tempDir;

    @Test
    void convertsTxtSaveTargetToYang() throws Exception {
        Path saved = controller.save(tempDir.resolve("model.txt"), "module model {}");

        assertEquals("model.yang", saved.getFileName().toString());
        assertEquals("module model {}", Files.readString(saved));
    }

    @Test
    void appendsYangExtensionWhenMissing() {
        Path target = controller.ensureYangExtension(tempDir.resolve("model"));

        assertEquals("model.yang", target.getFileName().toString());
    }

    @Test
    void computesDefaultFileNames() {
        assertEquals("model.yang", controller.defaultYangFileName(null));
        assertEquals("router.yang", controller.defaultYangFileName(Path.of("router.txt")));
        assertEquals("sample.xml", controller.defaultXmlFileName(null));
        assertEquals("router.xml", controller.defaultXmlFileName(Path.of("router.yang")));
    }

    @Test
    void exportsXmlWithXmlExtension() throws Exception {
        YangNode root = new YangNode(YangNodeType.MODULE, "sample-module");
        YangNode leaf = new YangNode(YangNodeType.LEAF, "enabled");
        leaf.setDataType("boolean");
        root.addChild(leaf);

        Path saved = controller.exportXml(tempDir.resolve("sample.txt"), new YangDocument(root, "", null));

        assertEquals("sample.xml", saved.getFileName().toString());
        assertTrue(Files.readString(saved).contains("<enabled>true</enabled>"));
    }
}
