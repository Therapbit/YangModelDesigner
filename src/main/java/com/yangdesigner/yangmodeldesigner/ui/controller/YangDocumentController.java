package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.service.YangXmlSampleGenerator;

import java.io.IOException;
import java.nio.file.Path;

public final class YangDocumentController {
    private final YangDocumentService documentService;
    private final YangXmlSampleGenerator xmlSampleGenerator;

    public YangDocumentController(YangDocumentService documentService, YangXmlSampleGenerator xmlSampleGenerator) {
        this.documentService = documentService;
        this.xmlSampleGenerator = xmlSampleGenerator;
    }

    public String read(Path file) throws IOException {
        return documentService.read(file);
    }

    public Path save(Path file, String text) throws IOException {
        Path target = ensureYangExtension(file);
        documentService.write(target, text);
        return target;
    }

    public Path exportYang(Path file, String text) throws IOException {
        return save(file, text);
    }

    public Path exportXml(Path file, YangDocument document) throws IOException {
        Path target = ensureXmlExtension(file);
        documentService.write(target, xmlSampleGenerator.generate(document));
        return target;
    }

    public Path ensureYangExtension(Path file) {
        String fileName = file.getFileName().toString();
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".yang")) {
            return file;
        }
        if (lower.endsWith(".txt")) {
            return file.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".yang");
        }
        return file.resolveSibling(fileName + ".yang");
    }

    public Path ensureXmlExtension(Path file) {
        String fileName = file.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".xml")) {
            return file;
        }
        int extension = fileName.lastIndexOf('.');
        if (extension > 0) {
            return file.resolveSibling(fileName.substring(0, extension) + ".xml");
        }
        return file.resolveSibling(fileName + ".xml");
    }

    public String defaultYangFileName(Path currentFile) {
        if (currentFile != null) {
            return ensureYangExtension(currentFile).getFileName().toString();
        }
        return "model.yang";
    }

    public String defaultXmlFileName(Path currentFile) {
        if (currentFile == null) {
            return "sample.xml";
        }
        String fileName = currentFile.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return (extension > 0 ? fileName.substring(0, extension) : fileName) + ".xml";
    }
}
